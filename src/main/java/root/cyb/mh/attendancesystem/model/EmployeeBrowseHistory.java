package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_browse_history", indexes = {
    @Index(name = "idx_ebh_employee", columnList = "employee_id"),
    @Index(name = "idx_ebh_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBrowseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Employee employee;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String pageTitle;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
