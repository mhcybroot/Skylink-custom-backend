package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Archived snapshot of a PaymentRequest that has been deleted.
 * All fields are denormalized — no foreign keys — so the history persists
 * even after the original record and its relations are gone.
 */
@Entity
@Data
@Table(name = "deleted_payment_requests")
public class DeletedPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The original payment_requests.id before deletion */
    private Long originalId;

    private String workOrderNumber;
    private BigDecimal amount;
    private LocalDate requestDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String contractorName;
    private String clientName;
    private String companyName;
    private String paymentMethodName;

    /** e.g. "PENDING", "APPROVED", "REJECTED" */
    private String finalStatus;

    /** e.g. "UNPAID", "PAID", "ISSUE" */
    private String finalPaymentStatus;

    /** e.g. "NOT_UPDATED", "UPDATED" */
    private String finalPpwStatus;

    /** Username or employee name of whoever submitted the request */
    private String requesterName;

    /** Username of the ADMIN who performed the deletion */
    private String deletedBy;

    /** Timestamp when deletion occurred */
    @Column(nullable = false)
    private LocalDateTime deletedAt;
}
