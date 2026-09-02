package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchdogAlertDto {
    private String id;
    private String alertType; // BANDWIDTH_SPIKE, PORT_DOWN, ROGUE_IP, LATENCY_SPIKE, OFFLINE_NODE
    private String severity; // CRITICAL, HIGH, MEDIUM, INFO
    private String severityBadgeClass; // bg-danger, bg-warning, bg-info
    private String title;
    private String message;
    private String targetDeviceOrUser;
    private String ipAddress;
    private String timestamp;
    private String actionUrl;
}
