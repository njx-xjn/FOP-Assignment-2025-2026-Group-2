import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class GUI extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private employee loggedInUser;

    // Services
    private CSVfile csvFile = new CSVfile();
    private DataLoader dataLoader = new DataLoader();
    private Map<String, employee> employees;
    private Map<String, Model> models;
    private Map<String, String> outlets;

    public GUI() {
        setTitle("GoldenHour Management System");
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load Data
        employees = csvFile.loadEmployee();
        models = dataLoader.loadModels();
        outlets = csvFile.loadOutlets(); 

        mainPanel.add(createLoginPanel(), "LOGIN");

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("GoldenHour Login");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");

        loginBtn.addActionListener(e -> {
            String id = userField.getText().trim(); // Trim to prevent space issues
            String pass = new String(passField.getPassword());
            if (employees.containsKey(id) && employees.get(id).getPassword().equals(pass)) {
                loggedInUser = employees.get(id);
                JOptionPane.showMessageDialog(this, "Welcome " + loggedInUser.getName());

                mainPanel.add(createDashboardPanel(), "DASHBOARD");
                cardLayout.show(mainPanel, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);
        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1; panel.add(userField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        return panel;
    }

    private JPanel createDashboardPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. Attendance Tab
        AttendanceTab attendanceTab = new AttendanceTab(dataLoader, loggedInUser);
        tabbedPane.addTab("Attendance", attendanceTab.createPanel());

        // 2. Stock Count Tab
        StockCountTab stockCountTab = new StockCountTab(models, loggedInUser, this);
        tabbedPane.addTab("Stock Count", stockCountTab.createPanel());

        Runnable refreshStockUI = stockCountTab::refreshTable;

        // 3. Stock In/Out Tab
        StockInOutTab stockInOutTab = new StockInOutTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        tabbedPane.addTab("Stock In/Out", stockInOutTab.createPanel());

        // 4. Sales Tab
        SalesTab salesTab = new SalesTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        tabbedPane.addTab("Sales", salesTab.createPanel());

        // 5. Sales History Tab (No ID column)
        tabbedPane.addTab("Sales History", createSalesHistoryPanel());

        // 6. MANAGER ONLY TAB: PERFORMANCE METRICS
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            tabbedPane.addTab("Employee Performance", createPerformancePanel());
        }

        // 7. Edit Tab
        EditTab editTab = new EditTab(models, dataLoader, this);
        tabbedPane.addTab("Edit", editTab.createPanel());

        // 8. Search Tab
        SearchTab searchTab = new SearchTab(models, outlets, dataLoader);
        tabbedPane.addTab("Search", searchTab.createPanel());

        // 9. Analytics Tab
        tabbedPane.addTab("Analytics", createAnalyticsPanel());

        // 10. Register Employee Tab (Manager Only)
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            tabbedPane.addTab("Register Employee", createRegisterPanel());
        }

        JPanel p = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout (" + loggedInUser.getName() + ")");
        logout.addActionListener(e -> {
            loggedInUser = null;
            cardLayout.show(mainPanel, "LOGIN");
        });

        p.add(tabbedPane, BorderLayout.CENTER);
        p.add(logout, BorderLayout.SOUTH);
        return p;
    }

    // --- FIXED: EMPLOYEE PERFORMANCE REPORT ---
    private JPanel createPerformancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Analytics analytics = new Analytics();

        String[] columns = {"Rank", "Employee Name", "Total Sales (RM)", "No. Of Transactions Handled"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);

        JButton btnGenerate = new JButton("Generate Performance Report");
        btnGenerate.addActionListener(e -> {
            tableModel.setRowCount(0);
            
            // Refresh employee data to ensure C6001 is mapped to Tan Guan Han
            employees = csvFile.loadEmployee();
            
            List<Analytics.PerformanceEntry> performanceData = analytics.getEmployeePerformance();

            int rank = 1;
            for (Analytics.PerformanceEntry entry : performanceData) {
                String name = "Unknown Staff";
                // CRITICAL: Trim the ID to handle any hidden spaces in the text files
                String cleanID = (entry.empId != null) ? entry.empId.trim() : "";

                // Case-insensitive fuzzy search to ensure mapping
                if (employees.containsKey(cleanID)) {
                    name = employees.get(cleanID).getName();
                } else {
                    for (employee emp : employees.values()) {
                        if (emp.getID().trim().equalsIgnoreCase(cleanID)) {
                            name = emp.getName();
                            break;
                        }
                    }
                }
                
                tableModel.addRow(new Object[]{
                        rank == 1 ? "🥇 #1" : "#" + rank,
                        name,
                        String.format("%.2f", entry.totalSales),
                        entry.transactionCount
                });
                rank++;
            }
        });

        panel.add(new JLabel("Manager Confidential: Staff Performance (Highest to Lowest)", JLabel.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnGenerate, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSalesHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Analytics analytics = new Analytics();

        JPanel filterPanel = new JPanel(new FlowLayout());
        JTextField startField = new JTextField(LocalDate.now().minusWeeks(1).toString(), 8);
        JTextField endField = new JTextField(LocalDate.now().toString(), 8);
        
        String[] sortOptions = {"Date", "Amount", "Customer"};
        JComboBox<String> sortBox = new JComboBox<>(sortOptions);
        JCheckBox ascCheck = new JCheckBox("Ascending", true);
        JButton applyBtn = new JButton("Apply Filter & Sort");

        filterPanel.add(new JLabel("Start Date:")); filterPanel.add(startField);
        filterPanel.add(new JLabel("End Date:")); filterPanel.add(endField);
        filterPanel.add(new JLabel("Sort By:")); filterPanel.add(sortBox);
        filterPanel.add(ascCheck); filterPanel.add(applyBtn);

        String[] columns = {"Date", "Customer", "Model", "Qty", "Total (RM)"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        JLabel cumulativeLabel = new JLabel("Total Cumulative Sales: RM 0.00");
        cumulativeLabel.setFont(new Font("Arial", Font.BOLD, 14));

        applyBtn.addActionListener(e -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText());
                LocalDate end = LocalDate.parse(endField.getText());
                
                List<Transaction> allTxns = dataLoader.loadTransactions(); 
                List<Transaction> filtered = analytics.filterSalesByDate(allTxns, start, end);
                
                analytics.sortSales(filtered, (String)sortBox.getSelectedItem(), ascCheck.isSelected());
                
                tableModel.setRowCount(0);
                for (Transaction t : filtered) {
                    tableModel.addRow(new Object[]{
                        t.getDate(), 
                        t.getCustomerName(), 
                        t.getModelName(), 
                        t.getQuantity(), 
                        String.format("%.2f", t.getTotalAmount())
                    });
                }
                cumulativeLabel.setText(String.format("Total Cumulative Sales: RM %.2f", analytics.calculateCumulativeTotal(filtered)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error processing records: " + ex.getMessage());
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(cumulativeLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        Analytics analytics = new Analytics();
        JLabel totalSales = new JLabel("Total Sales: RM " + analytics.calculateTotalSales());
        JLabel topModel = new JLabel("Top Model: " + analytics.getTopSellingModel());
        JButton refresh = new JButton("Refresh Data");
        refresh.addActionListener(e -> {
            totalSales.setText("Total Sales: RM " + analytics.calculateTotalSales());
            topModel.setText("Top Model: " + analytics.getTopSellingModel());
        });
        panel.add(totalSales); panel.add(topModel); panel.add(refresh);
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField nameField = new JTextField(20);
        JTextField idField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Part-time", "Full-time", "Manager"});
        JComboBox<String> outletBox = new JComboBox<>();
        for (Map.Entry<String, String> entry : outlets.entrySet()) outletBox.addItem(entry.getKey());
        JButton registerBtn = new JButton("Register");
        registerBtn.addActionListener(e -> {
            if (nameField.getText().isEmpty() || idField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }
            employee newEmp = new employee(idField.getText(), nameField.getText(), (String)roleBox.getSelectedItem(), new String(passField.getPassword()), (String)outletBox.getSelectedItem());
            employees.put(idField.getText(), newEmp);
            csvFile.uploadEmployeeCSV(employees);
            JOptionPane.showMessageDialog(this, "Employee successfully registered!");
        });
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Name:"), gbc); gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("ID:"), gbc); gbc.gridx = 1; panel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc); gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Role:"), gbc); gbc.gridx = 1; panel.add(roleBox, gbc);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Outlet:"), gbc); gbc.gridx = 1; panel.add(outletBox, gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; panel.add(registerBtn, gbc);
        return panel;
    }

    public static void main(String[] args) {
        new GUI();
    }
}