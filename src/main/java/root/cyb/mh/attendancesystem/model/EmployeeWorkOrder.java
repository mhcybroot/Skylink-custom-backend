package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "employee_work_orders")
public class EmployeeWorkOrder extends BaseWorkOrder {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by_employee_id")
    private Employee importedBy;
}
