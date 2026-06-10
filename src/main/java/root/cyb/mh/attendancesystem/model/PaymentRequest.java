package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import root.cyb.mh.attendancesystem.model.enums.PPWStatus;
import root.cyb.mh.attendancesystem.model.enums.PaymentPriority;
import root.cyb.mh.attendancesystem.model.enums.PaymentStatus;
import root.cyb.mh.attendancesystem.model.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "payment_requests")
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate requestDate;

    private java.time.LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        lastModified = java.time.LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "employee_requester_id")
    private root.cyb.mh.attendancesystem.model.Employee employeeRequester;

    @Column(nullable = false)
    private String workOrderNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    // @Column(nullable = false) - Deprecated
    private String contractorName;

    @ManyToOne
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    private String paymentMethodId; // Optional, e.g., CashApp ID, Zelle -> Deprecated

    @ManyToOne
    @JoinColumn(name = "payment_method_ref_id")
    private PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String paymentAccountDetails;

    private String paymentReferenceNumber;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private String clientCode; // -> Deprecated

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPriority priority;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // Review Fields
    private String checkStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @ManyToOne
    @JoinColumn(name = "approval_authority_id")
    private User approvalAuthority;

    @ManyToOne
    @JoinColumn(name = "approval_employee_id")
    private root.cyb.mh.attendancesystem.model.Employee approvalEmployee;

    @Enumerated(EnumType.STRING)
    private PPWStatus ppwUpdateStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private Integer reviewUpdateCount = 0;

    @Column(columnDefinition = "TEXT")
    private String employeeNote;

    private java.time.LocalDateTime lastEmailSentAt;
    private String lastEmailSentTo;

    @Column(nullable = true)
    private String paymentProofPath;

    private Boolean isPartialPayment;
    private String duplicateReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
}
