import javax.swing.*;
import java.awt.*;

/**
 * Test to show HomePageFrame with digital clock integration
 * This simulates the integration without requiring server connection
 */
public class HomePageWithClockTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create a minimal test that shows HomePageFrame layout with clock
            JFrame testFrame = new JFrame("Chat Room with Digital Clock");
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testFrame.setLayout(new BorderLayout());
            
            // Simulate main content (simplified version of HomePageFrame content)
            JPanel mainContent = new JPanel(new BorderLayout());
            mainContent.setBackground(new Color(245, 245, 245)); // AppTheme.BACKGROUND
            
            // Header
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(64, 81, 181)); // AppTheme.PRIMARY
            headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            
            JLabel titleLabel = new JLabel("聊天应用");
            titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            
            JLabel userLabel = new JLabel("当前用户: 测试用户");
            userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            userLabel.setForeground(Color.WHITE);
            
            headerPanel.add(titleLabel, BorderLayout.WEST);
            headerPanel.add(userLabel, BorderLayout.EAST);
            
            // Content area
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBackground(new Color(245, 245, 245));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Left panel - User list simulation
            JPanel leftPanel = new JPanel();
            leftPanel.setBackground(Color.WHITE);
            leftPanel.setBorder(BorderFactory.createTitledBorder("用户列表"));
            leftPanel.setPreferredSize(new Dimension(200, 300));
            leftPanel.add(new JLabel("• 用户1 (在线)"));
            
            // Right panel - Group list simulation  
            JPanel rightPanel = new JPanel();
            rightPanel.setBackground(Color.WHITE);
            rightPanel.setBorder(BorderFactory.createTitledBorder("群组列表"));
            rightPanel.setPreferredSize(new Dimension(200, 300));
            rightPanel.add(new JLabel("• 群组1"));
            
            // Center panel - Welcome message
            JPanel centerPanel = new JPanel();
            centerPanel.setBackground(Color.WHITE);
            centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            
            JLabel welcomeLabel = new JLabel("<html><div style='text-align:center'>" +
                    "<h2>欢迎使用聊天系统</h2>" +
                    "<p>• 双击用户名开始私聊</p>" +
                    "<p>• 双击群组名称进入群聊</p>" +
                    "<p>• 查看下方的世界时钟</p>" +
                    "</div></html>");
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            centerPanel.add(welcomeLabel);
            
            contentPanel.add(leftPanel, BorderLayout.WEST);
            contentPanel.add(rightPanel, BorderLayout.EAST);
            contentPanel.add(centerPanel, BorderLayout.CENTER);
            
            mainContent.add(headerPanel, BorderLayout.NORTH);
            mainContent.add(contentPanel, BorderLayout.CENTER);
            
            // Add digital clock at the bottom
            DigitalClockPanel digitalClock = new DigitalClockPanel();
            
            testFrame.add(mainContent, BorderLayout.CENTER);
            testFrame.add(digitalClock, BorderLayout.SOUTH);
            
            testFrame.setSize(800, 600);
            testFrame.setLocationRelativeTo(null);
            testFrame.setVisible(true);
            
            // Add window listener to stop timer on close
            testFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    digitalClock.stopTimer();
                    System.exit(0);
                }
            });
        });
    }
}