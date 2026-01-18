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
        // 1. THE WRAPPER
        // This acts as the background canvas. GridBagLayout here ensures the 
        // white form stays perfectly centered even if you maximize the window.
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        // 2. THE FORM CONTAINER (The Visual Card)
        CustomComponents.RoundedPanel formPanel = new CustomComponents.RoundedPanel(25, Color.WHITE);
        formPanel.setLayout(new GridBagLayout()); // We use GridBag inside the card too
        
        // STYLING:
        // CompoundBorder lets us have TWO borders:
        // Outer: A thin grey line (LineBorder)
        // Inner: Invisible padding (EmptyBorder) so content isn't touching the grey line.
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(30, 50, 30, 50)));

        // --- 3. LAYOUT RULES (The "GBC") ---
        // GridBagConstraints is the settings object. We change its settings, 
        // then "add" a component using those settings.
        GridBagConstraints gbc = new GridBagConstraints();
        
        // PADDING: 'insets' adds 10px of white space (Top, Left, Bottom, Right) around every cell.
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // STRETCHING: 'HORIZONTAL' means "If the column is wide, stretch the component to fill it".
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- 4. PREPARING COMPONENTS ---
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

        // (Action Listener logic omitted for brevity - same as before)
        registerBtn.addActionListener(e -> { /* ... logic ... */ });

        // --- 5. ASSEMBLING THE GRID (THE TRICKY PART) ---
        
        // -- ROW 0: THE HEADER --
        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        
        // SPANNING: This tells the layout "Merge the next 2 columns together".
        // This centers the header across the Labels AND the TextFields.
        gbc.gridwidth = 2; 
        formPanel.add(header, gbc);

        // -- ROW 1: NAME --
        gbc.gridwidth = 1; // RESET! Important: set back to 1 column width.
        
        gbc.gridx = 0; // Col 0 (Left)
        gbc.gridy = 1; // Row 1
        formPanel.add(new JLabel("Full Name:"), gbc);
        
        gbc.gridx = 1; // Col 1 (Right)
        formPanel.add(nameField, gbc); // Note: gridy is still 1 (we are on the same row)

        // -- ROW 2: ID --
        gbc.gridx = 0; 
        gbc.gridy = 2; // Move down to Row 2
        formPanel.add(new JLabel("Staff ID:"), gbc);
        
        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        // -- ROW 3: PASSWORD --
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        formPanel.add(passField, gbc);

        // -- ROW 4: ROLE --
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Role:"), gbc);
        
        gbc.gridx = 1;
        formPanel.add(roleBox, gbc);

        // -- ROW 5: OUTLET --
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Outlet:"), gbc);
        
        gbc.gridx = 1;
        formPanel.add(outletBox, gbc);

        // -- ROW 6: BUTTON --
        gbc.gridx = 0;
        gbc.gridy = 6;
        
        // SPANNING AGAIN: Make the button wide (span 2 cols) like the header.
        gbc.gridwidth = 2; 
        
        // RESIZING: 'fill = NONE' means "Don't stretch the button to full width".
        // We want the button to keep its own size (preferredSize) and just sit in the center.
        gbc.fill = GridBagConstraints.NONE; 
        
        // MARGIN: Add extra space on top (20px) so it doesn't touch the dropdown above.
        gbc.insets = new Insets(20, 10, 10, 10);
        formPanel.add(registerBtn, gbc);

        // Final Step: Add the form into the wrapper
        wrapper.add(formPanel);
        return wrapper;
    }
}