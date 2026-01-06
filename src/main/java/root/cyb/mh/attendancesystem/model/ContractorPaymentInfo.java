package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

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
    private Contractor contractor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String accountDetails;

    // Optional: Is this the primary one?
    // For now we will rely on Contractor.defaultPaymentMethod as the "Primary" one,
    // and this list as "All Available Accounts".
}
