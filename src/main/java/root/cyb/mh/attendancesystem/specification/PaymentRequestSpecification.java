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
            String workOrderNumber,
            String requesterName,
            PaymentPriority priority,
            RequestStatus status,
            PaymentStatus paymentStatus,
            PPWStatus ppwUpdateStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Date Range
            if (startDate != null && endDate != null) {
                predicates.add(cb.between(root.get("requestDate"), startDate, endDate));
            } else if (startDate != null) {
                predicates.add(cb.equal(root.get("requestDate"), startDate));
            }

            // Contractor
            if (contractorId != null) {
                predicates.add(cb.equal(root.get("contractor").get("id"), contractorId));
            }

            // Client
            if (clientId != null) {
                predicates.add(cb.equal(root.get("client").get("id"), clientId));
            }

            // Payment Method
            if (paymentMethodId != null) {
                predicates.add(cb.equal(root.get("paymentMethod").get("id"), paymentMethodId));
            }

            // Work Order
            if (workOrderNumber != null && !workOrderNumber.trim().isEmpty()) {
                predicates
                        .add(cb.like(cb.lower(root.get("workOrderNumber")), "%" + workOrderNumber.toLowerCase() + "%"));
            }

            // Priority
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // Status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Payment Status
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }

            // PPW Status
            if (ppwUpdateStatus != null) {
                predicates.add(cb.equal(root.get("ppwUpdateStatus"), ppwUpdateStatus));
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
