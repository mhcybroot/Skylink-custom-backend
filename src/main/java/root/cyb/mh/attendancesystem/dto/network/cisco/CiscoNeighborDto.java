package root.cyb.mh.attendancesystem.dto.network.cisco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiscoNeighborDto {
    private String localPort;
    private String neighborDeviceId;
    private String neighborPort;
    private String neighborSysName;
    private String protocol; // "LLDP" or "CDP"
}
