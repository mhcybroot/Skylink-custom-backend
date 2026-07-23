package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Data
public class MonthlyLeaveBreakdownDto {
    private String month;
    private int monthValue; // 1 to 12
    private int year;
    
    private int paidLeavesTaken;
    private int unpaidLeavesTaken;
    private int absentDays;
    private int sickLeavesTaken;
    private int casualLeavesTaken;
    private int otherLeavesTaken;
    private int totalDays;

    private List<DateProof> paidDates = new ArrayList<>();
    private List<DateProof> unpaidDates = new ArrayList<>();
    private List<DateProof> absentDates = new ArrayList<>();
    private List<DateProof> sickDates = new ArrayList<>();
    private List<DateProof> casualDates = new ArrayList<>();
    private List<DateProof> otherDates = new ArrayList<>();
    private List<DateProof> holidayDates = new ArrayList<>();
    private List<DateProof> weekendDates = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DateProof {
        private String date; // Using ISO String YYYY-MM-DD for easier JSON serialization
        private String proof;
    }
}
