package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_vlans", indexes = {
        @Index(name = "idx_net_vlan_id", columnList = "vlan_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkVlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vlan_id", nullable = false, unique = true)
    private Integer vlanId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String subnet;

    @Column(length = 255)
    private String description;

    @Column(name = "badge_color", length = 30)
    @Builder.Default
    private String badgeColor = "primary";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getDisplayName() {
        return "VLAN " + vlanId + (name != null && !name.isBlank() ? " (" + name + ")" : "");
    }
}
