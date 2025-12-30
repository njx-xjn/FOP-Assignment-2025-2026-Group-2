
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class StockCountTab {
    private Map<String, Model> models;
    private employee loggedInUser;
    private DefaultTableModel stockCountModel;
    private JComboBox<String> sessionBox;
    private Component parentComponent;

    public StockCountTab(Map<String, Model> models, employee loggedInUser, Component parentComponent) {
        this.models = models;
        this.loggedInUser = loggedInUser;
        this.parentComponent = parentComponent;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: Selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Session:"));
        sessionBox = new JComboBox<>(new String[] { "Morning Stock Count", "Night Stock Count" });
        topPanel.add(sessionBox);

        // Center: Input Table
        stockCountModel = new DefaultTableModel(new Object[] { "Model", "Enter Count" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only count column is editable
            }
        };

        refreshTable(); // Populate models

        JTable table = new JTable(stockCountModel);
        table.setRowHeight(25);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom: Verify Button
        JButton verifyBtn = new JButton("Verify Stock");
        verifyBtn.setFont(new Font("Arial", Font.BOLD, 14));
        verifyBtn.setBackground(new Color(144, 238, 144)); // Light green

        verifyBtn.addActionListener(e -> performStockVerification());
        panel.add(verifyBtn, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshTable() {
        if (stockCountModel == null || models == null)
            return;
        stockCountModel.setRowCount(0);

        for (Model m : models.values()) {
            stockCountModel.addRow(new Object[] { m.getModelName(), "0" });
        }
    }

    private void performStockVerification() {
        StringBuilder report = new StringBuilder();
        String session = (String) sessionBox.getSelectedItem();
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        String currentOutlet = (loggedInUser != null) ? loggedInUser.getOutlet() : "C60";

        report.append("=== ").append(session).append(" ===\n");
        report.append("Date: ").append(date).append("\n");
        report.append("Time: ").append(time).append("\n\n");

        int totalChecked = 0;
        int correct = 0;
        int mismatches = 0;
        boolean hasMismatch = false;

        for (int i = 0; i < stockCountModel.getRowCount(); i++) {
            String modelName = (String) stockCountModel.getValueAt(i, 0);
            String inputStr = (String) stockCountModel.getValueAt(i, 1);
            int counted = 0;
            try {
                counted = Integer.parseInt(inputStr);
            } catch (NumberFormatException ex) {
                counted = 0; // Default to 0 if invalid
            }

            // Get System Stock
            int systemStock = 0;
            if (models.containsKey(modelName)) {
                systemStock = models.get(modelName).getStock(currentOutlet);
            }

            totalChecked++;
            report.append("Model: ").append(modelName).append(" - Counted: ").append(counted).append("\n");
            report.append("Store Record: ").append(systemStock).append("\n");

            if (counted == systemStock) {
                report.append("Stock tally correct.\n\n");
                correct++;
            } else {
                int diff = Math.abs(counted - systemStock);
                report.append("! Mismatch detected (").append(diff).append(" unit difference)\n\n");
                mismatches++;
                hasMismatch = true;
            }
        }

        report.append("Total Models Checked: ").append(totalChecked).append("\n");
        report.append("Tally Correct: ").append(correct).append("\n");
        report.append("Mismatches: ").append(mismatches).append("\n");
        report.append(session).append(" completed.\n");

        if (hasMismatch) {
            report.append("\nWarning: Please verify stock.");
        }

        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(parentComponent, scrollPane, "Stock Verification Report",
                hasMismatch ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
}
