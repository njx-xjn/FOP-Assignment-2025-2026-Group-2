import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EditTab {
    // SHARED DATA
    // We get these from GUI.java so that when we edit a stock count here,
    // it updates the "real" data immediately across the whole app.
    private Map<String, Model> models;
    private DataLoader dataLoader;
    private Component parentComponent; // Reference to the Main Window (for popups)

    // UI Components
    private JComboBox<String> editTypeBox;
    private JPanel contentPanel;
    private CardLayout cardLayout; // The "Switcher" layout

    // Stock Edit Components
    private JTextField stockModelField;
    private JComboBox<String> outletBox; 
    private JLabel currentStockLabel;    
    private JLabel outletStockLabel;     
    private JTextField newStockField;
    private JButton updateStockBtn;
    private String currentModelName;
    
    // Hardcoded list of outlets. In a real enterprise app, we might load this from a DB,
    // but for this assignment, using the array ensures we match the CSV headers exactly.
    private final String[] outletCodes = {"C60", "C61", "C62", "C63", "C64", "C65", "C66", "C67", "C68", "C69"};

    // Sales Edit Components
    private JTextField salesDateField;
    private JTextField salesCustomerField;
    private JButton updateSalesBtn;

    // State variables to remember which file and which lines we are currently editing
    private File currentSalesFile;
    private int currentBlockStartIndex; 
    private int currentBlockEndIndex; 

    // --- CONSTRUCTOR ---
    // ** CONNECTION TO GUI.JAVA **
    // GUI.java creates this class and passes the 'models' map.
    // This is crucial: By passing the map, we are editing the LIVE data.
    public EditTab(Map<String, Model> models, DataLoader dataLoader, Component parentComponent) {
        this.models = models;
        this.dataLoader = dataLoader;
        this.parentComponent = parentComponent;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // --- TOP BAR (The Switcher) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Edit Type: "));
        
        // User selects "Stock" or "Sales" here
        editTypeBox = new JComboBox<>(new String[] { "Edit Stock Information", "Edit Sales Information" });        
        topPanel.add(editTypeBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // --- CENTER CONTENT (CardLayout) ---
        // CardLayout acts like a deck of cards. We stack the "Stock Panel" and "Sales Panel"
        // on top of each other and only show one at a time.
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Create the two different interface screens
        contentPanel.add(createStockEditPanel(), "STOCK");
        contentPanel.add(createSalesEditPanel(), "SALES");

        panel.add(contentPanel, BorderLayout.CENTER);

        // LISTENER: When dropdown changes, flip the card
        editTypeBox.addActionListener(e -> {
            String selected = (String) editTypeBox.getSelectedItem();
            if (selected.contains("Stock")) {
                cardLayout.show(contentPanel, "STOCK");
            } else {
                cardLayout.show(contentPanel, "SALES");
            }
        });

        return panel;
    }

    // --- STOCK EDIT PANEL ---
    // This allows Managers to manually fix stock counts (e.g., if a physical count was wrong).
    private JPanel createStockEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Using GridBagLayout because we need a precise "Form" layout (Labels aligned left, Inputs aligned right)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10); // Padding between cells
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Search Input
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Enter Model Name:"), gbc);
        stockModelField = new JTextField(15);
        gbc.gridx = 1;
        formPanel.add(stockModelField, gbc);
        JButton searchBtn = new JButton("Search");
        gbc.gridx = 2;
        formPanel.add(searchBtn, gbc);

        // Row 1: Outlet Dropdown
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Select Outlet:"), gbc);
        
        outletBox = new JComboBox<>(outletCodes);
        outletBox.setBackground(new Color(240, 240, 240)); 
        
        gbc.gridx = 1;
        formPanel.add(outletBox, gbc);

        // Row 2: Current Status (Specific Outlet)
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Current Stock (Selected Outlet):"), gbc);
        outletStockLabel = new JLabel("-");
        outletStockLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 1;
        formPanel.add(outletStockLabel, gbc);

        // Row 3: Total Status (Global)
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Total Stock (All Outlets):"), gbc);
        currentStockLabel = new JLabel("-");
        gbc.gridx = 1;
        formPanel.add(currentStockLabel, gbc);

        // Row 4: New Value Input
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Enter New Stock for Outlet:"), gbc);
        newStockField = new JTextField(10);
        newStockField.setEnabled(false); // Disabled until a model is found
        gbc.gridx = 1;
        formPanel.add(newStockField, gbc);

        // Row 5: Action Button
        updateStockBtn = new JButton("Update Selected Outlet Stock");
        updateStockBtn.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; // Span across 3 columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(updateStockBtn, gbc);

        // Event Listeners
        searchBtn.addActionListener(e -> updateStockDisplay());
        outletBox.addActionListener(e -> updateStockDisplay()); // Refresh display if outlet changes

        // UPDATE LOGIC
        updateStockBtn.addActionListener(e -> {
            try {
                // Parse input
                int newStock = Integer.parseInt(newStockField.getText().trim());
                if (newStock < 0) throw new NumberFormatException();

                // 1. Update the Memory Object (Live Data)
                Model m = models.get(currentModelName);
                String selectedOutlet = (String) outletBox.getSelectedItem();
                m.setStock(selectedOutlet, newStock);
                
                // 2. Save to CSV immediately so changes persist after restart
                dataLoader.saveModels(models, Arrays.asList(outletCodes));

                JOptionPane.showMessageDialog(parentComponent, "Stock updated successfully for " + selectedOutlet);
                updateStockDisplay(); // Refresh UI to show new numbers
                newStockField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parentComponent, "Invalid stock value. Please enter a positive integer.");
            }
        });

        panel.add(formPanel, BorderLayout.NORTH);
        return panel;
    }

    // Helper: Refreshes the text labels showing current stock
    private void updateStockDisplay() {
        String modelName = stockModelField.getText().trim();
        // Check if the typed model actually exists in our Map
        if (models.containsKey(modelName)) {
            currentModelName = modelName;
            Model m = models.get(modelName);
            String selectedOutlet = (String) outletBox.getSelectedItem();

            outletStockLabel.setText(String.valueOf(m.getStock(selectedOutlet)));
            currentStockLabel.setText(String.valueOf(m.getTotalStock()));
            
            // Enable editing now that we found the model
            newStockField.setEnabled(true);
            updateStockBtn.setEnabled(true);
        } else {
            // Model not found
            if(!modelName.isEmpty()) JOptionPane.showMessageDialog(parentComponent, "Model not found.");
            outletStockLabel.setText("-");
            currentStockLabel.setText("-");
            newStockField.setEnabled(false);
            updateStockBtn.setEnabled(false);
        }
    }

    // --- SALES EDIT PANEL ---
    // This allows editing past sales receipts. 
    // It's trickier because we have to edit a text file, not just a variable in memory.
    private JPanel createSalesEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        
        formPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5); 
        gbc.anchor = GridBagConstraints.WEST;

        // Search Inputs
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Enter Transaction Date (YYYY-MM-DD):"), gbc);
        salesDateField = new JTextField(15);
        salesDateField.setText(java.time.LocalDate.now().toString()); // Default to today
        gbc.gridx = 1;
        formPanel.add(salesDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Enter Customer Name:"), gbc);
        salesCustomerField = new JTextField(15);
        gbc.gridx = 1;
        formPanel.add(salesCustomerField, gbc);

        JButton searchBtn = new JButton("Find Record");
        gbc.gridx = 2; gbc.gridy = 1;
        formPanel.add(searchBtn, gbc);

        // Visual Separator
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        gbc.insets = new Insets(5, 5, 5, 5);
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Display Area (Shows the text found in the file)
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Current Record Details:"), gbc);

        JTextArea recordSummaryArea = new JTextArea(10, 35);
        recordSummaryArea.setEditable(false);
        recordSummaryArea.setBackground(new Color(240, 240, 240)); 
        JScrollPane scrollPane = new JScrollPane(recordSummaryArea);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(scrollPane, gbc);

        // Edit Controls
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Select Field to Edit:"), gbc);

        // The specific lines in the text file we know how to change
        String[] fields = { "Customer Name", "Model", "Quantity", "Total Price", "Transaction Method" };
        JComboBox<String> fieldSelectBox = new JComboBox<>(fields);        
        gbc.gridx = 1;
        formPanel.add(fieldSelectBox, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Enter New Value:"), gbc);

        JTextField newValueField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(newValueField, gbc);

        updateSalesBtn = new JButton("Confirm Update");
        updateSalesBtn.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(updateSalesBtn, gbc);

        // Hook up listeners
        searchBtn.addActionListener(e -> findSalesRecord(recordSummaryArea));

        updateSalesBtn.addActionListener(e -> {
            String field = (String) fieldSelectBox.getSelectedItem();
            String newVal = newValueField.getText().trim();
            if (newVal.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "Please enter a new value.");
                return;
            }
            // Trigger the file rewrite logic
            updateSalesRecord(field, newVal, recordSummaryArea);
        });

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    // --- FIND SALES RECORD LOGIC ---
    // Scans a text file line-by-line to find a block of text matching the customer.
    private void findSalesRecord(JTextArea summaryArea) {
        String date = salesDateField.getText().trim();
        String customer = salesCustomerField.getText().trim();

        // Basic validation
        if (date.isEmpty() || customer.isEmpty()) {
            JOptionPane.showMessageDialog(parentComponent, "Please enter both Date and Customer Name.");
            return;
        }

        // Locate the specific daily file
        File dir = new File("SalesReceipt");
        File file = new File(dir, "sales_" + date + ".txt");
        if (!file.exists()) {
            JOptionPane.showMessageDialog(parentComponent, "No sales records found for this date.");
            return;
        }

        currentSalesFile = file;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Read entire file into memory list
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            boolean matchFound = false;
            int startIdx = -1;
            int endIdx = -1;

            // Algorithm: Loop through lines looking for "Customer Name: [Input]"
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                if (l.contains("Customer Name:") && l.contains(customer)) {
                    
                    // Found it! Now backtrack UP to find the start of the receipt (===)
                    int tempStart = i;
                    while (tempStart >= 0 && !lines.get(tempStart).startsWith("===")) {
                        tempStart--;
                    }
                    startIdx = tempStart;

                    // Now look DOWN to find the end of the receipt (dashed line)
                    int tempEnd = i;
                    while (tempEnd < lines.size()) {
                        String currentLine = lines.get(tempEnd);
                        // We check length > 40 to find the long separator line
                        if (currentLine.startsWith("---") && currentLine.length() > 40) {
                            break;
                        }
                        tempEnd++;
                    }
                    if (tempEnd >= lines.size()) tempEnd = lines.size() - 1;
                    
                    endIdx = tempEnd;
                    matchFound = true;
                    break; // Stop after first match
                }
            }

            if (matchFound && startIdx != -1) {
                // Save indices so we know exactly which lines to replace later
                currentBlockStartIndex = startIdx;
                currentBlockEndIndex = endIdx;

                // Show the found record in the text area
                StringBuilder sb = new StringBuilder();
                for (int i = startIdx; i <= endIdx; i++) {
                    String lineContent = lines.get(i);
                    // Filter out "System success messages" that shouldn't be edited
                    if (!isStatusLine(lineContent)) {
                        sb.append(lineContent).append("\n");
                    }
                }
                summaryArea.setText(sb.toString());
                updateSalesBtn.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(parentComponent, "Record not found for customer: " + customer);
                summaryArea.setText("");
                updateSalesBtn.setEnabled(false);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Error reading file.");
        }
    }

    // --- UPDATE SALES RECORD LOGIC ---
    // This performs "Surgery" on the text file. 
    // It keeps the top part, replaces the middle part (edited lines), and keeps the bottom part.
    private void updateSalesRecord(String field, String newVal, JTextArea summaryArea) {
        if (currentSalesFile == null || !currentSalesFile.exists())
            return;

        try {
            // 1. Read the WHOLE file into memory again
            BufferedReader br = new BufferedReader(new FileReader(currentSalesFile));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();

            // 2. Reconstruct the specific block of lines we are editing
            List<String> newBlock = new ArrayList<>();
            for (int i = currentBlockStartIndex; i <= currentBlockEndIndex && i < lines.size(); i++) {
                String original = lines.get(i);
                
                // Skip status lines
                if (isStatusLine(original)) continue;

                String updated = original;

                // Logic: Check which field was selected and swap the text
                if (field.equals("Customer Name") && original.startsWith("Customer Name:")) {
                    updated = "Customer Name: " + newVal;
                } else if (field.equals("Model") && original.startsWith("Model:")) {
                    updated = "Model: " + newVal;
                } else if (field.equals("Quantity") && original.startsWith("Quantity:")) {
                    updated = "Quantity: " + newVal;
                } else if (field.equals("Total Price") && original.startsWith("Subtotal:")) {
                    updated = "Subtotal: RM" + newVal;
                } else if (field.equals("Transaction Method") && original.startsWith("Enter transaction method:")) {
                    updated = "Enter transaction method: " + newVal;
                }

                newBlock.add(updated);
            }

            // 3. Rebuild the full file list
            List<String> validLines = new ArrayList<>();
            // Add lines BEFORE the edited block
            for (int i = 0; i < currentBlockStartIndex; i++)
                validLines.add(lines.get(i));
            // Add the NEW edited block
            validLines.addAll(newBlock);
            // Add lines AFTER the edited block
            for (int i = currentBlockEndIndex + 1; i < lines.size(); i++)
                validLines.add(lines.get(i));

            // 4. Write everything back to the file
            PrintWriter pw = new PrintWriter(new FileWriter(currentSalesFile));
            for (String l : validLines) {
                pw.println(l);
            }
            pw.close();

            JOptionPane.showMessageDialog(parentComponent, "Sales information updated successfully.");

            // Update the display area to show the new values
            StringBuilder sb = new StringBuilder();
            for (String l : newBlock)
                sb.append(l).append("\n");
            summaryArea.setText(sb.toString());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Error updating file: " + e.getMessage());
        }
    }

    // Helper: Identify lines we don't want to show/edit (like "Transaction successful")
    private boolean isStatusLine(String line) {
        return line.isEmpty() ||
               line.contains("Unit Price:") ||
               line.contains("Transaction successful.") ||
               line.contains("Sale recorded successfully.") ||
               line.contains("Model quantities updated successfully.") ||
               line.contains("Receipt generated: sales_");
    }
}