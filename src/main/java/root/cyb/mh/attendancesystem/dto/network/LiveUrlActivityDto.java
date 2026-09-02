package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveUrlActivityDto {
    private String employeeName;
    private String clientIp;
    private String clientMac;
    private String clientOs;
    private String switchPort;
    private String visitedDomain;
    private String visitedUrl;
    private String serviceCategory;
    private String categoryBadgeClass;
    private String serviceIcon;
    private String destinationIp;
    private String destinationPort;
    private String protocol;
    private String liveBandwidth;
    private int activeSockets;
    private String lastSeen;
}
