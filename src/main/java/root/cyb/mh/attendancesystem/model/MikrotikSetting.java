package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_mikrotik_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private String host = "116.206.59.142";

    @Column(nullable = false)
    @Builder.Default
    private Integer port = 8225;

    @Column(nullable = false)
    @Builder.Default
    private String username = "skylink-sync";

    @Column(columnDefinition = "TEXT")
    private String password;

    @Builder.Default
    private Boolean useSsl = true;

    @Builder.Default
    private Boolean autoSyncEnabled = true;

    @Builder.Default
    private String pollCron = "0 */5 * * * *";

    private LocalDateTime lastSyncTime;

    private String lastSyncStatus;

    @Column(columnDefinition = "TEXT")
    private String lastSyncMessage;

    @Builder.Default
    private Integer lastLeasesCount = 0;

    @Builder.Default
    private Integer lastArpCount = 0;

    @Builder.Default
    private Integer lastInterfacesCount = 0;
}
