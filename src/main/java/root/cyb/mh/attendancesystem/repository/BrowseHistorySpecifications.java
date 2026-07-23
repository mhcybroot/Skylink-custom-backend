package root.cyb.mh.attendancesystem.repository;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;

import java.time.LocalDateTime;

public class BrowseHistorySpecifications {

    public static Specification<EmployeeBrowseHistory> employeeSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return null;
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("employee").get("name")), likePattern),
                cb.like(cb.lower(root.get("employee").get("id")), likePattern)
            );
        };
    }

    public static Specification<EmployeeBrowseHistory> urlContains(String url) {
        return (root, query, cb) -> {
            if (url == null || url.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("url")), "%" + url.trim().toLowerCase() + "%");
        };
    }

    public static Specification<EmployeeBrowseHistory> domainContains(String domain) {
        return (root, query, cb) -> {
            if (domain == null || domain.trim().isEmpty()) return null;
            // Matches http://domain, https://domain, or www.domain
            return cb.like(cb.lower(root.get("url")), "%://" + domain.trim().toLowerCase() + "%");
        };
    }

    public static Specification<EmployeeBrowseHistory> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("pageTitle")), "%" + title.trim().toLowerCase() + "%");
        };
    }

    public static Specification<EmployeeBrowseHistory> dateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get("timestamp"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("timestamp"), startDate);
            } else if (endDate != null) {
                return cb.lessThanOrEqualTo(root.get("timestamp"), endDate);
            }
            return null;
        };
    }

    public static Specification<EmployeeBrowseHistory> fetchEmployee() {
        return (root, query, cb) -> {
            // Check to ensure we only add fetch for data queries, not count queries
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("employee", JoinType.LEFT);
            }
            return null;
        };
    }
}
