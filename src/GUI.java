import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// THE MAIN WINDOW (THE MOTHERBOARD)
// This class is the "Container" for the whole application.
// It manages the window frame, the login switching, and the background timer.
public class GUI extends JFrame {
    
    // LAYOUT MANAGER: CardLayout
    // Think of this like a stack of physical index cards. 
    // Card 1 = Login Screen. 
    // Card 2 = Dashboard.
    // We only show one card at a time.
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    // PUBLIC SESSION VARIABLE
    // We keep this public so other tabs (like Profile or Sales) can ask: 
    // "Who is currently logged in?" without needing to pass the name everywhere.
    public employee loggedInUser; 

    // DEBOUNCE FLAG
    // The timer ticks every minute. We use this boolean to make sure we don't 
    // accidentally send the email 60 times during the minute of 9:55 PM.
    private boolean emailSentToday = false;

    // --- SHARED DATA (SINGLE SOURCE OF TRUTH) ---
    // We load the heavy data (CSV files) ONCE here when the app starts.
    // Then we pass references of this data down to the tabs.
    // This makes the app fast because we aren't re-reading files constantly.
    private DataLoader dataLoader = new DataLoader();
    private Map<String, employee> employees;
    private Map<String, Model> models;
    private Map<String, String> outlets;

    // --- GLOBAL STYLE CONSTANTS ---
    // We define colors here so if we want to change the "Theme" later, 
    // we only change it in one place.
    public static final Color PRIMARY_COLOR = new Color(44, 62, 80); // Dark Navy
    public static final Color ACCENT_COLOR = new Color(243, 156, 18); // Golden/Orange
    public static final Color BG_COLOR = new Color(240, 243, 244); // Very Light Gray
    public static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 22);

    // --- CONSTRUCTOR ---
    // This runs exactly once when you double-click the app icon.
    public GUI() {
        // 1. UI SETUP: Make Java Swing look modern (remove the Windows 95 look)
        setupModernUI();

        setTitle("GoldenHour Management System");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close app when X is clicked
        setLocationRelativeTo(null); // Center the window on the user's monitor

        // 2. DATA LOADING: Fetch everything from the text files
        employees = dataLoader.loadEmployee();
        models = dataLoader.loadModels();
        outlets = dataLoader.loadOutlets();

        // 3. CONTAINER SETUP
        mainPanel.setBackground(BG_COLOR);

        // 4. INIT LOGIN SCREEN
        // We pass 'this' (the GUI instance) to LoginPanel.
        // Why? So LoginPanel can call 'this.onLoginSuccess()' later.
        LoginPanel loginPanel = new LoginPanel(this, employees);
        
        // Add Login as the first "Card" in the deck. Key = "LOGIN"
        mainPanel.add(loginPanel.createPanel(), "LOGIN");

        add(mainPanel);
        setVisible(true); // Show the window

        // --- BACKGROUND WORKER ---
        // Create a timer that ticks every 60,000ms (1 minute).
        // It runs the checkAndSendEmail() method to see if it's time to report to HQ.
        Timer emailTimer = new Timer(60000, e -> checkAndSendEmail());
        emailTimer.start();
    }

    // --- LOGIN BRIDGE ---
    // This method is called by LoginPanel.java when the password is correct.
    // It acts as the bridge to transition the UI from "Logged Out" to "Logged In".
    public void onLoginSuccess(employee user, JTextField userField, JPasswordField passField) {
        this.loggedInUser = user;
        
        // LAZY LOADING: We only build the Dashboard AFTER a successful login.
        // This ensures the dashboard can generate the "Welcome, [User]" text correctly.
        mainPanel.add(createDashboardPanel(userField, passField), "DASHBOARD");
        
        // FLIP THE CARD: Hide Login, Show Dashboard.
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    // --- AUTO-EMAIL LOGIC ---
    private void checkAndSendEmail() {
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // TRIGGER: It is 9:55 PM (21:55) AND we haven't sent it yet.
        if (now.getHour() == 21 && now.getMinute() >= 55 && !emailSentToday) {
            System.out.println("Auto-Email Triggered...");
            performAutoEmail();
            emailSentToday = true; // Set flag to true so we stop sending
        }
        
        // RESET: At Midnight (00:00), reset the flag so it works again tomorrow.
        if (now.getHour() == 0 && now.getMinute() == 0) {
            emailSentToday = false;
        }
    }

    // --- BACKGROUND EMAIL SENDER ---
    private void performAutoEmail() {
        // THREADING: We run this in a new Thread.
        // If we didn't, the entire app window would "freeze" for 5 seconds while sending the email.
        new Thread(() -> {
            try {
                LocalDate reportDateObj = LocalDate.now();
                String reportDate = reportDateObj.toString();

                // REUSE LOGIC: We instantiate our Analytics/History engines here 
                // to calculate the totals for the email body.
                Analytics analytics = new Analytics();
                History history = new History(); 

                List<Transaction> all = dataLoader.loadTransactions();
                
                // Filter: Get transactions for TODAY only
                List<Transaction> reportTransactions = history.filterSalesByDate(all, reportDateObj, reportDateObj);
                
                // Calculate Total Sales amount
                double totalSales = analytics.calculateCumulativeTotal(reportTransactions);

                // Construct the email text
                String summary = "Dear Headquarters,\n\n"
                        + "Please find attached the daily sales report for " + reportDate + ".\n\n"
                        + "Summary:\n"
                        + "- Total Sales: RM " + String.format("%.2f", totalSales) + "\n"
                        + "- Report Date: " + reportDate + "\n\n"
                        + "Best Regards,\nGoldenHour System";

                // Attachment: Look for the specific text file generated by SalesPanel
                String filename = "SalesReceipt/sales_" + reportDate + ".txt";
                java.io.File f = new java.io.File(filename);
                
                // Validation: Only send if the file actually exists
                if (!f.exists()) {
                    System.out.println("No sales receipt file found for " + reportDate + ". Skipping email.");
                    return;
                }

                // Call the EmailService class to handle the SMTP connection
                EmailService emailService = new EmailService();
                emailService.sendDailyReport("25006144@siswa.um.edu.my", summary, f.getAbsolutePath());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // --- UI STYLING ---
    // Sets up the "Nimbus" Look and Feel, but overrides the colors to match our Brand.
    private void setupModernUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { 
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            // Overriding standard Nimbus colors
            UIManager.put("Control", BG_COLOR);
            UIManager.put("nimbusBase", PRIMARY_COLOR);
            UIManager.put("nimbusBlueGrey", PRIMARY_COLOR);
            UIManager.put("nimbusFocus", ACCENT_COLOR); // Orange highlight on focus

            // Standardize button/label colors
            Color white = Color.WHITE;
            UIManager.put("Button.foreground", white);
            // ... (other UI properties) ...
        } catch (Exception e) {
            // Fallback to default Java look if Nimbus crashes (rare)
        }
    }

    // --- DASHBOARD BUILDER ---
    // This constructs the main application interface (Header + Tabs + Footer)
    private JPanel createDashboardPanel(JTextField loginUserField, JPasswordField loginPassField) {
        
        // 1. HEADER PANEL (BorderLayout.NORTH)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(25, 40, 25, 40)); // Padding

        // Brand Name (Left Side)
        JLabel brandLabel = new JLabel("GOLDENHOUR SYSTEM");
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        // User Profile Info (Right Side)
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userInfoPanel.setOpaque(false); // Make transparent to show blue header background

        // Show Role (e.g., MANAGER) in Gold/Orange
        JLabel roleLabel = new JLabel(loggedInUser.getRole().toUpperCase());
        roleLabel.setForeground(ACCENT_COLOR);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setBorder(new EmptyBorder(0, 0, 0, 15)); // Gap between role and name

        // Show Name in White
        JLabel userLabel = new JLabel(loggedInUser.getName());
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        userInfoPanel.add(roleLabel);
        userInfoPanel.add(userLabel);
        headerPanel.add(brandLabel, BorderLayout.WEST);
        headerPanel.add(userInfoPanel, BorderLayout.EAST);

        // 2. MAIN TABS (BorderLayout.CENTER)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // --- DEPENDENCY INJECTION ---
        // This is where we plug everything together.
        // We create each Tab's class and pass it the data it needs (dataLoader, user, etc).

        // Tab 1: Attendance
        addTab(tabbedPane, "ATTENDANCE", new AttendanceTab(dataLoader, loggedInUser).createPanel());

        // Tab 2: Stock Count
        StockCountTab stockCountTab = new StockCountTab(models, loggedInUser, this);
        addTab(tabbedPane, "STOCK COUNT", stockCountTab.createPanel());

        // CALLBACK: Creates a function we can pass to other tabs.
        // If SalesTab makes a sale, it calls this to update the StockTab immediately.
        Runnable refreshStockUI = stockCountTab::refreshTable;

        // Tab 3: Stock In/Out
        StockInOutTab stockInOutTab = new StockInOutTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        addTab(tabbedPane, "STOCK IN/OUT", stockInOutTab.createPanel());

        // Tab 4: POS Sales
        SalesPanel salesTab = new SalesPanel(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        addTab(tabbedPane, "POS SALES", salesTab.createPanel());

        // Tab 5: History
        HistoryPanel historyPanel = new HistoryPanel(dataLoader);
        addTab(tabbedPane, "HISTORY", historyPanel.createPanel());

        // Tab 6: Performance (SECURITY CHECK: Only Managers see this)
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            PerformancePanel perfPanel = new PerformancePanel(dataLoader, employees);
            addTab(tabbedPane, "PERFORMANCE", perfPanel.createPanel());
        }

        // Tab 7: Edit Data (Manual Corrections)
        EditTab editTab = new EditTab(models, dataLoader, this);
        addTab(tabbedPane, "EDIT DATA", editTab.createPanel());

        // Tab 8: Search
        SearchPanel searchTab = new SearchPanel(models, outlets, dataLoader);
        addTab(tabbedPane, "SEARCH", searchTab.createPanel());

        // Tab 9: Analytics (Visual Graphs)
        AnalyticsPanel analyticsPanel = new AnalyticsPanel(dataLoader);
        addTab(tabbedPane, "ANALYTICS", analyticsPanel.createPanel());

        // Tab 10: Register Staff (Managers Only)
        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            RegisterPanel regPanel = new RegisterPanel(dataLoader, employees, outlets, this);
            addTab(tabbedPane, "REGISTER STAFF", regPanel.createPanel());
        }

        // 3. FOOTER PANEL (BorderLayout.SOUTH)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Logout Button
        CustomComponents.ModernButton logout = new CustomComponents.ModernButton("LOGOUT SYSTEM", new Color(231, 76, 60), Color.WHITE);
        logout.setPreferredSize(new Dimension(150, 40));

        // LOGOUT LOGIC
        logout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // Clear the password fields so next user doesn't see them
                loginUserField.setText("");
                loginPassField.setText("");
                loggedInUser = null; // Destroy session
                
                // Flip the Card back to the Login Screen
                cardLayout.show(mainPanel, "LOGIN"); 
            }
        });
        footerPanel.add(logout);

        // FINAL ASSEMBLY
        JPanel mainDashboard = new JPanel(new BorderLayout());
        mainDashboard.add(headerPanel, BorderLayout.NORTH);   // Top
        mainDashboard.add(tabbedPane, BorderLayout.CENTER);   // Middle
        mainDashboard.add(footerPanel, BorderLayout.SOUTH);   // Bottom

        return mainDashboard;
    }

    // HELPER: Adds a consistent border/padding to every tab content
    private void addTab(JTabbedPane pane, String title, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20)); // White space around the edges
        wrapper.add(content);
        pane.addTab("    " + title + "    ", wrapper); // Spaces in title for wider tabs
    }

    // MAIN ENTRY POINT
    // This is where Java starts the application.
    public static void main(String[] args) {
        // SwingUtilities.invokeLater ensures thread safety (standard Java GUI practice)
        SwingUtilities.invokeLater(() -> new GUI());
    }
}