package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUsageItemDto {
    private Long id;
    private String employeeName;
    private String hostname;
    private String ipAddress;
    private String macAddress;
    private String clientOs;
    private String switchPort;

    private Long totalBytes;
    private String totalFormatted;

    private Long workDevBytes;
    private String workDevFormatted;
    private double workDevPercent;

    private Long communicationBytes;
    private String communicationFormatted;
    private double communicationPercent;

    private Long mediaEntertainmentBytes;
    private String mediaEntertainmentFormatted;
    private double mediaEntertainmentPercent;

    private Long generalWebBytes;
    private String generalWebFormatted;
    private double generalWebPercent;

    private double productivityScore;
    private String productivityLabel;
    private String productivityBadgeClass;

    private int activeSockets;
    private String topVisitedDomain;
}
