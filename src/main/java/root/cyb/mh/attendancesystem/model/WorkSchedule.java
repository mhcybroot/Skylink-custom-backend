package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalTime;

@Entity
@Data
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private int lateToleranceMinutes;
    private int earlyLeaveToleranceMinutes;

    // Store as comma-separated integers (1=Mon, 7=Sun)
    // Default: 6,7 (Saturday, Sunday)
    // Default: 6,7 (Saturday, Sunday)
    private String weekendDays = "6,7";

    private Integer defaultAnnualLeaveQuota = 12; // Days per year
    private Integer defaultSickLeaveQuota = 6; // Sick days per year
    private Integer defaultCasualLeaveQuota = 6; // Casual & other days per year

    public Integer getDefaultSickLeaveQuota() {
        if (defaultSickLeaveQuota != null) {
            return defaultSickLeaveQuota;
        }
        if (defaultAnnualLeaveQuota != null) {
            return defaultAnnualLeaveQuota / 2;
        }
        return 6;
    }

    public Integer getDefaultCasualLeaveQuota() {
        if (defaultCasualLeaveQuota != null) {
            return defaultCasualLeaveQuota;
        }
        if (defaultAnnualLeaveQuota != null) {
            int sick = getDefaultSickLeaveQuota();
            return Math.max(0, defaultAnnualLeaveQuota - sick);
        }
        return 6;
    }

    public Integer getDefaultAnnualLeaveQuota() {
        if (defaultAnnualLeaveQuota != null) {
            return defaultAnnualLeaveQuota;
        }
        return getDefaultSickLeaveQuota() + getDefaultCasualLeaveQuota();
    }

    // Late Penalty Config
    private Integer latePenaltyThreshold = 3; // e.g. every 3 late days
    private Double latePenaltyDeduction = 0.5; // deduct 0.5 day salary

    // Daily Rate Config
    // Options: "STANDARD_30", "ACTUAL_WORKING_DAYS", "FIXED_DAYS"
    private String dailyRateBasis = "STANDARD_30";
    private Integer dailyRateFixedValue = 30; // Used if FIXED_DAYS is selected

    // Default constructor with standard values if needed
    public WorkSchedule() {
        this.startTime = LocalTime.of(9, 0);
        this.endTime = LocalTime.of(18, 0);
        this.lateToleranceMinutes = 15;
        this.earlyLeaveToleranceMinutes = 15;
    }
}
