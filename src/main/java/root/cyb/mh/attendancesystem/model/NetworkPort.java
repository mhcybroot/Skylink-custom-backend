package root.cyb.mh.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.PortMode;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "network_ports", indexes = {
        @Index(name = "idx_net_port_device", columnList = "device_id"),
        @Index(name = "idx_net_port_num", columnList = "portNumber"),
        @Index(name = "idx_net_port_ip", columnList = "ipAddress"),
        @Index(name = "idx_net_port_mac", columnList = "macAddress"),
        @Index(name = "idx_net_port_user", columnList = "hostnameOrUser"),
        @Index(name = "idx_net_port_vlan", columnList = "vlan"),
        @Index(name = "idx_net_port_status", columnList = "portStatus")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_port_number", columnNames = {"device_id", "portNumber"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkPort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties({"ports", "issues"})
    private NetworkDevice device;

    @Column(nullable = false, length = 50)
    private String portNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PortMode portMode = PortMode.ACCESS;

    @Column(length = 100)
    private String vlan;

    @Column(length = 60)
    private String ipAddress;

    @Column(length = 60)
    private String macAddress;

    @Column(length = 150)
    private String hostnameOrUser;

    @Column(length = 100)
    private String deviceCategory;

    @Column(length = 50)
    @Builder.Default
    private String speedNegotiation = "1 Gbps";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PortStatus portStatus = PortStatus.EMPTY_DISABLED;

    @Column(nullable = false)
    @Builder.Default
    private boolean isUplink = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isTrunk = false;

    @Column(length = 150)
    private String targetDeviceName;

    @Column(length = 50)
    private String targetPortName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "port", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("port")
    @OrderBy("reportedAt DESC")
    @Builder.Default
    private List<NetworkIssue> issues = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.portStatus == null) {
            this.portStatus = (this.hostnameOrUser != null && !this.hostnameOrUser.isBlank()) 
                    ? PortStatus.ACTIVE_CONNECTED 
                    : PortStatus.EMPTY_DISABLED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
