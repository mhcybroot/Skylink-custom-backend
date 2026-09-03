package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private LocalDateTime importDate;

    private Integer recordsProcessed = 0;

    private String status; // SUCCESS, FAILED, PARTIAL

    private String importType; // WORK_ORDER, EMPLOYEE, etc.

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer failureCount = 0;

    private Integer successCount = 0;

    private Integer totalRecords = 0;
}
