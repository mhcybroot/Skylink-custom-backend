package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_call_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeUsername;

    private String callerName;
    
    private String callNumber;
    
    private String callType;
    
    private Integer durationSeconds;
    
    private LocalDateTime callTimestamp;
    
    private LocalDateTime syncedAt;
}
