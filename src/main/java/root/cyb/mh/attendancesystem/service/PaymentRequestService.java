package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;
import root.cyb.mh.attendancesystem.model.enums.RequestStatus;
import root.cyb.mh.attendancesystem.model.enums.PaymentStatus;
import root.cyb.mh.attendancesystem.model.enums.PPWStatus;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentRequestService {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.UserRepository userRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.PaymentRequestActivityRepository paymentRequestActivityRepository;

    public void logActivity(PaymentRequest request, String username, String actionType, String details) {
        root.cyb.mh.attendancesystem.model.PaymentRequestActivity activity = new root.cyb.mh.attendancesystem.model.PaymentRequestActivity();
        activity.setPaymentRequest(request);
        activity.setUsername(username);
        activity.setActionType(actionType);
        activity.setDetails(details);
        activity.setTimestamp(java.time.LocalDateTime.now());
        paymentRequestActivityRepository.save(activity);
    }

    public List<root.cyb.mh.attendancesystem.model.PaymentRequestActivity> getActivitiesForRequest(Long requestId) {
        return paymentRequestActivityRepository.findByPaymentRequestIdOrderByTimestampDesc(requestId);
    }

    public PaymentRequest createRequest(PaymentRequest paymentRequest, User requester) {
        paymentRequest.setRequester(requester);
        PaymentRequest saved = saveRequest(paymentRequest);
        notifyAdminsNewRequest(saved);
        logActivity(saved, requester.getUsername(), "CREATED", "Request submitted with amount $" + saved.getAmount());
        return saved;
    }

    public PaymentRequest createRequest(PaymentRequest paymentRequest,
            root.cyb.mh.attendancesystem.model.Employee employeeRequester) {
        paymentRequest.setEmployeeRequester(employeeRequester);
        PaymentRequest saved = saveRequest(paymentRequest);
        notifyAdminsNewRequest(saved);
        logActivity(saved, employeeRequester.getName(), "CREATED", "Request submitted with amount $" + saved.getAmount());
        return saved;
    }

    private PaymentRequest saveRequest(PaymentRequest paymentRequest) {
        paymentRequest.setRequestDate(LocalDate.now());
        paymentRequest.setStatus(RequestStatus.PENDING);
        if (paymentRequest.getPaymentStatus() == null) {
            paymentRequest.setPaymentStatus(PaymentStatus.UNPAID);
        }
        if (paymentRequest.getPpwUpdateStatus() == null) {
            paymentRequest.setPpwUpdateStatus(PPWStatus.NOT_UPDATED);
        }

        // Backfill deprecated fields for DB compatibility
        if (paymentRequest.getContractor() != null) {
            paymentRequest.setContractorName(paymentRequest.getContractor().getName());
        }
        if (paymentRequest.getClient() != null) {
            paymentRequest.setClientCode(paymentRequest.getClient().getCode());
        }
        if (paymentRequest.getPaymentMethod() != null) {
            paymentRequest.setPaymentMethodId(paymentRequest.getPaymentMethod().getMethodName());
        }

        return paymentRequestRepository.save(paymentRequest);
    }

    public List<PaymentRequest> getAllRequests() {
        return paymentRequestRepository.findAllByOrderByLastModifiedDesc();
    }

    public List<PaymentRequest> getRequestsByRequester(User requester) {
        return paymentRequestRepository.findByRequesterOrderByLastModifiedDesc(requester);
    }

    public List<PaymentRequest> getRequestsByRequester(root.cyb.mh.attendancesystem.model.Employee employeeRequester) {
        return paymentRequestRepository.findByEmployeeRequesterOrderByLastModifiedDesc(employeeRequester);
    }

    public List<PaymentRequest> getTeamRequests(root.cyb.mh.attendancesystem.model.Employee supervisor) {
        List<root.cyb.mh.attendancesystem.model.Employee> subordinates = employeeRepository
                .findByReportsTo_IdOrReportsToAssistant_Id(supervisor.getId(), supervisor.getId());

        if (subordinates.isEmpty()) {
            return List.of();
        }
        return paymentRequestRepository.findByEmployeeRequesterInOrderByLastModifiedDesc(subordinates);
    }

    public Optional<PaymentRequest> getRequestById(Long id) {
        return paymentRequestRepository.findById(id);
    }

    public PaymentRequest updateRequest(PaymentRequest paymentRequest) {
        return paymentRequestRepository.save(paymentRequest);
    }

    private void notifyAdminsNewRequest(PaymentRequest request) {
        String requesterName = getRequesterName(request);
        String message = "New Payment Request submitted by " + requesterName;
        String link = "/payment-requests/" + request.getId();

        // 1. Notify Admins
        List<User> admins = userRepository.findByRole("ADMIN");
        for (User admin : admins) {
            notificationService.sendNotification(admin.getUsername(), "New Payment Request", message, "INFO", link);
        }

        // 2. Notify HR
        List<User> hrs = userRepository.findByRole("HR");
        for (User hr : hrs) {
            // Avoid duplicate if user is both ADMIN and HR (optional check, but good
            // practice)
            if (admins.stream().noneMatch(a -> a.getUsername().equals(hr.getUsername()))) {
                notificationService.sendNotification(hr.getUsername(), "New Payment Request", message, "INFO", link);
            }
        }

        // 3. Notify Supervisor
        if (request.getEmployeeRequester() != null) {
            root.cyb.mh.attendancesystem.model.Employee supervisor = request.getEmployeeRequester().getReportsTo();
            if (supervisor != null) {
                // Use supervisor.getId() as username based on existing pattern
                // Avoid if supervisor is already notified as Admin/HR? (Unlikely but possible)
                // Just send it. Duplicate notifications are better than missing ones, or logic
                // is complex to dedup across entities.
                notificationService.sendNotification(supervisor.getId(), "New Payment Request (Team)", message, "INFO",
                        link);
            }
        }
    }

    public void notifyRequesterStatusChange(PaymentRequest request) {
        String username = null;
        if (request.getRequester() != null) {
            username = request.getRequester().getUsername();
        } else if (request.getEmployeeRequester() != null) {
            username = request.getEmployeeRequester().getId();
        }

        if (username != null) {
            String title = "Request " + request.getStatus();
            String message = "Your Payment Request #" + request.getId() + " was " + request.getStatus();
            String link = "/payment-requests/" + request.getId();
            String type = request.getStatus() == RequestStatus.APPROVED ? "SUCCESS" : "ERROR"; // or WARNING
            notificationService.sendNotification(username, title, message, type, link);
        }
    }

    public void notifyRequesterPaymentStatusChange(PaymentRequest request) {
        String username = null;
        if (request.getRequester() != null) {
            username = request.getRequester().getUsername();
        } else if (request.getEmployeeRequester() != null) {
            username = request.getEmployeeRequester().getId();
        }

        if (username != null) {
            String title = "Payment Status Updated";
            String message = "Payment Request #" + request.getId() + " is now " + request.getPaymentStatus();
            String link = "/payment-requests/" + request.getId();
            String type = "INFO";
            if (request.getPaymentStatus() == root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID) {
                type = "SUCCESS";
            }
            notificationService.sendNotification(username, title, message, type, link);
        }
    }

    public void notifyRequesterUpdate(PaymentRequest request, java.util.List<String> changes) {
        if (changes == null || changes.isEmpty())
            return;

        String username = null;
        if (request.getRequester() != null) {
            username = request.getRequester().getUsername();
        } else if (request.getEmployeeRequester() != null) {
            username = request.getEmployeeRequester().getId();
        }

        if (username != null) {
            String title = "Request Updated";
            String link = "/payment-requests/" + request.getId();
            String type = "INFO";

            // Priority Status Messages
            if (changes.contains("Status")) {
                if (request.getStatus() == root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED) {
                    title = "Request APPROVED";
                    type = "SUCCESS";
                } else if (request.getStatus() == root.cyb.mh.attendancesystem.model.enums.RequestStatus.REJECTED) {
                    title = "Request REJECTED";
                    type = "ERROR";
                }
            } else if (changes.contains("Payment Status")
                    && request.getPaymentStatus() == root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID) {
                title = "Payment Sent (PAID)";
                type = "SUCCESS";
            }

            String message;
            if (changes.size() == 1) {
                message = "Your request #" + request.getId() + " updated: " + changes.get(0) + " changed.";
            } else {
                message = "Your request #" + request.getId() + " updated. Changes: " + String.join(", ", changes);
            }

            notificationService.sendNotification(username, title, message, type, link);
        }
    }

    private String getRequesterName(PaymentRequest request) {
        if (request.getRequester() != null)
            return request.getRequester().getUsername();
        if (request.getEmployeeRequester() != null)
            return request.getEmployeeRequester().getName();
        return "Unknown";
    }

    public void deleteRequest(Long id) {
        paymentRequestActivityRepository.deleteAll(paymentRequestActivityRepository.findByPaymentRequestIdOrderByTimestampDesc(id));
        paymentRequestRepository.deleteById(id);
    }

    public void sortRequests(List<PaymentRequest> requests, String sortField, String sortDir) {
        java.util.Comparator<PaymentRequest> comparator = null;

        switch (sortField) {
            case "id":
                comparator = java.util.Comparator.comparing(PaymentRequest::getId);
                break;
            case "requestDate":
                comparator = java.util.Comparator.comparing(PaymentRequest::getRequestDate);
                break;
            case "requester":
                comparator = java.util.Comparator.comparing(r -> {
                    if (r.getRequester() != null)
                        return r.getRequester().getUsername().toLowerCase();
                    if (r.getEmployeeRequester() != null)
                        return r.getEmployeeRequester().getName().toLowerCase();
                    return "";
                });
                break;
            case "workOrderNumber":
                comparator = java.util.Comparator.comparing(r -> r.getWorkOrderNumber().toLowerCase());
                break;
            case "amount":
                comparator = java.util.Comparator.comparing(PaymentRequest::getAmount);
                break;
            case "contractorName":
                comparator = java.util.Comparator.comparing(r -> {
                    if (r.getContractor() != null)
                        return r.getContractor().getName().toLowerCase();
                    return r.getContractorName() != null ? r.getContractorName().toLowerCase() : "";
                });
                break;
            case "priority":
                comparator = java.util.Comparator.comparing(PaymentRequest::getPriority);
                break;
            case "status":
                comparator = java.util.Comparator.comparing(PaymentRequest::getStatus);
                break;
            default: // "lastModified" or others
                comparator = java.util.Comparator.comparing(PaymentRequest::getLastModified,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
                break;
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        requests.sort(comparator);
    }
}
