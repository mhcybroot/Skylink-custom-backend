package root.cyb.mh.attendancesystem.dto.network;

import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.IssueSeverity;
import root.cyb.mh.attendancesystem.model.enums.IssueStatus;
import root.cyb.mh.attendancesystem.model.enums.IssueType;

@Data
public class IssueLogRequest {
    private Long deviceId;
    private Long portId;
    private IssueType issueType;
    private IssueSeverity severity;
    private IssueStatus status;
    private String title;
    private String description;
    private String reportedBy;
    private boolean markPortProblematic = true;
}
