package root.cyb.mh.attendancesystem.specification;

import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.model.WorkOrder;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderSpecifications {

    public static Specification<WorkOrder> withFilters(String status,
            Boolean clientInvoicePaid,
            Boolean contractorInvoicePaid,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            String workType,
            String clientName,
            String contractorName) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Date Range Filter (Fixed: Moved from dateReceived to dateDueClient)
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("dateDueClient"), startDate, endDate));
            }

            // Global Text Search
            if (search != null && !search.trim().isEmpty()) {
                String searchLike = "%" + search.trim().toLowerCase() + "%";
                Predicate woNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("woNumber")), searchLike);
                Predicate invNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceNumber")), searchLike);
                Predicate ppwNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("ppwNumber")), searchLike);
                Predicate loanNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("loanNumber")), searchLike);
                Predicate address = criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), searchLike);

                predicates.add(criteriaBuilder.or(woNum, invNum, ppwNum, loanNum, address));
            }

            // Work Type Filter
            if (workType != null && !workType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("workType")),
                        "%" + workType.trim().toLowerCase() + "%"));
            }

            // Client Name Filter
            // Note: client is a ManyToOne, but we also have originalClientString.
            // Ideally we search the relationship, or the string if relationship is null.
            // For now, let's search the relationship name if we can, or simplified
            // approach:
            // Since we have Client entity, let's use a Join if possible, or just exact
            // match if ID passed?
            // The user input is likely text. Let's try to join or just stick to simple
            // string match on originalClientString fallback?
            // Better: Search in Client.name
            if (clientName != null && !clientName.trim().isEmpty()) {
                // Determine if we need to join.
                // Simple version: Filter by Client Name in the joined entity
                // root.join("client").get("name")
                // Need to handle potential nulls if we want left join, but 'client' field might
                // be null.
                // Safest to do:
                // Predicate clientJoin =
                // criteriaBuilder.like(criteriaBuilder.lower(root.join("client",
                // jakarta.persistence.criteria.JoinType.LEFT).get("name")), "%" +
                // clientName.toLowerCase() + "%");
                // Predicate clientString =
                // criteriaBuilder.like(criteriaBuilder.lower(root.get("originalClientString")),
                // "%" + clientName.toLowerCase() + "%");
                // predicates.add(criteriaBuilder.or(clientJoin, clientString));
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("originalClientString")),
                        "%" + clientName.trim().toLowerCase() + "%"));
            }

            // Contractor Name Filter
            if (contractorName != null && !contractorName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("originalContractorString")),
                        "%" + contractorName.trim().toLowerCase() + "%"));
            }

            // Status Filter
            if (status != null && !status.isEmpty()) {
                if ("closed".equalsIgnoreCase(status)) {
                    // Status is 'Complete' or 'Closed'
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete"),
                            criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed")));
                } else if ("cancelled".equalsIgnoreCase(status)) {
                    // Status is 'Cancelled'
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "cancelled"));
                } else if ("open".equalsIgnoreCase(status)) {
                    // Status is NOT 'Complete', 'Closed', or 'Cancelled'
                    Predicate isComplete = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete");
                    Predicate isClosed = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed");
                    Predicate isCancelled = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")),
                            "cancelled");

                    predicates.add(criteriaBuilder.not(criteriaBuilder.or(isComplete, isClosed, isCancelled)));
                } else if (!"all".equalsIgnoreCase(status)) {
                    // Exact status match if specific status provided and not handled above
                    predicates.add(
                            criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), status.toLowerCase()));
                }
            }

            // Client Invoice Paid Filter
            if (clientInvoicePaid != null) {
                Predicate isPaid = criteriaBuilder.equal(root.get("clientInvoicePaid"), clientInvoicePaid);
                // Optional: Ensure it has a total? Maybe not strictly required for "Paid
                // status" check but good for data integrity
                // Keeping existing logic
                Predicate hasTotal = criteriaBuilder.isNotNull(root.get("clientInvoiceTotal"));
                // Predicate positiveTotal =
                // criteriaBuilder.greaterThan(root.get("clientInvoiceTotal"), BigDecimal.ZERO);
                // Remove positiveTotal constraint for now to be broader
                predicates.add(criteriaBuilder.and(isPaid, hasTotal));
            }

            // Contractor Invoice Paid Filter
            if (contractorInvoicePaid != null) {
                Predicate isPaid = criteriaBuilder.equal(root.get("contractorInvoicePaid"), contractorInvoicePaid);
                Predicate hasTotal = criteriaBuilder.isNotNull(root.get("contractorInvoiceTotal"));
                predicates.add(criteriaBuilder.and(isPaid, hasTotal));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
