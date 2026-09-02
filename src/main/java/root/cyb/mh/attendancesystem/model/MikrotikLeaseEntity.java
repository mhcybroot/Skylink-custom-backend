package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_mikrotik_leases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikLeaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mikrotikId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "mac_address")
    private String macAddress;

    @Column(name = "host_name")
    private String hostName;

    private String server;

    private String status;

    private String expiresAfter;

    private Boolean dynamic;

    private Boolean disabled;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime lastSeen;
}
