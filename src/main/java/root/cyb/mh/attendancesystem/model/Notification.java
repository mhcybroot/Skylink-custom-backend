package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We store username for simplicity and loose coupling, or could use ManyToOne
    // User
    @Column(nullable = false)
    private String recipientUsername;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String type; // INFO, SUCCESS, WARNING, ERROR

    private String linkAction; // URL to navigate to

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // For badge grouping or distinct logic
    private String category; // PAYMENT, SYSTEM, etc.
}
