package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

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

    @ManyToOne
    @JoinColumn(name = "default_payment_method_id")
    private PaymentMethod defaultPaymentMethod;

    @Column(columnDefinition = "TEXT")
    private String accountDetails;

    private boolean active = true;
}
