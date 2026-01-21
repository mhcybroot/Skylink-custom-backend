package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import root.cyb.mh.attendancesystem.model.ImportLog;
import java.util.List;

public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {
    List<ImportLog> findAllByOrderByImportDateDesc();
}
