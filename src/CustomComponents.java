import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomComponents {

    // --- BUTTON ---
    // A custom button because the default Java Swing buttons look like Windows 95.
    public static class ModernButton extends JButton {
        private Color baseColor;
        private Color hoverColor;

        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.baseColor = bg;
            this.hoverColor = bg.brighter(); // Auto-calculate a lighter version for hover effect

            // Turn off all the default ugly borders and backgrounds
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // Make mouse look like a hand

            // Mouse Listener: Simulates CSS ":hover"
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverColor);
                    repaint(); // Force a redraw immediately
                }
                public void mouseExited(MouseEvent e) {
                    setBackground(baseColor);
                    repaint();
                }
            });
        }

        // PAINTING LOGIC: This runs every time the screen updates
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            
            // "Antialiasing" = Turn on smooth edges (no pixelated jagged lines)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Pick color based on if user is clicking or hovering
            if (getModel().isPressed()) {
                g2.setColor(baseColor.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(baseColor);
            }
            
            // Draw a rounded rectangle (15px corner radius)
            // This is the actual "background" of the button
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
            g2.dispose();
            
            // Let the parent class draw the Text on top of our background
            super.paintComponent(g);
        }
    }

    // --- TEXT FIELD ---
    // Standard text box, just added padding so text isn't glued to the edges
    public static class ModernTextField extends JTextField {
        public ModernTextField(String text) {
            super(text, 15);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            // Compound Border = Outer Grey Line + Inner Whitespace Padding
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)));
        }
    }

    // --- PASSWORD FIELD ---
    // Same as above, just hides characters
    public static class ModernPasswordField extends JPasswordField {
        public ModernPasswordField(String text) {
            super(text, 15);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)));
        }
    }

    // --- ROUNDED PANEL ---
    // A container used for the stats cards. 
    // We need this because standard JPanels are always sharp rectangles.
    public static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.cornerRadius = radius;
            this.backgroundColor = bgColor;
            // IMPORTANT: setOpaque(false) makes the corners transparent.
            // If we didn't do this, you'd see ugly white/grey corners behind the rounded edge.
            setOpaque(false); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(backgroundColor);
            // Draw the rounded shape
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }
    }

    // --- SIMPLE BAR CHART ---
    // A custom component that manually draws a bar graph using Math.
    public static class SimpleBarChart extends JPanel {
        // LinkedHashMap keeps the order correct (e.g. 9am, then 10am, not random)
        private Map<String, Double> data = new LinkedHashMap<>();

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint(); // Trigger a redraw whenever new data arrives
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty())
                return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Get total canvas size
            int width = getWidth();
            int height = getHeight();
            int padding = 40; // Space for labels at bottom

            // MATH: Find the highest value in the dataset.
            // We need this to "scale" the bars. 
            // If max is 1000, then a 500 bar should be 50% height.
            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxVal == 0) maxVal = 1; // Prevent crash (division by zero)

            // Calculate how wide each bar can be to fit them all on screen
            int barWidth = (width - 2 * padding) / data.size();
            int x = padding; // Starting X position

            // Draw the bottom axis line
            g2.setColor(Color.BLACK);
            g2.drawLine(padding, height - padding, width - padding, height - padding);

            // --- DRAWING LOOP ---
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                
                // MATH: Convert Value -> Pixel Height
                // (Value / Max) gives us percentage (e.g. 0.5)
                // Multiply by available height to get pixel height
                int barHeight = (int) ((val / maxVal) * (height - 2 * padding));

                // MATH: Calculate Y Position
                // Java coordinates start at Top-Left (0,0).
                // So "Height - Padding - barHeight" means: 
                // "Go to bottom, move up the padding, then move up the height of the bar to find the top edge."
                g2.setColor(new Color(52, 152, 219));
                g2.fillRect(x + 5, height - padding - barHeight, barWidth - 10, barHeight);

                // --- TEXT LABELS ---
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

                FontMetrics fm = g2.getFontMetrics(); // Helper to measure text width
                
                // Draw Time Label (bottom)
                int textWidth = fm.stringWidth(entry.getKey());
                // Math to center the text under the bar
                g2.drawString(entry.getKey(), x + (barWidth - textWidth) / 2, height - padding + 15);

                // Draw Value Label (top of bar), only if > 0
                if (val > 0) {
                    String valStr = String.valueOf((int) val);
                    int valWidth = fm.stringWidth(valStr);
                    // Draw text slightly above the blue bar
                    g2.drawString(valStr, x + (barWidth - valWidth) / 2, height - padding - barHeight - 5);
                }
                
                // Move X pointer to the right for the next bar
                x += barWidth;
            }
        }
    }
}