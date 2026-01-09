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
            LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Date Range Filter
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("dateReceived"), startDate, endDate));
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
                    // Status is NOT 'Complete', 'Closed', or 'Cancelled' (and not null preferably,
                    // or allow null as open)
                    // The original logic was: !"Complete" && !"Closed" && !"Cancelled"
                    Predicate isComplete = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete");
                    Predicate isClosed = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed");
                    Predicate isCancelled = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")),
                            "cancelled");

                    predicates.add(criteriaBuilder.not(criteriaBuilder.or(isComplete, isClosed, isCancelled)));
                }
            }

            // Client Invoice Paid Filter
            if (clientInvoicePaid != null) {
                Predicate isPaid = criteriaBuilder.equal(root.get("clientInvoicePaid"), clientInvoicePaid);
                Predicate hasTotal = criteriaBuilder.isNotNull(root.get("clientInvoiceTotal"));
                Predicate positiveTotal = criteriaBuilder.greaterThan(root.get("clientInvoiceTotal"), BigDecimal.ZERO);

                predicates.add(criteriaBuilder.and(isPaid, hasTotal, positiveTotal));
            }

            // Contractor Invoice Paid Filter
            if (contractorInvoicePaid != null) {
                Predicate isPaid = criteriaBuilder.equal(root.get("contractorInvoicePaid"), contractorInvoicePaid);
                Predicate hasTotal = criteriaBuilder.isNotNull(root.get("contractorInvoiceTotal"));
                Predicate positiveTotal = criteriaBuilder.greaterThan(root.get("contractorInvoiceTotal"),
                        BigDecimal.ZERO);

                predicates.add(criteriaBuilder.and(isPaid, hasTotal, positiveTotal));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
