import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * 主界面：用户列表与群组列表，负责全局事件分发和窗口管理
 */
public class HomePageFrame extends JFrame implements ChatClient.MessageCallback {
    private final Host currentUser;

    private JList<String> userList;
    private JList<String> groupList;
    private DefaultListModel<String> userListModel;
    private DefaultListModel<String> groupListModel;

    private JButton createGroupBtn;
    private JButton joinGroupBtn;

    // === PATCH G-2 : 在 GroupChatFrame.java 中添加 ===
    private String groupId;

    // 左侧/右侧成员面板引用
    private DefaultListModel<String> memberModel;
    public void updateMembers(Group g) {
        if (g == null) return;
        SwingUtilities.invokeLater(() -> {
            memberModel.clear();
            for (String m : g.getMembers()) memberModel.addElement(m);
        });
    }

    public HomePageFrame(Host host) {
        this.currentUser = host;
        this.groupId = groupId;
        initFrameSetup();

        initComponentsLayout();
        registerCallbacks();
        setupRefreshTimer();
        pack();
        setSize(700, 500);
        setMinimumSize(new Dimension(700, 800));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initFrameSetup() {
        setTitle("主页 - 当前用户: " + currentUser.getName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(
                        HomePageFrame.this,
                        "确定要退出吗？",
                        "退出确认",
                        JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    // 执行清理操作
                    if (currentUser.getChatClient() != null) {
                        // 将耗时操作放到新线程中
                        new Thread(() -> {
                            try {
                                System.out.println("Attempting to disconnect client...");
                                currentUser.getChatClient().disconnect(1); // 假设这个方法耗时
                                System.out.println("Client disconnected.");
                            } catch (Exception ex) {
                                // 最好记录日志或通知用户断开连接时出错
                                System.err.println("Error during disconnect: " + ex.getMessage());
                                ex.printStackTrace(); // 打印堆栈信息，方便调试
                            } finally {

                                SwingUtilities.invokeLater(() -> {
                                    //     dispose(); // 释放窗口资源
                                    //     System.exit(0); // 退出程序
                                });
                                // 但通常情况下，直接调用 System.exit(0) 已经足够
                            }
                        }).start(); // 启动新线程


                    }
                    dispose(); // 释放窗口资源
                    System.out.println("Window disposed. Exiting application...");
                    System.exit(0); // 退出程序

                } else {
                    // 用户选择了 "NO"，什么也不做，窗口保持打开
                }
            }
        });
    }

    private void initComponentsLayout() {
        // 设置整体背景色
        getContentPane().setBackground(AppTheme.BACKGROUND);
        setLayout(new BorderLayout(15, 15));

        // 顶部标题栏
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(AppTheme.PRIMARY);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("聊天应用");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("当前用户: " + currentUser.getName());
        userLabel.setFont(AppTheme.FONT_NORMAL);
        userLabel.setForeground(Color.WHITE);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(userLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 主内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(AppTheme.BACKGROUND);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 用户列表面板
        JPanel userPanel = createPanelWithShadow();
        userPanel.setLayout(new BorderLayout());
        userPanel.setBorder(BorderFactory.createCompoundBorder(
                userPanel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel userTitleLabel = new JLabel("用户列表");
        userTitleLabel.setFont(AppTheme.FONT_LARGE);
        userTitleLabel.setForeground(AppTheme.PRIMARY);
        userTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = userList.getSelectedValue();
                    if (sel != null) {
                        String username = sel.split(" ")[0];
                        if (!username.equals(currentUser.getName())) {
                            openChatWithUser(username);
                        }
                    }
                }
            }
        });

        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(null);
        userScrollPane.getViewport().setBackground(Color.WHITE);

        userPanel.add(userTitleLabel, BorderLayout.NORTH);
        userPanel.add(userScrollPane, BorderLayout.CENTER);

        // 群组列表面板
        JPanel groupPanel = createPanelWithShadow();
        groupPanel.setLayout(new BorderLayout());
        groupPanel.setBorder(BorderFactory.createCompoundBorder(
                groupPanel.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel groupTitleLabel = new JLabel("群组列表");
        groupTitleLabel.setFont(AppTheme.FONT_LARGE);
        groupTitleLabel.setForeground(AppTheme.PRIMARY);
        groupTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setCellRenderer(new GroupListCellRenderer());
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null && !selectedGroup.equals("(暂无可用群组)")) {
                        String groupName = selectedGroup.split(" \\(")[0].trim();
                        String groupId = findGroupIdByName(groupName);
                        if (groupId != null) {
                            openChatWithGroup(groupId);
                        }
                    }
                }
            }
        });

        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setBorder(null);
        groupScrollPane.getViewport().setBackground(Color.WHITE);

        // 群组按钮面板
        JPanel groupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        groupButtonPanel.setOpaque(false);

        createGroupBtn = new RoundedButton("创建群聊");
        joinGroupBtn = new RoundedButton("加入群聊");

        createGroupBtn.addActionListener(e -> onCreateGroup());
        joinGroupBtn.addActionListener(e -> onJoinGroup());

        groupButtonPanel.add(createGroupBtn);
        groupButtonPanel.add(joinGroupBtn);

        groupPanel.add(groupTitleLabel, BorderLayout.NORTH);
        groupPanel.add(groupScrollPane, BorderLayout.CENTER);
        groupPanel.add(groupButtonPanel, BorderLayout.SOUTH);

        // 将用户和群组面板添加到内容面板
        contentPanel.add(userPanel, BorderLayout.WEST);
        contentPanel.add(groupPanel, BorderLayout.EAST);

        // 中间区域可以放置一个欢迎信息或者系统公告
        JPanel welcomePanel = createPanelWithShadow();
        welcomePanel.setLayout(new BorderLayout());
        welcomePanel.setBorder(BorderFactory.createCompoundBorder(
                welcomePanel.getBorder(),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel welcomeLabel = new JLabel("<html><div style='text-align:center'>" +
                "<h2>欢迎使用聊天系统</h2>" +
                "<p>• 双击用户名开始私聊</p>" +
                "<p>• 双击群组名称进入群聊</p>" +
                "<p>• 使用右侧按钮创建或加入群组</p>" +
                "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setFont(AppTheme.FONT_NORMAL);

        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        contentPanel.add(welcomePanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // 刷新列表内容
        refreshUserList();
        refreshGroupList();
    }

    // 创建带阴影效果的面板
    private JPanel createPanelWithShadow() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制阴影
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, AppTheme.BORDER_RADIUS, AppTheme.BORDER_RADIUS);

                // 绘制面板背景
                g2.setColor(AppTheme.CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, AppTheme.BORDER_RADIUS, AppTheme.BORDER_RADIUS);
                g2.dispose();
            }
        };

        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private void registerCallbacks() {
        ChatClient client = currentUser.getChatClient();
        if (client != null) {
            client.setMessageCallback(this);
        }
    }

    private JScrollPane createUserListPanel() {
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = userList.getSelectedValue();
                    if (sel != null) {
                        String username = sel.split(" ")[0];
                        if (!username.equals(currentUser.getName())) {
                            openChatWithUser(username);
                        }
                    }
                }
            }
        });
        JScrollPane scroll = new JScrollPane(userList);
        scroll.setPreferredSize(new Dimension(350, 0));
        scroll.setBorder(BorderFactory.createTitledBorder("用户列表"));
        return scroll;
    }

    private JScrollPane createGroupListPanel() {
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setVisibleRowCount(15);
        // 设置允许多选
        groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 创建一个包含列表和按钮的面板
        JPanel groupPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(groupList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // 移除内部滚动面板的边框
        groupPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton createGroupButton = new JButton("创建群聊");
        JButton joinGroupButton = new JButton("加入群聊");
        buttonPanel.add(createGroupButton);
        buttonPanel.add(joinGroupButton);
        groupPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加创建群聊按钮的事件监听
        createGroupButton.addActionListener(e -> onCreateGroup());

        // 添加加入群聊按钮的事件监听
        joinGroupButton.addActionListener(e -> onJoinGroup());

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedGroup = groupList.getSelectedValue().split(" \\(")[0].trim(); // 只获取括号前的群名
                    System.out.println("双击后获取到的群聊："+selectedGroup);
                    String selectedGroupID=null;
                    for(Group g:Group.GroupList)
                    {   System.out.println("查看群聊"+g.getGroupName());
                        if(g.getGroupName().equals(selectedGroup))
                            selectedGroupID=g.getId();
                    }
                    if (selectedGroup != null &&selectedGroupID!=null) {
                        openChatWithGroup(selectedGroupID);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(HomePageFrame.this,
                                "没有此群聊", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // 创建包含整个面板的滚动面板
        JScrollPane mainScrollPane = new JScrollPane(groupPanel);
        mainScrollPane.setPreferredSize(new Dimension(350, 0));
        mainScrollPane.setBorder(BorderFactory.createTitledBorder("群组列表"));

        return mainScrollPane;
    }

    private String findGroupIdByName(String name) {
        for (Group g : Group.GroupList) {
            if (g.getGroupName().equals(name)) return g.getId();
        }
        return null;
    }

    public void refreshUserList() {
        String sel = userList != null ? userList.getSelectedValue() : null;
        userListModel.clear();
        for (user u : user.userList) {
            if (!u.getName().equals(currentUser.getName())) {
                String s = u.getName() + (u.isStatus() ? " (在线)" : " (离线)");
                userListModel.addElement(s);
            }
        }
        if (userListModel.isEmpty()) userListModel.addElement("(暂无其他用户)");
        if (sel != null) userList.setSelectedValue(sel, true);
    }

    public void refreshGroupList() {
        String sel = groupList != null ? groupList.getSelectedValue() : null;
        groupListModel.clear();
        for (Group g : Group.GroupList) {
            if (g.isMember(currentUser.getName())) {
                String s = g.getGroupName() + " (创建者: " + g.getCreatorUsername() + ")";
                groupListModel.addElement(s);
            }
        }
        if (groupListModel.isEmpty()) groupListModel.addElement("(暂无可用群组)");
        if (sel != null) groupList.setSelectedValue(sel, true);
    }

    private void openChatWithUser(String username) {
        ChatFrame f = findChatFrame(username);
        if(f==null) {
            f = createChatFrame(username);
        }
        f.setTitle("与 " + username + " 聊天中");
        f.setVisible(true);
        f.toFront();
    }

    private void openChatWithGroup(String groupId) {
        Group target = Group.findById(groupId);
        if (target == null) return;

        // 若已有窗口，前置即可
        for (Window w : Window.getWindows()) {
            if (w instanceof GroupChatFrame gcf && gcf.getGroup().getId().equals(groupId)) {
                gcf.setVisible(true);
                gcf.toFront();
                return;
            }
        }
        // 否则新建
        GroupChatFrame gcf = new GroupChatFrame(currentUser, target,this);
        gcf.setTitle("群聊: " + target.getGroupName());
    }


    private final Map<String, ChatFrame> chatWindows = new HashMap<>();

    /**
     * 任何场合只要要放进 Map，就调用这个方法得到规范化 key
     * - 去掉空格
     * - 去掉后缀 "(在线)/(离线)" 等
     * - 一律转小写
     */
    private String normalizeUserKey(String raw) {
        String key = raw.trim();               // 首尾空格
        key = key.replaceAll("\\s*\\(.*?\\)$", ""); // 去掉 "(在线)" 之类
        return key.toLowerCase();              // 大小写归一
    }



    private ChatFrame findChatFrame(String peer) {
        final String peerKey = normalizeUserKey(peer);
        System.out.println("peerKey = " + peerKey);
        ChatFrame frame = chatWindows.get(peerKey);
        if (frame != null && frame.isDisplayable()) {
            return frame;
        }
        return null;
    }

    private ChatFrame createChatFrame(String peer) {
        final String peerKey = normalizeUserKey(peer);
        ChatFrame frame = new ChatFrame(currentUser, peerKey); // peerKey 已是纯用户名
        chatWindows.put(peerKey, frame);

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                chatWindows.remove(peerKey);
            }
        });
        return frame;
    }

    // ================= MessageCallback =================
    @Override
    public void onMessageReceived(String from, String content) {
        // 忽略自己的回显
        if (from.equalsIgnoreCase(currentUser.getName()))
            return;

        SwingUtilities.invokeLater(() -> {
            ChatFrame cf = findChatFrame(from);   // peer = 发送者
            cf.displayMessage(from, content);             // 写一条气泡
            cf.setVisible(true);
            cf.toFront();                                 // 提前台
        });
    }


    /** 创建群聊：向服务器发送 CREATE_GROUP|id|name|creator|members */
    private void onCreateGroup() {
        // 检查是否选择了用户
        int[] selectedIndices = userList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "请在用户列表中选择至少一个用户作为群成员",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String groupName = JOptionPane.showInputDialog(
                this, "请输入群聊名称：", "新建群聊", JOptionPane.PLAIN_MESSAGE);
        if (groupName == null || groupName.trim().isEmpty()) return;

        // 生成 6 位数字 ID（与服务器保持一致）
        String id = String.format("%06d", (int)(Math.random() * 1_000_000));

        // 从用户列表中获取选择的用户作为初始群成员
        String creator = currentUser.getName();
        String[] selectedUsers = userList.getSelectedValuesList().stream()
                .map(s -> s.split(" ")[0])  // 去掉状态标识，只取用户名
                .filter(name -> !name.equals(currentUser.getName()))  // 排除自己（因为creator已经是自己）
                .toArray(String[]::new);

        // 构建成员列表字符串，始终包括创建者自己
        StringBuilder membersBuilder = new StringBuilder(creator);
        for (String user : selectedUsers) {
            membersBuilder.append(",").append(user);
        }
        String members = membersBuilder.toString();    // 格式：creator,user1,user2,...
        String cmd = "CREATE_GROUP|" + id + "|" + groupName + "|" + creator + "|" + members;
        currentUser.getChatClient().sendCommand(cmd);

        // 本地立即创建一份（等待服务器广播也行，但这样 UI 会更丝滑）
        List<String> allMembers = new ArrayList<>();
        allMembers.add(creator); // 首先添加创建者
        for (String user : selectedUsers) {
            allMembers.add(user); // 添加所有选中的成员
        }
        Group g = new Group(groupName, creator, allMembers);
        g.setId(id); // 确保ID设置正确
        Group.GroupList.add(g);
        refreshGroupList();

        // 通知用户，显示群聊 ID
        JOptionPane.showMessageDialog(this,
                "<html>群聊 '" + groupName + "' 创建成功！<br><br>" +
                        "<b>群ID: " + id + "</b><br><br>" +
                        "请记住此 ID，其他用户需要通过此 ID 加入群聊。</html>",
                "成功",
                JOptionPane.INFORMATION_MESSAGE);

        // 创建后立即打开群聊窗口
        openChatWithGroup(id);
    }
    private void onJoinGroup() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel label = new JLabel("请输入6位群聊ID：");
        JTextField idField = new JTextField(10);
        idField.setFont(new Font(idField.getFont().getName(), Font.BOLD, 16));

        panel.add(label);
        panel.add(idField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "加入群聊", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String groupId = idField.getText().trim();
        if (groupId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入群聊ID", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Group grp = Group.findById(groupId);
        if (grp == null) {
            JOptionPane.showMessageDialog(this, "找不到该群聊", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (grp.isMember(currentUser.getName())) {
            JOptionPane.showMessageDialog(this,
                    "您已在群聊 '" + grp.getGroupName() + "' 中，直接打开聊天窗口",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            openChatWithGroup(groupId);
            return;
        }

        // 1) 本地加成员
        grp.addMember(currentUser.getName());
        refreshGroupList();
        Group.saveGroupsToFile("groupstmp.txt");

        // 2) 发指令给服务器
        currentUser.getChatClient().sendCommand("JOIN_GROUP|" + groupId + "|" + currentUser.getName());

        // 通知用户
        JOptionPane.showMessageDialog(this,
                "成功加入群聊 '" + grp.getGroupName() + "'！",
                "成功",
                JOptionPane.INFORMATION_MESSAGE);

        // 3) 开窗
        openChatWithGroup(groupId);
    }

    // --- PATCH 1 --- 复制到 HomePageFrame 类最末尾（其他方法之后）
    /** 把一条群聊文本写到对应 GroupChatFrame；若没开窗口就顺手打开 */
    private void handleIncomingGroupMessage(String groupId, String from, String content) {
        SwingUtilities.invokeLater(() -> {
            // 1. 找群对象
            Group target = null;
            for (Group g : Group.GroupList) if (g.getId().equals(groupId)) { target = g; break; }
            if (target == null) { System.err.println("未知群组 " + groupId); return; }

            // 2. 查找是否已有窗口
            for (Window w : Window.getWindows()) {
                if (w instanceof GroupChatFrame gcf && gcf.getGroup().getId().equals(groupId)) {
                    gcf.displayMessage(from, content);
                    gcf.setVisible(true);
                    gcf.toFront();
                    return;
                }
            }
            // 3. 新开窗口
            GroupChatFrame gcf = new GroupChatFrame(currentUser, target,this);
            gcf.setTitle("群聊: " + target.getGroupName());
            gcf.displayMessage(from, content);
            gcf.setVisible(true);
        });
    }

    /** 若有窗口正在显示该群，则刷新成员面板并弹系统提示 */
    private void syncMembersAndNotify(String groupId, String username) {
        for (Window w : Window.getWindows()) {
            if (w instanceof GroupChatFrame gcf && gcf.getGroup().getId().equals(groupId)) {
                gcf.updateMembersList();
                gcf.displayMessage("系统消息", username + " 加入了群聊");
                break;
            }
        }
    }


    @Override
    public void onGroupMessageReceived(String from, String groupId, String content) {
        // 查找对应的群聊窗口
        boolean messageHandled = false;
        for (Window window : Window.getWindows()) {
            if (window instanceof GroupChatFrame frame &&
                    frame.getGroup().getId().equals(groupId)) {
                frame.displayMessage(from, content);
                messageHandled = true;
                break;
            }
        }


    }


    @Override
    public void onGroupMemberJoined(String groupId, String groupName, String username) {
        // 1. 本地把成员加进 Group 对象
        Group grp = Group.findById(groupId);
        if (grp != null && !grp.isMember(username)) grp.addMember(username);

        // 2. 刷新左侧群组列表
        SwingUtilities.invokeLater(this::refreshGroupList);

        // 3. 如果窗口已开，刷新成员 + 系统提示
        SwingUtilities.invokeLater(() -> syncMembersAndNotify(groupId, username));
    }
    private void setupRefreshTimer() {
        // 创建一个每分钟执行一次的定时器
        Timer refreshTimer = new Timer(15000, e -> {
            refreshUserList();
            refreshGroupList();
            //System.out.println("自动刷新列表完成: " + new Date());
        });

        // 启动定时器
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        // 确保窗口关闭时停止定时器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                refreshTimer.stop();
            }
        });
    }
    /* 其余回调保持不变 */




    @Override public void onUserStatusChanged() { SwingUtilities.invokeLater(this::refreshUserList); }
    @Override public void onUserListUpdated() { SwingUtilities.invokeLater(this::refreshUserList); SwingUtilities.invokeLater(this::refreshGroupList); }

    @Override
    public void onGroupFileListReceived(String groupId, List<String> files) {
        // 转发给对应的群聊窗口
        boolean messageHandled = false;
        for (Window window : Window.getWindows()) {
            if (window instanceof GroupChatFrame frame &&
                    frame.getGroup().getId().equals(groupId)) {
                frame.onGroupFileListReceived(groupId, files);
                messageHandled = true;
                break;
            }
        }

        // 如果没有群聊窗口打开，可以添加处理逻辑或忽略
        if (!messageHandled && !files.isEmpty()) {
            Group group = Group.findById(groupId);
            if (group != null) {
                JOptionPane.showMessageDialog(this,
                        "收到" + group.getGroupName() + "群的文件列表，请打开群聊窗口查看",
                        "群文件通知",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }


}
