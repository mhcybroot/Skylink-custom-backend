package root.cyb.mh.attendancesystem.dto.network.mikrotik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikEnrichedLeaseDto {
    private String id;
    private String ipAddress;
    private String macAddress;
    private String hostName;
    private String server;
    private String status;
    private String expiresAfter;
    private Boolean dynamic;
    private Boolean disabled;
    private String comment;

    // Switch Port Mapping
    private boolean mapped;
    private Long portId;
    private String portNumber;
    private Long deviceId;
    private String deviceName;
    private String assignedUser;
    private String vlan;
}
