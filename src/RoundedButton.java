import javax.swing.*;
import java.awt.*;

public class RoundedButton extends JButton {
    public RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFont(AppTheme.FONT_NORMAL);
        setForeground(Color.WHITE);
        setBackground(AppTheme.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
            g2.setColor(AppTheme.PRIMARY_DARK);
        } else if (getModel().isRollover()) {
            g2.setColor(AppTheme.PRIMARY.brighter());
        } else {
            g2.setColor(AppTheme.PRIMARY);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), AppTheme.BORDER_RADIUS, AppTheme.BORDER_RADIUS);
        g2.dispose();

        super.paintComponent(g);
    }
}
