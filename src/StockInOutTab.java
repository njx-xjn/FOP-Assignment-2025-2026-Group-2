
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StockInOutTab {
    private Map<String, Model> models;
    private Map<String, String> outlets;
    private DataLoader dataLoader;
    private employee loggedInUser;
    private Component parentComponent;
    private Runnable onStockUpdate; // Callback to refresh other UI

    private JPanel stockItemsPanel;
    private List<StockTransferRow> stockTransferRows;

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

        // Top: Settings
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Stock In", "Stock Out" });

        // Prepare Outlet List (Include HQ)
        List<String> outletList = new ArrayList<>();
        outletList.add("HQ (Service Center)");
        for (Map.Entry<String, String> start : outlets.entrySet()) {
            outletList.add(start.getKey() + " (" + start.getValue() + ")");
        }

        JComboBox<String> fromBox = new JComboBox<>(outletList.toArray(new String[0]));
        JComboBox<String> toBox = new JComboBox<>(outletList.toArray(new String[0]));

        topPanel.add(new JLabel("Transaction Type:"));
        topPanel.add(typeBox);
        topPanel.add(new JLabel("From:"));
        topPanel.add(fromBox);
        topPanel.add(new JLabel("To:"));
        topPanel.add(toBox);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center: Items
        stockItemsPanel = new JPanel();
        stockItemsPanel.setLayout(new BoxLayout(stockItemsPanel, BoxLayout.Y_AXIS));
        stockTransferRows = new ArrayList<>();

        // Add first row by default
        addStockTransferRow();

        panel.add(new JScrollPane(stockItemsPanel), BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel btmPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Stock");
        JButton doneBtn = new JButton("Done");

        addBtn.addActionListener(e -> addStockTransferRow());

        doneBtn.addActionListener(e -> {
            String type = (String) typeBox.getSelectedItem();
            String fromFull = (String) fromBox.getSelectedItem();
            String toFull = (String) toBox.getSelectedItem();

            String fromCode = fromFull.startsWith("HQ") ? "HQ" : fromFull.split(" ")[0];
            String toCode = toFull.startsWith("HQ") ? "HQ" : toFull.split(" ")[0];

            if (fromCode.equals(toCode)) {
                JOptionPane.showMessageDialog(parentComponent, "From and To cannot be the same!");
                return;
            }

            StringBuilder receiptModels = new StringBuilder();
            int totalQty = 0;

            // Validate
            for (StockTransferRow row : stockTransferRows) {
                String model = row.getSelectedModel();
                int qty = row.getQuantity();

                if (qty <= 0)
                    continue;

                if (!fromCode.equals("HQ")) {
                    int avail = models.get(model).getStock(fromCode);
                    if (avail < qty) {
                        JOptionPane.showMessageDialog(parentComponent,
                                "Insufficient stock for " + model + " at " + fromCode);
                        return;
                    }
                }
            }

            // Commit
            for (StockTransferRow row : stockTransferRows) {
                String modelStr = row.getSelectedModel();
                int qty = row.getQuantity();
                if (qty <= 0)
                    continue;

                Model m = models.get(modelStr);
                if (!fromCode.equals("HQ"))
                    m.reduceStock(fromCode, qty);
                if (!toCode.equals("HQ"))
                    m.addStock(toCode, qty);

                receiptModels.append("    - ").append(modelStr).append(" (Quantity: ").append(qty).append(")\n");
                totalQty += qty;
            }

            if (totalQty == 0) {
                JOptionPane.showMessageDialog(parentComponent, "No valid items entered.");
                return;
            }

            // Save and Refresh
            dataLoader.saveModels(models, new ArrayList<>(outlets.keySet()));
            if (onStockUpdate != null)
                onStockUpdate.run();

            // Receipt
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

            dataLoader.appendReceipt(rc.toString());

            JOptionPane.showMessageDialog(parentComponent, "Transfer Successful!\nReceipt generated.");

            // Reset
            stockItemsPanel.removeAll();
            stockTransferRows.clear();
            addStockTransferRow();
            stockItemsPanel.revalidate();
            stockItemsPanel.repaint();
        });

        btmPanel.add(addBtn);
        btmPanel.add(doneBtn);
        panel.add(btmPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addStockTransferRow() {
        StockTransferRow row = new StockTransferRow(models.keySet().toArray(new String[0]));
        stockTransferRows.add(row);
        stockItemsPanel.add(row.getPanel());
        stockItemsPanel.revalidate();
        stockItemsPanel.repaint();
    }

    class StockTransferRow {
        private JPanel panel;
        private JComboBox<String> modelBox;
        private JTextField qtyField;

        public StockTransferRow(String[] modelNames) {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            modelBox = new JComboBox<>(modelNames);
            qtyField = new JTextField(5);

            panel.add(new JLabel("Model:"));
            panel.add(modelBox);
            panel.add(new JLabel("Qty:"));
            panel.add(qtyField);
            panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        public JPanel getPanel() {
            return panel;
        }

        public String getSelectedModel() {
            return (String) modelBox.getSelectedItem();
        }

        public int getQuantity() {
            try {
                return Integer.parseInt(qtyField.getText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
