import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GistureFrame extends JFrame implements ActionListener {

    private JButton registerButton = new JButton("注册");
    private JPasswordField passwordField = new JPasswordField(15);
    private JPasswordField checkpasswordField = new JPasswordField(15);
    private JTextField usernameField = new JTextField(15);
    private JButton rebackLoginButton = new JButton("返回登录");


    public static void main(String[] args) {
        // 设置全局字体
        GistureFrame frame = new GistureFrame();
    }
    public GistureFrame() {
        initFrameSetup();
        initComponentsLayout();
        this.setVisible(true);
    }

    private void initFrameSetup() {
        this.setTitle("注册界面");
        test.setGlobalFont(new FontUIResource(new Font("微软雅黑", Font.PLAIN, 30)));
        this.setAlwaysOnTop(true);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);
    }

    private void initComponentsLayout() {
        // 注意：你之前直接对JFrame设置了布局，但更标准的做法是操作其内容面板
        // this.setLayout(new GridBagLayout()); // 这一行可以移除

        ImageIcon bg = new ImageIcon("1.png"); // 确保 1.png 在项目根目录或正确路径下

        // --- 背景设置 ---
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

        // --- 组件添加 ---
        // 1.用户名 Label
        JLabel usernameLabel = new JLabel("用户名:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        pan.add(usernameLabel, gbc); // 添加到内容面板 pan

        // 2.用户名输入框
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        pan.add(usernameField, gbc); // 添加到内容面板 pan

        // 3. Password Label
        JLabel passwordLabel = new JLabel("输入密码:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        pan.add(passwordLabel, gbc); // 添加到内容面板 pan

        // 4. Password 输入框
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        pan.add(passwordField, gbc); // 添加到内容面板 pan

        // 5. 确认密码
        JLabel checkpasswordLabel = new JLabel("确认密码:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        pan.add(checkpasswordLabel, gbc); // 添加到内容面板 pan

        // 6.确认密码输入框
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        pan.add(checkpasswordField, gbc); // 添加到内容面板 pan

        // --- 按钮面板和按钮样式的修改 ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        // 关键修改1：让按钮面板透明
        buttonPanel.setOpaque(false);

        // 可选修改：美化按钮样式，让它们也透明
        registerButton.setContentAreaFilled(false);
        rebackLoginButton.setContentAreaFilled(false);
        // 如果不想要边框，可以取消下面两行的注释
        // registerButton.setBorderPainted(false);
        // rebackLoginButton.setBorderPainted(false);

        buttonPanel.add(registerButton);
        buttonPanel.add(rebackLoginButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        pan.add(buttonPanel, gbc); // 添加到内容面板 pan

        // --- 监听器 ---
        registerButton.addActionListener(this);
        rebackLoginButton.addActionListener(this);

        // 关键修改2：注释掉pack()，否则它会覆盖setSize()的效果，让窗口变小
        // this.pack();
    }
    public void checkgisture() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String checkpassword = new String(checkpasswordField.getPassword());
        //判断用户名是否存在
        for (user u : user.userList) { // Assuming user.userList is accessible
            if (u.getName().equals(username)) {
                JOptionPane.showMessageDialog(this, "用户名已存在", "错误", JOptionPane.ERROR_MESSAGE);
                System.out.println("GistureFrame DEBUG: 用户名已存在");
                return;
            }
        }
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.equals(checkpassword)) {
            System.out.println("GistureFrame DEBUG: 密码一致，准备连接服务器进行注册用户: " + username);
            ChatClient tempClient = new ChatClient("RegClient-" + username); // 给临时客户端一个可识别的名字
            if (tempClient.connect(0)) { // mode 0 for registration, does not send TOLOGIN
                System.out.println("GistureFrame DEBUG: 成功连接到服务器 (tempClient)。准备发送注册命令。");
                tempClient.registerNewUser(username, password); // 这个方法现在内部会打印更多日志
                System.out.println("GistureFrame DEBUG: registerNewUser 方法已调用。客户端连接状态: " + tempClient.isConnected());
                tempClient.sendCommand("RETOLOGIN");
                JOptionPane.showMessageDialog(this, "注册请求已发送。", "提示", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                new LoginFrame().setVisible(true); // 重新创建 LoginFrame
            } else {
                System.err.println("GistureFrame DEBUG: 连接服务器失败 (tempClient)。注册中止。");
                JOptionPane.showMessageDialog(this, "无法连接到服务器进行注册。请稍后再试。", "连接错误", JOptionPane.ERROR_MESSAGE);
                // 确保用户知道注册未发生
            }
        } else {
            JOptionPane.showMessageDialog(this, "两次密码不一致", "错误", JOptionPane.ERROR_MESSAGE);
            System.out.println("GistureFrame DEBUG: 两次密码不一致。");
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == registerButton) {
            checkgisture();
        } else if (source == rebackLoginButton) {
            this.dispose();
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true); //手动返回
        }
    }

}