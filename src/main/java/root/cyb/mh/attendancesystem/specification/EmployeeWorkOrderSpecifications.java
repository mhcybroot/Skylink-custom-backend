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
        return withFilters(status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, clientName, contractorName, null, null, null, null, null, null);
    }

    public static Specification<EmployeeWorkOrder> withFilters(String status,
            Boolean clientInvoicePaid,
            Boolean contractorInvoicePaid,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            String workType,
            String clientName,
            String contractorName,
            Integer series) {
        return withFilters(status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, clientName, contractorName, series, null, null, null, null, null);
    }

    public static Specification<EmployeeWorkOrder> withFilters(
            String status,
            Boolean clientInvoicePaid,
            Boolean contractorInvoicePaid,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            String workType,
            String clientName,
            String contractorName,
            Integer series,
            String admin,
            String customerBank,
            String state,
            String dateType,
            String archiveStatus) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Series Filter (e.g. 100 -> 100-199)
            if (series != null) {
                int prefix = series / 100;
                Predicate rawClientSeries = criteriaBuilder.like(root.get("originalClientString"), prefix + "%");
                predicates.add(rawClientSeries);
            }

            // Dynamic Date Range Filter based on dateType
            String dateCol = "dateDueClient";
            if (dateType != null && !dateType.trim().isEmpty()) {
                switch (dateType.toLowerCase()) {
                    case "invoice":
                        dateCol = "invoiceDate";
                        break;
                    case "received":
                        dateCol = "dateReceived";
                        break;
                    case "sent_client":
                        dateCol = "sentToClientDate";
                        break;
                    case "paid":
                        dateCol = "clientPaidDate";
                        break;
                    case "due":
                    default:
                        dateCol = "dateDueClient";
                        break;
                }
            }
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get(dateCol), startDate, endDate));
            } else if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(dateCol), startDate));
            } else if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(dateCol), endDate));
            }

            // Smart Unified Global Search across all key fields
            if (search != null && !search.trim().isEmpty()) {
                String searchLike = "%" + search.trim().toLowerCase() + "%";
                Predicate woNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("woNumber")), searchLike);
                Predicate invNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceNumber")), searchLike);
                Predicate ppwNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("ppwNumber")), searchLike);
                Predicate loanNum = criteriaBuilder.like(criteriaBuilder.lower(root.get("loanNumber")), searchLike);
                Predicate address = criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), searchLike);
                Predicate city = criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), searchLike);
                Predicate statePred = criteriaBuilder.like(criteriaBuilder.lower(root.get("state")), searchLike);
                Predicate zip = criteriaBuilder.like(criteriaBuilder.lower(root.get("zip")), searchLike);
                Predicate clientPred = criteriaBuilder.like(criteriaBuilder.lower(root.get("originalClientString")), searchLike);
                Predicate contractorPred = criteriaBuilder.like(criteriaBuilder.lower(root.get("originalContractorString")), searchLike);
                Predicate adminPred = criteriaBuilder.like(criteriaBuilder.lower(root.get("admin")), searchLike);
                Predicate bankPred = criteriaBuilder.like(criteriaBuilder.lower(root.get("customerBank")), searchLike);
                Predicate workTypePred = criteriaBuilder.like(criteriaBuilder.lower(root.get("workType")), searchLike);

                predicates.add(criteriaBuilder.or(
                        woNum, invNum, ppwNum, loanNum, address, city, statePred, zip,
                        clientPred, contractorPred, adminPred, bankPred, workTypePred
                ));
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

            // Admin / Coordinator Filter
            if (admin != null && !admin.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("admin")),
                        "%" + admin.trim().toLowerCase() + "%"));
            }

            // Customer Bank Filter
            if (customerBank != null && !customerBank.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customerBank")),
                        "%" + customerBank.trim().toLowerCase() + "%"));
            }

            // State Filter
            if (state != null && !state.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("state")), state.trim().toLowerCase()));
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
                if (Boolean.TRUE.equals(clientInvoicePaid)) {
                    predicates.add(criteriaBuilder.isTrue(root.get("clientInvoicePaid")));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.isFalse(root.get("clientInvoicePaid")),
                            criteriaBuilder.isNull(root.get("clientInvoicePaid"))
                    ));
                }
            }

            // Contractor Invoice Paid Filter
            if (contractorInvoicePaid != null) {
                if (Boolean.TRUE.equals(contractorInvoicePaid)) {
                    predicates.add(criteriaBuilder.isTrue(root.get("contractorInvoicePaid")));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.isFalse(root.get("contractorInvoicePaid")),
                            criteriaBuilder.isNull(root.get("contractorInvoicePaid"))
                    ));
                }
            }

            // Archive Status Filter (Active vs Archived)
            if ("archived".equalsIgnoreCase(archiveStatus)) {
                Predicate isClosed = criteriaBuilder.or(
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete"),
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed")
                );
                Predicate isPaid = criteriaBuilder.isTrue(root.get("clientInvoicePaid"));
                predicates.add(criteriaBuilder.and(isClosed, isPaid));
            } else if ("active".equalsIgnoreCase(archiveStatus)) {
                Predicate isClosed = criteriaBuilder.or(
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "complete"),
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), "closed")
                );
                // In SQL: (status NOT in ('complete','closed') OR status is NULL) OR (clientInvoicePaid is false OR clientInvoicePaid is null)
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.or(criteriaBuilder.not(isClosed), criteriaBuilder.isNull(root.get("status"))),
                        criteriaBuilder.or(criteriaBuilder.isFalse(root.get("clientInvoicePaid")), criteriaBuilder.isNull(root.get("clientInvoicePaid")))
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
