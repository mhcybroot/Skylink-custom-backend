package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeUsername;

    @Column(nullable = false)
    private String packageName;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime interceptedAt = LocalDateTime.now();
}
