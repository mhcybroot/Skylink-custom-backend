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

    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        LocalDate today = LocalDate.now();

        // 1. Requests Today
        stats.setRequestsToday(paymentRequestRepository.countByRequestDate(today));

        // 2. Amount Requested Today
        BigDecimal todayAmt = paymentRequestRepository.sumAmountByRequestDate(today);
        stats.setAmountRequestedToday(todayAmt != null ? todayAmt : BigDecimal.ZERO);

        // 3. Pending Requests
        long pending = paymentRequestRepository.countByStatus(RequestStatus.PENDING);
        stats.setPendingRequests(pending);
        stats.setPendingCount(pending); // Chart data

        // 4. Pending Amount
        BigDecimal pendingAmt = paymentRequestRepository.sumAmountByStatus(RequestStatus.PENDING);
        stats.setPendingAmount(pendingAmt != null ? pendingAmt : BigDecimal.ZERO);

        // 5. Urgent Pending
        stats.setUrgentPendingRequests(
                paymentRequestRepository.countByStatusAndPriority(RequestStatus.PENDING, PaymentPriority.URGENT));

        // 6. Total Approved
        long approved = paymentRequestRepository.countByStatus(RequestStatus.APPROVED);
        stats.setTotalApproved(approved);
        stats.setApprovedCount(approved); // Chart data

        // 7. Total Paid Amount
        BigDecimal paidAmt = paymentRequestRepository.sumAmountByPaymentStatus(PaymentStatus.PAID);
        stats.setTotalPaidAmount(paidAmt != null ? paidAmt : BigDecimal.ZERO);

        // 8. Recent Activity
        stats.setRecentActivity(paymentRequestRepository.findTop5ByOrderByLastModifiedDesc());

        // 9. Status Dist (Rejected)
        stats.setRejectedCount(paymentRequestRepository.countByStatus(RequestStatus.REJECTED));

        // 10. My Action Items (Ideally filtered by user, currently mapped to all
        // pending for Admin)
        stats.setMyActionItems(paymentRequestRepository.countPendingRequests());

        return stats;
    }
}
