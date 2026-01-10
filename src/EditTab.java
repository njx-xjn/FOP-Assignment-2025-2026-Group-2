import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EditTab {
    private Map<String, Model> models;
    private DataLoader dataLoader;
    private Component parentComponent;

    // UI Components
    private JComboBox<String> editTypeBox;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Stock Edit Components
    private JTextField stockModelField;
    private JComboBox<String> outletBox; // NEW: Outlet selection
    private JLabel currentStockLabel;    // Total Stock
    private JLabel outletStockLabel;     // NEW: Specific Outlet Stock
    private JTextField newStockField;
    private JButton updateStockBtn;
    private String currentModelName;
    
    // Defined Outlets
    private final String[] outletCodes = {"C60", "C61", "C62", "C63", "C64", "C65", "C67", "C68", "C69"};

    // Sales Edit Components
    private JTextField salesDateField;
    private JTextField salesCustomerField;
    private JButton updateSalesBtn;

    private File currentSalesFile;
    private int currentBlockStartIndex; 
    private int currentBlockEndIndex; 

    public EditTab(Map<String, Model> models, DataLoader dataLoader, Component parentComponent) {
        this.models = models;
        this.dataLoader = dataLoader;
        this.parentComponent = parentComponent;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: Edit Type Selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Edit Type: "));
        editTypeBox = new JComboBox<>(new String[] { "Edit Stock Information", "Edit Sales Information" });
        topPanel.add(editTypeBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // Center: Card Layout for content
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        contentPanel.add(createStockEditPanel(), "STOCK");
        contentPanel.add(createSalesEditPanel(), "SALES");

        panel.add(contentPanel, BorderLayout.CENTER);

        // Logic to switch views
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

    // --- UPDATED STOCK EDIT PANEL ---
    private JPanel createStockEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Search
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Enter Model Name:"), gbc);
        stockModelField = new JTextField(15);
        gbc.gridx = 1;
        formPanel.add(stockModelField, gbc);
        JButton searchBtn = new JButton("Search");
        gbc.gridx = 2;
        formPanel.add(searchBtn, gbc);

        // Row 1: Outlet Selection
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Select Outlet:"), gbc);
        outletBox = new JComboBox<>(outletCodes);
        gbc.gridx = 1;
        formPanel.add(outletBox, gbc);

        // Row 2: Outlet Specific Info
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Current Stock (Selected Outlet):"), gbc);
        outletStockLabel = new JLabel("-");
        outletStockLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 1;
        formPanel.add(outletStockLabel, gbc);

        // Row 3: Total Info
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Total Stock (All Outlets):"), gbc);
        currentStockLabel = new JLabel("-");
        gbc.gridx = 1;
        formPanel.add(currentStockLabel, gbc);

        // Row 4: New Value Input
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Enter New Stock for Outlet:"), gbc);
        newStockField = new JTextField(10);
        newStockField.setEnabled(false);
        gbc.gridx = 1;
        formPanel.add(newStockField, gbc);

        // Row 5: Update Button
        updateStockBtn = new JButton("Update Selected Outlet Stock");
        updateStockBtn.setEnabled(false);
        updateStockBtn.setBackground(new Color(25, 25, 112));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(updateStockBtn, gbc);

        // Logic: Search Action
        searchBtn.addActionListener(e -> updateStockDisplay());

        // Logic: Change outlet selection (auto-refresh labels)
        outletBox.addActionListener(e -> updateStockDisplay());

        // Logic: Update Action
        updateStockBtn.addActionListener(e -> {
            try {
                int newStock = Integer.parseInt(newStockField.getText().trim());
                if (newStock < 0) throw new NumberFormatException();

                Model m = models.get(currentModelName);
                String selectedOutlet = (String) outletBox.getSelectedItem();

                // Update internal data
                m.setStock(selectedOutlet, newStock);

                // Save to CSV using the list of outlet codes for header consistency
                dataLoader.saveModels(models, Arrays.asList(outletCodes));

                JOptionPane.showMessageDialog(parentComponent, "Stock updated successfully for " + selectedOutlet);
                updateStockDisplay(); // Refresh the labels
                newStockField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parentComponent, "Invalid stock value. Please enter a positive integer.");
            }
        });

        panel.add(formPanel, BorderLayout.NORTH);
        return panel;
    }

    // Helper method to refresh the labels during search or outlet switch
    private void updateStockDisplay() {
        String modelName = stockModelField.getText().trim();
        if (models.containsKey(modelName)) {
            currentModelName = modelName;
            Model m = models.get(modelName);
            String selectedOutlet = (String) outletBox.getSelectedItem();

            outletStockLabel.setText(String.valueOf(m.getStock(selectedOutlet)));
            currentStockLabel.setText(String.valueOf(m.getTotalStock()));
            
            newStockField.setEnabled(true);
            updateStockBtn.setEnabled(true);
        } else {
            if(!modelName.isEmpty()) JOptionPane.showMessageDialog(parentComponent, "Model not found.");
            outletStockLabel.setText("-");
            currentStockLabel.setText("-");
            newStockField.setEnabled(false);
            updateStockBtn.setEnabled(false);
        }
    }

    // --- SALES EDIT PANEL (Original Features Preserved) ---
    private JPanel createSalesEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Enter Transaction Date (YYYY-MM-DD):"), gbc);
        salesDateField = new JTextField(15);
        salesDateField.setText(java.time.LocalDate.now().toString());
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

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Current Record Details:"), gbc);

        JTextArea recordSummaryArea = new JTextArea(8, 30);
        recordSummaryArea.setEditable(false);
        recordSummaryArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane(recordSummaryArea);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(scrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Select Field to Edit:"), gbc);

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
        updateSalesBtn.setBackground(new Color(173, 216, 230));
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(updateSalesBtn, gbc);

        searchBtn.addActionListener(e -> findSalesRecord(recordSummaryArea));

        updateSalesBtn.addActionListener(e -> {
            String field = (String) fieldSelectBox.getSelectedItem();
            String newVal = newValueField.getText().trim();
            if (newVal.isEmpty()) {
                JOptionPane.showMessageDialog(parentComponent, "Please enter a new value.");
                return;
            }
            updateSalesRecord(field, newVal, recordSummaryArea);
        });

        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    private void findSalesRecord(JTextArea summaryArea) {
        String date = salesDateField.getText().trim();
        String customer = salesCustomerField.getText().trim();

        if (date.isEmpty() || customer.isEmpty()) {
            JOptionPane.showMessageDialog(parentComponent, "Please enter both Date and Customer Name.");
            return;
        }

        File dir = new File("SalesReceipt");
        File file = new File(dir, "sales_" + date + ".txt");
        if (!file.exists()) {
            JOptionPane.showMessageDialog(parentComponent, "No sales records found for this date.");
            return;
        }

        currentSalesFile = file;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            boolean matchFound = false;
            int startIdx = -1;
            int endIdx = -1;

            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                if (l.contains("Customer Name:") && l.contains(customer)) {
                    int tempStart = i;
                    while (tempStart >= 0 && !lines.get(tempStart).startsWith("===")) {
                        tempStart--;
                    }
                    startIdx = tempStart;

                    int tempEnd = i;
                    while (tempEnd < lines.size() && !lines.get(tempEnd).startsWith("---")) {
                        tempEnd++;
                    }
                    endIdx = tempEnd;

                    matchFound = true;
                    break;
                }
            }

            if (matchFound && startIdx != -1) {
                currentBlockStartIndex = startIdx;
                currentBlockEndIndex = endIdx;

                StringBuilder sb = new StringBuilder();
                for (int i = startIdx; i <= endIdx; i++) {
                    sb.append(lines.get(i)).append("\n");
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

    private void updateSalesRecord(String field, String newVal, JTextArea summaryArea) {
        if (currentSalesFile == null || !currentSalesFile.exists())
            return;

        try {
            BufferedReader br = new BufferedReader(new FileReader(currentSalesFile));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();

            List<String> newBlock = new ArrayList<>();
            for (int i = currentBlockStartIndex; i <= currentBlockEndIndex && i < lines.size(); i++) {
                String original = lines.get(i);
                String updated = original;

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

            List<String> validLines = new ArrayList<>();
            for (int i = 0; i < currentBlockStartIndex; i++)
                validLines.add(lines.get(i));
            validLines.addAll(newBlock);
            for (int i = currentBlockEndIndex + 1; i < lines.size(); i++)
                validLines.add(lines.get(i));

            PrintWriter pw = new PrintWriter(new FileWriter(currentSalesFile));
            for (String l : validLines) {
                pw.println(l);
            }
            pw.close();

            JOptionPane.showMessageDialog(parentComponent, "Sales information updated successfully.");

            StringBuilder sb = new StringBuilder();
            for (String l : newBlock)
                sb.append(l).append("\n");
            summaryArea.setText(sb.toString());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Error updating file: " + e.getMessage());
        }
    }
}