import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class GUI extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private employee loggedInUser;

    // Auto-Email Flag
    private boolean emailSentToday = false;

    // Services
    private CSVfile csvFile = new CSVfile();
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
        employees = csvFile.loadEmployee();
        models = dataLoader.loadModels();
        outlets = csvFile.loadOutlets();

        // Main Container Style
        mainPanel.setBackground(BG_COLOR);

        mainPanel.add(createLoginPanel(), "LOGIN");

        add(mainPanel);
        setVisible(true);

        // --- BACKGROUND TIMER ---
        Timer emailTimer = new Timer(60000, e -> checkAndSendEmail());
        emailTimer.start();
    }

    private void checkAndSendEmail() {
        java.time.LocalTime now = java.time.LocalTime.now();
        // Trigger at 9:55 PM (21:55)
        // Ensure it only sends once per day
        if (now.getHour() == 21 && now.getMinute() >= 55 && !emailSentToday) {
            System.out.println("Auto-Email Triggered...");
            performAutoEmail();
            emailSentToday = true;
        }

        // Reset flag at midnight
        if (now.getHour() == 0 && now.getMinute() == 0) {
            emailSentToday = false;
        }
    }

    private void performAutoEmail() {
        new Thread(() -> {
            try {
                // Report is for Today
                LocalDate reportDateObj = LocalDate.now();
                String reportDate = reportDateObj.toString();

                // 1. Generate Summary
                Analytics analytics = new Analytics();
                // Filter today's sales
                List<Transaction> all = dataLoader.loadTransactions();
                List<Transaction> reportTransactions = analytics.filterSalesByDate(all, reportDateObj, reportDateObj);
                double totalSales = analytics.calculateCumulativeTotal(reportTransactions);

                String summary = "Dear Headquarters,\n\n"
                        + "Please find attached the daily sales report for " + reportDate + ".\n\n"
                        + "Summary:\n"
                        + "- Total Sales: RM " + String.format("%.2f", totalSales) + "\n"
                        + "- Report Date: " + reportDate + "\n\n"
                        + "Best Regards,\nGoldenHour System";

                // 2. Locate File
                String filename = "SalesReceipt/sales_" + reportDate + ".txt";
                java.io.File f = new java.io.File(filename);
                if (!f.exists()) {
                    System.out.println(
                            "No sales receipt file found for " + reportDate + " (" + filename + "). Skipping email.");
                    return;
                }

                // 3. Send
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
            // --- GLOBAL THEME COLORS ---
            UIManager.put("Control", BG_COLOR);
            UIManager.put("nimbusBase", PRIMARY_COLOR); // Buttons Background
            UIManager.put("nimbusBlueGrey", PRIMARY_COLOR);
            UIManager.put("nimbusFocus", ACCENT_COLOR);

            // --- BUTTONS: Force WHITE text so they are visible on Dark Navy ---
            Color white = Color.WHITE;
            UIManager.put("Button.foreground", white);
            UIManager.put("Button.textForeground", white);
            UIManager.put("Button[Enabled].textForeground", white);
            UIManager.put("Button[MouseOver].textForeground", white);
            UIManager.put("Button[Pressed].textForeground", white);
            UIManager.put("Button[Focused].textForeground", white);
            UIManager.put("Button[Default].textForeground", white);

            // --- LABELS & INPUTS: Force BLACK text so they are visible on White Background
            // ---
            // This fixes the "Model" and "Enter Count" invisibility issue
            Color black = Color.BLACK;

            UIManager.put("Label.foreground", black);
            UIManager.put("Label.textForeground", black); // Nimbus specific key

            UIManager.put("TextField.foreground", black);
            UIManager.put("TextField.background", Color.WHITE);

            UIManager.put("PasswordField.foreground", black);
            UIManager.put("PasswordField.background", Color.WHITE);

            UIManager.put("ComboBox.foreground", black);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ComboBox.disabledBackground", Color.WHITE);

            // --- TABLE STYLING ---
            UIManager.put("Table.alternatingRowColor", new Color(248, 249, 250));
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
            UIManager.put("TabbedPane.selected", ACCENT_COLOR);

        } catch (Exception e) {
            // Fallback
        }
    }

    // --- HELPER: STYLE METHODS ---
    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(MAIN_FONT);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(253, 235, 208));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 45));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    // --- 1. LOGIN PANEL ---
    private JPanel createLoginPanel() {
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(PRIMARY_COLOR);

        RoundedPanel cardPanel = new RoundedPanel(30, Color.WHITE);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(new EmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header
        JLabel title = new JLabel("GOLDENHOUR");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(PRIMARY_COLOR);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Management Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(Color.GRAY);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        // Inputs
        ModernTextField userField = new ModernTextField("");
        ModernPasswordField passField = new ModernPasswordField("");

        // Button
        ModernButton loginBtn = new ModernButton("LOGIN TO DASHBOARD", ACCENT_COLOR, Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(200, 45));

        loginBtn.addActionListener(e -> {
            String id = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (employees.containsKey(id) && employees.get(id).getPassword().equals(pass)) {
                loggedInUser = employees.get(id);
                mainPanel.add(createDashboardPanel(), "DASHBOARD");
                cardLayout.show(mainPanel, "DASHBOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid User ID or Password", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Layout
        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 10, 30, 10);
        cardPanel.add(subtitle, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 10, 5, 10);
        JLabel lblUser = new JLabel("User ID");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cardPanel.add(lblUser, gbc);

        gbc.gridy = 3;
        cardPanel.add(userField, gbc);

        gbc.gridy = 4;
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cardPanel.add(lblPass, gbc);

        gbc.gridy = 5;
        cardPanel.add(passField, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(30, 10, 10, 10);
        gbc.fill = GridBagConstraints.NONE;
        cardPanel.add(loginBtn, gbc);

        backgroundPanel.add(cardPanel);
        return backgroundPanel;
    }

    // --- 2. DASHBOARD ---
    private JPanel createDashboardPanel() {
        // Header
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

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        addTab(tabbedPane, "ATTENDANCE", new AttendanceTab(dataLoader, loggedInUser).createPanel());

        StockCountTab stockCountTab = new StockCountTab(models, loggedInUser, this);
        addTab(tabbedPane, "STOCK COUNT", stockCountTab.createPanel());

        Runnable refreshStockUI = stockCountTab::refreshTable;

        StockInOutTab stockInOutTab = new StockInOutTab(models, outlets, dataLoader, loggedInUser, this,
                refreshStockUI);
        addTab(tabbedPane, "STOCK IN/OUT", stockInOutTab.createPanel());

        SalesTab salesTab = new SalesTab(models, outlets, dataLoader, loggedInUser, this, refreshStockUI);
        addTab(tabbedPane, "POS SALES", salesTab.createPanel());

        addTab(tabbedPane, "HISTORY", createSalesHistoryPanel());

        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            addTab(tabbedPane, "PERFORMANCE", createPerformancePanel());
        }

        EditTab editTab = new EditTab(models, dataLoader, this);
        addTab(tabbedPane, "EDIT DATA", editTab.createPanel());

        SearchTab searchTab = new SearchTab(models, outlets, dataLoader);
        addTab(tabbedPane, "SEARCH", searchTab.createPanel());

        addTab(tabbedPane, "ANALYTICS", createAnalyticsPanel());

        if (loggedInUser.getRole().equalsIgnoreCase("Manager")) {
            addTab(tabbedPane, "REGISTER STAFF", createRegisterPanel());
        }

        // Footer / Logout
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        ModernButton logout = new ModernButton("LOGOUT SYSTEM", new Color(231, 76, 60), Color.WHITE);
        logout.setPreferredSize(new Dimension(150, 40));

        logout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
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
        pane.addTab("   " + title + "   ", wrapper);
    }

    // --- 3. PERFORMANCE PANEL ---
    private JPanel createPerformancePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        Analytics analytics = new Analytics();

        String[] columns = { "Rank", "Employee Name", "Total Sales (RM)", "Txn Count" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        ModernButton btnGenerate = new ModernButton("GENERATE REPORT", ACCENT_COLOR, Color.WHITE);

        btnGenerate.addActionListener(e -> {
            tableModel.setRowCount(0);
            employees = csvFile.loadEmployee();

            List<Analytics.PerformanceEntry> performanceData = analytics.getEmployeePerformance();

            int rank = 1;
            for (Analytics.PerformanceEntry entry : performanceData) {
                String name = "Unknown Staff";
                String cleanID = (entry.empId != null) ? entry.empId.trim() : "";

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

                String rankStr = "RANK " + rank;
                if (rank == 1)
                    rankStr = "TOP RANK";

                tableModel.addRow(new Object[] {
                        rankStr,
                        name,
                        String.format("%.2f", entry.totalSales),
                        entry.transactionCount
                });
                rank++;
            }
        });

        JLabel header = new JLabel("STAFF PERFORMANCE METRICS");
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR);
        header.setHorizontalAlignment(JLabel.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnGenerate);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // --- 4. SALES HISTORY PANEL ---
    private JPanel createSalesHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);

        Analytics analytics = new Analytics();

        // Filter Panel
        RoundedPanel filterPanel = new RoundedPanel(15, new Color(248, 249, 250));
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JTextField startField = new ModernTextField(LocalDate.now().minusWeeks(1).toString());
        startField.setColumns(8);
        JTextField endField = new ModernTextField(LocalDate.now().toString());
        endField.setColumns(8);

        String[] sortOptions = { "Date", "Amount", "Customer" };
        JComboBox<String> sortBox = new JComboBox<>(sortOptions);
        sortBox.setFont(MAIN_FONT);
        sortBox.setBackground(Color.WHITE);

        JCheckBox ascCheck = new JCheckBox("Ascending", true);
        ascCheck.setBackground(new Color(248, 249, 250));
        ascCheck.setFont(MAIN_FONT);

        ModernButton applyBtn = new ModernButton("APPLY FILTERS", PRIMARY_COLOR, Color.WHITE);
        applyBtn.setPreferredSize(new Dimension(140, 35));

        filterPanel.add(new JLabel("Start:"));
        filterPanel.add(startField);
        filterPanel.add(new JLabel("End:"));
        filterPanel.add(endField);
        filterPanel.add(new JLabel("Sort:"));
        filterPanel.add(sortBox);
        filterPanel.add(ascCheck);
        filterPanel.add(applyBtn);

        String[] columns = { "Date", "Customer", "Model", "Qty", "Total (RM)" };
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        JLabel cumulativeLabel = new JLabel("Total Sales: RM 0.00");
        cumulativeLabel.setFont(HEADER_FONT);
        cumulativeLabel.setForeground(PRIMARY_COLOR);
        cumulativeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        applyBtn.addActionListener(e -> {
            try {
                LocalDate start = LocalDate.parse(startField.getText());
                LocalDate end = LocalDate.parse(endField.getText());

                List<Transaction> allTxns = dataLoader.loadTransactions();
                List<Transaction> filtered = analytics.filterSalesByDate(allTxns, start, end);

                analytics.sortSales(filtered, (String) sortBox.getSelectedItem(), ascCheck.isSelected());

                tableModel.setRowCount(0);
                for (Transaction t : filtered) {
                    tableModel.addRow(new Object[] {
                            t.getDate(),
                            t.getCustomerName(),
                            t.getModelName(),
                            t.getQuantity(),
                            String.format("%.2f", t.getTotalAmount())
                    });
                }
                cumulativeLabel
                        .setText(String.format("Total Sales: RM %.2f", analytics.calculateCumulativeTotal(filtered)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error processing records. Check date format (YYYY-MM-DD).");
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(cumulativeLabel, BorderLayout.SOUTH);

        return panel;
    }

    // --- 5. ANALYTICS PANEL ---
    private JPanel createAnalyticsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(15, 15));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        Analytics analytics = new Analytics();

        // 1. Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBackground(Color.WHITE);

        JLabel lblPeriod = new JLabel("Time Period:");
        lblPeriod.setFont(new Font("Segoe UI", Font.BOLD, 14));

        String[] periods = { "Today", "This Week", "This Month" };
        JComboBox<String> periodBox = new JComboBox<>(periods);
        periodBox.setFont(MAIN_FONT);
        periodBox.setBackground(Color.WHITE);

        ModernButton refreshBtn = new ModernButton("REFRESH", PRIMARY_COLOR, Color.WHITE);
        refreshBtn.setPreferredSize(new Dimension(100, 30));

        controls.add(lblPeriod);
        controls.add(periodBox);
        controls.add(refreshBtn);

        // 2. Metrics Cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setBackground(Color.WHITE);

        JPanel cardSales = createAnalyticsCard("TOTAL SALES", "RM 0.00", new Color(46, 204, 113));
        JPanel cardModel = createAnalyticsCard("TOP MODEL", "-", new Color(52, 152, 219));
        JPanel cardAvg = createAnalyticsCard("AVG DAILY", "RM 0.00", new Color(155, 89, 182));

        cardsPanel.add(cardSales);
        cardsPanel.add(cardModel);
        cardsPanel.add(cardAvg);

        // 3. Chart Area
        SimpleBarChart chart = new SimpleBarChart();
        chart.setPreferredSize(new Dimension(800, 300));
        chart.setBorder(new LineBorder(new Color(230, 230, 230), 1));

        // Action Logic
        Runnable updateData = () -> {
            String selectedPeriod = (String) periodBox.getSelectedItem();
            List<Transaction> all = dataLoader.loadTransactions();

            // Filter
            LocalDate start = analytics.getStartDateForPeriod(selectedPeriod);
            List<Transaction> filtered = analytics.filterSalesByDate(all, start, LocalDate.now());

            // Update Cards
            double total = analytics.calculateCumulativeTotal(filtered);
            String topModel = analytics.getTopSellingModelForList(filtered);
            double avg = analytics.calculateAverageDailySales(filtered, selectedPeriod);

            updateCardValue(cardSales, String.format("RM %.2f", total));
            updateCardValue(cardModel, topModel);
            updateCardValue(cardAvg, String.format("RM %.2f", avg));

            // Update Chart
            Map<String, Double> trend = analytics.getTrendData(filtered, selectedPeriod);
            chart.setData(trend);
        };

        periodBox.addActionListener(e -> updateData.run());
        refreshBtn.addActionListener(e -> updateData.run());

        // Initial Load
        updateData.run();

        wrapper.add(controls, BorderLayout.NORTH);
        wrapper.add(cardsPanel, BorderLayout.CENTER);
        wrapper.add(chart, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createAnalyticsCard(String title, String value, Color color) {
        RoundedPanel card = new RoundedPanel(20, color);
        card.setLayout(new GridLayout(2, 1));
        card.setPreferredSize(new Dimension(200, 120)); // Adjusted size
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel(title, JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(255, 255, 255, 220));

        JLabel lblValue = new JLabel(value, JLabel.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24)); // Slightly smaller font
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

    // --- CHART COMPONENT ---
    public static class SimpleBarChart extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty())
                return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 40;

            // Find max value for scaling
            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxVal == 0)
                maxVal = 1;

            int barWidth = (width - 2 * padding) / data.size();
            int x = padding;

            g2.setColor(Color.BLACK);
            g2.drawLine(padding, height - padding, width - padding, height - padding); // X-Axis

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                int barHeight = (int) ((val / maxVal) * (height - 2 * padding));

                // Draw Bar
                g2.setColor(new Color(52, 152, 219));
                g2.fillRect(x + 5, height - padding - barHeight, barWidth - 10, barHeight);

                // Draw Label
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

                // Center text
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(entry.getKey());
                g2.drawString(entry.getKey(), x + (barWidth - textWidth) / 2, height - padding + 15);

                // Draw Value
                if (val > 0) {
                    String valStr = String.valueOf((int) val);
                    int valWidth = fm.stringWidth(valStr);
                    g2.drawString(valStr, x + (barWidth - valWidth) / 2, height - padding - barHeight - 5);
                }

                x += barWidth;
            }
        }
    }

    // --- 6. REGISTER PANEL ---
    private JPanel createRegisterPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        RoundedPanel formPanel = new RoundedPanel(25, Color.WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(30, 50, 30, 50));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(30, 50, 30, 50)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("REGISTER NEW STAFF");
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR);
        header.setHorizontalAlignment(SwingConstants.CENTER);

        ModernTextField nameField = new ModernTextField("");
        ModernTextField idField = new ModernTextField("");
        ModernPasswordField passField = new ModernPasswordField("");

        JComboBox<String> roleBox = new JComboBox<>(new String[] { "Part-time", "Full-time", "Manager" });
        roleBox.setFont(MAIN_FONT);
        roleBox.setBackground(Color.WHITE);

        JComboBox<String> outletBox = new JComboBox<>();
        outletBox.setFont(MAIN_FONT);
        outletBox.setBackground(Color.WHITE);
        for (Map.Entry<String, String> entry : outlets.entrySet())
            outletBox.addItem(entry.getKey());

        ModernButton registerBtn = new ModernButton("CREATE ACCOUNT", ACCENT_COLOR, Color.WHITE);

        registerBtn.addActionListener(e -> {
            if (nameField.getText().isEmpty() || idField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }
            employee newEmp = new employee(idField.getText(), nameField.getText(), (String) roleBox.getSelectedItem(),
                    new String(passField.getPassword()), (String) outletBox.getSelectedItem());
            employees.put(idField.getText(), newEmp);
            csvFile.uploadEmployeeCSV(employees);
            JOptionPane.showMessageDialog(this, "Employee successfully registered!");

            nameField.setText("");
            idField.setText("");
            passField.setText("");
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(header, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Staff ID:"), gbc);
        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Outlet:"), gbc);
        gbc.gridx = 1;
        formPanel.add(outletBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 10, 10, 10);
        formPanel.add(registerBtn, gbc);

        wrapper.add(formPanel);
        return wrapper;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI());
    }

    // ==========================================
    // CUSTOM UI COMPONENT CLASSES (PUBLIC STATIC)
    // ==========================================

    public static class ModernButton extends JButton {
        private Color baseColor;
        private Color hoverColor;

        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.baseColor = bg;
            this.hoverColor = bg.brighter();
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverColor);
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    setBackground(baseColor);
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isPressed()) {
                g2.setColor(baseColor.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(baseColor);
            }
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class ModernTextField extends JTextField {
        public ModernTextField(String text) {
            super(text, 15);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)));
        }
    }

    public static class ModernPasswordField extends JPasswordField {
        public ModernPasswordField(String text) {
            super(text, 15);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)));
        }
    }

    public static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.cornerRadius = radius;
            this.backgroundColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }
    }
}