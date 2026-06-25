package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String principalName; // The login ID of the User or Employee

    // Comma-separated list of pinned items (e.g., "dashboard,sheets,verify")
    @Column(length = 255)
    private String pinnedNavItems;
}
