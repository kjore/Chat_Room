import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * 群聊窗口，采用现代化UI设计，支持气泡聊天、文件传输和成员列表。
 */
public class GroupChatFrame extends JFrame
        implements ActionListener, ChatClient.MessageCallback {

    private final Host currentUser;
    private final Group group;

    // --- UI 组件（将主要面板提升为成员变量） ---
    private JPanel headerPanel;
    private JSplitPane splitPane;
    private JPanel inputPanel;
    private ChatBubblePanel chatBubblePanel;
    private JScrollPane chatScrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private final JButton uploadBtn = new JButton("上传文件");
    private final JButton downloadBtn = new JButton("群文件列表");
    private JButton backButton;
    private JList<String> membersList;
    private DefaultListModel<String> membersModel;

    private int unreadCount = 0;
    private boolean isFrameActive = false;


    public GroupChatFrame(Host currentUser, Group group) {
        this.currentUser = currentUser;
        this.group = group;

        initFrameSetup();
        initComponents();
        applyModernStyle();

        // 注册回调，接收群文件列表、消息等
        currentUser.getChatClient().setMessageCallback(this);

        setSize(1000, 800);
        setLocationRelativeTo(null);
        setVisible(true);
        setupActivityListener();
        // 确保每次打开群聊窗口都重新注册回调
        if (currentUser.getChatClient() != null) {
            System.out.println("为群聊窗口[" + group.getGroupName() + "]设置消息回调");
            currentUser.getChatClient().setMessageCallback(this);
        }
    }

    public Group getGroup() {
        return this.group;
    }


    public void displayMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            chatBubblePanel.addMessage(sender, content, false);

            // 如果窗口不是当前活动窗口，增加未读消息计数
            if (!isFrameActive) {
                unreadCount++;
                updateTitle();

                // 可以添加音效提示（可选）
                Toolkit.getDefaultToolkit().beep();
            }
        });
    }

    private void initFrameSetup() {
        setTitle("群聊: " + group.getGroupName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));

        // --- 顶部标题栏 ---
        headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("  " + group.getGroupName() + " (ID: " + group.getId() + ")");
        backButton = new JButton("返回");
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(backButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- 中部：聊天记录 + 成员列表 ---
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setDividerSize(8);
        splitPane.setContinuousLayout(true);

        // 左侧：聊天气泡面板
        chatBubblePanel = new ChatBubblePanel(currentUser.getName());
        chatScrollPane = new JScrollPane(chatBubblePanel);
        splitPane.setLeftComponent(chatScrollPane);
        // 右侧：成员列表
        membersModel = new DefaultListModel<>();
        updateMembersList();
        membersList = new JList<>(membersModel);
        membersList.setCellRenderer(new MemberCellRenderer());
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel membersLabel = new JLabel("群成员", SwingConstants.CENTER);
        rightPanel.add(membersLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(membersList), BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // --- 底部：输入框 + 按钮 ---
        inputPanel = new JPanel(new BorderLayout(10, 0));
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sendButton = new JButton("发送");
        buttonPanel.add(uploadBtn);
        buttonPanel.add(downloadBtn);
        buttonPanel.add(sendButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // --- 事件绑定 ---
        sendButton.addActionListener(this);
        backButton.addActionListener(this);
        messageField.addActionListener(this);
        uploadBtn.addActionListener(this);
        downloadBtn.addActionListener(this);
    }

    // 添加窗口监听器来检测窗口是否处于活动状态
    private void setupActivityListener() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                isFrameActive = true;
                resetUnreadCounter();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                isFrameActive = false;
            }
        });
    }

    // 重置未读消息计数并更新标题
    private void resetUnreadCounter() {
        if (unreadCount > 0) {
            unreadCount = 0;
            updateTitle();
        }
    }

    // 更新窗口标题以反映未读消息状态
    private void updateTitle() {
        if (unreadCount > 0) {
            setTitle("群聊: " + group.getGroupName() + " (" + unreadCount + "条新消息)");
        } else {
            setTitle("群聊: " + group.getGroupName());
        }
    }
    private void applyModernStyle() {
        // --- 整体背景和边距 ---
        getContentPane().setBackground(AppTheme.BACKGROUND);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- 标题栏 (直接使用成员变量) ---
        headerPanel.setBackground(AppTheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.getComponent(0).setFont(AppTheme.FONT_LARGE); // titleLabel
        headerPanel.getComponent(0).setForeground(Color.WHITE);
        backButton.setFont(AppTheme.FONT_NORMAL);
        backButton.setForeground(Color.WHITE);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);

        // --- 分割面板 (直接使用成员变量) ---
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // --- 聊天区 ---
        chatScrollPane.setBorder(new RoundedBorder(8, AppTheme.DIVIDER));
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);

        // --- 成员列表区 ---
        JPanel rightPanel = (JPanel) splitPane.getRightComponent();
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        JLabel membersLabel = (JLabel) rightPanel.getComponent(0);
        membersLabel.setFont(AppTheme.FONT_LARGE);
        membersLabel.setForeground(AppTheme.TEXT_PRIMARY);
        membersLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JScrollPane membersScrollPane = (JScrollPane) rightPanel.getComponent(1);
        membersScrollPane.setOpaque(false);
        membersScrollPane.getViewport().setOpaque(false);
        membersScrollPane.setBorder(new RoundedBorder(8, AppTheme.DIVIDER));
        membersList.setBackground(AppTheme.BACKGROUND);

        // --- 输入区 (直接使用成员变量) ---
        inputPanel.setOpaque(false);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        messageField.setFont(AppTheme.FONT_NORMAL);
        messageField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, AppTheme.DIVIDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // --- 按钮 ---
        styleButtonAsRounded(sendButton);
        styleButtonAsRounded(uploadBtn);
        styleButtonAsRounded(downloadBtn);
    }

    private void styleButtonAsRounded(JButton button) {
        button.setContentAreaFilled(false);
        button.setFont(AppTheme.FONT_NORMAL);
        button.setForeground(Color.WHITE);
        button.setBackground(AppTheme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setUI(new BasicButtonUI() {
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

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == sendButton || src == messageField) {
            sendMessage();
        } else if (src == uploadBtn) {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentUser.getChatClient().uploadGroupFile(group.getId(), fc.getSelectedFile());
            }
        } else if (src == downloadBtn) {
            currentUser.getChatClient().sendCommand("GROUP_FILE_LIST_REQUEST|" + group.getId());
        } else if (src == backButton) {
            setVisible(false);
        }
    }

    private void sendMessage() {
        String txt = messageField.getText().trim();
        if (txt.isEmpty()) return;
        currentUser.getChatClient().sendCommand("GROUP_MSG|" + group.getId() + "|" + txt);
        messageField.setText("");
        messageField.requestFocusInWindow();
    }

    @Override
    public void onGroupMessageReceived(String from, String groupId, String content) {
        if (this.group.getId().equals(groupId)) {
            displayMessage(from, content);
        }
    }

    @Override
    public void onGroupFileListReceived(String groupId, List<String> files) {
        if (!this.group.getId().equals(groupId)) return;
        SwingUtilities.invokeLater(() -> {
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "群里当前没有文件。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String choice = (String) JOptionPane.showInputDialog(this, "请选择要下载的文件：", "下载群文件",
                    JOptionPane.PLAIN_MESSAGE, null, files.toArray(new String[0]), files.get(0));
            if (choice != null) {
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File(choice));
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File dest = fc.getSelectedFile();
                    new Thread(() -> {
                        try {
                            FileTransferManager.downloadGroup(groupId, choice, dest);
                            JOptionPane.showMessageDialog(this, "下载完成：" + dest.getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "下载失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }).start();
                }
            }
        });
    }

    @Override
    public void onMessageReceived(String from, String content) {}

    @Override
    public void onUserStatusChanged() {
        updateMembersList();
    }

    @Override
    public void onUserListUpdated() {
        updateMembersList();
    }

    // 在GroupChatFrame的onGroupMemberJoined方法中添加
    @Override
    public void onGroupMemberJoined(String groupId, String groupName, String username) {
        System.out.println("群聊窗口收到成员加入通知: " + groupId + " - " + username);

        if (this.group.getId().equals(groupId)) {
            // 确保本地Group对象与服务器同步
            if (!this.group.isMember(username)) {
                this.group.addMember(username);
                Group.saveGroupsToFile("groupstmp.txt"); // 保存到本地
            }

            // 更新UI
            SwingUtilities.invokeLater(this::updateMembersList);

            // 添加系统消息提示
            SwingUtilities.invokeLater(() ->
                    chatBubblePanel.addMessage("系统", username + "加入了群聊", true));
        }
    }

    public void updateMembersList() {
        SwingUtilities.invokeLater(() -> {
            membersModel.clear();
            for (String m : group.getMembers()) {
                membersModel.addElement(m);
            }
        });
    }
    // 修改现有的displayMessage方法

    private class MemberCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int idx, boolean sel, boolean foc) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, foc);
            String name = value.toString();
            boolean online = user.userList.stream().anyMatch(u -> u.getName().equals(name) && u.isStatus());
            lbl.setIcon(createIcon(online ? Color.GREEN : Color.GRAY, 10));
            lbl.setText(name + (name.equals(group.getCreatorUsername()) ? " (群主)" : ""));
            lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            lbl.setOpaque(false);
            if (sel) {
                lbl.setOpaque(true);
                lbl.setBackground(AppTheme.PRIMARY.brighter());
                lbl.setForeground(Color.WHITE);
            }
            return lbl;
        }

        private Icon createIcon(Color c, int d) {
            return new Icon() {
                public int getIconWidth() { return d; }
                public int getIconHeight() { return d; }
                public void paintIcon(Component cmp, Graphics g, int x, int y) {
                    g.setColor(c);
                    g.fillOval(x, y, d, d);
                }
            };
        }
    }
}
