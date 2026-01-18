import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsPanel {

    private DataLoader dataLoader;

    public AnalyticsPanel(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(15, 15));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Instantiate both Analytics (for math) and History (for filtering)
        Analytics analytics = new Analytics();
        History history = new History();

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(Color.WHITE);

        JLabel lblPeriod = new JLabel("Time Period:");
        lblPeriod.setFont(new Font("Segoe UI", Font.BOLD, 14));

        String[] periods = { "Today", "This Week", "This Month" };
        JComboBox<String> periodBox = new JComboBox<>(periods);
        periodBox.setFont(GUI.MAIN_FONT);
        periodBox.setBackground(Color.WHITE);

        CustomComponents.ModernButton refreshBtn = new CustomComponents.ModernButton("REFRESH", GUI.PRIMARY_COLOR, Color.WHITE);
        refreshBtn.setPreferredSize(new Dimension(100, 30));

        controls.add(lblPeriod);
        controls.add(periodBox);
        controls.add(refreshBtn);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setBackground(Color.WHITE);

        JPanel cardSales = createAnalyticsCard("TOTAL SALES", "RM 0.00", new Color(46, 204, 113));
        JPanel cardModel = createAnalyticsCard("TOP MODEL", "-", new Color(52, 152, 219));
        JPanel cardAvg = createAnalyticsCard("AVG DAILY", "RM 0.00", new Color(155, 89, 182));

        cardsPanel.add(cardSales);
        cardsPanel.add(cardModel);
        cardsPanel.add(cardAvg);

        CustomComponents.SimpleBarChart chart = new CustomComponents.SimpleBarChart();
        chart.setPreferredSize(new Dimension(800, 300));
        chart.setBorder(new LineBorder(new Color(230, 230, 230), 1));

        Runnable updateData = () -> {
            String selectedPeriod = (String) periodBox.getSelectedItem();
            List<Transaction> all = dataLoader.loadTransactions();
            
            // Get date range from Analytics
            LocalDate start = analytics.getStartDateForPeriod(selectedPeriod);
            
            // Filter using History class
            List<Transaction> filtered = history.filterSalesByDate(all, start, LocalDate.now());

            // Calculate stats using Analytics class
            double total = analytics.calculateCumulativeTotal(filtered);
            String topModel = analytics.getTopSellingModelForList(filtered);
            double avg = analytics.calculateAverageDailySales(filtered, selectedPeriod);

            updateCardValue(cardSales, String.format("RM %.2f", total));
            updateCardValue(cardModel, topModel);
            updateCardValue(cardAvg, String.format("RM %.2f", avg));

            Map<String, Double> trend = analytics.getTrendData(filtered, selectedPeriod);
            chart.setData(trend);
        };

        periodBox.addActionListener(e -> updateData.run());
        refreshBtn.addActionListener(e -> updateData.run());

        updateData.run();

        wrapper.add(controls, BorderLayout.NORTH);
        wrapper.add(cardsPanel, BorderLayout.CENTER);
        wrapper.add(chart, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createAnalyticsCard(String title, String value, Color color) {
        CustomComponents.RoundedPanel card = new CustomComponents.RoundedPanel(20, color);
        card.setLayout(new GridLayout(2, 1));
        card.setPreferredSize(new Dimension(200, 120));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel(title, JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(255, 255, 255, 220));

        JLabel lblValue = new JLabel(value, JLabel.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(Color.WHITE);

        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }

    private void updateCardValue(JPanel card, String newValue) {
        if (card.getComponentCount() > 1 && card.getComponent(1) instanceof JLabel) {
            ((JLabel) card.getComponent(1)).setText(newValue);
        }
    }
}