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
    private long invoicedWorkOrders;

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

        // Operational Metrics
        private long closedWorkOrders;
        private long invoicedWorkOrders;
        private BigDecimal avgCost;
        private BigDecimal avgRevenue;
        private BigDecimal avgMargin;

        // Financial Aggregates
        private BigDecimal totalContractorPaid;
        private BigDecimal totalClientPaid; // Realized Revenue
        private BigDecimal totalWriteOffs;
        private BigDecimal totalClientDiscount;
    }

    // Monthly Comparison
    private List<MonthlyStat> monthlyStats;

    // Monthly Performance by Series
    private List<MonthlySeriesStat> monthlySeriesPerformance;

    // Geographic Analysis
    private List<StateStat> topStatesByVolume;
    private List<StateStat> topStatesByRevenue;
    private List<ZipStat> topZipsByVolume;
    private List<ZipStat> topZipsByRevenue;

    // State Efficiency Snapshot
    private List<StateStat> highMarginStates;
    private List<StateStat> moderateMarginHighVolumeStates;
    private List<StateStat> lowRiskStates;

    @Data
    @AllArgsConstructor
    public static class MonthlyStat {
        private String month; // e.g. "Jan 2025"
        private String yearMonth; // e.g. "2025-01" for sorting
        private long totalWorkOrders;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin;
    }

    @Data
    @AllArgsConstructor
    public static class MonthlySeriesStat {
        private String month; // "Jan 2026"
        private String yearMonth; // "2026-01" (for sorting)
        private String seriesName; // "Series 100", "Series 200", etc.
        private long workOrderCount;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin; // Percentage
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

    @Data
    @AllArgsConstructor
    public static class StateStat {
        private String state;
        private long count;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin;
    }

    @Data
    @AllArgsConstructor
    public static class ZipStat {
        private String zip;
        private long count;
        private BigDecimal revenue;
    }
}
