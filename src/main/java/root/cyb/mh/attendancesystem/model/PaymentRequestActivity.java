package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_request_activities")
public class PaymentRequestActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private PaymentRequest paymentRequest;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String actionType; // "CREATED", "VIEWED", "UPDATED", "NOTE_ADDED"

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
