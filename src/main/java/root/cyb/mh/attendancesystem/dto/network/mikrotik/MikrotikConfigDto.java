package root.cyb.mh.attendancesystem.dto.network.mikrotik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikConfigDto {
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean useSsl;
    private boolean autoSyncEnabled;
    private String pollIntervalCron;
    private LocalDateTime lastSyncTime;
    private String lastSyncStatus;
    private String lastSyncMessage;
}
