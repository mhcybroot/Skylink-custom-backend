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

        long countByStatus(root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.status = :status")
        java.math.BigDecimal sumAmountByStatus(root.cyb.mh.attendancesystem.model.enums.RequestStatus status);

        long countByStatusAndPriority(root.cyb.mh.attendancesystem.model.enums.RequestStatus status,
                        root.cyb.mh.attendancesystem.model.enums.PaymentPriority priority);

        @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM PaymentRequest p WHERE p.paymentStatus = :status")
        java.math.BigDecimal sumAmountByPaymentStatus(root.cyb.mh.attendancesystem.model.enums.PaymentStatus status);

        // Recent 5
        List<PaymentRequest> findTop5ByOrderByLastModifiedDesc();

        // My Action Items (Admin) - Pending requests needing review
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM PaymentRequest p WHERE p.status = 'PENDING'")
        long countPendingRequests();

        // History Views
        List<PaymentRequest> findByRequestDateOrderByLastModifiedDesc(java.time.LocalDate date);

        List<PaymentRequest> findByRequestDateBetweenOrderByRequestDateDesc(java.time.LocalDate startDate,
                        java.time.LocalDate endDate);
}
