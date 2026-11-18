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
@Table(name = "deleted_importer_work_orders", indexes = {
        @Index(name = "idx_del_imp_wo_number", columnList = "wo_number"),
        @Index(name = "idx_del_imp_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_del_imp_del_by", columnList = "deleted_by_employee_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedImporterWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_id")
    private Long originalId;

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

    @Column(name = "importer_employee_id", length = 100)
    private String importerEmployeeId;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_name", length = 150)
    private String deletedByName;

    @Column(name = "deleted_by_employee_id", length = 100)
    private String deletedByEmployeeId;

    @PrePersist
    protected void onCreate() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }
}
