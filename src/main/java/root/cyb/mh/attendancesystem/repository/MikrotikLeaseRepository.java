package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.MikrotikLeaseEntity;

import java.util.List;

@Repository
public interface MikrotikLeaseRepository extends JpaRepository<MikrotikLeaseEntity, Long> {

    @Query("SELECT l FROM MikrotikLeaseEntity l WHERE " +
           "LOWER(l.ipAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.macAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.hostName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.server) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.comment) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY l.ipAddress ASC")
    List<MikrotikLeaseEntity> searchLeases(@Param("query") String query);
}
