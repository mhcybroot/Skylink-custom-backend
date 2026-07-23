package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.DeletedProcessingWorkOrder;

import java.util.List;

@Repository
public interface DeletedProcessingWorkOrderRepository extends JpaRepository<DeletedProcessingWorkOrder, Long> {

    List<DeletedProcessingWorkOrder> findAllByOrderByDeletedAtDesc();

}
