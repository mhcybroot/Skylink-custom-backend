package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;

import java.util.List;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
        List<PaymentRequest> findByRequester(User requester);

        List<PaymentRequest> findByRequesterOrderByLastModifiedDesc(User requester);

        List<PaymentRequest> findByEmployeeRequester(root.cyb.mh.attendancesystem.model.Employee employeeRequester);

        List<PaymentRequest> findByEmployeeRequesterOrderByLastModifiedDesc(
                        root.cyb.mh.attendancesystem.model.Employee employeeRequester);

        List<PaymentRequest> findByEmployeeRequesterInOrderByLastModifiedDesc(
                        List<root.cyb.mh.attendancesystem.model.Employee> subordinates);

        List<PaymentRequest> findAllByOrderByLastModifiedDesc();
}
