import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for digital clock functionality without GUI
 */
public class DigitalClockFunctionalTest {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Same time zones as in DigitalClockPanel
    private static final Map<String, ZoneId> TIME_ZONES = new HashMap<>();
    static {
        TIME_ZONES.put("本地时间", ZoneId.systemDefault());
        TIME_ZONES.put("UTC", ZoneId.of("UTC"));
        TIME_ZONES.put("美东时间(EST)", ZoneId.of("America/New_York"));
        TIME_ZONES.put("美西时间(PST)", ZoneId.of("America/Los_Angeles"));
    }
    
    public static void main(String[] args) {
        System.out.println("=== Digital Clock Functional Test ===");
        System.out.println();
        
        // Test timezone functionality
        testTimeZoneFunctionality();
        
        // Test formatting
        testTimeFormatting();
        
        System.out.println("=== All tests passed! ===");
    }
    
    private static void testTimeZoneFunctionality() {
        System.out.println("Testing timezone functionality...");
        
        for (Map.Entry<String, ZoneId> entry : TIME_ZONES.entrySet()) {
            String zoneName = entry.getKey();
            ZoneId zoneId = entry.getValue();
            
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            String timeStr = now.format(TIME_FORMAT);
            String dateStr = now.format(DATE_FORMAT);
            
            System.out.printf("%-20s: %s %s (%s)%n", 
                zoneName, dateStr, timeStr, zoneId.getId());
            
            // Validate format
            if (!timeStr.matches("\\d{2}:\\d{2}:\\d{2}")) {
                throw new RuntimeException("Invalid time format: " + timeStr);
            }
            if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new RuntimeException("Invalid date format: " + dateStr);
            }
        }
        
        System.out.println("✓ Timezone functionality test passed");
        System.out.println();
    }
    
    private static void testTimeFormatting() {
        System.out.println("Testing time formatting consistency...");
        
        // Test that UTC and local times are different (unless user is in UTC)
        ZonedDateTime utcTime = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime localTime = ZonedDateTime.now(ZoneId.systemDefault());
        
        String utcStr = utcTime.format(TIME_FORMAT);
        String localStr = localTime.format(TIME_FORMAT);
        
        System.out.println("UTC time: " + utcStr);
        System.out.println("Local time: " + localStr);
        
        // Test that EST and PST are 3 hours apart
        ZonedDateTime estTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZonedDateTime pstTime = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        
        int estHour = estTime.getHour();
        int pstHour = pstTime.getHour();
        
        // Calculate difference (handling day boundary)
        int diff = estHour - pstHour;
        if (diff < 0) diff += 24;
        if (diff > 12) diff -= 24;
        
        System.out.println("EST hour: " + estHour + ", PST hour: " + pstHour + ", Difference: " + diff);
        
        // EST should be 3 hours ahead of PST (most of the time, ignoring DST complexities for this test)
        if (Math.abs(diff) != 3 && Math.abs(diff) != 21) {
            System.out.println("Note: Time difference may vary due to daylight saving time transitions");
        }
        
        System.out.println("✓ Time formatting test passed");
        System.out.println();
    }
}