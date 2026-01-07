package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;

import java.util.List;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<PaymentRequest> {
        List<PaymentRequest> findByRequester(User requester);

        List<PaymentRequest> findByContractorIdOrderByRequestDateDesc(Long contractorId);

        List<PaymentRequest> findByContractorIdAndEmployeeRequesterOrderByRequestDateDesc(Long contractorId,
                        root.cyb.mh.attendancesystem.model.Employee employeeRequester);

        List<PaymentRequest> findByRequesterOrderByLastModifiedDesc(User requester);

        List<PaymentRequest> findByEmployeeRequester(root.cyb.mh.attendancesystem.model.Employee employeeRequester);

        List<PaymentRequest> findByEmployeeRequesterOrderByLastModifiedDesc(
                        root.cyb.mh.attendancesystem.model.Employee employeeRequester);

        List<PaymentRequest> findByEmployeeRequesterInOrderByLastModifiedDesc(
                        List<root.cyb.mh.attendancesystem.model.Employee> subordinates);

        List<PaymentRequest> findAllByOrderByLastModifiedDesc();

        // Aggregations for Dashboard
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.requestDate = :date")
        long countByRequestDate(java.time.LocalDate date);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.requestDate = :date")
        java.math.BigDecimal sumAmountByRequestDate(java.time.LocalDate date);

        List<PaymentRequest> findByPaymentStatus(root.cyb.mh.attendancesystem.model.enums.PaymentStatus status);

        long countByStatus(root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.status = :status")
        java.math.BigDecimal sumAmountByStatus(root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        long countByStatusAndPriority(root.cyb.mh.attendancesystem.model.enums.RequestStatus status,
                        root.cyb.mh.attendancesystem.model.enums.PaymentPriority priority);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = :status")
        java.math.BigDecimal sumAmountByPaymentStatus(root.cyb.mh.attendancesystem.model.enums.PaymentStatus status);

        // Recent 5
        List<PaymentRequest> findTop5ByLastModifiedIsNotNullOrderByLastModifiedDesc();

        // My Action Items (Admin) - Pending requests needing review
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.status = 'PENDING'")
        long countPendingRequests();

        // History Views
        List<PaymentRequest> findByRequestDateOrderByLastModifiedDesc(java.time.LocalDate date);

        List<PaymentRequest> findByRequestDateBetweenOrderByRequestDateDesc(java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        // --- NEW AGGREGATIONS FOR DASHBOARD ENHANCEMENTS ---

        // 1. Financials
        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID' AND p.requestDate BETWEEN :startDate AND :endDate")
        java.math.BigDecimal sumPaidAmountBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @org.springframework.data.jpa.repository.Query("SELECT AVG(p.amount) FROM PaymentRequest p")
        java.math.BigDecimal findAverageRequestAmount();

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.status = 'APPROVED' AND p.paymentStatus != 'PAID'")
        java.math.BigDecimal findUnpaidApprovedLiability();

        // 2. Trends (Last 6 Months)
        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), SUM(p.amount) "
                        +
                        "FROM PaymentRequest p WHERE p.status = 'APPROVED' AND p.requestDate >= :startDate " +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY extract(year from p.requestDate), extract(month from p.requestDate)")
        List<Object[]> findMonthlySpendingTrend(java.time.LocalDate startDate);

        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), COUNT(p) "
                        +
                        "FROM PaymentRequest p WHERE p.requestDate >= :startDate " +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY extract(year from p.requestDate), extract(month from p.requestDate)")
        List<Object[]> findMonthlyVolumeTrend(java.time.LocalDate startDate);

        // 3. Operational Analysis
        @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod.methodName, COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod IS NOT NULL GROUP BY p.paymentMethod.methodName")
        List<Object[]> countByPaymentMethodGroup();

        @org.springframework.data.jpa.repository.Query("SELECT p.client.name, SUM(p.amount) FROM PaymentRequest p WHERE p.client IS NOT NULL GROUP BY p.client.name")
        List<Object[]> sumAmountByClientGroup();

        @org.springframework.data.jpa.repository.Query("SELECT p.priority, COUNT(p) FROM PaymentRequest p GROUP BY p.priority")
        List<Object[]> countByPriorityGroup();

        @org.springframework.data.jpa.repository.Query("SELECT p.ppwUpdateStatus, COUNT(p) FROM PaymentRequest p WHERE p.ppwUpdateStatus IS NOT NULL GROUP BY p.ppwUpdateStatus")
        List<Object[]> countByPpwStatusGroup();

        @org.springframework.data.jpa.repository.Query("SELECT p.paymentStatus, COUNT(p) FROM PaymentRequest p WHERE p.paymentStatus IS NOT NULL GROUP BY p.paymentStatus")
        List<Object[]> countByPaymentStatusGroup();

        // 4. Leaderboards
        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.name, SUM(p.amount) FROM PaymentRequest p WHERE p.contractor IS NOT NULL GROUP BY p.contractor.name ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopContractorsBySpend(org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(r.username, e.name), COUNT(p) " +
                        "FROM PaymentRequest p " +
                        "LEFT JOIN p.requester r " +
                        "LEFT JOIN p.employeeRequester e " +
                        "GROUP BY r.username, e.name " +
                        "ORDER BY COUNT(p) DESC")
        List<Object[]> findTopRequesters(org.springframework.data.domain.Pageable pageable);

        List<PaymentRequest> findTop5ByAmountGreaterThanOrderByRequestDateDesc(java.math.BigDecimal amount);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Contractor c WHERE c.active = true")
        long countActiveContractors();

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Contractor c WHERE c.active = false")
        long countInactiveContractors();

        // --- Company Dashboard ---
        List<PaymentRequest> findByCompany(root.cyb.mh.attendancesystem.model.Company company);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = :status")
        long countByCompanyAndStatus(Long companyId, root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId")
        java.math.BigDecimal sumAmountByCompany(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), SUM(p.amount) "
                        +
                        "FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'APPROVED' " +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY extract(year from p.requestDate), extract(month from p.requestDate)")
        List<Object[]> findCompanyMonthlySpendingTrend(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.name, SUM(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.contractor IS NOT NULL GROUP BY p.contractor.name ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopContractorsByCompany(Long companyId, org.springframework.data.domain.Pageable pageable);

        // --- NEW INSIGHTS ---
        @org.springframework.data.jpa.repository.Query("SELECT MAX(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId")
        java.math.BigDecimal findMaxOneTimeSpendByCompany(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(r.username, e.name), COUNT(p) " +
                        "FROM PaymentRequest p " +
                        "LEFT JOIN p.requester r " +
                        "LEFT JOIN p.employeeRequester e " +
                        "WHERE p.company.id = :companyId " +
                        "GROUP BY r.username, e.name " +
                        "ORDER BY COUNT(p) DESC")
        List<Object[]> findMostFrequentRequesterByCompany(Long companyId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT p.client.name, SUM(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.client IS NOT NULL GROUP BY p.client.name")
        List<Object[]> sumAmountByCompanyAndClientGroup(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod.methodName, SUM(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.paymentMethod IS NOT NULL GROUP BY p.paymentMethod.methodName")
        List<Object[]> sumAmountByCompanyAndPaymentMethodGroup(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.requestDate) FROM PaymentRequest p WHERE p.company.id = :companyId")
        java.time.LocalDate findFirstTransactionDateByCompany(Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), SUM(p.amount) "
                        +
                        "FROM PaymentRequest p WHERE p.company.id = :companyId " +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopSpendingMonthByCompany(Long companyId, org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(p.amount) FROM PaymentRequest p")
        java.math.BigDecimal findMaxTransactionAmount();

        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), SUM(p.amount) "
                        +
                        "FROM PaymentRequest p WHERE p.status = 'APPROVED' " +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopSpendingMonthGlobal(org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod.methodName, COUNT(p) " +
                        "FROM PaymentRequest p " +
                        "WHERE p.paymentMethod IS NOT NULL " +
                        "GROUP BY p.paymentMethod.methodName " +
                        "ORDER BY COUNT(p) DESC")
        List<Object[]> findMostFrequentPaymentMethodGlobal(org.springframework.data.domain.Pageable pageable);
}
