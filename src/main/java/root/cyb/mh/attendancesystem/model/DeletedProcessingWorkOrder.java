package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "deleted_processing_work_orders")
public class DeletedProcessingWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_id")
    private Long originalId;

    @Column(name = "wo_number", nullable = false)
    private String woNumber;

    @Column(name = "wo_type")
    private String woType;

    @Column(name = "client_code")
    private String clientCode;

    private String address;

    @Column(name = "photo_count")
    private Integer photoCount;

    @Column(name = "work_date")
    private LocalDate workDate;

    private String category;

    @Column(name = "late_status")
    private String lateStatus;

    private String analyst;

    private String status;

    @Column(name = "date_due")
    private LocalDate dateDue;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ba_from_wo")
    private String baFromWo;

    @Column(name = "ba_by_analyst")
    private String baByAnalyst;

    @Column(name = "bid_count")
    private Integer bidCount;

    @Column(name = "bid_amount", precision = 19, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "client_invoice", precision = 19, scale = 2)
    private BigDecimal clientInvoice;

    @Column(name = "crew_invoice", precision = 19, scale = 2)
    private BigDecimal crewInvoice;

    @Column(name = "assigned_analyst_employee_id")
    private String assignedAnalystEmployeeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_employee_id")
    private String createdByEmployeeId;

    @Column(name = "last_updated_by_employee_id")
    private String lastUpdatedByEmployeeId;

    // Soft delete tracking fields
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_employee_id")
    private String deletedByEmployeeId;

    @Column(name = "deleted_by_name")
    private String deletedByName;

    @PrePersist
    protected void onCreate() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }
}
