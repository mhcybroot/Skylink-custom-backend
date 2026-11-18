package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImporterWorkOrderBulkRequest {

    private String entryDate;
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String woNumber;
        private String clientCode;
        private String workType;
        private BigDecimal value;
    }
}
