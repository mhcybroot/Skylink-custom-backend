package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "importer_work_orders", indexes = {
        @Index(name = "idx_imp_wo_entry_date", columnList = "entry_date"),
        @Index(name = "idx_imp_wo_importer_emp_id", columnList = "importer_employee_id"),
        @Index(name = "idx_imp_wo_number", columnList = "wo_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImporterWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wo_number", nullable = false, length = 100)
    private String woNumber;

    @Column(name = "client_code", length = 50)
    private String clientCode;

    @Column(name = "work_type", length = 100)
    private String workType;

    @Column(name = "value_amount", precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "importer_name", length = 150)
    private String importerName;

    @Column(name = "importer_employee_id", nullable = false, length = 100)
    private String importerEmployeeId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.entryDate == null) {
            this.entryDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
