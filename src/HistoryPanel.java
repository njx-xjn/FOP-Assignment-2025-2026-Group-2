import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class HistoryPanel {

    private DataLoader dataLoader;

    public HistoryPanel(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        // Instantiate both classes
        Analytics analytics = new Analytics();
        History history = new History();

        CustomComponents.RoundedPanel filterPanel = new CustomComponents.RoundedPanel(15, new Color(248, 249, 250));
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JTextField startField = new CustomComponents.ModernTextField(LocalDate.now().minusWeeks(1).toString());
        startField.setColumns(8);
        JTextField endField = new CustomComponents.ModernTextField(LocalDate.now().toString());
        endField.setColumns(8);

        String[] sortOptions = { "Date", "Amount", "Customer" };
        JComboBox<String> sortBox = new JComboBox<>(sortOptions);
        sortBox.setFont(GUI.MAIN_FONT);
        sortBox.setBackground(Color.WHITE);

        JCheckBox ascCheck = new JCheckBox("Ascending", true);
        ascCheck.setBackground(new Color(248, 249, 250));
        ascCheck.setFont(GUI.MAIN_FONT);

        CustomComponents.ModernButton applyBtn = new CustomComponents.ModernButton("APPLY FILTERS", GUI.PRIMARY_COLOR, Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(140, 35));

        filterPanel.add(new JLabel("Start:"));
        filterPanel.add(startField);
        filterPanel.add(new JLabel("End:"));
        filterPanel.add(endField);
        filterPanel.add(new JLabel("Sort:"));
        filterPanel.add(sortBox);
        filterPanel.add(ascCheck);
        filterPanel.add(applyBtn);

        String[] columns = { "Date", "Customer", "Model", "Qty", "Total (RM)" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        JLabel cumulativeLabel = new JLabel("Total Sales: RM 0.00");
        cumulativeLabel.setFont(GUI.HEADER_FONT);
        cumulativeLabel.setForeground(GUI.PRIMARY_COLOR);
        cumulativeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        applyBtn.addActionListener(e -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText());
                LocalDate end = LocalDate.parse(endField.getText());
                List<Transaction> allTxns = dataLoader.loadTransactions();
                
                // UPDATED: Use history for filtering
                List<Transaction> filtered = history.filterSalesByDate(allTxns, start, end);
                
                // UPDATED: Use history for sorting
                history.sortSales(filtered, (String) sortBox.getSelectedItem(), ascCheck.isSelected());

                tableModel.setRowCount(0);
                for (Transaction t : filtered) {
                    tableModel.addRow(new Object[] {
                            t.getDate(),
                            t.getCustomerName(),
                            t.getModelName(),
                            t.getQuantity(),
                            String.format("%.2f", t.getTotalAmount())
                    });
                }
                // Keep analytics for math
                cumulativeLabel.setText(String.format("Total Sales: RM %.2f", analytics.calculateCumulativeTotal(filtered)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error processing records. Check date format (YYYY-MM-DD).");
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(cumulativeLabel, BorderLayout.SOUTH);

        return panel;
    }

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