package root.cyb.mh.attendancesystem.dto.network.cisco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiscoSyncResultDto {
    private Long deviceId;
    private String switchName;
    private String switchIp;
    private boolean success;
    private long durationMs;
    private int portsUpdated;
    private int activePorts;
    private int downPorts;
    private int macsDiscovered;
    private int issuesLogged;
    private String message;
    private LocalDateTime timestamp;
    private String sysDescr;
    private String uptime;
    private List<CiscoNeighborDto> neighbors;
}
