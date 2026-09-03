package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "work_orders")
public class WorkOrder extends BaseWorkOrder {
}
