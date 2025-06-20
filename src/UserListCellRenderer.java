import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

public class UserListCellRenderer extends DefaultListCellRenderer {
    // 圆点大小
    private static final int DOT_SIZE = 10;
    // 在线和离线状态的颜色
    private static final Color ONLINE_COLOR = new Color(0, 180, 0);  // 绿色
    private static final Color OFFLINE_COLOR = new Color(180, 180, 180);  // 灰色
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        String text = (String) value;
        boolean isOnline = text.contains("(在线)");

        // 在线状态圆点
        JLabel dotLabel = new JLabel();
        dotLabel.setPreferredSize(new Dimension(DOT_SIZE, DOT_SIZE));
        dotLabel.setOpaque(false);

        // 使用自定义边框创建圆点
        dotLabel.setBorder(new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isOnline ? ONLINE_COLOR : OFFLINE_COLOR);
                g2.fillOval(x, y, DOT_SIZE, DOT_SIZE);
                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(0, 0, 0, 0);
            }

            @Override
            public boolean isBorderOpaque() {
                return true;
            }
        });

        // 用户名和状态
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(AppTheme.FONT_NORMAL);
        textLabel.setForeground(AppTheme.TEXT_PRIMARY);

        // 创建一个面板来放置圆点，以便垂直居中
        JPanel dotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dotPanel.setOpaque(false);
        dotPanel.add(dotLabel);

        panel.add(dotPanel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);

        if (isSelected) {
            panel.setBackground(AppTheme.PRIMARY.brighter());
            textLabel.setForeground(Color.WHITE);
        } else {
            panel.setBackground(isOnline ? new Color(240, 255, 240) : AppTheme.CARD_BACKGROUND);
        }

        return panel;
    }
}