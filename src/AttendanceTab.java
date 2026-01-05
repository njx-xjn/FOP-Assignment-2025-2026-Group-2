
import javax.swing.*;
import java.awt.*;

public class AttendanceTab {
    private DataLoader dataLoader;
    private employee loggedInUser;

    public AttendanceTab(DataLoader dataLoader, employee loggedInUser) {
        this.dataLoader = dataLoader;
        this.loggedInUser = loggedInUser;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout());

        JButton clockIn = new JButton("Clock In");
        JButton clockOut = new JButton("Clock Out");
        JTextArea displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setBackground(new Color(230, 240, 255)); // Light blue bg like image
        displayArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        clockIn.addActionListener(e -> {
            String result = dataLoader.clockIn(loggedInUser.getID());
            StringBuilder sb = new StringBuilder();
            sb.append("=== Attendance Clock In ===\n\n");
            sb.append("Employee ID: ").append(loggedInUser.getID()).append("\n");
            sb.append("Name: ").append(loggedInUser.getName()).append("\n\n");

            if (result.startsWith("Error") || result.startsWith("Already")) {
                sb.append(result);
            } else {
                sb.append(result); // Result contains "Clock In Successful!..."
            }
            displayArea.setText(sb.toString());
        });

        clockOut.addActionListener(e -> {
            String result = dataLoader.clockOut(loggedInUser.getID());
            StringBuilder sb = new StringBuilder();
            sb.append("=== Attendance Clock Out ===\n\n");
            sb.append("Employee ID: ").append(loggedInUser.getID()).append("\n");
            sb.append("Name: ").append(loggedInUser.getName()).append("\n\n");

            sb.append(result);
            displayArea.setText(sb.toString());
        });

        btnPanel.add(clockIn);
        btnPanel.add(clockOut);

        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        return panel;
    }
}
