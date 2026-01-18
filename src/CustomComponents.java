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

    // --- TEXT FIELD ---
    public static class ModernTextField extends JTextField {
        public ModernTextField(String text) {
            super(text, 15);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)));
        }
    }

    // --- PASSWORD FIELD ---
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

    // --- SIMPLE BAR CHART ---
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

            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxVal == 0) maxVal = 1;

            int barWidth = (width - 2 * padding) / data.size();
            int x = padding;

            g2.setColor(Color.BLACK);
            g2.drawLine(padding, height - padding, width - padding, height - padding);

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                int barHeight = (int) ((val / maxVal) * (height - 2 * padding));

                g2.setColor(new Color(52, 152, 219));
                g2.fillRect(x + 5, height - padding - barHeight, barWidth - 10, barHeight);

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(entry.getKey());
                g2.drawString(entry.getKey(), x + (barWidth - textWidth) / 2, height - padding + 15);

                if (val > 0) {
                    String valStr = String.valueOf((int) val);
                    int valWidth = fm.stringWidth(valStr);
                    g2.drawString(valStr, x + (barWidth - valWidth) / 2, height - padding - barHeight - 5);
                }
                x += barWidth;
            }
        }
    }
}
