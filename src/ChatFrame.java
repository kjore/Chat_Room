import chatclient.logger.MessageLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 私聊窗口（支持本地历史记录回放 + 文件发送）
 * 依赖：
 *   - Host              当前登录用户对象（封装 ChatClient）
 *   - MessageLogger     用于读写本地聊天记录
 *   - ChatClient        提供 sendMessage/sendFileToUser 接口
 */
public class ChatFrame extends JFrame implements ActionListener {



    /* ===== UI 组件 ===== */
    // private final JTextArea chatArea = new JTextArea(); // 删除这行
    private final ChatBubblePanel chatBubblePanel; // 替换为气泡面板
    private final JScrollPane scrollPane; // 添加滚动面板
    private final JTextField messageField = new JTextField(30);
    private final JButton sendButton = new JButton("发送");
    private final JButton fileButton = new JButton("📎 文件");
    private final JButton backButton = new JButton("返回");

    // 添加消息去重所需的数据结构
    private final Map<String, Long> recentMessages = new HashMap<>();
    private static final long DEDUP_WINDOW = 1000; // 1秒去重窗口

    /* ===== 会话信息 ===== */
    private final Host currentUser;
    private final String peerId;
    private final String loginUser;

    public ChatFrame(Host host, String chatWithUser) {
        this.currentUser = host;
        this.loginUser = host.getName();
        this.peerId = chatWithUser;

        // 初始化气泡面板和滚动面板
        chatBubblePanel = new ChatBubblePanel(loginUser);
        scrollPane = new JScrollPane(chatBubblePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        initFrameSetup();
        initComponentsLayout();
        loadHistory();

        pack();
        setSize(700, 800);
        setMinimumSize(new Dimension(700, 800));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** 基础窗口属性 */
    private void initFrameSetup() {
        setTitle("与 " + peerId + " 聊天中");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    /** 构建 UI 布局 */
    private void initComponentsLayout() {
        setLayout(new BorderLayout(10, 10));

        // 添加滚动面板(包含气泡面板)
        add(scrollPane, BorderLayout.CENTER);

        // 输入与按钮区
        JPanel south = new JPanel(new BorderLayout(5, 5));

        // 回退按钮
        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        westPanel.add(backButton);
        south.add(westPanel, BorderLayout.WEST);

        // 文本输入框
        south.add(messageField, BorderLayout.CENTER);

        // 发送与文件按钮
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        eastPanel.add(fileButton);
        eastPanel.add(sendButton);
        south.add(eastPanel, BorderLayout.EAST);

        south.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(south, BorderLayout.SOUTH);

        // 注册监听
        sendButton.addActionListener(this);
        fileButton.addActionListener(this);
        backButton.addActionListener(this);
        messageField.addActionListener(this);
        applyModernStyle();
    }

    private void applyModernStyle() {
        // 设置整体背景色
        getContentPane().setBackground(AppTheme.BACKGROUND);

        // 美化滚动面板 - 使用已有的scrollPane成员变量
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new RoundedBorder(8, AppTheme.DIVIDER));

        // 删除这段重复创建scrollPane的代码
        // JScrollPane scrollPane = new JScrollPane(chatArea) {...};

        // 美化输入区域
        messageField.setFont(AppTheme.FONT_NORMAL);
        messageField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 美化按钮 - 修改现有按钮样式而非重新创建
        styleButtonAsRounded(sendButton);
        styleButtonAsRounded(fileButton);

        // 返回按钮使用不同样式
        backButton.setFont(AppTheme.FONT_NORMAL);
        backButton.setForeground(AppTheme.PRIMARY);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);

        // 重新布局UI组件
        setLayout(new BorderLayout(15, 15));

        // 添加顶部标题栏
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppTheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("与 " + peerId + " 聊天中");
        titleLabel.setFont(AppTheme.FONT_LARGE);
        titleLabel.setForeground(Color.WHITE);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(backButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 添加聊天区域
        JPanel chatPanel = new JPanel(new BorderLayout(0, 15));
        chatPanel.setOpaque(false);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chatPanel.add(scrollPane, BorderLayout.CENTER);  // 使用已有的scrollPane成员变量

        // 添加输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.CENTER);
    }
    // 添加辅助方法来设置按钮的圆角风格
    private void styleButtonAsRounded(JButton button) {
        button.setContentAreaFilled(false);
        button.setFont(AppTheme.FONT_NORMAL);
        button.setForeground(Color.WHITE);
        button.setBackground(AppTheme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // 添加自定义绘制
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                AbstractButton b = (AbstractButton) c;
                if (b.getModel().isPressed()) {
                    g2.setColor(AppTheme.PRIMARY_DARK);
                } else if (b.getModel().isRollover()) {
                    g2.setColor(AppTheme.PRIMARY.brighter());
                } else {
                    g2.setColor(AppTheme.PRIMARY);
                }

                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), AppTheme.BORDER_RADIUS, AppTheme.BORDER_RADIUS);
                g2.dispose();

                super.paint(g, c);
            }
        });
    }

    private void loadHistory() {
        // 记录加载历史记录的时间
        final long loadTime = System.currentTimeMillis();
        // 定义时间窗口（例如1秒）
        final long timeWindow = 10000;

        MessageLogger.replay(loginUser, peerId, line -> {
            // 提取时间戳
            int timestampEnd = line.indexOf("] ");
            if (timestampEnd > 0) {
                String timestampStr = line.substring(1, timestampEnd);
                try {
                    // 尝试解析时间戳（假设格式是毫秒时间戳或其他可解析格式）
                    long msgTime = Long.parseLong(timestampStr);

                    // 如果消息时间太接近加载时间，可能是刚刚通过回调显示的消息，跳过
                    if (loadTime - msgTime < timeWindow) {
                        return;
                    }
                } catch (NumberFormatException e) {
                    // 时间戳格式不是数字，忽略错误继续处理
                }

                String messageContent = line.substring(timestampEnd + 2);
                String[] parts = messageContent.split(" : ", 2);
                if (parts.length == 2) {
                    String sender = parts[0].trim();
                    String content = parts[1].trim();
                    chatBubblePanel.addMessage(sender, content, false);
                }
            }
        });
    }


    /** 发送文本消息 */
    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;
        if (currentUser != null && currentUser.getChatClient() != null) {
            currentUser.getChatClient().sendMessage(peerId, msg);
            displayMessage(loginUser, msg);
        }
        messageField.setText("");
        messageField.requestFocusInWindow();
    }

    /** 添加一行到聊天区 */
    /** 显示消息 - 修改为使用气泡面板 */
    public void displayMessage(String sender, String content)
    {
        chatBubblePanel.addMessage(sender, content, false);
    }


    /** 事件处理 */
    @Override public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == sendButton || src == messageField) {
            sendMessage();
        }
        else if (src == fileButton) {
            // 弹出文件选择
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                // 调用 ChatClient 的文件传输接口
                currentUser.getChatClient().sendFileToUser(peerId, f);
                System.out.println("向"+peerId+"发送了一文件");
            }
        }
        else if (src == backButton) {
            setVisible(false);
        }
    }
}
