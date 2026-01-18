import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class LoginPanel {

    private GUI mainFrame;
    private Map<String, employee> employees;

    public LoginPanel(GUI mainFrame, Map<String, employee> employees) {
        this.mainFrame = mainFrame;
        this.employees = employees;
    }

    public JPanel createPanel() {
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(GUI.PRIMARY_COLOR);

        CustomComponents.RoundedPanel cardPanel = new CustomComponents.RoundedPanel(30, Color.WHITE);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(new EmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header
        JLabel title = new JLabel("GOLDENHOUR");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(GUI.PRIMARY_COLOR);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Management Portal");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(Color.GRAY);
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        // Inputs
        CustomComponents.ModernTextField userField = new CustomComponents.ModernTextField("");
        CustomComponents.ModernPasswordField passField = new CustomComponents.ModernPasswordField("");

        // Button
        CustomComponents.ModernButton loginBtn = new CustomComponents.ModernButton("LOGIN TO DASHBOARD", GUI.ACCENT_COLOR, Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(200, 45));

        // Shared Login Logic
        java.awt.event.ActionListener loginAction = e -> {
            String id = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (employees.containsKey(id) && employees.get(id).getPassword().equals(pass)) {
                // Call method in main GUI to switch screens
                mainFrame.onLoginSuccess(employees.get(id), userField, passField);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Invalid User ID or Password", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        };

        loginBtn.addActionListener(loginAction);
        userField.addActionListener(loginAction);
        passField.addActionListener(loginAction);

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
}