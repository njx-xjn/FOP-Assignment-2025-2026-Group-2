
import javax.swing.*;
import java.awt.*;
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
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load Data
        employees = csvFile.loadEmployee();
        models = dataLoader.loadModels();
        outlets = csvFile.loadOutlets(); // Load Outlets for Registration Dropdown

        // Panels
        mainPanel.add(createLoginPanel(), "LOGIN");
        // Dashboard is created dynamically after login because it needs loggedInUser

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
            String id = userField.getText();
            String pass = new String(passField.getPassword());
            if (employees.containsKey(id) && employees.get(id).getPassword().equals(pass)) {
                loggedInUser = employees.get(id);
                JOptionPane.showMessageDialog(this, "Welcome " + loggedInUser.getName());

                // Create Dashboard dynamically
                mainPanel.add(createDashboardPanel(), "DASHBOARD");
                cardLayout.show(mainPanel, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
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

        // Define a refresh callback for other tabs to update Stock Count UI
        Runnable refreshStockUI = stockCountTab::refreshTable;

        // 3. Stock In/Out Tab
        StockInOutTab stockInOutTab = new StockInOutTab(models, outlets, dataLoader, loggedInUser, this,
                refreshStockUI);
        tabbedPane.addTab("Stock In/Out", stockInOutTab.createPanel());

        // 4. Sales Tab
        SalesTab salesTab = new SalesTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        tabbedPane.addTab("Sales", salesTab.createPanel());

        // 5. Edit Tab (New)
        EditTab editTab = new EditTab(models, dataLoader, this);
        tabbedPane.addTab("Edit", editTab.createPanel());

        // 6. Search Tab
        SearchTab searchTab = new SearchTab(models, outlets, dataLoader);
        tabbedPane.addTab("Search", searchTab.createPanel());

        // 6. Analytics Tab
        // 5. Analytics Tab (Kept inline or extracted? User said "each tab ... must have
        // own file". Let's extract Analytics too if easy, or keep it simple.
        // User listed: attendance,stockCount,stockInOut,sales.java explicitly. Did not
        // mention Analytics. I'll keep Analytics inline or extract if needed.
        // Actually, looking at the code, I can keep Analytics inline or use the
        // existing Analytics class (which is logic, not UI).
        // Wait, current GUI has createAnalyticsPanel. I should probably move the UI
        // part to AnalyticsTab.java?
        // User didn't explicitly ask for AnalyticsTab, but said "make it clean". I'll
        // keep it inline for now to avoid over-engineering unless strictly needed,
        // but looking at `createAnalyticsPanel` in GUI.java, it's small.
        tabbedPane.addTab("Analytics", createAnalyticsPanel());

        // Manager-Only Tab
        if (loggedInUser instanceof manager) {
            tabbedPane.addTab("Register Employee", createRegisterPanel());
        }

        JPanel p = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            loggedInUser = null;
            cardLayout.show(mainPanel, "LOGIN");
        });

        p.add(tabbedPane, BorderLayout.CENTER);
        p.add(logout, BorderLayout.SOUTH);
        return p;
    }

    // Kept Analytics UI here as it wasn't explicitly requested to move, but it's
    // small.
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

        panel.add(totalSales);
        panel.add(topModel);
        return panel;
    }

    // Kept Register Panel here as it wasn't requested to move (Manager only).
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("=== Register New Employee ===");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JTextField nameField = new JTextField(20);
        JTextField idField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        String[] roles = { "Part-time", "Full-time" };
        JComboBox<String> roleBox = new JComboBox<>(roles);

        JComboBox<String> outletBox = new JComboBox<>();
        for (Map.Entry<String, String> entry : outlets.entrySet()) {
            outletBox.addItem(entry.getKey());
        }

        JButton registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(173, 216, 230));

        registerBtn.addActionListener(e -> {
            String name = nameField.getText();
            String id = idField.getText();
            String pass = new String(passField.getPassword());
            String role = (String) roleBox.getSelectedItem();
            String selectedOutlet = (String) outletBox.getSelectedItem();

            if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (employees.containsKey(id)) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists!", "Registration Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                employee newEmp = new employee(id, name, role, pass, selectedOutlet);
                employees.put(id, newEmp);
                csvFile.uploadEmployeeCSV(employees);
                JOptionPane.showMessageDialog(this, "Employee successfully registered!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                nameField.setText("");
                idField.setText("");
                passField.setText("");
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JLabel("Enter Employee Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Enter Employee ID:"), gbc);
        gbc.gridx = 1;
        panel.add(idField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Set Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Set Role:"), gbc);
        gbc.gridx = 1;
        panel.add(roleBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Set Outlet:"), gbc);
        gbc.gridx = 1;
        panel.add(outletBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(registerBtn, gbc);

        return panel;
    }

    public static void main(String[] args) {
        new GUI();
    }
}
