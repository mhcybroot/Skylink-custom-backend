package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkStatsDto {
    private long totalDevices;
    private long totalPorts;
    private long activeConnectedPorts;
    private long degradedPorts;
    private long openIssues;
    private long resolvedIssues;
    private long managedSwitches;
    private long unmanagedSwitches;
    private long routers;
    private long accessPoints;
}
