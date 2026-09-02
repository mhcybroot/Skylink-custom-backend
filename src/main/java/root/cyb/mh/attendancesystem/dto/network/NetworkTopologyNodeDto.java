package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkTopologyNodeDto {
    private String id; // e.g. "dev-1", "dev-2", "user-10.10.15.250"
    private String label; // "Switch 01 (Server Switch)"
    private String type; // "ROUTER", "MANAGED_SWITCH", "SERVER", "ACCESS_POINT", "WORKSTATION"
    private String ipAddress;
    private String macAddress;
    private String status; // "ONLINE", "WARNING", "OFFLINE"
    private Integer activePorts;
    private Integer totalPorts;
}
