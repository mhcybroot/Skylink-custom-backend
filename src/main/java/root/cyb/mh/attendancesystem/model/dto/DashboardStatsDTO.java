package root.cyb.mh.attendancesystem.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import lombok.Data;

@Data
public class DashboardStatsDTO {
    // 1. Key Daily Stats
    private long requestsToday;
    private BigDecimal amountRequestedToday;
    private long myActionItems;
    private long urgentPendingRequests;

    // 2. Financials
    private long pendingRequests;
    private BigDecimal pendingAmount;
    private long totalApproved;
    private BigDecimal totalPaidAmount;

    // [NEW] Financial Deep Dive
    private BigDecimal paidThisMonth;
    private BigDecimal averageRequestAmount;
    private BigDecimal unpaidApprovedLiability;

    // 3. Charts Data (Maps for Labels -> Values)
    private long approvedCount;
    private long rejectedCount;
    private long pendingCount;

    // [NEW] Trends & Analysis
    private Map<String, BigDecimal> monthlySpendingTrend; // Month -> Amount
    private Map<String, Long> monthlyVolumeTrend; // Month -> Count
    private Map<String, Long> paymentMethodDistribution;
    private Map<String, BigDecimal> clientCostDistribution;
    private Map<String, Long> priorityDistribution;
    private Map<String, Long> ppwStatusDistribution;
    private Map<String, Long> paymentStatusDistribution;
    private double rejectionRate;

    // 4. Tables / Leaderboards
    private List<PaymentRequest> recentActivity;

    // [NEW] Leaderboards
    private Map<String, BigDecimal> topContractors; // Name -> Total Spend
    private Map<String, Long> topRequesters; // Name -> Count
    private List<PaymentRequest> highValueRequests;
    private long activeContractorsCount;
    private long inactiveContractorsCount;
}
