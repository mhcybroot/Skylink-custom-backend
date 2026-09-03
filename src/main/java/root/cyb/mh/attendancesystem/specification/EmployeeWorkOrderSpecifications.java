package root.cyb.mh.attendancesystem.specification;

import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeWorkOrderSpecifications {

    public static Specification<EmployeeWorkOrder> withFilters(String status,
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

            // Date Range Filter
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
            if (clientName != null && !clientName.trim().isEmpty()) {
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
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete"),
                            criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed")));
                } else if ("cancelled".equalsIgnoreCase(status)) {
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "cancelled"));
                } else if ("open".equalsIgnoreCase(status)) {
                    Predicate isComplete = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete");
                    Predicate isClosed = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed");
                    Predicate isCancelled = criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "cancelled");

                    predicates.add(criteriaBuilder.not(criteriaBuilder.or(isComplete, isClosed, isCancelled)));
                } else if (!"all".equalsIgnoreCase(status)) {
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), status.toLowerCase()));
                }
            }

            // Client Invoice Paid Filter
            if (clientInvoicePaid != null) {
                Predicate isPaid = criteriaBuilder.equal(root.get("clientInvoicePaid"), clientInvoicePaid);
                Predicate hasTotal = criteriaBuilder.isNotNull(root.get("clientInvoiceTotal"));
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
