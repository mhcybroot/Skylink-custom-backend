package root.cyb.mh.attendancesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveQuotaDto {
    private String employeeId;
    private String employeeName;
    private int annualQuota;
    private int yearlyLeavesTaken;
    private int paidLeavesTaken;
    private int unpaidLeavesTaken;
    private int absentDays;
    private int sickLeavesTaken;
    private int casualLeavesTaken;
    private int otherLeavesTaken;
    private double usagePercentage;

    // Split Quota Fields
    private int sickQuota;
    private int sickPaidTaken;
    private int sickUnpaidTaken;
    private int sickRemaining;
    private double sickUsagePercentage;

    private int casualQuota;
    private int casualPaidTaken;
    private int casualUnpaidTaken;
    private int casualRemaining;
    private double casualUsagePercentage;
}
