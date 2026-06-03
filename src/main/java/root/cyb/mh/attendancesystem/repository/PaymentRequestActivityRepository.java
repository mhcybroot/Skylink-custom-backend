package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.PaymentRequestActivity;
import java.util.List;

@Repository
public interface PaymentRequestActivityRepository extends JpaRepository<PaymentRequestActivity, Long> {
    List<PaymentRequestActivity> findByPaymentRequestIdOrderByTimestampDesc(Long paymentRequestId);
}
