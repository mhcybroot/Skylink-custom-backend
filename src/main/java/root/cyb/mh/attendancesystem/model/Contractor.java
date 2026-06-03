package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "contractors")
public class Contractor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String email;

    @ManyToOne
    @JoinColumn(name = "default_payment_method_id")
    private PaymentMethod defaultPaymentMethod;

    @Column(columnDefinition = "TEXT")
    private String accountDetails;

    @OneToMany(mappedBy = "contractor", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<ContractorPaymentInfo> paymentInfos = new java.util.ArrayList<>();

    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    private boolean active = true;

    @Column(length = 20)
    private String zipCode;

    @Column(length = 100)
    private String area;
}
