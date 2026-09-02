package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.MikrotikArpEntity;

import java.util.List;

@Repository
public interface MikrotikArpRepository extends JpaRepository<MikrotikArpEntity, Long> {

    @Query("SELECT a FROM MikrotikArpEntity a WHERE " +
           "LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.macAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.networkInterface) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.comment) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY a.ipAddress ASC")
    List<MikrotikArpEntity> searchArp(@Param("query") String query);
}
