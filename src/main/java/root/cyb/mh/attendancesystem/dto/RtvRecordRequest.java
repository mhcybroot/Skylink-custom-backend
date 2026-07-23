package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.IssueFromEnum;
import root.cyb.mh.attendancesystem.model.enums.RtvStatusEnum;

@Data
public class RtvRecordRequest {

    private String woNumber;
    private String clientCode;
    private String originalProcessorId;
    private String rtvSolvedById;
    private IssueFromEnum issueFrom;
    private String details;
    private RtvStatusEnum rtvStatus;
    private String notes;
}
