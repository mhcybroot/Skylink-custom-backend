package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class DailyAttendanceDto {
    private String employeeName;
    private String departmentName;
    private String employeeId;
    private LocalTime inTime;
    private LocalTime outTime;
    private String status; // PRESENT, LATE, EARLY_LEAVE, ABSENT
    private String statusColor; // success, warning, danger, etc.
    private String designation;
    private String avatarPath;

    // Live Work Status Integration Fields
    private String currentWorkStatus; // e.g., "WORKING", "ON_BREAK", "COMPLETED_DAY"
    private String currentWorkStatusColor; // e.g., "primary", "warning", "success"
    private String activeWorkDuration; // Formatted as "07h 45m"
    private String totalBreakDuration; // Formatted as "00h 30m"
}
