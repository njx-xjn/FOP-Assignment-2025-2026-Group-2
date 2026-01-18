import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsPanel {

    // Reference to data source (Database/Files)
    private DataLoader dataLoader;

    // Add data loader via constructor so we share the same data as the rest of the app
    public AnalyticsPanel(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        // --- 1. MAIN LAYOUT (The Wrapper) ---
        // We use BorderLayout to stick the Controls to the Top (NORTH), 
        // the Chart to the Bottom (SOUTH), and the Cards fill the Middle (CENTER).
        // (15, 15) adds a gap between these sections.
        JPanel wrapper = new JPanel(new BorderLayout(15, 15));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20)); // Padding around edges

        // Instantiate logic classes for calculations
        Analytics analytics = new Analytics();
        History history = new History();

        // --- 2. TOP CONTROLS (North) ---
        // FlowLayout.LEFT just lines items up horizontally starting from the left
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(Color.WHITE);

        JLabel lblPeriod = new JLabel("Time Period:");
        lblPeriod.setFont(new Font("Segoe UI", Font.BOLD, 14));

        String[] periods = { "Today", "This Week", "This Month" };
        JComboBox<String> periodBox = new JComboBox<>(periods);
        
        // ** CONNECTION TO GUI.JAVA **
        // We borrow the static font defined in GUI.java so this dropdown matches the rest of the app.
        periodBox.setFont(GUI.MAIN_FONT);
        periodBox.setBackground(Color.WHITE);

        // ** CONNECTION TO GUI.JAVA **
        // We use GUI.PRIMARY_COLOR (Dark Navy) for the button background.
        // If we change the brand color in GUI.java, this button updates automatically.
        CustomComponents.ModernButton refreshBtn = new CustomComponents.ModernButton("REFRESH", GUI.PRIMARY_COLOR, Color.WHITE);
        refreshBtn.setPreferredSize(new Dimension(100, 30)); // Force button size

        // Add components to the control panel
        controls.add(lblPeriod);
        controls.add(periodBox);
        controls.add(refreshBtn);

        // --- 3. STATS CARDS (Center) ---
        // Using GridLayout(1, 3) forces all 3 cards to be exactly the same width.
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setBackground(Color.WHITE);

        // Generate the 3 cards using helper method
        // Colors: Green for Sales, Blue for Top Model, Purple for Avg
        JPanel cardSales = createAnalyticsCard("TOTAL SALES", "RM 0.00", new Color(46, 204, 113));
        JPanel cardModel = createAnalyticsCard("TOP MODEL", "-", new Color(52, 152, 219));
        JPanel cardAvg = createAnalyticsCard("AVG DAILY", "RM 0.00", new Color(155, 89, 182));

        cardsPanel.add(cardSales);
        cardsPanel.add(cardModel);
        cardsPanel.add(cardAvg);

        // --- 4. CHART (South) ---
        // Initialize custom bar chart component
        CustomComponents.SimpleBarChart chart = new CustomComponents.SimpleBarChart();
        chart.setPreferredSize(new Dimension(800, 300)); // Fixed height so it doesn't collapse
        chart.setBorder(new LineBorder(new Color(230, 230, 230), 1)); // Subtle grey border

        // --- 5. DATA UPDATE LOGIC ---
        // This Runnable block handles refreshing data when period changes or button is clicked
        Runnable updateData = () -> {
            // A. Get current selection
            String selectedPeriod = (String) periodBox.getSelectedItem();
            
            // B. Reload fresh transactions
            List<Transaction> all = dataLoader.loadTransactions();
            
            // C. Filter dates (Using History logic)
            LocalDate start = analytics.getStartDateForPeriod(selectedPeriod);
            List<Transaction> filtered = history.filterSalesByDate(all, start, LocalDate.now());

            // D. Recalculate stats (Using Analytics logic)
            double total = analytics.calculateCumulativeTotal(filtered);
            String topModel = analytics.getTopSellingModelForList(filtered);
            double avg = analytics.calculateAverageDailySales(filtered, selectedPeriod);

            // E. Update UI text
            updateCardValue(cardSales, String.format("RM %.2f", total));
            updateCardValue(cardModel, topModel);
            updateCardValue(cardAvg, String.format("RM %.2f", avg));

            // F. Push new data to chart
            Map<String, Double> trend = analytics.getTrendData(filtered, selectedPeriod);
            chart.setData(trend);
        };

        // Hook up listeners
        periodBox.addActionListener(e -> updateData.run());
        refreshBtn.addActionListener(e -> updateData.run());

        // Initial load on startup
        updateData.run();

        // --- 6. FINAL ASSEMBLY ---
        wrapper.add(controls, BorderLayout.NORTH);    // Controls stick to top
        wrapper.add(cardsPanel, BorderLayout.CENTER); // Cards fill the middle space
        wrapper.add(chart, BorderLayout.SOUTH);       // Chart sticks to bottom

        return wrapper;
    }

    // Helper: Creates the colorful stats cards with consistent styling
    private JPanel createAnalyticsCard(String title, String value, Color color) {
        CustomComponents.RoundedPanel card = new CustomComponents.RoundedPanel(20, color);
        // Inside the card, stack Title on top of Value
        card.setLayout(new GridLayout(2, 1)); 
        card.setPreferredSize(new Dimension(200, 120));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel(title, JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(255, 255, 255, 220)); // Semi-transparent white

        JLabel lblValue = new JLabel(value, JLabel.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(Color.WHITE); // Solid white

        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }

    // Helper: Safely updates the text inside a card without crashing
    private void updateCardValue(JPanel card, String newValue) {
        // Index 1 is the Value Label (Index 0 is the Title)
        if (card.getComponentCount() > 1 && card.getComponent(1) instanceof JLabel) {
            ((JLabel) card.getComponent(1)).setText(newValue);
        }
    }
}