package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.DeletedImporterWorkOrder;

import java.util.List;

@Repository
public interface DeletedImporterWorkOrderRepository extends JpaRepository<DeletedImporterWorkOrder, Long> {

    List<DeletedImporterWorkOrder> findAllByOrderByDeletedAtDesc();

    List<DeletedImporterWorkOrder> findByDeletedByEmployeeIdOrderByDeletedAtDesc(String deletedByEmployeeId);
}
