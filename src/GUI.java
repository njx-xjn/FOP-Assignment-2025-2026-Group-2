import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class GUI extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    public employee loggedInUser; // Made public to be accessible if needed

    // Auto-Email Flag
    private boolean emailSentToday = false;

    // Services
    private DataLoader dataLoader = new DataLoader();
    private Map<String, employee> employees;
    private Map<String, Model> models;
    private Map<String, String> outlets;

    // --- DESIGN CONSTANTS ---
    public static final Color PRIMARY_COLOR = new Color(44, 62, 80); // Dark Navy
    public static final Color ACCENT_COLOR = new Color(243, 156, 18); // Golden/Orange
    public static final Color BG_COLOR = new Color(240, 243, 244); // Very Light Gray
    public static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 22);

    public GUI() {
        // 1. Setup Look and Feel
        setupModernUI();

        setTitle("GoldenHour Management System");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load Data
        employees = dataLoader.loadEmployee();
        models = dataLoader.loadModels();
        outlets = dataLoader.loadOutlets();

        // Main Container Style
        mainPanel.setBackground(BG_COLOR);

        // Add Login Panel using the new separate class
        LoginPanel loginPanel = new LoginPanel(this, employees);
        mainPanel.add(loginPanel.createPanel(), "LOGIN");

        add(mainPanel);
        setVisible(true);

        // --- BACKGROUND TIMER ---
        Timer emailTimer = new Timer(60000, e -> checkAndSendEmail());
        emailTimer.start();
    }

    // Callback called by LoginPanel.java when login is successful
    public void onLoginSuccess(employee user, JTextField userField, JPasswordField passField) {
        this.loggedInUser = user;
        // Create Dashboard only after login
        mainPanel.add(createDashboardPanel(userField, passField), "DASHBOARD");
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    private void checkAndSendEmail() {
        java.time.LocalTime now = java.time.LocalTime.now();
        if (now.getHour() == 21 && now.getMinute() >= 55 && !emailSentToday) {
            System.out.println("Auto-Email Triggered...");
            performAutoEmail();
            emailSentToday = true;
        }
        if (now.getHour() == 0 && now.getMinute() == 0) {
            emailSentToday = false;
        }
    }

    private void performAutoEmail() {
        new Thread(() -> {
            try {
                LocalDate reportDateObj = LocalDate.now();
                String reportDate = reportDateObj.toString();

                // FIXED: Instantiate both Analytics and History
                Analytics analytics = new Analytics();
                History history = new History(); 

                List<Transaction> all = dataLoader.loadTransactions();
                
                // FIXED: Use 'history' object to filter, not 'analytics'
                List<Transaction> reportTransactions = history.filterSalesByDate(all, reportDateObj, reportDateObj);
                
                // Use 'analytics' object for calculations
                double totalSales = analytics.calculateCumulativeTotal(reportTransactions);

                String summary = "Dear Headquarters,\n\n"
                        + "Please find attached the daily sales report for " + reportDate + ".\n\n"
                        + "Summary:\n"
                        + "- Total Sales: RM " + String.format("%.2f", totalSales) + "\n"
                        + "- Report Date: " + reportDate + "\n\n"
                        + "Best Regards,\nGoldenHour System";

                String filename = "SalesReceipt/sales_" + reportDate + ".txt";
                java.io.File f = new java.io.File(filename);
                if (!f.exists()) {
                    System.out.println("No sales receipt file found for " + reportDate + " (" + filename + "). Skipping email.");
                    return;
                }

                EmailService emailService = new EmailService();
                emailService.sendDailyReport("25006144@siswa.um.edu.my", summary, f.getAbsolutePath());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void setupModernUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("Control", BG_COLOR);
            UIManager.put("nimbusBase", PRIMARY_COLOR);
            UIManager.put("nimbusBlueGrey", PRIMARY_COLOR);
            UIManager.put("nimbusFocus", ACCENT_COLOR);

            Color white = Color.WHITE;
            UIManager.put("Button.foreground", white);
            UIManager.put("Button.textForeground", white);
            
            Color black = Color.BLACK;
            UIManager.put("Label.foreground", black);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("PasswordField.background", Color.WHITE);
            UIManager.put("ComboBox.background", Color.WHITE);
            
            UIManager.put("Table.alternatingRowColor", new Color(248, 249, 250));
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
            UIManager.put("TabbedPane.selected", ACCENT_COLOR);

        } catch (Exception e) {
            // Fallback
        }
    }

    // --- DASHBOARD (Main Skeleton) ---
    private JPanel createDashboardPanel(JTextField loginUserField, JPasswordField loginPassField) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(25, 40, 25, 40));

        JLabel brandLabel = new JLabel("GOLDENHOUR SYSTEM");
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userInfoPanel.setOpaque(false);

        JLabel roleLabel = new JLabel(loggedInUser.getRole().toUpperCase());
        roleLabel.setForeground(ACCENT_COLOR);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setBorder(new EmptyBorder(0, 0, 0, 15));

        JLabel userLabel = new JLabel(loggedInUser.getName());
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        userInfoPanel.add(roleLabel);
        userInfoPanel.add(userLabel);

        headerPanel.add(brandLabel, BorderLayout.WEST);
        headerPanel.add(userInfoPanel, BorderLayout.EAST);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // 1. Attendance
        addTab(tabbedPane, "ATTENDANCE", new AttendanceTab(dataLoader, loggedInUser).createPanel());

        // 2. Stock Count
        StockCountTab stockCountTab = new StockCountTab(models, loggedInUser, this);
        addTab(tabbedPane, "STOCK COUNT", stockCountTab.createPanel());

        Runnable refreshStockUI = stockCountTab::refreshTable;

        // 3. Stock In/Out
        StockInOutTab stockInOutTab = new StockInOutTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        addTab(tabbedPane, "STOCK IN/OUT", stockInOutTab.createPanel());

        // 4. POS Sales
        SalesPanel salesTab = new SalesPanel(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        addTab(tabbedPane, "POS SALES", salesTab.createPanel());

        // 5. History (Using NEW FILE)
        HistoryPanel historyPanel = new HistoryPanel(dataLoader);
        addTab(tabbedPane, "HISTORY", historyPanel.createPanel());

        // 6. Performance (Using NEW FILE)
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            PerformancePanel perfPanel = new PerformancePanel(dataLoader, employees);
            addTab(tabbedPane, "PERFORMANCE", perfPanel.createPanel());
        }

        // 7. Edit Data
        EditTab editTab = new EditTab(models, dataLoader, this);
        addTab(tabbedPane, "EDIT DATA", editTab.createPanel());

        // 8. Search
        SearchPanel searchTab = new SearchPanel(models, outlets, dataLoader);
        addTab(tabbedPane, "SEARCH", searchTab.createPanel());

        // 9. Analytics (Using NEW FILE)
        AnalyticsPanel analyticsPanel = new AnalyticsPanel(dataLoader);
        addTab(tabbedPane, "ANALYTICS", analyticsPanel.createPanel());

        // 10. Register Staff (Using NEW FILE)
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            RegisterPanel regPanel = new RegisterPanel(dataLoader, employees, outlets, this);
            addTab(tabbedPane, "REGISTER STAFF", regPanel.createPanel());
        }

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        CustomComponents.ModernButton logout = new CustomComponents.ModernButton("LOGOUT SYSTEM", new Color(231, 76, 60), Color.WHITE);
        logout.setPreferredSize(new Dimension(150, 40));

        logout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                loginUserField.setText("");
                loginPassField.setText("");
                loggedInUser = null;
                cardLayout.show(mainPanel, "LOGIN");
            }
        });
        footerPanel.add(logout);

        JPanel mainDashboard = new JPanel(new BorderLayout());
        mainDashboard.add(headerPanel, BorderLayout.NORTH);
        mainDashboard.add(tabbedPane, BorderLayout.CENTER);
        mainDashboard.add(footerPanel, BorderLayout.SOUTH);

        return mainDashboard;
    }

    private void addTab(JTabbedPane pane, String title, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));
        wrapper.add(content);
        pane.addTab("    " + title + "    ", wrapper);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI());
    }
}