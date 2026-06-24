package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProcessingWorkOrderImportDTO {
    private String woNumber;
    private String status;
    private LocalDate dateDue;
    private String clientCode;
    private String category;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String contractor;
    private String admin;
    private String icons;
    private String workType;
    private Integer photoCount;
    
    private boolean exists;
    private String resolution; // "UPDATE", "DUPLICATE", "SKIP"
    private boolean skipCategory;
    
    private String assignedAnalystEmployeeId;
}
