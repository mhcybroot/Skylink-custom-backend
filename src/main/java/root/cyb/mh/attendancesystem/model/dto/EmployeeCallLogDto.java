package root.cyb.mh.attendancesystem.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmployeeCallLogDto {
    private String callerName;
    private String callNumber;
    private String callType; // INCOMING, OUTGOING, MISSED, REJECTED
    private Integer durationSeconds;
    private LocalDateTime callTimestamp;
}
