import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesPanel {
    // SHARED DATA
    private Map<String, Model> models;
    private Map<String, String> outlets;
    private DataLoader dataLoader;
    private employee loggedInUser;
    private Component parentComponent;
    
    // CALLBACK: A function we call after a sale to tell other tabs "Hey, stock changed!"
    private Runnable onStockUpdate; 

    // UI Components for the "Shopping Cart"
    private JPanel salesItemsPanel;
    private List<SalesRow> salesRows;

    // CONSTRUCTOR
    public SalesPanel(Map<String, Model> models, Map<String, String> outlets, DataLoader dataLoader,
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

        // --- 1. TOP PANEL: Customer Info ---
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField custNameField = new JTextField();
        JComboBox<String> paymentBox = new JComboBox<>(
                new String[] { "Cash", "Credit Card", "Debit Card", "E-Wallet" });

        topPanel.add(new JLabel("Customer Name:"));
        topPanel.add(custNameField);
        topPanel.add(new JLabel("Payment Method:"));
        topPanel.add(paymentBox);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- 2. CENTER PANEL: The "Shopping Cart" List ---
        // We use BoxLayout.Y_AXIS to stack rows vertically (like a receipt list)
        salesItemsPanel = new JPanel();
        salesItemsPanel.setLayout(new BoxLayout(salesItemsPanel, BoxLayout.Y_AXIS));
        salesRows = new ArrayList<>();

        // Add the first empty row by default
        addSalesRow();

        panel.add(new JScrollPane(salesItemsPanel), BorderLayout.CENTER);

        // --- 3. BOTTOM PANEL: Action Buttons ---
        JPanel btmPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Item");
        JButton confirmBtn = new JButton("Confirm Sale");

        // Logic: Add another row to the cart
        addBtn.addActionListener(e -> addSalesRow());

        // --- CHECKOUT LOGIC ---
        confirmBtn.addActionListener(e -> {
            String customer = custNameField.getText();
            String method = (String) paymentBox.getSelectedItem();

            // Validation 1: Customer Name is required
            if (customer.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "Please enter customer name.");
                return;
            }

            // Determine which outlet this sale belongs to (Default to HQ 'C60' if unknown)
            String outletCode = (loggedInUser != null) ? loggedInUser.getOutlet() : "C60";
            
            double grantTotal = 0;
            StringBuilder receiptItems = new StringBuilder();
            List<Transaction> pendingTxns = new ArrayList<>();

            // Loop through every row in the cart
            for (SalesRow row : salesRows) {
                String modelStr = row.getModelName();
                int qty = row.getQuantity();

                // Skip empty or invalid rows
                if (modelStr.isEmpty() || qty <= 0)
                    continue;

                // Validation 2: Does model exist?
                if (!models.containsKey(modelStr)) {
                    JOptionPane.showMessageDialog(parentComponent, "Model " + modelStr + " not found!");
                    return;
                }

                // Validation 3: Is there enough stock?
                Model model = models.get(modelStr);
                if (model.getStock(outletCode) < qty) {
                    JOptionPane.showMessageDialog(parentComponent, "Insufficient stock for " + modelStr);
                    return;
                }

                // Calculate Totals
                double price = model.getPrice();
                double rowTotal = price * qty;
                grantTotal += rowTotal;

                // Add to Receipt Text
                receiptItems.append("Model: ").append(modelStr).append("\n");
                receiptItems.append("Quantity: ").append(qty).append("\n");
                receiptItems.append("Unit Price: RM").append(price).append("\n");
                receiptItems.append("-----------------------------\n");

                // Create Transaction Object (Ready to be processed)
                Transaction t = new Transaction(loggedInUser.getID(), outletCode, modelStr, qty, rowTotal, customer);
                pendingTxns.add(t);
            }

            if (pendingTxns.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "No valid items to sell.");
                return;
            }

            // --- COMMIT THE SALE ---
            
            // 1. Update Memory (Reduce Stock)
            for (Transaction t : pendingTxns) {
                models.get(t.getModelName()).reduceStock(outletCode, t.getQuantity());
            }

            // 2. Save to CSV
            dataLoader.saveModels(models, new ArrayList<>(outlets.keySet()));
            
            // 3. Refresh UI (Tell StockCountTab to update numbers)
            if (onStockUpdate != null)
                onStockUpdate.run();

            // 4. GENERATE RECEIPT TEXT FILE
            StringBuilder rc = new StringBuilder();
            rc.append("=== Record New Sale ===\n");
            rc.append("Date: ").append(java.time.LocalDate.now()).append("\n");
            rc.append("Time: ").append(java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))).append("\n");
            
            // Critical: Save Employee ID so "PerformancePanel" can give credit later
            rc.append("Employee: ").append(loggedInUser.getID()).append("\n"); 
            
            rc.append("Customer Name: ").append(customer).append("\n");
            rc.append("Item(s) Purchased:\n");
            rc.append(receiptItems);
            rc.append("Enter transaction method: ").append(method).append("\n");
            rc.append("Subtotal: RM").append(String.format("%.2f", grantTotal)).append("\n\n");
            rc.append("Transaction successful.\n");
            rc.append("Sale recorded successfully.\n");
            rc.append("Model quantities updated successfully.\n");
            rc.append("Receipt generated: sales_").append(java.time.LocalDate.now()).append(".txt");

            // Save receipt to file system
            dataLoader.appendSalesReceipt(rc.toString());

            // 5. Success Message
            JOptionPane.showMessageDialog(parentComponent, "Sale Recorded Successfully!\nTotal: RM" + grantTotal);

            // 6. Reset Form for next customer
            custNameField.setText("");
            salesItemsPanel.removeAll();
            salesRows.clear();
            addSalesRow();
            salesItemsPanel.revalidate();
            salesItemsPanel.repaint();
        });

        btmPanel.add(addBtn);
        btmPanel.add(confirmBtn);
        panel.add(btmPanel, BorderLayout.SOUTH);

        return panel;
    }

    // Helper: Adds a new visual row for entering an item
    private void addSalesRow() {
        SalesRow row = new SalesRow();
        salesRows.add(row);
        salesItemsPanel.add(row.getPanel());
        salesItemsPanel.revalidate(); // Refresh layout to show new row
        salesItemsPanel.repaint();
    }

    // INNER CLASS: Represents one row in the shopping cart (Model Name + Quantity)
    class SalesRow {
        private JPanel panel;
        private JTextField modelField;
        private JTextField qtyField;

        public SalesRow() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            modelField = new JTextField(10);
            qtyField = new JTextField(3);

            panel.add(new JLabel("Model:"));
            panel.add(modelField);
            panel.add(new JLabel("Qty:"));
            panel.add(qtyField);
            panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        public JPanel getPanel() {
            return panel;
        }

        public String getModelName() {
            return modelField.getText();
        }

        public int getQuantity() {
            try {
                return Integer.parseInt(qtyField.getText());
            } catch (Exception e) {
                return 0; // Return 0 if user types "abc"
            }
        }
    }
}