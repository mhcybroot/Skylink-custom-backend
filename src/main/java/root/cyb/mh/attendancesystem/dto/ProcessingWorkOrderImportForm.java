package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Data
public class ProcessingWorkOrderImportForm {
    private List<ProcessingWorkOrderImportDTO> rows = new ArrayList<>();
    private Set<String> skippedCategories = new HashSet<>();
}
