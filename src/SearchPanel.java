import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class SearchPanel {

    // Dependencies
    private Map<String, Model> models;     // Live Stock Data
    private Map<String, String> outlets;   // Outlet Names
    private DataLoader dataLoader;         // To access file search functions

    // UI Components
    private JComboBox<String> searchTypeBox;
    private JTextField searchInputField;
    private JTextArea resultArea;
    private JLabel searchLabel;

    public SearchPanel(Map<String, Model> models, Map<String, String> outlets, DataLoader dataLoader) {
        this.models = models;
        this.outlets = outlets;
        this.dataLoader = dataLoader;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // --- TOP PANEL: CONFIGURATION ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // 1. Search Type Dropdown
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchTypeBox = new JComboBox<>(new String[] { "Search Stock Information", "Search Sales Information" });
        configPanel.add(new JLabel("Search Type: "));
        configPanel.add(searchTypeBox);

        // 2. Search Input
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLabel = new JLabel("Search Model Name: ");
        searchInputField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(GUI.PRIMARY_COLOR); // Use brand color

        inputPanel.add(searchLabel);
        inputPanel.add(searchInputField);
        inputPanel.add(searchBtn);

        topPanel.add(configPanel);
        topPanel.add(inputPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: RESULTS DISPLAY ---
        resultArea = new JTextArea();
        resultArea.setEditable(false); // Read-only
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Monospace aligns text nicely
        resultArea.setBackground(new Color(230, 240, 255)); // Light blue background
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ScrollPane ensures we can read long results
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // --- EVENT LISTENERS ---
        
        // Listener: Switch Labels when Dropdown changes
        searchTypeBox.addActionListener(e -> {
            String selected = (String) searchTypeBox.getSelectedItem();
            if (selected.contains("Stock")) {
                searchLabel.setText("Search Model Name: ");
            } else {
                searchLabel.setText("Search (Date/Cust/Model): ");
            }
            resultArea.setText(""); // Clear previous results
        });

        // Listener: Run search when button clicked
        searchBtn.addActionListener(e -> performSearch());

        return panel;
    }

    // --- SEARCH LOGIC ---
    private void performSearch() {
        String query = searchInputField.getText().trim();
        
        // Validation: Don't search for empty strings
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter a search term.");
            return;
        }

        resultArea.setText("Searching...\n\n");
        String type = (String) searchTypeBox.getSelectedItem();

        // MODE 1: STOCK SEARCH (Memory Lookup)
        // This is extremely fast because 'models' is a HashMap in RAM.
        if (type.contains("Stock")) {
            
            if (models.containsKey(query)) {
                Model m = models.get(query);
                StringBuilder sb = new StringBuilder();
                sb.append("Model: ").append(m.getModelName()).append("\n");
                sb.append("Unit Price: RM").append(String.format("%.2f", m.getPrice())).append("\n\n");
                sb.append("Stock by Outlet:\n");

                // Loop through all outlets to show stock distribution
                int count = 0;
                for (Map.Entry<String, String> entry : outlets.entrySet()) {
                    String code = entry.getKey();
                    String name = entry.getValue();
                    
                    // Formatting: String.format("%-20s", ...) adds padding for alignment
                    sb.append(String.format("%-20s: %d", name, m.getStock(code)));
                    count++;
                    
                    // New line every 3 items so it doesn't get too wide
                    if (count % 3 == 0)
                        sb.append("\n"); 
                    else
                        sb.append("    ");
                }

                resultArea.setText(sb.toString());
            } else {
                resultArea.setText("Model '" + query + "' not found.");
            }

        } else {
            // MODE 2: SALES SEARCH (File Scan)
            // This is slower because we have to ask DataLoader to open text files on the disk.
            // It scans through receipts for keywords (Customer Name, Date, etc.)
            String results = dataLoader.searchSalesReceipts(query);
            resultArea.setText(results);
        }
    }
}