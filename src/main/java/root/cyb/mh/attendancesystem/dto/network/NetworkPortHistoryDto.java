package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkPortHistoryDto {
    private Long id;
    private Long deviceId;
    private String deviceName;
    private Long portId;
    private String portNumber;
    private PortHistoryEventType eventType;
    private String oldValue;
    private String newValue;
    private String summary;
    private String source;
    private LocalDateTime recordedAt;
}
