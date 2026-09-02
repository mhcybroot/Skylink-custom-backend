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
public class MikrotikSyncResultDto {
    private boolean success;
    private long durationMs;
    private int leasesFound;
    private int portsUpdated;
    private int arpEntriesFound;
    private int interfacesSynced;
    private int issuesLogged;
    private String message;
    private LocalDateTime timestamp;
}
