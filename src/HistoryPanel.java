import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class HistoryPanel {

    // Reference to the database loader
    private DataLoader dataLoader;

    // Constructor: Inject dependencies
    public HistoryPanel(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        // --- MAIN LAYOUT ---
        // BorderLayout with 15px gaps between sections
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        // --- LOGIC ENGINES ---
        // We need 'Analytics' for doing math (Summing totals)
        // We need 'History' for processing lists (Filtering dates, Sorting)
        Analytics analytics = new Analytics();
        History history = new History();

        // --- FILTER BAR (TOP) ---
        // A nice gray rounded box to hold the inputs
        CustomComponents.RoundedPanel filterPanel = new CustomComponents.RoundedPanel(15, new Color(248, 249, 250));
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15)); // Align left with padding

        // Date Inputs (Default to "Last Week" -> "Today")
        JTextField startField = new CustomComponents.ModernTextField(LocalDate.now().minusWeeks(1).toString());
        startField.setColumns(8);
        JTextField endField = new CustomComponents.ModernTextField(LocalDate.now().toString());
        endField.setColumns(8);

        // Sorting Controls
        String[] sortOptions = { "Date", "Amount", "Customer" };
        JComboBox<String> sortBox = new JComboBox<>(sortOptions);
        sortBox.setFont(GUI.MAIN_FONT);
        sortBox.setBackground(Color.WHITE);

        JCheckBox ascCheck = new JCheckBox("Ascending", true);
        ascCheck.setBackground(new Color(248, 249, 250)); // Match panel background
        ascCheck.setFont(GUI.MAIN_FONT);

        // Apply Button
        CustomComponents.ModernButton applyBtn = new CustomComponents.ModernButton("APPLY FILTERS", GUI.PRIMARY_COLOR, Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(140, 35));

        // Add everything to the top bar
        filterPanel.add(new JLabel("Start:"));
        filterPanel.add(startField);
        filterPanel.add(new JLabel("End:"));
        filterPanel.add(endField);
        filterPanel.add(new JLabel("Sort:"));
        filterPanel.add(sortBox);
        filterPanel.add(ascCheck);
        filterPanel.add(applyBtn);

        // --- DATA TABLE (CENTER) ---
        String[] columns = { "Date", "Customer", "Model", "Qty", "Total (RM)" };
        // DefaultTableModel allows us to add/remove rows dynamically
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        
        // Helper method to make the table look pretty (see bottom of file)
        styleTable(table);

        // --- TOTAL LABEL (BOTTOM) ---
        JLabel cumulativeLabel = new JLabel("Total Sales: RM 0.00");
        cumulativeLabel.setFont(GUI.HEADER_FONT);
        cumulativeLabel.setForeground(GUI.PRIMARY_COLOR);
        cumulativeLabel.setHorizontalAlignment(SwingConstants.RIGHT); // Align to bottom-right

        // --- BUTTON ACTION LISTENER ---
        // This is where the magic happens when you click "APPLY FILTERS"
        applyBtn.addActionListener(e -> {
            try {
                // 1. Get Dates from text fields
                LocalDate start = LocalDate.parse(startField.getText());
                LocalDate end = LocalDate.parse(endField.getText());
                
                // 2. Load fresh data from file
                List<Transaction> allTxns = dataLoader.loadTransactions();
                
                // 3. FILTER: Use History class logic
                List<Transaction> filtered = history.filterSalesByDate(allTxns, start, end);
                
                // 4. SORT: Use History class logic
                // We cast selectedItem to String because JComboBox returns 'Object'
                history.sortSales(filtered, (String) sortBox.getSelectedItem(), ascCheck.isSelected());

                // 5. UPDATE TABLE
                tableModel.setRowCount(0); // Clear old data
                for (Transaction t : filtered) {
                    tableModel.addRow(new Object[] {
                            t.getDate(),
                            t.getCustomerName(),
                            t.getModelName(),
                            t.getQuantity(),
                            String.format("%.2f", t.getTotalAmount()) // Format currency
                    });
                }
                
                // 6. UPDATE TOTAL LABEL
                // Use Analytics class logic for the math
                double total = analytics.calculateCumulativeTotal(filtered);
                cumulativeLabel.setText(String.format("Total Sales: RM %.2f", total));
                
            } catch (Exception ex) {
                // Show error if date format is wrong (e.g., user typed "Today" instead of "2023-10-25")
                JOptionPane.showMessageDialog(null, "Error processing records. Check date format (YYYY-MM-DD).");
            }
        });

        // Assemble the panel
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER); // ScrollPane lets us scroll if list is long
        panel.add(cumulativeLabel, BorderLayout.SOUTH);

        return panel;
    }

    // Helper: Centralizes all the table styling code to keep main method clean
    private void styleTable(JTable table) {
        table.setRowHeight(40); // Make rows taller (easier to read)
        table.setFont(GUI.MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230)); // Subtle grey grid lines
        table.setShowVerticalLines(false); // Only show horizontal lines (cleaner look)
        
        // Selection Colors (Light Orange background, Black text)
        table.setSelectionBackground(new Color(253, 235, 208));
        table.setSelectionForeground(Color.BLACK);

        // Header Styling (Blue background, White text)
        JTableHeader header = table.getTableHeader();
        header.setBackground(GUI.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));

        // Center Alignment for all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}