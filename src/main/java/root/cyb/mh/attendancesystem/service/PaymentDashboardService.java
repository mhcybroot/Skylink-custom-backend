package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.dto.DashboardStatsDTO;
import root.cyb.mh.attendancesystem.model.enums.PaymentStatus;
import root.cyb.mh.attendancesystem.model.enums.PaymentPriority;
import root.cyb.mh.attendancesystem.model.enums.RequestStatus;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PaymentDashboardService {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private SystemSettingService systemSettingService;

    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        LocalDate today = LocalDate.now();

        // 1. Key Daily Stats (Existing)
        stats.setRequestsToday(paymentRequestRepository.countByRequestDate(today));
        BigDecimal todayAmt = paymentRequestRepository.sumAmountByRequestDate(today);
        stats.setAmountRequestedToday(todayAmt != null ? todayAmt : BigDecimal.ZERO);
        stats.setMyActionItems(paymentRequestRepository.countPendingRequests());
        stats.setUrgentPendingRequests(
                paymentRequestRepository.countByStatusAndPriority(RequestStatus.PENDING, PaymentPriority.URGENT));

        // 2. Financials (Enhanced)
        long pending = paymentRequestRepository.countByStatus(RequestStatus.PENDING);
        stats.setPendingRequests(pending);
        stats.setPendingCount(pending);

        BigDecimal pendingAmt = paymentRequestRepository.sumAmountByStatus(RequestStatus.PENDING);
        stats.setPendingAmount(pendingAmt != null ? pendingAmt : BigDecimal.ZERO);

        long approved = paymentRequestRepository.countByStatus(RequestStatus.APPROVED);
        stats.setTotalApproved(approved);
        stats.setApprovedCount(approved);

        BigDecimal paidAmt = paymentRequestRepository.sumAmountByPaymentStatus(PaymentStatus.PAID);
        stats.setTotalPaidAmount(paidAmt != null ? paidAmt : BigDecimal.ZERO);

        // New Financial Metrics
        LocalDate startOfMonth = today.withDayOfMonth(1);
        BigDecimal paidMonth = paymentRequestRepository.sumPaidAmountBetween(startOfMonth, today);
        stats.setPaidThisMonth(paidMonth != null ? paidMonth : BigDecimal.ZERO);

        BigDecimal avgAmt = paymentRequestRepository.findAverageRequestAmount();
        stats.setAverageRequestAmount(avgAmt != null ? avgAmt : BigDecimal.ZERO);

        BigDecimal liability = paymentRequestRepository.findUnpaidApprovedLiability();
        stats.setUnpaidApprovedLiability(liability != null ? liability : BigDecimal.ZERO);

        // 3. Trends & Distributions (New)
        stats.setRejectedCount(paymentRequestRepository.countByStatus(RequestStatus.REJECTED));

        // Rejection Rate
        long totalReqs = stats.getApprovedCount() + stats.getRejectedCount() + stats.getPendingCount();
        stats.setRejectionRate(totalReqs > 0 ? (double) stats.getRejectedCount() / totalReqs * 100 : 0.0);

        // Map Data
        stats.setMonthlySpendingTrend(
                convertTrendData(paymentRequestRepository.findMonthlySpendingTrend(today.minusMonths(6))));
        stats.setMonthlyVolumeTrend(
                convertTrendCountData(paymentRequestRepository.findMonthlyVolumeTrend(today.minusMonths(6))));

        stats.setPaymentMethodDistribution(convertCountData(paymentRequestRepository.countByPaymentMethodGroup()));
        stats.setClientCostDistribution(convertAmountData(paymentRequestRepository.sumAmountByClientGroup()));
        stats.setPriorityDistribution(convertCountData(paymentRequestRepository.countByPriorityGroup()));
        stats.setPpwStatusDistribution(convertCountData(paymentRequestRepository.countByPpwStatusGroup()));
        stats.setPaymentStatusDistribution(convertCountData(paymentRequestRepository.countByPaymentStatusGroup()));

        // 7. Recent Activity (Filtered)
        stats.setRecentActivity(paymentRequestRepository.findTop5ByLastModifiedIsNotNullOrderByLastModifiedDesc());
        stats.setTopContractors(convertAmountData(paymentRequestRepository
                .findTopContractorsBySpend(org.springframework.data.domain.PageRequest.of(0, 5))));
        stats.setTopRequesters(convertCountData(
                paymentRequestRepository.findTopRequesters(org.springframework.data.domain.PageRequest.of(0, 5))));

        // Configurable Threshold
        String limitStr = systemSettingService.getValue("DASHBOARD_HIGH_VALUE_THRESHOLD", "1000");
        java.math.BigDecimal threshold = new java.math.BigDecimal(limitStr);
        stats.setHighValueThreshold(threshold);

        stats.setHighValueRequests(
                paymentRequestRepository.findTop5ByAmountGreaterThanOrderByRequestDateDesc(threshold));

        String updateLimitStr = systemSettingService.getValue("PAYMENT_REVIEW_UPDATE_LIMIT", "3");
        stats.setReviewUpdateLimit(Integer.parseInt(updateLimitStr));

        stats.setActiveContractorsCount(paymentRequestRepository.countActiveContractors());
        stats.setInactiveContractorsCount(paymentRequestRepository.countInactiveContractors());

        return stats;
    }

    // Helpers
    private java.util.Map<String, BigDecimal> convertAmountData(java.util.List<Object[]> data) {
        java.util.Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        for (Object[] row : data) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            BigDecimal value = (BigDecimal) row[1];
            map.put(key, value);
        }
        return map;
    }

    private java.util.Map<String, Long> convertCountData(java.util.List<Object[]> data) {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        for (Object[] row : data) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long value = (Long) row[1];
            map.put(key, value);
        }
        return map;
    }

    // Specialized for Year, Month, Sum schema
    private java.util.Map<String, BigDecimal> convertTrendData(java.util.List<Object[]> data) {
        java.util.Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        for (Object[] row : data) {
            int year = (int) row[0];
            int month = (int) row[1];
            BigDecimal value = (BigDecimal) row[2];
            String key = java.time.Month.of(month).name().substring(0, 3) + " " + year;
            map.put(key, value);
        }
        return map;
    }

    // Specialized for Year, Month, Count schema
    private java.util.Map<String, Long> convertTrendCountData(java.util.List<Object[]> data) {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        for (Object[] row : data) {
            int year = (int) row[0];
            int month = (int) row[1];
            Long value = (Long) row[2];
            String key = java.time.Month.of(month).name().substring(0, 3) + " " + year;
            map.put(key, value);
        }
        return map;
    }
}
