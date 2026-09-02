package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkTopologyLinkDto {
    private String source; // Node ID
    private String target; // Node ID
    private String sourcePort; // e.g. "ether10"
    private String targetPort; // e.g. "GE23"
    private String linkSpeed; // "1 Gbps"
    private String linkType; // "TRUNK", "UPLINK", "ACCESS"
    private String status; // "UP", "DOWN"
    private String vlanTags; // "1, 10, 15, 16"
}
