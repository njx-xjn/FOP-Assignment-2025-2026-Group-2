import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class PerformancePanel {

    // Dependencies we need to do our job
    private DataLoader dataLoader;
    private Map<String, employee> employees;

    // Constructor: Dependencies injected from GUI.java
    public PerformancePanel(DataLoader dataLoader, Map<String, employee> employees) {
        this.dataLoader = dataLoader;
        this.employees = employees;
    }

    public JPanel createPanel() {
        // --- 1. LAYOUT SETUP ---
        // Standard BorderLayout with gaps
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        // LOGIC ENGINE:
        // We use the 'Performance' class to handle the heavy math (summing up sales).
        Performance performance = new Performance();

        // --- 2. TABLE SETUP ---
        // Define columns for the leaderboard
        String[] columns = { "Rank", "Employee Name", "Total Sales (RM)", "Txn Count" };
        
        // DefaultTableModel allows us to add rows dynamically later
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        
        // Apply our custom visual style (see helper method at bottom)
        styleTable(table);

        // --- 3. CONTROLS ---
        CustomComponents.ModernButton btnGenerate = new CustomComponents.ModernButton("GENERATE REPORT", GUI.ACCENT_COLOR, Color.WHITE);

        // --- 4. ACTION LISTENER (THE BRAIN) ---
        // This runs when the Manager clicks "Generate Report"
        btnGenerate.addActionListener(e -> {
            // A. Reset: Clear existing rows so we don't duplicate data if clicked twice
            tableModel.setRowCount(0);
            
            // B. Reload Data: Load fresh employee list from file. 
            // We do this just in case a new staff member was registered 5 minutes ago.
            Map<String, employee> freshEmployees = dataLoader.loadEmployee();
            
            // C. Get Stats: Ask Performance.java for the sorted list of sales
            List<Performance.PerformanceEntry> performanceData = performance.getEmployeePerformance();

            int rank = 1;
            
            // D. Process Data: Loop through the results and put them in the table
            for (Performance.PerformanceEntry entry : performanceData) {
                
                // NAME LOOKUP LOGIC:
                // The transaction only has an ID (e.g., "001"). We need to find the Name ("Ali").
                String name = "Unknown Staff";
                String cleanID = (entry.empId != null) ? entry.empId.trim() : "";

                // Attempt 1: Fast Lookup
                // Check if the ID exists directly in the Map (HashMap is very fast)
                if (freshEmployees.containsKey(cleanID)) {
                    name = freshEmployees.get(cleanID).getName();
                } else {
                    // Attempt 2: Brute-Force Lookup (Fallback)
                    // If Fast Lookup failed (maybe due to case sensitivity "A01" vs "a01"),
                    // loop through everyone to find a match.
                    for (employee emp : freshEmployees.values()) {
                        if (emp.getID().trim().equalsIgnoreCase(cleanID)) {
                            name = emp.getName();
                            break; // Found them, stop looking
                        }
                    }
                }

                // E. Formatting: Create the Rank string
                String rankStr = "RANK " + rank;
                if (rank == 1) rankStr = "TOP RANK"; // Special badge for #1

                // F. Add Row: Push the final calculated data into the table
                tableModel.addRow(new Object[] {
                        rankStr,
                        name,
                        String.format("%.2f", entry.totalSales), // Format money to 2 decimals
                        entry.transactionCount
                });
                rank++; // Increment rank for the next person
            }
        });

        // --- 5. HEADER LABEL ---
        JLabel header = new JLabel("STAFF PERFORMANCE METRICS");
        header.setFont(GUI.HEADER_FONT);
        header.setForeground(GUI.PRIMARY_COLOR);
        header.setHorizontalAlignment(JLabel.CENTER);

        // --- 6. FINAL ASSEMBLY ---
        panel.add(header, BorderLayout.NORTH); // Title at top
        panel.add(new JScrollPane(table), BorderLayout.CENTER); // Table in middle (scrollable)

        // Wrap button in a panel so it stays small and centered at the bottom
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnGenerate);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- HELPER: TABLE STYLING ---
    // Makes the table look consistent with the rest of the application
    private void styleTable(JTable table) {
        table.setRowHeight(40); // Taller rows are easier to read
        table.setFont(GUI.MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230)); // Light grey grid lines
        table.setShowVerticalLines(false); // Clean look (horizontal lines only)
        
        // Highlight color when a row is selected
        table.setSelectionBackground(new Color(253, 235, 208)); 
        table.setSelectionForeground(Color.BLACK);

        // Header styling (Dark Blue background, White text)
        JTableHeader header = table.getTableHeader();
        header.setBackground(GUI.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));

        // Center align text in all cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}