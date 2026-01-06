package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String phone;
    private String email;

    // SMTP Config
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;

    private boolean active = true;
}
