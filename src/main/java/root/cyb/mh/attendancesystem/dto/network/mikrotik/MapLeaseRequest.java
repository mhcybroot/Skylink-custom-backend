package root.cyb.mh.attendancesystem.dto.network.mikrotik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapLeaseRequest {
    private Long portId;
    private String ipAddress;
    private String macAddress;
    private String hostName;
    private String deviceCategory;
}
