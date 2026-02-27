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

        // --- CLIENT DASHBOARD ---
        long countByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID' AND p.requestDate BETWEEN :startDate AND :endDate")
        java.math.BigDecimal sumPaidAmountByClientIdBetween(Long clientId, java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.name, SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID' GROUP BY p.contractor.name ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopContractorsByClientId(Long clientId, org.springframework.data.domain.Pageable pageable);

        List<PaymentRequest> findByClientIdOrderByRequestDateDesc(Long clientId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT extract(year from p.requestDate), extract(month from p.requestDate), SUM(p.amount) "
                        +
                        "FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID' AND p.requestDate >= :startDate "
                        +
                        "GROUP BY extract(year from p.requestDate), extract(month from p.requestDate) " +
                        "ORDER BY extract(year from p.requestDate), extract(month from p.requestDate)")
        List<Object[]> findMonthlySpendingTrendByClientId(Long clientId, java.time.LocalDate startDate);

        // --- CLIENT ANALYTICS (15+ METRICS) ---

        // Financial Performance
        @org.springframework.data.jpa.repository.Query("SELECT AVG(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal findAvgAmountByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal findMaxAmountByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal findMinAmountByClientId(Long clientId);

        // Operational Efficiency
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.workOrderNumber) FROM PaymentRequest p WHERE p.client.id = :clientId")
        long countDistinctWorkOrdersByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = :status")
        long countByClientIdAndStatus(Long clientId, root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        // Vendor Relationships
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.contractor IS NOT NULL")
        long countDistinctContractorsByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.name, COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.contractor IS NOT NULL GROUP BY p.contractor.name ORDER BY COUNT(p) DESC")
        List<Object[]> findMostFrequentVendorByClientId(Long clientId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.contractor IS NOT NULL AND p.requestDate >= :sinceDate")
        long countNewVendorsByClientIdSince(Long clientId, java.time.LocalDate sinceDate);

        // Risk & Payment Analysis
        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND (p.paymentStatus = 'PENDING' OR p.paymentStatus = 'APPROVED')")
        java.math.BigDecimal sumOutstandingByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND (p.priority = 'HIGH' OR p.priority = 'URGENT')")
        long countHighPriorityByClientId(Long clientId);

        @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod.methodName, COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentMethod IS NOT NULL GROUP BY p.paymentMethod.methodName ORDER BY COUNT(p) DESC")
        List<Object[]> findPaymentMethodDistributionByClientId(Long clientId,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PENDING'")
        long countPendingByClientId(Long clientId);

        // Growth & Trends
        @org.springframework.data.jpa.repository.Query("SELECT extract(month from p.requestDate), SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year GROUP BY extract(month from p.requestDate) ORDER BY SUM(p.amount) DESC")
        List<Object[]> findPeakMonthByClientIdAndYear(Long clientId, int year);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        java.math.BigDecimal sumByClientIdYearMonth(Long clientId, int year, int month);

        // --- GLOBAL CLIENT ANALYTICS ---

        // Financial Overview
        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumTotalRevenue();

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID' AND p.requestDate BETWEEN :startDate AND :endDate")
        java.math.BigDecimal sumRevenueByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PENDING' OR p.paymentStatus = 'APPROVED'")
        java.math.BigDecimal sumTotalOutstanding();

        @org.springframework.data.jpa.repository.Query("SELECT p.client.id, p.client.name, SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID' GROUP BY p.client.id, p.client.name ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopClientsByRevenue(org.springframework.data.domain.Pageable pageable);

        // Operational Metrics
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.workOrderNumber) FROM PaymentRequest p")
        long countTotalWorkOrders();

        // Vendor Ecosystem
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor) FROM PaymentRequest p WHERE p.contractor IS NOT NULL")
        long countTotalUniqueVendors();

        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.name, COUNT(DISTINCT p.client) FROM PaymentRequest p WHERE p.contractor IS NOT NULL GROUP BY p.contractor.name ORDER BY COUNT(DISTINCT p.client) DESC")
        List<Object[]> findMostUsedVendors(org.springframework.data.domain.Pageable pageable);

        // Growth Trends
        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        java.math.BigDecimal sumRevenueByYearMonth(int year, int month);

        // ============================================
        // PAYMENT METHOD DASHBOARD ANALYTICS
        // ============================================

        // Count transactions by payment method
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId")
        long countByPaymentMethodId(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count transactions by payment method and approval status (RequestStatus)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = :status")
        long countByPaymentMethodIdAndStatus(@org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("status") root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        // Sum amount by payment method (paid only)
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumByPaymentMethodIdPaid(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Average amount by payment method (paid only)
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal avgByPaymentMethodIdPaid(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Max amount by payment method (paid only)
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal maxByPaymentMethodIdPaid(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count this month/year transactions by payment method
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        long countByPaymentMethodIdAndYearMonth(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // Top clients using this payment method
        @org.springframework.data.jpa.repository.Query("SELECT p.client.id, p.client.name, COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.client IS NOT NULL GROUP BY p.client.id, p.client.name ORDER BY COUNT(p) DESC")
        List<Object[]> findTopClientsByPaymentMethod(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId,
                        org.springframework.data.domain.Pageable pageable);

        // Top contractors by this payment method
        @org.springframework.data.jpa.repository.Query("SELECT p.contractor.id, p.contractor.name, SUM(p.amount) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.paymentStatus = 'PAID' AND p.contractor IS NOT NULL GROUP BY p.contractor.id, p.contractor.name ORDER BY SUM(p.amount) DESC")
        List<Object[]> findTopContractorsByPaymentMethod(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId,
                        org.springframework.data.domain.Pageable pageable);

        // All payment methods ranked by usage
        @org.springframework.data.jpa.repository.Query("SELECT p.paymentMethod.id, COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod IS NOT NULL GROUP BY p.paymentMethod.id ORDER BY COUNT(p) DESC")
        List<Object[]> findAllPaymentMethodsRankedByUsage();

        // ============================================
        // SWOT ANALYTICS - STRENGTHS
        // ============================================

        // Count high-value transactions (above threshold)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.amount > :threshold")
        long countHighValueTransactions(@org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // Count repeat clients (clients with more than 1 transaction)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.client.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.client IS NOT NULL AND p.client.id IN (SELECT p2.client.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId GROUP BY p2.client.id HAVING COUNT(p2) > 1)")
        long countRepeatClients(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count distinct clients using this method
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.client.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.client IS NOT NULL")
        long countDistinctClientsByPaymentMethod(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count repeat contractors
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.contractor IS NOT NULL AND p.contractor.id IN (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId GROUP BY p2.contractor.id HAVING COUNT(p2) > 1)")
        long countRepeatContractors(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count distinct contractors
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.contractor IS NOT NULL")
        long countDistinctContractorsByPaymentMethod(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // ============================================
        // SWOT ANALYTICS - WEAKNESSES
        // ============================================

        // Count rejected transactions
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'REJECTED'")
        long countRejectedByPaymentMethod(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Find oldest pending request date
        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.requestDate) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'PENDING'")
        java.time.LocalDate findOldestPendingDate(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count low-value transactions (below threshold)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.amount < :threshold")
        long countLowValueTransactions(@org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // Find last transaction date
        @org.springframework.data.jpa.repository.Query("SELECT MAX(p.requestDate) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId")
        java.time.LocalDate findLastTransactionDate(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // ============================================
        // SWOT ANALYTICS - OPPORTUNITIES
        // ============================================

        // Count new clients this month (first appearance)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.client.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.client IS NOT NULL AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month AND p.client.id NOT IN (SELECT p2.client.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId AND (extract(year from p2.requestDate) < :year OR (extract(year from p2.requestDate) = :year AND extract(month from p2.requestDate) < :month)))")
        long countNewClientsThisMonth(@org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // Count new contractors this month
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.contractor IS NOT NULL AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month AND p.contractor.id NOT IN (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId AND (extract(year from p2.requestDate) < :year OR (extract(year from p2.requestDate) = :year AND extract(month from p2.requestDate) < :month)))")
        long countNewContractorsThisMonth(@org.springframework.data.repository.query.Param("methodId") Long methodId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // Get day of week distribution (PostgreSQL compatible)
        @org.springframework.data.jpa.repository.Query(value = "SELECT to_char(request_date, 'Day') as day_name, COUNT(*) FROM payment_requests WHERE payment_method_ref_id = :methodId GROUP BY to_char(request_date, 'Day') ORDER BY COUNT(*) DESC", nativeQuery = true)
        List<Object[]> findDayOfWeekDistribution(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // ============================================
        // SWOT ANALYTICS - THREATS
        // ============================================

        // Top client amount for concentration
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'APPROVED' AND p.client.id = (SELECT p2.client.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId AND p2.status = 'APPROVED' GROUP BY p2.client.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal findTopClientAmount(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Top contractor amount for concentration
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'APPROVED' AND p.contractor.id = (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.paymentMethod.id = :methodId AND p2.status = 'APPROVED' GROUP BY p2.contractor.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal findTopContractorAmount(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count high-priority pending
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'PENDING' AND p.priority = 'HIGH'")
        long countHighPriorityPending(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Count issue transactions
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.paymentStatus = 'ISSUE'")
        long countIssueTransactions(@org.springframework.data.repository.query.Param("methodId") Long methodId);

        // Sum approved amount by payment method
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.paymentMethod.id = :methodId AND p.status = 'APPROVED'")
        java.math.BigDecimal sumApprovedByPaymentMethod(
                        @org.springframework.data.repository.query.Param("methodId") Long methodId);

        // ============================================
        // CLIENT SWOT ANALYTICS - STRENGTHS
        // ============================================

        // Sum approved amount by client
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED'")
        java.math.BigDecimal sumApprovedByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Total approved amount across all clients (for revenue share calculation)
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.status = 'APPROVED'")
        java.math.BigDecimal sumAllApproved();

        // Count approved by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED'")
        long countApprovedByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Count all by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId")
        long countAllByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // First transaction date by client
        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.requestDate) FROM PaymentRequest p WHERE p.client.id = :clientId")
        java.time.LocalDate findFirstTransactionDateByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // High-value transaction count by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.amount > :threshold")
        long countHighValueByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId,
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // Count active months by client
        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(DISTINCT to_char(request_date, 'YYYY-MM')) FROM payment_requests WHERE client_id = :clientId", nativeQuery = true)
        long countActiveMonthsByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // ============================================
        // CLIENT SWOT ANALYTICS - WEAKNESSES
        // ============================================

        // Count rejected by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'REJECTED'")
        long countRejectedByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Count pending by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'PENDING'")
        long countPendingByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Oldest pending date by client
        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.requestDate) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'PENDING'")
        java.time.LocalDate findOldestPendingDateByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Low-value transactions by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.amount < :threshold")
        long countLowValueByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId,
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // Issue transactions by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentStatus = 'ISSUE'")
        long countIssuesByClient(@org.springframework.data.repository.query.Param("clientId") Long clientId);

        // ============================================
        // CLIENT SWOT ANALYTICS - OPPORTUNITIES
        // ============================================

        // Sum by client and year-month
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED' AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        java.math.BigDecimal sumByClientAndYearMonth(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // Sum by client and year
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED' AND extract(year from p.requestDate) = :year")
        java.math.BigDecimal sumByClientAndYear(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId,
                        @org.springframework.data.repository.query.Param("year") int year);

        // New contractors this month for client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.contractor IS NOT NULL AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month AND p.contractor.id NOT IN (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.client.id = :clientId AND (extract(year from p2.requestDate) < :year OR (extract(year from p2.requestDate) = :year AND extract(month from p2.requestDate) < :month)))")
        long countNewContractorsThisMonthByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // Payment methods used by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.paymentMethod.id) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.paymentMethod IS NOT NULL")
        long countPaymentMethodsUsedByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Day of week distribution by client
        @org.springframework.data.jpa.repository.Query(value = "SELECT to_char(request_date, 'Day') as day_name, COUNT(*) FROM payment_requests WHERE client_id = :clientId GROUP BY to_char(request_date, 'Day') ORDER BY COUNT(*) DESC", nativeQuery = true)
        List<Object[]> findDayOfWeekDistributionByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // ============================================
        // CLIENT SWOT ANALYTICS - THREATS
        // ============================================

        // Top contractor amount by client
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED' AND p.contractor.id = (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.client.id = :clientId AND p2.status = 'APPROVED' AND p2.contractor IS NOT NULL GROUP BY p2.contractor.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal findTopContractorAmountByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Top payment method amount by client
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED' AND p.paymentMethod.id = (SELECT p2.paymentMethod.id FROM PaymentRequest p2 WHERE p2.client.id = :clientId AND p2.status = 'APPROVED' AND p2.paymentMethod IS NOT NULL GROUP BY p2.paymentMethod.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal findTopPaymentMethodAmountByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // High-priority pending by client
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'PENDING' AND p.priority = 'HIGH'")
        long countHighPriorityPendingByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Last transaction date by client
        @org.springframework.data.jpa.repository.Query("SELECT MAX(p.requestDate) FROM PaymentRequest p WHERE p.client.id = :clientId")
        java.time.LocalDate findLastTransactionDateByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // Average amount by client for threshold
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(p.amount), 0) FROM PaymentRequest p WHERE p.client.id = :clientId AND p.status = 'APPROVED'")
        java.math.BigDecimal avgApprovedByClient(
                        @org.springframework.data.repository.query.Param("clientId") Long clientId);

        // ============================================
        // VENDOR SWOT ANALYTICS - GLOBAL
        // ============================================

        // STRENGTHS: Count high-value vendors (vendors with spend > threshold)
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.contractor IS NOT NULL AND p.status = 'APPROVED' GROUP BY p.contractor.id HAVING SUM(p.amount) > :threshold")
        List<Object[]> findHighValueVendorCounts(
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // STRENGTHS: Average approved amount globally
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(p.amount), 0) FROM PaymentRequest p WHERE p.status = 'APPROVED'")
        java.math.BigDecimal avgApprovedGlobally();

        // WEAKNESSES: Count issue requests globally
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.paymentStatus = 'ISSUE'")
        long countIssueRequestsGlobally();

        // OPPORTUNITIES: Sum paid amount by year-month globally
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        java.math.BigDecimal sumPaidByYearMonth(@org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        // THREATS: Sum top 5 vendors spend
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.status = 'APPROVED' AND p.contractor.id IN (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.status = 'APPROVED' AND p2.contractor IS NOT NULL GROUP BY p2.contractor.id ORDER BY SUM(p2.amount) DESC LIMIT 5)")
        java.math.BigDecimal sumTop5VendorsSpend();

        // THREATS: Sum top payment method amount globally
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.status = 'APPROVED' AND p.paymentMethod.id = (SELECT p2.paymentMethod.id FROM PaymentRequest p2 WHERE p2.status = 'APPROVED' AND p2.paymentMethod IS NOT NULL GROUP BY p2.paymentMethod.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal sumTopPaymentMethodAmountGlobally();

        // THREATS: Count pending requests globally
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.status = 'PENDING'")
        long countPendingGlobally();

        // Count distinct vendors with activity
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.contractor IS NOT NULL AND p.amount > :threshold AND p.status = 'APPROVED'")
        long countVendorsAboveThreshold(
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // ============================================
        // COMPANY SWOT ANALYTICS
        // ============================================

        // STRENGTHS
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByCompanyId(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(p.amount), 0) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'APPROVED'")
        java.math.BigDecimal avgApprovedByCompanyInfo(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.contractor IS NOT NULL AND p.paymentStatus = 'PAID'")
        long countActiveContractorsByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'APPROVED' AND p.amount > :threshold")
        long countHighValueTransactionsByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId,
                        @org.springframework.data.repository.query.Param("threshold") java.math.BigDecimal threshold);

        // WEAKNESSES
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'PENDING'")
        long countPendingByCompanyId(@org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT MIN(p.requestDate) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'PENDING'")
        java.time.LocalDate findOldestPendingDateByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.paymentStatus = 'ISSUE'")
        long countIssueRequestsByCompany(@org.springframework.data.repository.query.Param("companyId") Long companyId);

        // OPPORTUNITIES
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year AND extract(month from p.requestDate) = :month")
        java.math.BigDecimal sumPaidByCompanyIdYearMonth(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId,
                        @org.springframework.data.repository.query.Param("year") int year,
                        @org.springframework.data.repository.query.Param("month") int month);

        @org.springframework.data.jpa.repository.Query("SELECT extract(month from p.requestDate), SUM(p.amount) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.paymentStatus = 'PAID' AND extract(year from p.requestDate) = :year GROUP BY extract(month from p.requestDate) ORDER BY SUM(p.amount) DESC")
        List<Object[]> findPeakMonthByCompanyIdAndYear(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId,
                        @org.springframework.data.repository.query.Param("year") int year);

        // THREATS
        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'APPROVED' AND p.contractor.id = (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.company.id = :companyId AND p2.status = 'APPROVED' GROUP BY p2.contractor.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal sumTopContractorSpendByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'APPROVED' AND p.paymentMethod.id = (SELECT p2.paymentMethod.id FROM PaymentRequest p2 WHERE p2.company.id = :companyId AND p2.status = 'APPROVED' AND p2.paymentMethod IS NOT NULL GROUP BY p2.paymentMethod.id ORDER BY SUM(p2.amount) DESC LIMIT 1)")
        java.math.BigDecimal sumTopPaymentMethodAmountByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.status = 'PENDING' AND (p.priority = 'HIGH' OR p.priority = 'URGENT')")
        long countHighPriorityPendingByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        // PLACEHOLDER FIXES
        @org.springframework.data.jpa.repository.Query(value = "SELECT COALESCE(AVG(CAST(last_modified AS DATE) - request_date), 0) FROM payment_requests WHERE company_id = :companyId AND payment_status = 'PAID'", nativeQuery = true)
        Double findAvgPaymentDurationDaysByCompany(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.contractor.id) FROM PaymentRequest p WHERE p.company.id = :companyId AND p.requestDate >= :sinceDate AND p.contractor.id NOT IN (SELECT p2.contractor.id FROM PaymentRequest p2 WHERE p2.company.id = :companyId AND p2.requestDate < :sinceDate)")
        long countNewContractorsByCompanySince(
                        @org.springframework.data.repository.query.Param("companyId") Long companyId,
                        @org.springframework.data.repository.query.Param("sinceDate") java.time.LocalDate sinceDate);
        // ============================================
        // GLOBAL CLIENT PORTFOLIO ANALYTICS (NATIVE)
        // ============================================

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(DISTINCT client_id) FROM payment_requests WHERE payment_status = 'PAID' AND request_date >= :since", nativeQuery = true)
        long countActiveClientsSince(
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDate since);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests GROUP BY client_id HAVING COUNT(DISTINCT work_order_number) > 1) as sub", nativeQuery = true)
        long countMultiProjectClients();

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests GROUP BY client_id HAVING MIN(request_date) >= :since) as sub", nativeQuery = true)
        long countNewClientsSince(@org.springframework.data.repository.query.Param("since") java.time.LocalDate since);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests GROUP BY client_id HAVING MAX(request_date) < :cutoff) as sub", nativeQuery = true)
        long countChurnedClientsBefore(
                        @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDate cutoff);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests GROUP BY client_id HAVING CAST(COUNT(CASE WHEN status='REJECTED' THEN 1 END) AS FLOAT) / NULLIF(COUNT(*),0) > :rate) as sub", nativeQuery = true)
        long countHighRejectionClients(@org.springframework.data.repository.query.Param("rate") double rate);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests WHERE status='PENDING' GROUP BY client_id HAVING AVG(DATE_PART('day', NOW() - CAST(request_date AS TIMESTAMP))) > :days) as sub", nativeQuery = true)
        long countStalledClients(@org.springframework.data.repository.query.Param("days") int days);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests WHERE status='PENDING' GROUP BY client_id HAVING CAST(COUNT(CASE WHEN priority IN ('HIGH','URGENT') THEN 1 END) AS FLOAT) / NULLIF(COUNT(*),0) > :rate) as sub", nativeQuery = true)
        long countUrgencyOverloadClients(@org.springframework.data.repository.query.Param("rate") double rate);

        @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM (SELECT client_id FROM payment_requests GROUP BY client_id HAVING COUNT(*) > :count) as sub", nativeQuery = true)
        long countHighVolumeClients(@org.springframework.data.repository.query.Param("count") int count);

        @org.springframework.data.jpa.repository.Query("SELECT c.area, SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE p.paymentStatus = 'PAID' GROUP BY c.area ORDER BY SUM(p.amount) DESC")
        java.util.List<Object[]> sumSpendByArea();

        @org.springframework.data.jpa.repository.Query("SELECT c.zipCode, SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE p.paymentStatus = 'PAID' GROUP BY c.zipCode ORDER BY SUM(p.amount) DESC")
        java.util.List<Object[]> sumSpendByZipCode();

        List<PaymentRequest> findByContractorArea(String area);

        List<PaymentRequest> findByContractorAreaIsNull();

        List<PaymentRequest> findByContractorZipCode(String zipCode);

        List<PaymentRequest> findByContractorZipCodeIsNull();

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE c.area = :area AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByArea(@org.springframework.data.repository.query.Param("area") String area);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE (c.area IS NULL OR c.area = '') AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByAreaUnknown();

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE c.zipCode = :zip AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByZip(@org.springframework.data.repository.query.Param("zip") String zip);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p JOIN p.contractor c WHERE (c.zipCode IS NULL OR c.zipCode = '') AND p.paymentStatus = 'PAID'")
        java.math.BigDecimal sumPaidAmountByZipUnknown();

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.data.jpa.repository.Query("UPDATE PaymentRequest p SET p.requester = null WHERE p.requester = :user")
        void clearRequesterReferences(@org.springframework.data.repository.query.Param("user") User user);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.data.jpa.repository.Query("UPDATE PaymentRequest p SET p.approvalAuthority = null WHERE p.approvalAuthority = :user")
        void clearApprovalAuthorityReferences(@org.springframework.data.repository.query.Param("user") User user);
}
