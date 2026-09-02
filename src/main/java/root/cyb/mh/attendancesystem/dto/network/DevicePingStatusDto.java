package root.cyb.mh.attendancesystem.dto.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePingStatusDto {
    private String deviceName;
    private String ipAddress;
    private String deviceCategory; // ROUTER, SWITCH, ACCESS_POINT, WAN_GATEWAY
    private String icon;

    @JsonProperty("isOnline")
    private boolean isOnline;

    private double responseTimeMs;
    private int packetLossPercent;
    private String statusBadge; // ONLINE, DEGRADED, OFFLINE
    private String statusColor; // success, warning, danger
    @Builder.Default
    private List<Double> latencyHistory = new ArrayList<>();
    private String lastChecked;

    public boolean isOnline() {
        return isOnline;
    }

    public boolean getIsOnline() {
        return isOnline;
    }
}
