package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.RtvAuditLog;

import java.util.List;

@Repository
public interface RtvAuditLogRepository extends JpaRepository<RtvAuditLog, Long> {

    List<RtvAuditLog> findByRtvIdOrderByTimestampDesc(Long rtvId);
}
