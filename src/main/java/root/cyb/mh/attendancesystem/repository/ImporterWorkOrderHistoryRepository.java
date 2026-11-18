package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.ImporterWorkOrderHistory;

import java.util.List;

@Repository
public interface ImporterWorkOrderHistoryRepository extends JpaRepository<ImporterWorkOrderHistory, Long> {

    List<ImporterWorkOrderHistory> findByWoNumberOrderByChangedAtDesc(String woNumber);

    List<ImporterWorkOrderHistory> findByImporterWorkOrderIdOrderByChangedAtDesc(Long importerWorkOrderId);
}
