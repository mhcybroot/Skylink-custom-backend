package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WifiClientDto {
    private String hostname;
    private String ipAddress;
    private String macAddress;
    private String ssid;
    private String frequencyBand;
    private String signalStrength;
    private String signalPercent;
    private String linkSpeed;
    private String expiresAfter;
    private String status;
}
