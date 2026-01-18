import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Map;

public class RegisterPanel {

    private DataLoader dataLoader;
    private Map<String, employee> employees;
    private Map<String, String> outlets;
    private Component parentFrame;

    public RegisterPanel(DataLoader dataLoader, Map<String, employee> employees, Map<String, String> outlets, Component parentFrame) {
        this.dataLoader = dataLoader;
        this.employees = employees;
        this.outlets = outlets;
        this.parentFrame = parentFrame;
    }

    public JPanel createPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        CustomComponents.RoundedPanel formPanel = new CustomComponents.RoundedPanel(25, Color.WHITE);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(30, 50, 30, 50)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("REGISTER NEW STAFF");
        header.setFont(GUI.HEADER_FONT);
        header.setForeground(GUI.PRIMARY_COLOR);
        header.setHorizontalAlignment(SwingConstants.CENTER);

        CustomComponents.ModernTextField nameField = new CustomComponents.ModernTextField("");
        CustomComponents.ModernTextField idField = new CustomComponents.ModernTextField("");
        CustomComponents.ModernPasswordField passField = new CustomComponents.ModernPasswordField("");

        JComboBox<String> roleBox = new JComboBox<>(new String[] { "Part-time", "Full-time"});
        roleBox.setFont(GUI.MAIN_FONT);
        roleBox.setBackground(Color.WHITE);

        JComboBox<String> outletBox = new JComboBox<>();
        outletBox.setFont(GUI.MAIN_FONT);
        outletBox.setBackground(Color.WHITE);
        for (Map.Entry<String, String> entry : outlets.entrySet())
            outletBox.addItem(entry.getKey());

        CustomComponents.ModernButton registerBtn = new CustomComponents.ModernButton("CREATE ACCOUNT", GUI.ACCENT_COLOR, Color.WHITE);

        registerBtn.addActionListener(e -> {
            if (nameField.getText().isEmpty() || idField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame, "Please fill all fields.");
                return;
            }
            employee newEmp = new employee(idField.getText(), nameField.getText(), (String) roleBox.getSelectedItem(),
                    new String(passField.getPassword()), (String) outletBox.getSelectedItem());
            employees.put(idField.getText(), newEmp);
            dataLoader.uploadEmployeeCSV(employees);
            JOptionPane.showMessageDialog(parentFrame, "Employee successfully registered!");

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
}
