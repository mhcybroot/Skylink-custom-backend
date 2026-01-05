package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String methodName; // e.g. "CashApp", "Zelle", "Check"

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean active = true;
}
