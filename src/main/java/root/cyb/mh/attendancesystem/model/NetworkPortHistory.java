package root.cyb.mh.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_port_history", indexes = {
        @Index(name = "idx_net_port_hist_device", columnList = "device_id"),
        @Index(name = "idx_net_port_hist_port", columnList = "port_id"),
        @Index(name = "idx_net_port_hist_event", columnList = "eventType"),
        @Index(name = "idx_net_port_hist_time", columnList = "recordedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkPortHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties({"ports", "issues"})
    private NetworkDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id", nullable = false)
    @JsonIgnoreProperties({"issues", "device"})
    private NetworkPort port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PortHistoryEventType eventType;

    @Column(length = 255)
    private String oldValue;

    @Column(length = 255)
    private String newValue;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(length = 100)
    private String source;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
}
