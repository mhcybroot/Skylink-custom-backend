package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortSessionHistoryDto {
    private String hostnameOrUser;
    private String deviceCategory;
    private String ipAddress;
    private String macAddress;
    private String vlan;
    private String speed;
    private LocalDateTime connectedAt;
    private LocalDateTime releasedAt;
    private String durationFormatted;
    private String releaseReason;
    private boolean active;
}
