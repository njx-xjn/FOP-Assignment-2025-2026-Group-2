import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Map;

public class StockCountTab {
    // SHARED DATA
    private Map<String, Model> models;
    private employee loggedInUser;
    
    // UI Components
    private DefaultTableModel stockCountModel;
    private JComboBox<String> sessionBox;
    private Component parentComponent; // For centering popups

    // Constructor: Dependencies injected from GUI.java
    public StockCountTab(Map<String, Model> models, employee loggedInUser, Component parentComponent) {
        this.models = models;
        this.loggedInUser = loggedInUser;
        this.parentComponent = parentComponent;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE); 

        // --- TOP: SESSION SELECTION ---
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

        // --- CENTER: THE DATA TABLE ---
        // We override 'isCellEditable' to control which cells the user can type in.
        stockCountModel = new DefaultTableModel(new Object[] { "Model", "Enter Count" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Column 0 is Model Name (Read-only)
                // Column 1 is Count Input (Editable)
                return column == 1; 
            }
        };

        // Populate table with list of models
        refreshTable(); 

        JTable table = new JTable(stockCountModel);
        
        // APPLY STYLING:
        // This makes sure the headers are visible (White text on Blue background)
        styleTable(table);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BOTTOM: VERIFY BUTTON ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.WHITE);
        
        JButton verifyBtn = new JButton("VERIFY STOCK");
        verifyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyBtn.setBackground(new Color(46, 204, 113)); // Green
        verifyBtn.setForeground(Color.WHITE);
        verifyBtn.setFocusPainted(false);
        verifyBtn.setPreferredSize(new Dimension(150, 40));
        verifyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // ACTION: Run the math logic when clicked
        verifyBtn.addActionListener(e -> performStockVerification());
        
        bottomPanel.add(verifyBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- HELPER: TABLE STYLING ---
    // Sets up fonts, colors, and header visibility.
    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(GUI.MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(253, 235, 208)); 
        table.setSelectionForeground(Color.BLACK);
        
        // HEADER STYLING
        JTableHeader header = table.getTableHeader();
        header.setBackground(GUI.PRIMARY_COLOR); // Dark Navy Background
        header.setForeground(Color.WHITE);       // White Text
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));
        
        // CENTER ALIGNMENT
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<table.getColumnCount(); i++){
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    // --- REFRESH DATA ---
    // Clears the table and re-adds all models with "0" as the starting count.
    public void refreshTable() {
        if (stockCountModel == null || models == null)
            return;
        stockCountModel.setRowCount(0);

        for (Model m : models.values()) {
            stockCountModel.addRow(new Object[] { m.getModelName(), "0" });
        }
    }

    // --- VERIFICATION LOGIC ---
    // Compares what the user typed vs. what the system database says.
    private void performStockVerification() {
        StringBuilder report = new StringBuilder();
        String session = (String) sessionBox.getSelectedItem();
        String date = java.time.LocalDate.now().toString();
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        
        // Determine current outlet (Default to C60 if unknown)
        String currentOutlet = (loggedInUser != null) ? loggedInUser.getOutlet() : "C60";

        // Build Report Header
        report.append("=== ").append(session).append(" ===\n");
        report.append("Date: ").append(date).append("\n");
        report.append("Time: ").append(time).append("\n\n");

        int totalChecked = 0;
        int correct = 0;
        int mismatches = 0;
        boolean hasMismatch = false;

        // Loop through every row in the table
        for (int i = 0; i < stockCountModel.getRowCount(); i++) {
            String modelName = (String) stockCountModel.getValueAt(i, 0);
            String inputStr = (String) stockCountModel.getValueAt(i, 1);
            
            // Parse User Input
            int counted = 0;
            try {
                counted = Integer.parseInt(inputStr);
            } catch (NumberFormatException ex) {
                counted = 0; // Default to 0 if they typed "abc" or left it blank
            }

            // Get Expected System Stock
            int systemStock = 0;
            if (models.containsKey(modelName)) {
                systemStock = models.get(modelName).getStock(currentOutlet);
            }

            totalChecked++;
            report.append("Model: ").append(modelName).append(" - Counted: ").append(counted).append("\n");
            report.append("Store Record: ").append(systemStock).append("\n");

            // Compare
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

        // Summary Footer
        report.append("Total Models Checked: ").append(totalChecked).append("\n");
        report.append("Tally Correct: ").append(correct).append("\n");
        report.append("Mismatches: ").append(mismatches).append("\n");
        report.append(session).append(" completed.\n");

        if (hasMismatch) {
            report.append("\nWarning: Please verify stock.");
        }

        // Show result in a scrollable popup
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        // Use WARNING icon if mismatches found, otherwise INFO icon
        JOptionPane.showMessageDialog(parentComponent, scrollPane, "Stock Verification Report",
                hasMismatch ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
}