import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatBubblePanel extends JPanel {
    private List<ChatMessage> messages = new ArrayList<>();
    private final Color myBubbleColor = new Color(220, 220, 220);  // 浅灰色气泡
    private final Color otherBubbleColor = new Color(220, 220, 220);  // 浅灰色气泡
    // 使用统一的头像样式
    private final Color avatarBackgroundColor = new Color(240, 240, 240); // 浅灰色背景
    private final Color avatarBorderColor = new Color(200, 200, 200);     // 灰色边框
    private final Color avatarTextColor = new Color(100, 100, 100);       // 深灰色文字
    private final int MAX_BUBBLE_WIDTH = 10000;
    private final int AVATAR_SIZE = 36; // 定义头像大小为固定值确保是正圆形
    private String currentUsername; // 当前用户名变量
    // 添加新的颜色常量
    private final Color myAvatarBackgroundColor = new Color(120, 180, 220);  // 蓝色系
    private final Color otherAvatarBackgroundColor = new Color(120, 200, 120);  // 灰色系

    public ChatBubblePanel(String currentUsername) {
        this.currentUsername = currentUsername;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        // 减小面板整体内边距，使气泡看起来更紧凑
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    public void addMessage(String sender, String content, boolean isFile) {
        ChatMessage message = new ChatMessage(sender, content, new Date(), isFile);
        messages.add(message);
        refreshMessages();
    }

    private void refreshMessages() {
        removeAll();

        for (ChatMessage message : messages) {
            add(createMessagePanel(message));
            // 进一步减小气泡之间的垂直间距
            add(Box.createVerticalStrut(1));
        }

        revalidate();
        repaint();

        // 滚动到底部
        Rectangle rect = getBounds();
        scrollRectToVisible(new Rectangle(0, rect.height, 1, 1));
    }
    /**
     * 通过在每个字符后插入零宽度空格，为长字符串提供换行机会。
     * @param text 原始文本
     * @return 处理后、可安全换行的文本
     */
    private String insertBreakableText(String text) {
        // 使用 StringBuilder 以提高效率
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c);
            // 在每个字符后都添加一个零宽度空格作为潜在的断点
            // 这对于处理没有空格的超长字符串至关重要
            sb.append('\u200B');
        }
        return sb.toString();
    }
    /**
     * 手动为字符串进行字符级换行。
     * 这个方法会处理已有换行符，并对过长的、没有空格的行进行强制换行。
     * @param text 原始文本
     * @param fm FontMetrics 用来测量文本宽度
     * @param maxWidth 允许的最大宽度（像素）
     * @return 已经插入了真实换行符 '\n' 的文本
     */
    private String wrapTextByChar(String text, FontMetrics fm, int maxWidth) {
        StringBuilder result = new StringBuilder();
        // 首先按已有的换行符分割，逐行处理
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            StringBuilder currentLine = new StringBuilder();

            for (char c : line.toCharArray()) {
                currentLine.append(c);
                // 检查当前行是否已超出最大宽度
                if (fm.stringWidth(currentLine.toString()) > maxWidth) {
                    // 如果超出，将最后一个字符回退
                    currentLine.deleteCharAt(currentLine.length() - 1);
                    // 将已满的行添加到结果中，并加上换行符
                    result.append(currentLine).append("\n");
                    // 用刚刚回退的字符开始新的一行
                    currentLine = new StringBuilder().append(c);
                }
            }
            // 添加该行的最后一部分
            result.append(currentLine);

            // 如果不是原始文本的最后一行，保留其原始的换行
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        return result.toString();
    }
    private JPanel createMessagePanel(ChatMessage message) {
        boolean isMyMessage = message.getSender().equals(currentUsername);

        // [布局基础] 确保最外层使用 BorderLayout
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);

        // --- 头像创建 ---
        JLabel avatarLabel = createAvatarLabel(message.getSender(), isMyMessage);

        // --- 气泡面板创建 ---
        JPanel bubblePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bubbleColor = isMyMessage ? myBubbleColor : otherBubbleColor;
                g2.setColor(bubbleColor);
                int radius = 15;
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
                g2.dispose();
            }
        };
        bubblePanel.setLayout(new BorderLayout());
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(false);

        // --- 宽度计算 ---
        Component parent = SwingUtilities.getRoot(this);
        int parentWidth = parent != null ? parent.getWidth() : 800;
        // 使用 80% 的宽度
        final int maxBubbleWidth = Math.max((int)(parentWidth * 0.60), 200);

        // --- 消息内容处理 ---
        JComponent contentComponent;
        if (message.isFile()) {
            contentComponent = createFileComponent(message.getContent());
        } else {
            JLabel textLabel = new JLabel();
            textLabel.setForeground(Color.BLACK);
            textLabel.setFont(getFont());

            FontMetrics fm = textLabel.getFontMetrics(textLabel.getFont());
            String wrappedText = wrapTextByChar(message.getContent(), fm, maxBubbleWidth);
            String htmlText = "<html>" + wrappedText.replaceAll("\n", "<br/>") + "</html>";
            textLabel.setText(htmlText);

            contentComponent = textLabel;
        }

        bubblePanel.add(contentComponent, BorderLayout.CENTER);

        // --- 整体布局 (移除时间相关代码) ---
        JPanel completeMessagePanel = new JPanel();
        completeMessagePanel.setOpaque(false);
        completeMessagePanel.setLayout(new BoxLayout(completeMessagePanel, BoxLayout.X_AXIS));

        bubblePanel.setAlignmentY(Component.TOP_ALIGNMENT);
        avatarLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        if (isMyMessage) {
            completeMessagePanel.add(bubblePanel);
            completeMessagePanel.add(Box.createHorizontalStrut(2));
            completeMessagePanel.add(avatarLabel);
        } else {
            completeMessagePanel.add(avatarLabel);
            completeMessagePanel.add(Box.createHorizontalStrut(2));
            completeMessagePanel.add(bubblePanel);
        }

        if (isMyMessage) {
            messagePanel.add(completeMessagePanel, BorderLayout.EAST);
        } else {
            messagePanel.add(completeMessagePanel, BorderLayout.WEST);
        }

        // 防止被BoxLayout纵向拉伸
        messagePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, messagePanel.getPreferredSize().height));

        return messagePanel;
    }
    private JLabel createAvatarLabel(String username, boolean isMyMessage) {
        // 使用完整用户名而不是仅第一个字符
        String displayText = username;
        // 如果用户名太长，可以考虑截取一部分
        if (displayText.length() > 3) {
            displayText = displayText.substring(0, 3);
        }

        // 根据是否为当前用户选择不同的头像背景色
        final Color bgColor = isMyMessage ? myAvatarBackgroundColor : otherAvatarBackgroundColor;

        // 创建一个自定义的JLabel，重写paintComponent方法来先绘制背景再绘制文本
        JLabel avatarLabel = new JLabel(displayText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. 绘制圆形背景
                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());

                // 2. 绘制圆形边框
                g2.setColor(avatarBorderColor);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);

                g2.dispose();

                // 3. 调用父类的方法，让JLabel在已绘制的背景上绘制文本
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                // 确保组件是正方形
                return new Dimension(AVATAR_SIZE, AVATAR_SIZE);
            }
        };

        // 设置标签的基本属性
        avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setMinimumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setMaximumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);

        // 根据用户名长度调整字体大小
        int fontSize = displayText.length() > 2 ? 10 : 14;
        avatarLabel.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        avatarLabel.setForeground(avatarTextColor); // 设置文本颜色

        // 不再需要设置边框，因为绘制逻辑已在paintComponent中完成
        return avatarLabel;
    }
    private JComponent createFileComponent(String fileName) {
        JPanel filePanel = new JPanel(new BorderLayout(8, 0));
        filePanel.setOpaque(false);

        // 文件图标
        JLabel fileIcon = new JLabel("📎");
        fileIcon.setFont(new Font("Dialog", Font.PLAIN, 24));

        // 文件名
        JLabel fileNameLabel = new JLabel(fileName);
        fileNameLabel.setFont(AppTheme.FONT_NORMAL);

        filePanel.add(fileIcon, BorderLayout.WEST);
        filePanel.add(fileNameLabel, BorderLayout.CENTER);

        return filePanel;
    }

    private String formatTime(Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(time);
    }

    // 内部类表示聊天消息
    private static class ChatMessage {
        private final String sender;
        private final String content;
        private final Date time;
        private final boolean isFile;

        public ChatMessage(String sender, String content, Date time, boolean isFile) {
            this.sender = sender;
            this.content = content;
            this.time = time;
            this.isFile = isFile;
        }

        public String getSender() { return sender; }
        public String getContent() { return content; }
        public Date getTime() { return time; }
        public boolean isFile() { return isFile; }
    }
}
