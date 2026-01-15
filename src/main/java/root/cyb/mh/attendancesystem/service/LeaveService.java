package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.LeaveRequest;
import root.cyb.mh.attendancesystem.repository.LeaveRequestRepository;

import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.UserRepository userRepository;

    public LeaveRequest createRequest(Employee employee, LeaveRequest request) {
        request.setEmployee(employee);
        request.setStatus(LeaveRequest.Status.PENDING);
        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        // Notify Supervisor
        if (employee.getReportsTo() != null) {
            sendNewRequestNotification(employee.getReportsTo().getId(), savedRequest);
        }

        // Notify Assistant Supervisor
        if (employee.getReportsToAssistant() != null) {
            sendNewRequestNotification(employee.getReportsToAssistant().getId(), savedRequest);
        }

        // Notify HRs
        List<root.cyb.mh.attendancesystem.model.User> hrUsers = userRepository.findByRole("HR");
        for (root.cyb.mh.attendancesystem.model.User hr : hrUsers) {
            sendNewRequestNotification(hr.getUsername(), savedRequest);
        }

        return savedRequest;
    }

    private void sendNewRequestNotification(String recipientUsername, LeaveRequest request) {
        String title = "New Leave Request";
        String message = String.format("%s has requested leave from %s to %s.",
                request.getEmployee().getName(), request.getStartDate(), request.getEndDate());
        String link = "/leave/manage"; // Link for approvers to manage requests

        try {
            notificationService.sendNotification(
                    recipientUsername,
                    title,
                    message,
                    "LEAVE_NEW",
                    link);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public List<LeaveRequest> getEmployeeHistory(String employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public List<LeaveRequest> getAllRequests() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<LeaveRequest> getRequestsForApprover(String approverId) {
        // Return requests where user is Primary OR Assistant
        return leaveRequestRepository.findByEmployee_ReportsTo_IdOrEmployee_ReportsToAssistant_Id(approverId,
                approverId);
    }

    public Optional<LeaveRequest> findById(Long id) {
        return leaveRequestRepository.findById(id);
    }

    public void updateStatus(Long requestId, LeaveRequest.Status newStatus, String comment, String reviewerRole,
            String reviewerEmail) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Request ID"));

        // HR Logic: Can only update if PENDING
        if ("ROLE_HR".equals(reviewerRole)) {
            if (request.getStatus() != LeaveRequest.Status.PENDING) {
                throw new IllegalStateException("HR cannot modify a request that is already processed.");
            }
        }

        // Admin Logic: Can override anything (no Check)

        request.setStatus(newStatus);
        request.setAdminComment(comment); // Overwrites previous comment if any
        request.setReviewedBy(reviewerEmail + " (" + reviewerRole + ")");

        leaveRequestRepository.save(request);

        // Notify Employee
        String title = "Leave Request Updated";
        String message = String.format("Your leave request for %s to %s has been %s by %s.",
                request.getStartDate(), request.getEndDate(), newStatus, reviewerRole);
        String link = "/leave/employee"; // Link to their leave history

        try {
            notificationService.sendNotification(
                    request.getEmployee().getId(), // Use ID as Principal
                    title,
                    message,
                    "LEAVE_UPDATE",
                    link);
        } catch (Exception e) {
            // Log error but don't fail transaction
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public void deleteRequest(Long id) {
        leaveRequestRepository.deleteById(id);
    }
}
