package root.cyb.mh.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.NetworkDeviceStatus;
import root.cyb.mh.attendancesystem.model.enums.NetworkDeviceType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "network_devices", indexes = {
        @Index(name = "idx_net_dev_name", columnList = "name"),
        @Index(name = "idx_net_dev_ip", columnList = "ipAddress"),
        @Index(name = "idx_net_dev_type", columnList = "deviceType"),
        @Index(name = "idx_net_dev_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NetworkDeviceType deviceType;

    @Column(length = 60)
    private String ipAddress;

    @Column(length = 60)
    private String macAddress;

    @Column(length = 150)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private int totalPorts = 24;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private NetworkDeviceStatus status = NetworkDeviceStatus.ONLINE;

    @Column(length = 255)
    private String managementUrl;

    @Column(length = 150)
    private String modelVendor;

    @Column(length = 100)
    private String serialNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // SNMP & SSH Management Config
    @Column(length = 100)
    @Builder.Default
    private String snmpCommunity = "public";

    @Builder.Default
    private Integer snmpPort = 161;

    @Column(length = 20)
    @Builder.Default
    private String snmpVersion = "2c";

    @Builder.Default
    private Integer sshPort = 22;

    @Column(length = 100)
    private String sshUsername;

    @Column(columnDefinition = "TEXT")
    private String sshPassword;

    private LocalDateTime lastSyncedAt;

    @Column(length = 50)
    private String lastSyncStatus;

    @Column(columnDefinition = "TEXT")
    private String lastSyncMessage;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("device")
    @OrderBy("portNumber ASC")
    @Builder.Default
    private List<NetworkPort> ports = new ArrayList<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("device")
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
        if (this.status == null) {
            this.status = NetworkDeviceStatus.ONLINE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
