
import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class SearchTab {
    private Map<String, Model> models;
    private Map<String, String> outlets;
    private DataLoader dataLoader;

    private JComboBox<String> searchTypeBox;
    private JTextField searchInputField;
    private JTextArea resultArea;
    private JLabel searchLabel;

    public SearchTab(Map<String, Model> models, Map<String, String> outlets, DataLoader dataLoader) {
        this.models = models;
        this.outlets = outlets;
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top Panel: Search Configuration
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchTypeBox = new JComboBox<>(new String[] { "Search Stock Information", "Search Sales Information" });
        configPanel.add(new JLabel("Search Type: "));
        configPanel.add(searchTypeBox);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLabel = new JLabel("Search Model Name: ");
        searchInputField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(173, 216, 230)); // Light blue like mockup

        inputPanel.add(searchLabel);
        inputPanel.add(searchInputField);
        inputPanel.add(searchBtn);

        topPanel.add(configPanel);
        topPanel.add(inputPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center Panel: Results
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBackground(new Color(230, 240, 255)); // Light blue per user request/mockup
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Logic
        searchTypeBox.addActionListener(e -> {
            String selected = (String) searchTypeBox.getSelectedItem();
            if (selected.contains("Stock")) {
                searchLabel.setText("Search Model Name: ");
            } else {
                searchLabel.setText("Search (Date/Cust/Model): ");
            }
            resultArea.setText(""); // Clear results on switch
        });

        searchBtn.addActionListener(e -> performSearch());

        return panel;
    }

    private void performSearch() {
        String query = searchInputField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter a search term.");
            return;
        }

        resultArea.setText("Searching...\n\n");
        String type = (String) searchTypeBox.getSelectedItem();

        if (type.contains("Stock")) {
            // STOCK SEARCH
            if (models.containsKey(query)) {
                Model m = models.get(query);
                StringBuilder sb = new StringBuilder();
                sb.append("Model: ").append(m.getModelName()).append("\n");
                sb.append("Unit Price: RM").append(String.format("%.2f", m.getPrice())).append("\n\n");
                sb.append("Stock by Outlet:\n");

                int count = 0;
                for (Map.Entry<String, String> entry : outlets.entrySet()) {
                    String code = entry.getKey();
                    String name = entry.getValue();
                    // Display like "KLCC: 1", 3 per line or simply listed
                    sb.append(String.format("%-20s: %d", name, m.getStock(code)));
                    count++;
                    if (count % 3 == 0)
                        sb.append("\n"); // Newline every 3 items
                    else
                        sb.append("    ");
                }

                resultArea.setText(sb.toString());
            } else {
                resultArea.setText("Model '" + query + "' not found.");
            }

        } else {
            // SALES SEARCH
            // Search via DataLoader through text files
            String results = dataLoader.searchSalesReceipts(query);
            resultArea.setText(results);
        }
    }
}
