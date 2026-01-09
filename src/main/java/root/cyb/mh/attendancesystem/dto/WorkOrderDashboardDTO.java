package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class WorkOrderDashboardDTO {
    private long totalWorkOrders;
    private long openWorkOrders;
    private long closedWorkOrders;
    private long cancelledWorkOrders;

    private BigDecimal totalRevenue; // Client Invoice Total
    private BigDecimal totalCost; // Contractor Invoice Total
    private BigDecimal totalMargin;

    // Averages
    private BigDecimal avgRevenue;
    private BigDecimal avgCost;
    private BigDecimal avgMargin;

    // Charts
    private Map<String, Long> statusDistribution;
    private List<ContractorStat> topContractors;
    private Map<String, Long> workOrdersOverTime;

    @Data
    public static class ContractorStat {
        private String name;
        private long count;

        public ContractorStat(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }
}
