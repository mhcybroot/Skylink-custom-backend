package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import root.cyb.mh.attendancesystem.model.DeletedPaymentRequest;

import java.util.List;

public interface DeletedPaymentRequestRepository extends JpaRepository<DeletedPaymentRequest, Long> {

    /** Returns all archived records, newest deletions first */
    List<DeletedPaymentRequest> findAllByOrderByDeletedAtDesc();
}
