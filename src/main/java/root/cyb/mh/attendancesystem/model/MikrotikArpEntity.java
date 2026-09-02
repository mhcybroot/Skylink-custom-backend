package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_mikrotik_arp_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikArpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mikrotikId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "mac_address")
    private String macAddress;

    private String networkInterface;

    private Boolean complete;

    private Boolean dynamic;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime lastSeen;
}
