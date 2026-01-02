package root.cyb.mh.attendancesystem.model.dto;

import java.math.BigDecimal;
import java.util.List;
import root.cyb.mh.attendancesystem.model.PaymentRequest;

public class DashboardStatsDTO {
    private long requestsToday;
    private BigDecimal amountRequestedToday;
    private long pendingRequests;
    private BigDecimal pendingAmount;
    private long urgentPendingRequests;
    private long totalApproved;
    private BigDecimal totalPaidAmount;
    private long myActionItems; // Requests waiting for this user

    // Charts Data
    private long approvedCount;
    private long rejectedCount;
    private long pendingCount;

    private List<PaymentRequest> recentActivity;

    // Getters and Setters
    public long getRequestsToday() {
        return requestsToday;
    }

    public void setRequestsToday(long requestsToday) {
        this.requestsToday = requestsToday;
    }

    public BigDecimal getAmountRequestedToday() {
        return amountRequestedToday;
    }

    public void setAmountRequestedToday(BigDecimal amountRequestedToday) {
        this.amountRequestedToday = amountRequestedToday;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(long pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public long getUrgentPendingRequests() {
        return urgentPendingRequests;
    }

    public void setUrgentPendingRequests(long urgentPendingRequests) {
        this.urgentPendingRequests = urgentPendingRequests;
    }

    public long getTotalApproved() {
        return totalApproved;
    }

    public void setTotalApproved(long totalApproved) {
        this.totalApproved = totalApproved;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public long getMyActionItems() {
        return myActionItems;
    }

    public void setMyActionItems(long myActionItems) {
        this.myActionItems = myActionItems;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public List<PaymentRequest> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<PaymentRequest> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
