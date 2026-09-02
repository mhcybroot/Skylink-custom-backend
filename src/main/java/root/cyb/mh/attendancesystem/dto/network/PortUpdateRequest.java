package root.cyb.mh.attendancesystem.dto.network;

import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.PortMode;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;

@Data
public class PortUpdateRequest {
    private Long id;
    private Long deviceId;
    private String portNumber;
    private PortMode portMode;
    private String vlan;
    private String ipAddress;
    private String macAddress;
    private String hostnameOrUser;
    private String deviceCategory;
    private String speedNegotiation;
    private PortStatus portStatus;
    private boolean isUplink;
    private boolean isTrunk;
    private String targetDeviceName;
    private String targetPortName;
    private String notes;
}
