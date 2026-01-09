package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
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
    private List<ClientStat> topClients;
    private List<WorkTypeStat> workTypeMargins;
    private List<StateStat> stateDistribution;

    // Performance Scorecards
    private List<ContractorScorecard> contractorScorecards;
    private ScorecardBenchmark benchmark;

    // Cycle Time Analysis
    private CycleTimeAnalysis cycleTimeAnalysis;

    // Profitability Analysis
    private ProfitabilityAnalysis profitabilityAnalysis;

    @Data
    @AllArgsConstructor
    public static class CycleTimeAnalysis {
        private Map<String, Double> byWorkType;
        private Map<String, Double> byContractor;
        private Map<String, Long> distribution; // Histogram buckets
    }

    @Data
    @AllArgsConstructor
    public static class ProfitabilityAnalysis {
        private Map<String, BigDecimal> byClient;
        private Map<String, BigDecimal> byState;
    }

    @Data
    @AllArgsConstructor
    public static class ScorecardBenchmark {
        private BigDecimal globalAverageCost;
        private Double globalAverageDays;
    }

    @Data
    @AllArgsConstructor
    public static class ClientStat {
        private String name;
        private BigDecimal totalRevenue;
    }

    @Data
    @AllArgsConstructor
    public static class StateStat {
        private String state;
        private Long count;
    }

    @Data
    @AllArgsConstructor
    public static class WorkTypeStat {
        private String workType;
        private BigDecimal totalMargin;
    }

    @Data
    @AllArgsConstructor
    public static class ContractorScorecard {
        private String name;
        private long totalWorkOrders;
        private BigDecimal averageCost;
        private Double averageDaysToComplete;
    }

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
