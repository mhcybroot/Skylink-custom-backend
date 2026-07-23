package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user_metadata_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMetadataProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // The username of the owner

    @Column(nullable = false)
    private String profileName; // e.g. "HQ Office Location"

    private Double latitude;
    private Double longitude;
    private Double altitude;

    private String cameraMake;
    private String cameraModel;
    private String software;
    private String artist;
    private String copyright;

    private String uploadBy;
    private String uploadTimestamp;
    private String dateTimeOriginal;
    private String uploadFrom;
    private String duplicate;

    @Column(length = 1000)
    private String description;

    @Column(length = 4000)
    private String customFieldsJson;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
