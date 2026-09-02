package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_network_usage_records", indexes = {
        @Index(name = "idx_emp_net_date", columnList = "record_date"),
        @Index(name = "idx_emp_net_ip", columnList = "ip_address"),
        @Index(name = "idx_emp_net_name", columnList = "employee_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeNetworkUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_name", length = 150)
    private String employeeName;

    @Column(length = 150)
    private String hostname;

    @Column(name = "ip_address", length = 60, nullable = false)
    private String ipAddress;

    @Column(name = "mac_address", length = 60)
    private String macAddress;

    @Column(name = "client_os", length = 60)
    private String clientOs;

    @Column(name = "switch_port", length = 100)
    private String switchPort;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "total_bytes_transferred")
    @Builder.Default
    private Long totalBytesTransferred = 0L;

    @Column(name = "work_dev_bytes")
    @Builder.Default
    private Long workDevBytes = 0L;

    @Column(name = "communication_bytes")
    @Builder.Default
    private Long communicationBytes = 0L;

    @Column(name = "media_entertainment_bytes")
    @Builder.Default
    private Long mediaEntertainmentBytes = 0L;

    @Column(name = "general_web_bytes")
    @Builder.Default
    private Long generalWebBytes = 0L;

    @Column(name = "productivity_score")
    @Builder.Default
    private Double productivityScore = 0.0;

    @Column(name = "active_sockets_count")
    @Builder.Default
    private Integer activeSocketsCount = 0;

    @Column(name = "top_visited_domain", length = 150)
    private String topVisitedDomain;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getTotalFormatted() {
        if (totalBytesTransferred == null || totalBytesTransferred == 0) return "0 B";
        if (totalBytesTransferred < 1024) return totalBytesTransferred + " B";
        if (totalBytesTransferred < 1024 * 1024) return String.format("%.1f KB", totalBytesTransferred / 1024.0);
        if (totalBytesTransferred < 1024 * 1024 * 1024) return String.format("%.1f MB", totalBytesTransferred / (1024.0 * 1024.0));
        return String.format("%.2f GB", totalBytesTransferred / (1024.0 * 1024.0 * 1024.0));
    }
}
