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
public class NetworkDeviceTelemetryDto {
    private Long deviceId;
    private String deviceName;
    private String deviceIp;
    private String deviceType;
    private Integer cpuUsagePercent;
    private Long freeMemoryBytes;
    private Long totalMemoryBytes;
    private Integer memoryUsagePercent;
    private Double temperatureCelsius;
    private Double voltage;
    private String fanStatus; // "NORMAL", "WARNING", "FAILED", "N/A"
    private String powerSupplyStatus; // "NORMAL", "REDUNDANT_ACTIVE", "FAILED", "N/A"
    private String uptimeFormatted;
    private String firmwareVersion;
    
    // PoE Specific Telemetry
    private Boolean supportsPoe;
    private Double poeTotalPowerWatts;
    private Double poeUsedPowerWatts;
    private Double poeAvailablePowerWatts;
    private Integer poeUsagePercent;
    
    private LocalDateTime polledAt;
}
