import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class PerformancePanel {

    private DataLoader dataLoader;
    private Map<String, employee> employees;

    public PerformancePanel(DataLoader dataLoader, Map<String, employee> employees) {
        this.dataLoader = dataLoader;
        this.employees = employees;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        // UPDATED: Use the new Performance class instead of Analytics
        Performance performance = new Performance();

        String[] columns = { "Rank", "Employee Name", "Total Sales (RM)", "Txn Count" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        CustomComponents.ModernButton btnGenerate = new CustomComponents.ModernButton("GENERATE REPORT", GUI.ACCENT_COLOR, Color.WHITE);

        btnGenerate.addActionListener(e -> {
            tableModel.setRowCount(0);
            // Refresh employee data
            Map<String, employee> freshEmployees = dataLoader.loadEmployee();
            
            // UPDATED: Call method from Performance class
            List<Performance.PerformanceEntry> performanceData = performance.getEmployeePerformance();

            int rank = 1;
            for (Performance.PerformanceEntry entry : performanceData) {
                String name = "Unknown Staff";
                String cleanID = (entry.empId != null) ? entry.empId.trim() : "";

                if (freshEmployees.containsKey(cleanID)) {
                    name = freshEmployees.get(cleanID).getName();
                } else {
                    for (employee emp : freshEmployees.values()) {
                        if (emp.getID().trim().equalsIgnoreCase(cleanID)) {
                            name = emp.getName();
                            break;
                        }
                    }
                }

                String rankStr = "RANK " + rank;
                if (rank == 1) rankStr = "TOP RANK";

                tableModel.addRow(new Object[] {
                        rankStr,
                        name,
                        String.format("%.2f", entry.totalSales),
                        entry.transactionCount
                });
                rank++;
            }
        });

        JLabel header = new JLabel("STAFF PERFORMANCE METRICS");
        header.setFont(GUI.HEADER_FONT);
        header.setForeground(GUI.PRIMARY_COLOR);
        header.setHorizontalAlignment(JLabel.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnGenerate);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // Helper reused for this table
    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(GUI.MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(253, 235, 208));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setBackground(GUI.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}