package root.cyb.mh.attendancesystem.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.model.*;
import root.cyb.mh.attendancesystem.model.enums.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentRequestSpecification {

    public static Specification<PaymentRequest> getFilterSpec(
            LocalDate startDate, LocalDate endDate,
            Long contractorId, Long clientId, Long paymentMethodId,
            String workOrderNumber, String requesterName,
            PaymentPriority priority, RequestStatus status,
            PaymentStatus paymentStatus, PPWStatus ppwUpdateStatus) {
        return getFilterSpec(
                startDate, endDate,
                contractorId != null ? List.of(contractorId) : null,
                clientId != null ? List.of(clientId) : null,
                paymentMethodId != null ? List.of(paymentMethodId) : null,
                workOrderNumber, requesterName,
                priority != null ? List.of(priority) : null,
                status != null ? List.of(status) : null,
                paymentStatus != null ? List.of(paymentStatus) : null,
                ppwUpdateStatus != null ? List.of(ppwUpdateStatus) : null
        );
    }

    public static Specification<PaymentRequest> getFilterSpec(
            LocalDate startDate, LocalDate endDate,
            List<Long> contractorIds, List<Long> clientIds, List<Long> paymentMethodIds,
            String workOrderNumber,
            String requesterName,
            List<PaymentPriority> priorities,
            List<RequestStatus> statuses,
            List<PaymentStatus> paymentStatuses,
            List<PPWStatus> ppwUpdateStatuses) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Date Range
            if (startDate != null && endDate != null) {
                predicates.add(cb.between(root.get("requestDate"), startDate, endDate));
            } else if (startDate != null) {
                predicates.add(cb.equal(root.get("requestDate"), startDate));
            }

            // Contractor
            if (contractorIds != null && !contractorIds.isEmpty()) {
                predicates.add(root.get("contractor").get("id").in(contractorIds));
            }

            // Client
            if (clientIds != null && !clientIds.isEmpty()) {
                predicates.add(root.get("client").get("id").in(clientIds));
            }

            // Payment Method
            if (paymentMethodIds != null && !paymentMethodIds.isEmpty()) {
                predicates.add(root.get("paymentMethod").get("id").in(paymentMethodIds));
            }

            // Work Order
            if (workOrderNumber != null && !workOrderNumber.trim().isEmpty()) {
                predicates
                        .add(cb.like(cb.lower(root.get("workOrderNumber")), "%" + workOrderNumber.toLowerCase() + "%"));
            }

            // Priority
            if (priorities != null && !priorities.isEmpty()) {
                predicates.add(root.get("priority").in(priorities));
            }

            // Status
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            // Payment Status
            if (paymentStatuses != null && !paymentStatuses.isEmpty()) {
                predicates.add(root.get("paymentStatus").in(paymentStatuses));
            }

            // PPW Status
            if (ppwUpdateStatuses != null && !ppwUpdateStatuses.isEmpty()) {
                predicates.add(root.get("ppwUpdateStatus").in(ppwUpdateStatuses));
            }

            // Requester (Name or Username)
            if (requesterName != null && !requesterName.trim().isEmpty()) {
                String pattern = "%" + requesterName.toLowerCase() + "%";

                Join<PaymentRequest, User> userJoin = root.join("requester", JoinType.LEFT);
                Join<PaymentRequest, Employee> empJoin = root.join("employeeRequester", JoinType.LEFT);

                Predicate userMatch = cb.like(cb.lower(userJoin.get("username")), pattern);
                Predicate empMatch = cb.like(cb.lower(empJoin.get("name")), pattern);

                predicates.add(cb.or(userMatch, empMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
