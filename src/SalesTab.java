import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesTab {
    private Map<String, Model> models;
    private Map<String, String> outlets;
    private DataLoader dataLoader;
    private employee loggedInUser;
    private Component parentComponent;
    private Runnable onStockUpdate; // Callback

    private JPanel salesItemsPanel;
    private List<SalesRow> salesRows;

    public SalesTab(Map<String, Model> models, Map<String, String> outlets, DataLoader dataLoader,
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

        // 1. Customer Details & Payment
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

        // 2. Items List
        salesItemsPanel = new JPanel();
        salesItemsPanel.setLayout(new BoxLayout(salesItemsPanel, BoxLayout.Y_AXIS));
        salesRows = new ArrayList<>();

        addSalesRow();

        panel.add(new JScrollPane(salesItemsPanel), BorderLayout.CENTER);

        // 3. Actions
        JPanel btmPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Item");
        JButton confirmBtn = new JButton("Confirm Sale");

        addBtn.addActionListener(e -> addSalesRow());

        confirmBtn.addActionListener(e -> {
            String customer = custNameField.getText();
            String method = (String) paymentBox.getSelectedItem();

            if (customer.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "Please enter customer name.");
                return;
            }

            String outletCode = (loggedInUser != null) ? loggedInUser.getOutlet() : "C60";
            double grantTotal = 0;
            StringBuilder receiptItems = new StringBuilder();
            List<Transaction> pendingTxns = new ArrayList<>();

            // Validate
            for (SalesRow row : salesRows) {
                String modelStr = row.getModelName();
                int qty = row.getQuantity();

                if (modelStr.isEmpty() || qty <= 0)
                    continue;

                if (!models.containsKey(modelStr)) {
                    JOptionPane.showMessageDialog(parentComponent, "Model " + modelStr + " not found!");
                    return;
                }

                Model model = models.get(modelStr);
                if (model.getStock(outletCode) < qty) {
                    JOptionPane.showMessageDialog(parentComponent, "Insufficient stock for " + modelStr);
                    return;
                }

                double price = model.getPrice();
                double rowTotal = price * qty;
                grantTotal += rowTotal;

                receiptItems.append("Model: ").append(modelStr).append("\n");
                receiptItems.append("Quantity: ").append(qty).append("\n");
                receiptItems.append("Unit Price: RM").append(price).append("\n");
                receiptItems.append("-----------------------------\n");

                // Transaction object creation with Employee ID
                Transaction t = new Transaction(loggedInUser.getID(), outletCode, modelStr, qty, rowTotal, customer);
                pendingTxns.add(t);
            }

            if (pendingTxns.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "No valid items to sell.");
                return;
            }

            // Commit stock changes
            for (Transaction t : pendingTxns) {
                models.get(t.getModelName()).reduceStock(outletCode, t.getQuantity());
            }

            // Save and Refresh
            dataLoader.saveModels(models, new ArrayList<>(outlets.keySet()));
            if (onStockUpdate != null)
                onStockUpdate.run();

            // --- RECEIPT GENERATION WITH EMPLOYEE ID ---
            StringBuilder rc = new StringBuilder();
            rc.append("=== Record New Sale ===\n");
            rc.append("Date: ").append(java.time.LocalDate.now()).append("\n");
            rc.append("Time: ").append(java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))).append("\n");
            
            // MODIFIED LOGIC: Save the ID (e.g., C6001) so the DataLoader can find it
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

            dataLoader.appendSalesReceipt(rc.toString());

            JOptionPane.showMessageDialog(parentComponent, "Sale Recorded Successfully!\nTotal: RM" + grantTotal);

            // Reset UI
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

    private void addSalesRow() {
        SalesRow row = new SalesRow();
        salesRows.add(row);
        salesItemsPanel.add(row.getPanel());
        salesItemsPanel.revalidate();
        salesItemsPanel.repaint();
    }

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
                return 0;
            }
        }
    }
}