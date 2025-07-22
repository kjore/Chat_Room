import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Digital clock panel displaying multiple time zones
 * Updates every second and integrates with the existing chat room theme
 */
public class DigitalClockPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Time zone configurations
    private static final Map<String, ZoneId> TIME_ZONES = new HashMap<>();
    static {
        TIME_ZONES.put("本地时间", ZoneId.systemDefault());
        TIME_ZONES.put("UTC", ZoneId.of("UTC"));
        TIME_ZONES.put("美东时间(EST)", ZoneId.of("America/New_York"));
        TIME_ZONES.put("美西时间(PST)", ZoneId.of("America/Los_Angeles"));
    }
    
    private Map<String, JLabel> timeLabels;
    private Map<String, JLabel> dateLabels;
    private Timer updateTimer;
    private boolean isVisible = true;
    
    public DigitalClockPanel() {
        initializeComponents();
        setupLayout();
        startTimer();
        updateAllTimes();
    }
    
    private void initializeComponents() {
        timeLabels = new HashMap<>();
        dateLabels = new HashMap<>();
        
        // Create labels for each time zone
        for (String zoneName : TIME_ZONES.keySet()) {
            JLabel timeLabel = new JLabel("00:00:00");
            timeLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
            timeLabel.setForeground(AppTheme.PRIMARY);
            timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            JLabel dateLabel = new JLabel("0000-00-00");
            dateLabel.setFont(AppTheme.FONT_NORMAL);
            dateLabel.setForeground(AppTheme.TEXT_SECONDARY);
            dateLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            timeLabels.put(zoneName, timeLabel);
            dateLabels.put(zoneName, dateLabel);
        }
    }
    
    private void setupLayout() {
        setBackground(AppTheme.CARD_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(AppTheme.DIVIDER),
                "世界时钟",
                0, 0, AppTheme.FONT_LARGE, AppTheme.PRIMARY
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Use GridLayout for evenly spaced time zones
        setLayout(new GridLayout(1, TIME_ZONES.size(), 15, 0));
        
        // Add panels for each time zone
        for (String zoneName : TIME_ZONES.keySet()) {
            JPanel zonePanel = createTimeZonePanel(zoneName);
            add(zonePanel);
        }
    }
    
    private JPanel createTimeZonePanel(String zoneName) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        
        // Zone name label
        JLabel nameLabel = new JLabel(zoneName);
        nameLabel.setFont(AppTheme.FONT_BOLD);
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Time and date labels
        JLabel timeLabel = timeLabels.get(zoneName);
        JLabel dateLabel = dateLabels.get(zoneName);
        
        // Create a panel for time and date
        JPanel timePanel = new JPanel(new BorderLayout());
        timePanel.setOpaque(false);
        timePanel.add(timeLabel, BorderLayout.CENTER);
        timePanel.add(dateLabel, BorderLayout.SOUTH);
        
        panel.add(nameLabel, BorderLayout.NORTH);
        panel.add(timePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void startTimer() {
        updateTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAllTimes();
            }
        });
        updateTimer.start();
    }
    
    private void updateAllTimes() {
        SwingUtilities.invokeLater(() -> {
            for (Map.Entry<String, ZoneId> entry : TIME_ZONES.entrySet()) {
                String zoneName = entry.getKey();
                ZoneId zoneId = entry.getValue();
                
                ZonedDateTime now = ZonedDateTime.now(zoneId);
                
                JLabel timeLabel = timeLabels.get(zoneName);
                JLabel dateLabel = dateLabels.get(zoneName);
                
                if (timeLabel != null && dateLabel != null) {
                    timeLabel.setText(now.format(TIME_FORMAT));
                    dateLabel.setText(now.format(DATE_FORMAT));
                }
            }
        });
    }
    
    public void stopTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
    
    public void resumeTimer() {
        if (updateTimer != null && !updateTimer.isRunning()) {
            updateTimer.start();
        }
    }
    
    public boolean isClockVisible() {
        return isVisible;
    }
    
    public void setClockVisible(boolean visible) {
        this.isVisible = visible;
        setVisible(visible);
        if (visible) {
            resumeTimer();
        } else {
            stopTimer();
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 100);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 80);
    }
}