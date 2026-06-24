package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.ProcessingWorkOrderHistory;

import java.util.List;

@Repository
public interface ProcessingWorkOrderHistoryRepository extends JpaRepository<ProcessingWorkOrderHistory, Long> {
    List<ProcessingWorkOrderHistory> findByWoNumberOrderByChangedAtDesc(String woNumber);
}
