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

    // Invoice Payment Status
    private long clientInvoicesPaid;
    private long clientInvoicesUnpaid;
    private long contractorInvoicesPaid;
    private long contractorInvoicesUnpaid;

    // Base Financials
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalMargin;
    private BigDecimal avgRevenue;
    private BigDecimal avgCost;
    private BigDecimal avgMargin;
    private BigDecimal globalGrossMarginPercent;

    // Detailed Financials
    private BigDecimal totalClientDiscount;
    private BigDecimal totalContractorDiscount;
    private BigDecimal totalWriteOffs;
    private BigDecimal realizedRevenue; // Actual Paid
    private BigDecimal realizedCost; // Actual Paid
    private BigDecimal unrealizedRevenue;

    // Charts
    private Map<String, Long> statusDistribution;
    private List<ContractorStat> topContractors;
    private Map<String, Long> workOrdersOverTime;
    private List<ClientStat> topClients;
    private List<WorkTypeStat> workTypeMargins;
    private List<StateStat> stateDistribution;
    private List<BankStat> topBanks;

    // Performance Scorecards
    private List<ContractorScorecard> contractorScorecards;
    private ScorecardBenchmark benchmark;

    // Cycle Time Analysis (New Definition)
    private CycleTimeAnalysis cycleTimeAnalysis;

    // Profitability Analysis
    private ProfitabilityAnalysis profitabilityAnalysis;

    @Data
    @AllArgsConstructor
    public static class CycleTimeAnalysis {
        private Double avgDaysDueToInvoice;
        private Double avgDaysInvoiceToPay;
        private Map<String, Double> invoicingLagByWorkType;
    }

    @Data
    @AllArgsConstructor
    public static class ProfitabilityAnalysis {
        private Map<String, BigDecimal> byClient;
        private Map<String, BigDecimal> byState;
        private Map<String, BigDecimal> byBank;
    }

    @Data
    @AllArgsConstructor
    public static class ScorecardBenchmark {
        private BigDecimal globalAverageCost;
        private Double globalAverageDaysToInvoice;
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
    public static class BankStat {
        private String name;
        private Long count;
        private BigDecimal revenue;
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
        private Double averageDaysToInvoice;
    }

    // Series Analysis (LLC/Series 100, 200, etc.)
    private List<SeriesStat> seriesStats;
    private SeriesStat grandTotalSeries;

    @Data
    @AllArgsConstructor
    public static class SeriesStat {
        private String seriesName; // "Series 100"
        private BigDecimal clientInvoiceTotal; // Revenue
        private BigDecimal contractorInvoiceTotal; // Cost
        private BigDecimal profitLoss; // Revenue - Cost
        private BigDecimal profitMarginPercent; // (Profit / Revenue) * 100
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
