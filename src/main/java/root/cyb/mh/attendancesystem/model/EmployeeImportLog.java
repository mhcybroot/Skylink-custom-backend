package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "employee_import_logs")
public class EmployeeImportLog extends BaseImportLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by_employee_id")
    private Employee importedBy;
}
