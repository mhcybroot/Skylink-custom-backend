package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeUsername;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String localFilePath;

    @Column(nullable = false)
    private String webPath;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}
