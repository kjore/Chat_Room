import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException; // 需要导入

public class LoginFrame extends JFrame implements ActionListener {
    private JCheckBox checkPassword = new JCheckBox("显示密码");
    private JPasswordField passwordField = new JPasswordField(15);
    private JButton registerButton = new JButton("注册");
    private JButton loginButton = new JButton("登录"); // loginButton 是成员变量，可以直接在 SwingWorker 中访问
    private JTextField usernameField = new JTextField(15);


    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == checkPassword) {
            if (checkPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('*');
            }
        } else if (source == registerButton) {
            this.dispose();
            GistureFrame gistureFrame = new GistureFrame();
        }
        else if (source == loginButton) {

            if (checkLoginCredentials()) {
                ChatClient chatClient = new ChatClient(usernameField.getText());

                if (chatClient.connect(0)) {                 // ① 只建立连接
                    Host host = new Host(
                            usernameField.getText(),
                            new String(passwordField.getPassword()),
                            true
                    );
                    host.setChatClient(chatClient);

                    /* ② 先创建主界面——构造器里会调用 chatClient.setMessageCallback(this) */
                    HomePageFrame homePageFrame = new HomePageFrame(host);
                    homePageFrame.setVisible(true);

                    /* ③ 回调已就绪，安全发送登录指令 */
                    chatClient.sendCommand("LOGIN:" + host.getName());

                    this.setVisible(false);                  // 关闭登录窗
                    System.out.println("登录成功");

                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "无法连接到聊天服务器",
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "用户名或密码错误",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public LoginFrame() {
        initFrameSetup();
        initComponentsLayout(); // 组件在这里初始化
        this.setVisible(true); // 窗口会立即显示
    }

    public void initFrameSetup() {
        this.setTitle("登录界面");
        test.setGlobalFont(new FontUIResource(new Font("微软雅黑", Font.PLAIN, 30)));
        this.setAlwaysOnTop(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
    }

    public void initComponentsLayout() {
        // --- 背景设置 ---
        ImageIcon bg = new ImageIcon("1.png"); // 确保 img.png 在正确路径
        JLabel label = new JLabel(bg);
        label.setBounds(0, 0, bg.getIconWidth(), bg.getIconHeight());
        this.getLayeredPane().add(label, Integer.valueOf(Integer.MIN_VALUE));

        JPanel pan = (JPanel) this.getContentPane();
        pan.setOpaque(false);
        pan.setLayout(new GridBagLayout()); // 为内容面板设置布局
        this.setSize(bg.getIconWidth(), bg.getIconHeight());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel usernameLabel = new JLabel("用户名:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        passwordField.setPreferredSize(new Dimension(250, 35)); // 设置首选宽度和高度
        pan.add(usernameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField.setPreferredSize(new Dimension(250, 35)); // 设置首选宽度和高度
        pan.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("密码:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        pan.add(passwordLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        pan.add(passwordField, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        checkPassword.setOpaque(false); // 使复选框背景透明
        checkPassword.setFont(new Font("微软雅黑", Font.PLAIN, 20)); // 设置复选框字体大小
        pan.add(checkPassword, gbc);
        checkPassword.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false); // 使按钮面板透明

        // 使按钮透明
        loginButton.setContentAreaFilled(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFont(new Font("微软雅黑", Font.PLAIN, 30)); // 调整字体大小为20
        loginButton.setFont(new Font("微软雅黑", Font.PLAIN, 30)); // 同时设置登录按钮字体
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        pan.add(buttonPanel, gbc);

        loginButton.addActionListener(this);
        registerButton.addActionListener(this);

        // pack() 会覆盖 setSize() 的效果，因此需要注释掉
        // this.pack();
    }

    public boolean checkLoginCredentials() {
        String name = usernameField.getText();
        String password = new String(passwordField.getPassword());
        // 假设 user.userList 是一个静态列表，并且已由 initUserstmp 中的后台任务填充
        if (user.userList == null) {
            JOptionPane.showMessageDialog(this, "用户信息尚未加载完成，请稍候。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        for (user u : user.userList) {
            System.out.println(u.getName()+","+u.getPassword());
            if (u.getName().equals(name) && u.getPassword().equals(password)) {
                System.out.println(name+"登录成功");
                return true;
            }
        }
        return false;
    }
}
