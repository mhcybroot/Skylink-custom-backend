package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class EmployeeDailyWorkStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private WorkStatus status = WorkStatus.NOT_ENTERED;

    private LocalDateTime workStartTime;
    private LocalDateTime workEndTime;
    private LocalDateTime currentBreakStartTime;

    private int totalBreakMinutes = 0;

    public EmployeeDailyWorkStatus(String employeeId, LocalDate date) {
        this.employeeId = employeeId;
        this.date = date;
    }
}
