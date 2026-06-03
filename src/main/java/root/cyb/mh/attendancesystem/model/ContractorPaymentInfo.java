package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "contractor_payment_infos")
public class ContractorPaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Contractor contractor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String accountDetails;

    // Optional: Is this the primary one?
    // For now we will rely on Contractor.defaultPaymentMethod as the "Primary" one,
    // and this list as "All Available Accounts".
    // Audit Fields
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    private String createdBy; // Username
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    private String deletedBy; // Username
    private java.time.LocalDateTime deletedAt;
}
