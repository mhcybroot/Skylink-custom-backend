package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopBandwidthUserDto {
    private String ipAddress;
    private String macAddress;
    private String hostnameOrUser;
    private String deviceCategory;
    private String clientOs; // "Windows 11", "Android", "Apple iOS", "Linux", "Generic"
    private String switchPort; // e.g. "Switch 03 (GE15)"
    private Double currentRateMbps;
    private Double downloadRateMbps;
    private Double uploadRateMbps;
    private Long totalBytesTransferred;
    private String totalFormatted; // e.g. "14.2 GB"
    private Integer activeConnections;
}
