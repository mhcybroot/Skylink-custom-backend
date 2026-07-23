package root.cyb.mh.attendancesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Employee {

    @Id
    @lombok.EqualsAndHashCode.Include
    private String id; // Corresponds to ZK User ID

    private String name;
    private String cardId;
    private String role;

    @jakarta.persistence.ManyToOne
    @JsonIgnoreProperties({"reportsTo", "reportsToAssistant", "department", "password", "photoBase64"})
    private Department department;

    @jakarta.persistence.ManyToOne
    @JsonIgnoreProperties({"reportsTo", "reportsToAssistant", "department", "password", "photoBase64"})
    private Employee reportsTo; // The supervisor this employee reports to

    @jakarta.persistence.ManyToOne
    @JsonIgnoreProperties({"reportsTo", "reportsToAssistant", "department", "password", "photoBase64"})
    private Employee reportsToAssistant; // The backup/assistant supervisor

    private String designation; // e.g. Manager, MD, Senior Developer

    private String email;

    // This field acts as the "Password" for Employee Login
    private String username;

    // Legacy password field, can be ignored or removed later
    private String password;
    @jakarta.persistence.Column(length = 100000) // Large text for Base64 image
    private String photoBase64;

    private Integer annualLeaveQuota; // Null means use global default

    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    private boolean isGuest = false;

    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    private boolean isAnalyst = false;

    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    private boolean isAnalystController = false;

    private java.time.LocalDate joiningDate;
    private java.time.LocalDate dateOfBirth;

    private String avatarPath;

    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    private boolean extensionForceLogout = false;

    // Payroll
    private Double monthlySalary;
    private Double fixedAllowance; // Recurring every month

    // Bank Details
    private String bankName;
    private String accountNumber;

    public int getEffectiveQuota(int globalDefault) {
        return annualLeaveQuota != null ? annualLeaveQuota : globalDefault;
    }
}
