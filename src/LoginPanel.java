import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class LoginPanel {

    // ** CONNECTION TO GUI.JAVA **
    // We hold a reference to the main window (Grandparent) so we can tell it 
    // when to switch screens after a successful login.
    private GUI mainFrame;
    
    // The database of valid users (loaded from CSV by the GUI)
    private Map<String, employee> employees;

    // Constructor: Dependencies are injected here
    public LoginPanel(GUI mainFrame, Map<String, employee> employees) {
        this.mainFrame = mainFrame;
        this.employees = employees;
    }

    public JPanel createPanel() {
        // --- 1. BACKGROUND ---
        // We use a GridBagLayout on the background to perfectly center the "Card" in the middle of the screen.
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(GUI.PRIMARY_COLOR); // Dark Navy background

        // --- 2. THE LOGIN CARD ---
        // This is the white box that holds the actual inputs.
        // We use a custom rounded panel for a modern look.
        CustomComponents.RoundedPanel cardPanel = new CustomComponents.RoundedPanel(30, Color.WHITE);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(new EmptyBorder(40, 60, 40, 60)); // Padding inside the white box

        // GridBagConstraints controls where items go inside the white box
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Space between text boxes
        gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch inputs to fill width

        // --- 3. UI COMPONENTS ---
        
        // Header Text
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

        // Login Button
        CustomComponents.ModernButton loginBtn = new CustomComponents.ModernButton("LOGIN TO DASHBOARD", GUI.ACCENT_COLOR, Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(200, 45));

        // --- 4. AUTHENTICATION LOGIC ---
        // This code runs when you click the button OR press Enter
        java.awt.event.ActionListener loginAction = e -> {
            String id = userField.getText().trim();
            String pass = new String(passField.getPassword());
            
            // Check credentials against the Map loaded from CSV
            if (employees.containsKey(id) && employees.get(id).getPassword().equals(pass)) {
                
                // ** CRITICAL CONNECTION TO GUI.JAVA **
                // Login is valid! We call the method in the Main Frame.
                // This method sets the 'loggedInUser' and flips the screen to the Dashboard.
                mainFrame.onLoginSuccess(employees.get(id), userField, passField);
                
            } else {
                // Login Failed
                JOptionPane.showMessageDialog(mainFrame, "Invalid User ID or Password", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        };

        // Attach the logic to the button AND the text fields (so hitting Enter works)
        loginBtn.addActionListener(loginAction);
        userField.addActionListener(loginAction);
        passField.addActionListener(loginAction);

        // --- 5. ASSEMBLING THE GRID ---
        // Adding items row by row (gridy 0, 1, 2...)
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        cardPanel.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 10, 30, 10); // Extra gap below subtitle
        cardPanel.add(subtitle, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 10, 5, 10); // Reset gap
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
        gbc.insets = new Insets(30, 10, 10, 10); // Big gap above button
        gbc.fill = GridBagConstraints.NONE; // Don't stretch the button too wide
        cardPanel.add(loginBtn, gbc);

        // Finally, add the White Card to the Blue Background
        backgroundPanel.add(cardPanel);
        return backgroundPanel;
    }
}