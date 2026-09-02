package root.cyb.mh.attendancesystem.dto.network.cisco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiscoTestConnectionRequest {
    private Long deviceId;
    private String host;
    @Builder.Default
    private int snmpPort = 161;
    @Builder.Default
    private String snmpCommunity = "public";
    @Builder.Default
    private String snmpVersion = "2c";
    @Builder.Default
    private int sshPort = 22;
    private String sshUsername;
    private String sshPassword;
    @Builder.Default
    private String method = "SNMP"; // "SNMP" or "SSH"
}
