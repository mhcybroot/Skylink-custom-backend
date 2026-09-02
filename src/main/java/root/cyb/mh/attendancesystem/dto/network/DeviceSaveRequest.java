package root.cyb.mh.attendancesystem.dto.network;

import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.NetworkDeviceStatus;
import root.cyb.mh.attendancesystem.model.enums.NetworkDeviceType;

@Data
public class DeviceSaveRequest {
    private Long id;
    private String name;
    private NetworkDeviceType deviceType;
    private String ipAddress;
    private String macAddress;
    private String location;
    private int totalPorts = 24;
    private NetworkDeviceStatus status = NetworkDeviceStatus.ONLINE;
    private String managementUrl;
    private String modelVendor;
    private String serialNumber;
    private String notes;

    private String snmpCommunity = "public";
    private Integer snmpPort = 161;
    private String snmpVersion = "2c";
    private Integer sshPort = 22;
    private String sshUsername;
    private String sshPassword;
}
