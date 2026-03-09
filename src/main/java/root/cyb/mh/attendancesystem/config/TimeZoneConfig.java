package root.cyb.mh.attendancesystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.util.TimeZone;

/**
 * Globally sets the JVM default timezone to the office timezone
 * (America/New_York / ET).
 * This propagates automatically to all LocalDateTime.now(), LocalDate.now(),
 * Hibernate JDBC operations, and Spring @Scheduled crons.
 */
@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone:America/New_York}")
    private String appTimezone;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(appTimezone));
        System.out.println(">>> JVM Timezone set to: " + TimeZone.getDefault().getID() + " ("
                + TimeZone.getDefault().getDisplayName() + ")");
    }
}
