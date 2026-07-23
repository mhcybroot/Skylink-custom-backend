package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user_custom_metadata_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomMetadataField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String fieldKey; // e.g. "project_name"

    @Column(nullable = false)
    private String fieldLabel; // e.g. "Project Name"

    private String fieldType; // "TEXT", "NUMBER"

    private Integer displayOrder;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
