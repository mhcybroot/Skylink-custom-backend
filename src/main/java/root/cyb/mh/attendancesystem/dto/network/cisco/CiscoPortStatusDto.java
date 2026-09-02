package root.cyb.mh.attendancesystem.dto.network.cisco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiscoPortStatusDto {
    private int ifIndex;
    private String portName; // e.g. "gi1", "GE01", "Port 1"
    private boolean adminUp;
    private boolean operUp;
    private String speed; // e.g. "1 Gbps", "100 Mbps"
    private String duplex; // "Full", "Half"
    private Integer vlan;
    private String macAddress; // MAC learned on this port (if single host)
    private Long inOctets;
    private Long outOctets;
    private Long inErrors;
    private Long outErrors;
}
