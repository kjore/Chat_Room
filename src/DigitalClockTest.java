import javax.swing.*;
import java.awt.*;

/**
 * Simple test to display the DigitalClockPanel standalone
 */
public class DigitalClockTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame testFrame = new JFrame("Digital Clock Test");
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            DigitalClockPanel clockPanel = new DigitalClockPanel();
            
            testFrame.add(clockPanel, BorderLayout.CENTER);
            testFrame.pack();
            testFrame.setLocationRelativeTo(null);
            testFrame.setVisible(true);
            
            // Add window listener to stop timer on close
            testFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    clockPanel.stopTimer();
                    System.exit(0);
                }
            });
        });
    }
}