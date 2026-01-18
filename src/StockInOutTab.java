import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// LOGISTICS MANAGER
// This class handles moving inventory between Headquarters (HQ) and Outlets.
// It updates the 'Model' objects and saves the changes to 'model.csv'.
public class StockInOutTab {
    
    // --- SHARED DATA ---
    private Map<String, Model> models;      // The live database of products
    private Map<String, String> outlets;    // List of stores (e.g., C60 -> KL)
    private DataLoader dataLoader;          // To save changes to file
    private employee loggedInUser;          // To record WHO made the transfer
    private Component parentComponent;      // For centering popup windows
    
    // CALLBACK: This is a "remote control" that lets this tab tell the 
    // "Stock Count" tab to refresh its table after we change the numbers here.
    private Runnable onStockUpdate; 

    // UI LIST: Where we store the list of visual rows (Model + Qty inputs)
    private JPanel stockItemsPanel;
    private List<StockTransferRow> stockTransferRows;

    // CONSTRUCTOR
    public StockInOutTab(Map<String, Model> models, Map<String, String> outlets, DataLoader dataLoader,
            employee loggedInUser, Component parentComponent, Runnable onStockUpdate) {
        this.models = models;
        this.outlets = outlets;
        this.dataLoader = dataLoader;
        this.loggedInUser = loggedInUser;
        this.parentComponent = parentComponent;
        this.onStockUpdate = onStockUpdate;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // --- 1. TOP PANEL: TRANSFER SETTINGS ---
        // GridLayout(3, 2) = 3 Rows, 2 Columns. 
        // Ideal for [Label] [Input] pairs.
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Stock In", "Stock Out" });

        // PREPARE OUTLET DROPDOWN LIST
        // We need to mix "HQ" (which isn't in the CSV) with the actual outlets.
        List<String> outletList = new ArrayList<>();
        outletList.add("HQ (Service Center)"); // Manually add HQ as the first option
        
        // Loop through the map of outlets to add them to the list
        for (Map.Entry<String, String> start : outlets.entrySet()) {
            // Format: "C60 (Kuala Lumpur)" so it's easy for humans to read
            outletList.add(start.getKey() + " (" + start.getValue() + ")");
        }

        // Convert the List to an Array for the JComboBox
        JComboBox<String> fromBox = new JComboBox<>(outletList.toArray(new String[0]));
        JComboBox<String> toBox = new JComboBox<>(outletList.toArray(new String[0]));

        // Add components to the grid
        topPanel.add(new JLabel("Transaction Type:"));
        topPanel.add(typeBox);
        topPanel.add(new JLabel("From:"));
        topPanel.add(fromBox);
        topPanel.add(new JLabel("To:"));
        topPanel.add(toBox);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- 2. CENTER PANEL: ITEMS LIST ---
        stockItemsPanel = new JPanel();
        // BoxLayout.Y_AXIS stacks items vertically like a tower
        stockItemsPanel.setLayout(new BoxLayout(stockItemsPanel, BoxLayout.Y_AXIS));
        stockTransferRows = new ArrayList<>();

        // Add the first row immediately so the screen isn't empty
        addStockTransferRow();

        // Wrap in ScrollPane so if they add 50 items, they can scroll down
        panel.add(new JScrollPane(stockItemsPanel), BorderLayout.CENTER);

        // --- 3. BOTTOM PANEL: BUTTONS ---
        JPanel btmPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Stock");
        JButton doneBtn = new JButton("Done");

        // LISTENER: Add another row visual element
        addBtn.addActionListener(e -> addStockTransferRow());

        // --- CORE TRANSFER LOGIC ---
        // This runs when "DONE" is clicked
        doneBtn.addActionListener(e -> {
            String type = (String) typeBox.getSelectedItem();
            String fromFull = (String) fromBox.getSelectedItem();
            String toFull = (String) toBox.getSelectedItem();

            // PARSING LOGIC:
            // The dropdown says "C60 (Kuala Lumpur)", but the database only knows "C60".
            // split(" ")[0] takes the first word before the space.
            // If it starts with "HQ", we hardcode it to "HQ".
            String fromCode = fromFull.startsWith("HQ") ? "HQ" : fromFull.split(" ")[0];
            String toCode = toFull.startsWith("HQ") ? "HQ" : toFull.split(" ")[0];

            // VALIDATION: Sanity Check
            if (fromCode.equals(toCode)) {
                JOptionPane.showMessageDialog(parentComponent, "From and To cannot be the same!");
                return;
            }

            StringBuilder receiptModels = new StringBuilder();
            int totalQty = 0;

            // VALIDATION: Stock Check Loop
            // Before we change ANY numbers, we must make sure ALL items are valid.
            for (StockTransferRow row : stockTransferRows) {
                String model = row.getSelectedModel();
                int qty = row.getQuantity();

                if (qty <= 0) continue; // Skip rows where user typed 0 or nothing

                // Logic: If moving items OUT of a store, check if that store has enough.
                // We assume HQ has infinite stock (common in basic inventory assignments).
                if (!fromCode.equals("HQ")) {
                    int avail = models.get(model).getStock(fromCode);
                    if (avail < qty) {
                        JOptionPane.showMessageDialog(parentComponent,
                                "Insufficient stock for " + model + " at " + fromCode);
                        return; // Stop the whole transaction!
                    }
                }
            }

            // --- COMMIT TRANSFER ---
            // If we get here, all checks passed. Now we actually change the numbers.
            for (StockTransferRow row : stockTransferRows) {
                String modelStr = row.getSelectedModel();
                int qty = row.getQuantity();
                if (qty <= 0) continue;

                Model m = models.get(modelStr);
                
                // MATH: Subtract from Source
                if (!fromCode.equals("HQ"))
                    m.reduceStock(fromCode, qty);
                
                // MATH: Add to Destination
                if (!toCode.equals("HQ"))
                    m.addStock(toCode, qty);

                // Add line to receipt text
                receiptModels.append("    - ").append(modelStr).append(" (Quantity: ").append(qty).append(")\n");
                totalQty += qty;
            }

            // If user clicked Done but didn't enter any quantities
            if (totalQty == 0) {
                JOptionPane.showMessageDialog(parentComponent, "No valid items entered.");
                return;
            }

            // SAVE: Write updated numbers to model.csv
            dataLoader.saveModels(models, new ArrayList<>(outlets.keySet()));
            
            // REFRESH: Tell other parts of the app to reload data
            if (onStockUpdate != null)
                onStockUpdate.run();

            // --- GENERATE RECEIPT TEXT ---
            // We build a string that looks like a formal document
            StringBuilder rc = new StringBuilder();
            rc.append("=== ").append(type).append(" ===\n");
            rc.append("Date: ").append(java.time.LocalDate.now()).append("\n");
            rc.append("Time: ").append(java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))).append("\n");
            rc.append("From: ").append(fromFull).append("\n");
            rc.append("To: ").append(toFull).append("\n");
            rc.append("Models Received:\n\n");
            rc.append(receiptModels);
            rc.append("\nTotal Quantity: ").append(totalQty).append("\n");
            rc.append("Name of Employee in Charge: ").append(loggedInUser != null ? loggedInUser.getName() : "Unknown")
                    .append("\n");
            rc.append("\nModel quantities updated successfully.\n");
            rc.append(type).append(" recorded.");

            // Save receipt to "StockReceipt" folder
            dataLoader.appendReceipt(rc.toString());

            JOptionPane.showMessageDialog(parentComponent, "Transfer Successful!\nReceipt generated.");

            // RESET FORM: Clear everything for the next user
            stockItemsPanel.removeAll();
            stockTransferRows.clear();
            addStockTransferRow(); // Add one fresh empty row
            stockItemsPanel.revalidate();
            stockItemsPanel.repaint();
        });

        btmPanel.add(addBtn);
        btmPanel.add(doneBtn);
        panel.add(btmPanel, BorderLayout.SOUTH);

        return panel;
    }

    // HELPER: Adds a visual row
    private void addStockTransferRow() {
        // Create the inner class object
        StockTransferRow row = new StockTransferRow(models.keySet().toArray(new String[0]));
        // Add to our list logic
        stockTransferRows.add(row);
        // Add to visual panel
        stockItemsPanel.add(row.getPanel());
        // Refresh UI
        stockItemsPanel.revalidate();
        stockItemsPanel.repaint();
    }

    // INNER CLASS: One row in the transfer list
    // This encapsulates the Model Dropdown and Quantity Field into a single object
    class StockTransferRow {
        private JPanel panel;
        private JComboBox<String> modelBox;
        private JTextField qtyField;

        public StockTransferRow(String[] modelNames) {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            modelBox = new JComboBox<>(modelNames);
            qtyField = new JTextField(5); // Width of 5 columns

            panel.add(new JLabel("Model:"));
            panel.add(modelBox);
            panel.add(new JLabel("Qty:"));
            panel.add(qtyField);
            
            // Thin grey border around each row to separate them visually
            panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        public JPanel getPanel() {
            return panel;
        }

        public String getSelectedModel() {
            return (String) modelBox.getSelectedItem();
        }

        // Helper to safely get the integer. Returns 0 if user types text.
        public int getQuantity() {
            try {
                return Integer.parseInt(qtyField.getText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}