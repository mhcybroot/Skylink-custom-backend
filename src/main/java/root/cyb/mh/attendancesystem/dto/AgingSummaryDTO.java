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
    }
}
