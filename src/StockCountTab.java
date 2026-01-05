import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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
        panel.setBackground(Color.WHITE); // Ensure clean background

        // --- TOP: SELECTION ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        topPanel.setBackground(Color.WHITE);
        
        JLabel lblSession = new JLabel("Select Session:");
        lblSession.setFont(GUI.MAIN_FONT);
        lblSession.setForeground(GUI.PRIMARY_COLOR);
        
        sessionBox = new JComboBox<>(new String[] { "Morning Stock Count", "Night Stock Count" });
        sessionBox.setFont(GUI.MAIN_FONT);
        sessionBox.setBackground(Color.WHITE);
        
        topPanel.add(lblSession);
        topPanel.add(sessionBox);

        // --- CENTER: TABLE ---
        stockCountModel = new DefaultTableModel(new Object[] { "Model", "Enter Count" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only count column is editable
            }
        };

        refreshTable(); // Populate models

        JTable table = new JTable(stockCountModel);
        
        // *** FIX FOR INVISIBLE HEADERS ***
        // We manually apply the same styling as the main GUI
        styleTable(table);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BOTTOM: VERIFY BUTTON ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.WHITE);
        
        // Use the custom button style from GUI if possible, or style manually
        JButton verifyBtn = new JButton("VERIFY STOCK");
        verifyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyBtn.setBackground(new Color(46, 204, 113)); // Green
        verifyBtn.setForeground(Color.WHITE);
        verifyBtn.setFocusPainted(false);
        verifyBtn.setPreferredSize(new Dimension(150, 40));
        verifyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        verifyBtn.addActionListener(e -> performStockVerification());
        
        bottomPanel.add(verifyBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- HELPER: Apply GUI Styles to this specific table ---
    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(GUI.MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(253, 235, 208)); 
        table.setSelectionForeground(Color.BLACK);
        
        // This specifically fixes the "Model" and "Enter Count" visibility
        JTableHeader header = table.getTableHeader();
        header.setBackground(GUI.PRIMARY_COLOR); // Dark Navy Background
        header.setForeground(Color.WHITE);       // White Text
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));
        
        // Center text in cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++){
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
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