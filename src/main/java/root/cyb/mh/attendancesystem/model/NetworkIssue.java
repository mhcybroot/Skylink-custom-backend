package root.cyb.mh.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.IssueSeverity;
import root.cyb.mh.attendancesystem.model.enums.IssueStatus;
import root.cyb.mh.attendancesystem.model.enums.IssueType;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_issues", indexes = {
        @Index(name = "idx_net_iss_device", columnList = "device_id"),
        @Index(name = "idx_net_iss_port", columnList = "port_id"),
        @Index(name = "idx_net_iss_status", columnList = "status"),
        @Index(name = "idx_net_iss_type", columnList = "issueType"),
        @Index(name = "idx_net_iss_severity", columnList = "severity"),
        @Index(name = "idx_net_iss_reported_at", columnList = "reportedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties({"ports", "issues"})
    private NetworkDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id")
    @JsonIgnoreProperties({"issues", "device"})
    private NetworkPort port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IssueSeverity severity = IssueSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String reportedBy;

    private LocalDateTime reportedAt;

    @Column(length = 100)
    private String resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.reportedAt == null) {
            this.reportedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = IssueStatus.OPEN;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
