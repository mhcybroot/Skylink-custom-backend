package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "importer_work_order_history", indexes = {
        @Index(name = "idx_imp_hist_wo", columnList = "wo_number"),
        @Index(name = "idx_imp_hist_wo_id", columnList = "importer_work_order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImporterWorkOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "importer_work_order_id")
    private Long importerWorkOrderId;

    @Column(name = "wo_number", nullable = false, length = 100)
    private String woNumber;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // CREATED, UPDATED_VALUE, UPDATED_WORK_TYPE, DELETED, etc.

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by_name", length = 150)
    private String changedByName;

    @Column(name = "changed_by_employee_id", length = 100)
    private String changedByEmployeeId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }
}
