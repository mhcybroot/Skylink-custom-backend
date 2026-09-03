package root.cyb.mh.attendancesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgingSummaryDTO {

    // Total Unpaid
    private long totalUnpaidCount = 0;
    private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;

    // Standard Due (Tier 1)
    private long standardDueCount = 0;
    private BigDecimal standardDueAmount = BigDecimal.ZERO;

    // Past Due (Tier 2)
    private long pastDueCount = 0;
    private BigDecimal pastDueAmount = BigDecimal.ZERO;

    // Critical Delinquent (Tier 3)
    private long criticalDueCount = 0;
    private BigDecimal criticalDueAmount = BigDecimal.ZERO;

    // Within Terms (Current)
    private long withinTermsCount = 0;
    private BigDecimal withinTermsAmount = BigDecimal.ZERO;

    // Active Global Default Settings
    private ClientDueConfig defaultConfig;

    // List of All Configured Client Overrides
    private List<ClientDueConfig> clientConfigs = new ArrayList<>();

    // Client Portfolio Aging Breakdown
    private List<ClientAgingStat> clientStats = new ArrayList<>();

    // Series Portfolio Aging Breakdown (100-199, 200-299, etc.)
    private List<SeriesAgingStat> seriesStats = new ArrayList<>();

    // Portfolio Weighted Average Days
    private double portfolioAverageDays = 0.0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientAgingStat {
        private String clientIdentifier;
        private String clientName;
        private int normalDueDays;
        private int overdueDays;
        private int criticalDueDays;
        private boolean customConfig;

        private long totalUnpaidCount = 0;
        private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;

        private long standardDueCount = 0;
        private BigDecimal standardDueAmount = BigDecimal.ZERO;

        private long pastDueCount = 0;
        private BigDecimal pastDueAmount = BigDecimal.ZERO;

        private long criticalDueCount = 0;
        private BigDecimal criticalDueAmount = BigDecimal.ZERO;

        private long withinTermsCount = 0;
        private BigDecimal withinTermsAmount = BigDecimal.ZERO;

        private BigDecimal totalWeightedDays = BigDecimal.ZERO;
        private long totalDaysSum = 0;

        public double getWithinTermsPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && withinTermsAmount != null) {
                return withinTermsAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) withinTermsCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getStandardPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && standardDueAmount != null) {
                return standardDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) standardDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getOverduePercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && pastDueAmount != null) {
                return pastDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) pastDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getCriticalPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && criticalDueAmount != null) {
                return criticalDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) criticalDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public boolean isPendingInvoiceAmount() {
            return totalUnpaidCount > 0 && (totalUnpaidAmount == null || totalUnpaidAmount.compareTo(BigDecimal.ZERO) == 0);
        }

        public double getWeightedAverageDays() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && totalWeightedDays != null) {
                return totalWeightedDays.divide(totalUnpaidAmount, 1, java.math.RoundingMode.HALF_UP).doubleValue();
            }
            if (totalUnpaidCount > 0) {
                return Math.round(((double) totalDaysSum / totalUnpaidCount) * 10.0) / 10.0;
            }
            return 0.0;
        }

        public String getRiskScore() {
            double critPct = getCriticalPercent();
            double overduePct = getOverduePercent();
            double avgDays = getWeightedAverageDays();

            if (critPct >= 30.0 || avgDays >= 60.0) {
                return "HIGH";
            } else if (critPct >= 10.0 || overduePct >= 25.0 || avgDays >= 48.0) {
                return "MODERATE";
            } else {
                return "LOW";
            }
        }

        public String getRiskScoreLabel() {
            String score = getRiskScore();
            switch (score) {
                case "HIGH":
                    return "High Risk";
                case "MODERATE":
                    return "Moderate";
                default:
                    return "Low Risk";
            }
        }

        public String getRiskBadgeClass() {
            String score = getRiskScore();
            switch (score) {
                case "HIGH":
                    return "bg-danger bg-opacity-10 text-danger border border-danger-subtle";
                case "MODERATE":
                    return "bg-warning bg-opacity-10 text-warning-emphasis border border-warning-subtle";
                default:
                    return "bg-success bg-opacity-10 text-success border border-success-subtle";
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesAgingStat {
        private String seriesName;       // "Series 100"
        private String seriesRange;      // "100–199"
        private int seriesBase;          // 100
        private int clientCount = 0;
        private List<String> clientCodes = new ArrayList<>();

        private long totalUnpaidCount = 0;
        private BigDecimal totalUnpaidAmount = BigDecimal.ZERO;

        private long withinTermsCount = 0;
        private BigDecimal withinTermsAmount = BigDecimal.ZERO;

        private long standardDueCount = 0;
        private BigDecimal standardDueAmount = BigDecimal.ZERO;

        private long pastDueCount = 0;
        private BigDecimal pastDueAmount = BigDecimal.ZERO;

        private long criticalDueCount = 0;
        private BigDecimal criticalDueAmount = BigDecimal.ZERO;

        private BigDecimal totalWeightedDays = BigDecimal.ZERO;
        private long totalDaysSum = 0;

        public double getWithinTermsPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && withinTermsAmount != null) {
                return withinTermsAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) withinTermsCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getStandardPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && standardDueAmount != null) {
                return standardDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) standardDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getOverduePercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && pastDueAmount != null) {
                return pastDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) pastDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public double getCriticalPercent() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && criticalDueAmount != null) {
                return criticalDueAmount.divide(totalUnpaidAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            if (totalUnpaidCount > 0) {
                return ((double) criticalDueCount / totalUnpaidCount) * 100.0;
            }
            return 0.0;
        }

        public boolean isPendingInvoiceAmount() {
            return totalUnpaidCount > 0 && (totalUnpaidAmount == null || totalUnpaidAmount.compareTo(BigDecimal.ZERO) == 0);
        }

        public double getWeightedAverageDays() {
            if (totalUnpaidAmount != null && totalUnpaidAmount.compareTo(BigDecimal.ZERO) > 0 && totalWeightedDays != null) {
                return totalWeightedDays.divide(totalUnpaidAmount, 1, java.math.RoundingMode.HALF_UP).doubleValue();
            }
            if (totalUnpaidCount > 0) {
                return Math.round(((double) totalDaysSum / totalUnpaidCount) * 10.0) / 10.0;
            }
            return 0.0;
        }

        public String getRiskScore() {
            double critPct = getCriticalPercent();
            double overduePct = getOverduePercent();
            double avgDays = getWeightedAverageDays();

            if (critPct >= 30.0 || avgDays >= 60.0) {
                return "HIGH";
            } else if (critPct >= 10.0 || overduePct >= 25.0 || avgDays >= 48.0) {
                return "MODERATE";
            } else {
                return "LOW";
            }
        }

        public String getRiskScoreLabel() {
            String score = getRiskScore();
            switch (score) {
                case "HIGH":
                    return "High Risk";
                case "MODERATE":
                    return "Moderate";
                default:
                    return "Low Risk";
            }
        }

        public String getRiskBadgeClass() {
            String score = getRiskScore();
            switch (score) {
                case "HIGH":
                    return "bg-danger bg-opacity-10 text-danger border border-danger-subtle";
                case "MODERATE":
                    return "bg-warning bg-opacity-10 text-warning-emphasis border border-warning-subtle";
                default:
                    return "bg-success bg-opacity-10 text-success border border-success-subtle";
            }
        }

        public String getClientsSummary() {
            if (clientCodes == null || clientCodes.isEmpty()) {
                return "";
            }
            if (clientCodes.size() <= 4) {
                return String.join(", ", clientCodes);
            }
            return String.join(", ", clientCodes.subList(0, 4)) + ", +" + (clientCodes.size() - 4) + " more";
        }
    }
}
