package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "import_logs")
public class ImportLog extends BaseImportLog {
}
