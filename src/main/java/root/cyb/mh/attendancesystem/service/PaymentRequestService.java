package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;
import root.cyb.mh.attendancesystem.model.enums.RequestStatus;
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

    public PaymentRequest createRequest(PaymentRequest paymentRequest, User requester) {
        paymentRequest.setRequester(requester);
        return saveRequest(paymentRequest);
    }

    public PaymentRequest createRequest(PaymentRequest paymentRequest,
            root.cyb.mh.attendancesystem.model.Employee employeeRequester) {
        paymentRequest.setEmployeeRequester(employeeRequester);
        return saveRequest(paymentRequest);
    }

    private PaymentRequest saveRequest(PaymentRequest paymentRequest) {
        paymentRequest.setRequestDate(LocalDate.now());
        paymentRequest.setStatus(RequestStatus.PENDING);

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

    public void deleteRequest(Long id) {
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
