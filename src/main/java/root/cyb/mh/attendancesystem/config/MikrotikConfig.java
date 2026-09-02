package root.cyb.mh.attendancesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@ConfigurationProperties(prefix = "app.network.mikrotik")
@Data
public class MikrotikConfig {

    private String host = "10.10.15.1";
    private int port = 443;
    private String username = "admin";
    private String password = "";
    private boolean useSsl = true;
    private boolean autoSyncEnabled = false;
    private String pollCron = "0 */5 * * * *";

    // Runtime state
    private LocalDateTime lastSyncTime;
    private String lastSyncStatus = "NEVER";
    private String lastSyncMessage = "No synchronization performed yet";
}
