import javax.swing.*;
import java.awt.*;

public class AttendanceTab {
    
    // Logic handler for saving data to text files
    private DataLoader dataLoader;
    
    // The employee currently using the system
    private employee loggedInUser;

    // --- CONSTRUCTOR ---
    // ** CONNECTION TO GUI.JAVA **
    // 1. This class is instantiated inside 'GUI.createDashboardPanel()'.
    // 2. GUI.java passes the 'loggedInUser' (who successfully logged in) to us here.
    //    We need this because when they click "Clock In", we need to know their ID.
    public AttendanceTab(DataLoader dataLoader, employee loggedInUser) {
        this.dataLoader = dataLoader;
        this.loggedInUser = loggedInUser;
    }

    public JPanel createPanel() {
        // Simple layout: Buttons on Top (North), Text Log in Center
        JPanel panel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout());

        JButton clockIn = new JButton("Clock In");
        JButton clockOut = new JButton("Clock Out");
        
        // Area to show "Success" or "Error" messages
        JTextArea displayArea = new JTextArea();
        displayArea.setEditable(false); // User can't type here, only read
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setBackground(new Color(230, 240, 255)); // Light blue bg for better readability
        displayArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        // --- CLOCK IN LOGIC ---
        clockIn.addActionListener(e -> {
            // Call the backend to record the time
            // We use 'loggedInUser.getID()' which was passed down from GUI.java
            String result = dataLoader.clockIn(loggedInUser.getID());
            
            // Format the output message nicely
            StringBuilder sb = new StringBuilder();
            sb.append("=== Attendance Clock In ===\n\n");
            sb.append("Employee ID: ").append(loggedInUser.getID()).append("\n");
            sb.append("Name: ").append(loggedInUser.getName()).append("\n\n");
            sb.append(result);
            displayArea.setText(sb.toString());
        });

        // --- CLOCK OUT LOGIC ---
        clockOut.addActionListener(e -> {
            // Call backend to close the attendance session
            String result = dataLoader.clockOut(loggedInUser.getID());
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Attendance Clock Out ===\n\n");
            sb.append("Employee ID: ").append(loggedInUser.getID()).append("\n");
            sb.append("Name: ").append(loggedInUser.getName()).append("\n\n");
            sb.append(result);
            displayArea.setText(sb.toString());
        });

        // Add buttons to the top row
        btnPanel.add(clockIn);
        btnPanel.add(clockOut);

        // Assemble the final panel
        panel.add(btnPanel, BorderLayout.NORTH);
        // Wrap text area in ScrollPane in case the message is long
        panel.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        
        return panel;
    }
}