import javax.swing.*;
import java.awt.*;


public class GroupListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        String text = (String) value;
        String groupName = text;
        String creatorInfo = "";

        if (text.contains(" (创建者: ")) {
            String[] parts = text.split(" \\(创建者: ");
            groupName = parts[0];
            creatorInfo = "创建者: " + parts[1].replace(")", "");
        }

        // 群组图标
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(30, 30));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(AppTheme.PRIMARY);
        iconLabel.setText(groupName.substring(0, 1).toUpperCase());
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setFont(AppTheme.FONT_BOLD);

        // 群组名称和创建者信息
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(groupName);
        nameLabel.setFont(AppTheme.FONT_NORMAL);
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel detailLabel = new JLabel(creatorInfo);
        detailLabel.setFont(new Font(AppTheme.FONT_NORMAL.getName(), Font.PLAIN, 12));
        detailLabel.setForeground(AppTheme.TEXT_SECONDARY);

        infoPanel.add(nameLabel);
        infoPanel.add(detailLabel);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);

        if (isSelected) {
            panel.setBackground(AppTheme.PRIMARY.brighter());
            nameLabel.setForeground(Color.WHITE);
            detailLabel.setForeground(Color.WHITE);
        } else {
            panel.setBackground(AppTheme.CARD_BACKGROUND);
        }

        return panel;
    }
}
