package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.RtvActionTypeEnum;

import java.time.LocalDateTime;

@Entity
@Table(name = "rtv_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RtvAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rtvId;

    private String woNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RtvActionTypeEnum actionType;

    private String actorUsername;

    private String actorFullName;

    private LocalDateTime timestamp;

    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String changeDescription;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
