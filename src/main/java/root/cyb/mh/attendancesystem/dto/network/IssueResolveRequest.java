package root.cyb.mh.attendancesystem.dto.network;

import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.IssueStatus;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;

@Data
public class IssueResolveRequest {
    private Long issueId;
    private IssueStatus status = IssueStatus.RESOLVED;
    private String resolvedBy;
    private String rootCause;
    private String resolutionNotes;
    private boolean restorePortStatus = true;
    private PortStatus newPortStatus = PortStatus.ACTIVE_CONNECTED;
}
