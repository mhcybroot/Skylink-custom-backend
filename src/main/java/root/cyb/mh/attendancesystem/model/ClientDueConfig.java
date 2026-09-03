package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_due_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDueConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * "DEFAULT" for the global default fallback, or client code / client name (e.g. "MCS", "C100")
     */
    @Column(nullable = false, unique = true)
    private String clientIdentifier;

    private String clientName;

    /**
     * Tier 1: Standard Due days threshold (default: 40)
     */
    @Column(nullable = false)
    private int normalDueDays = 40;

    /**
     * Tier 2: Past Due days threshold (default: 50)
     */
    @Column(nullable = false)
    private int overdueDays = 50;

    /**
     * Tier 3: Critical Delinquent days threshold (default: 60)
     */
    @Column(nullable = false)
    private int criticalDueDays = 60;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
