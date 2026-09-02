package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkPortRepository extends JpaRepository<NetworkPort, Long> {

    List<NetworkPort> findByDeviceIdOrderByPortNumberAsc(Long deviceId);

    Optional<NetworkPort> findByDeviceIdAndPortNumberIgnoreCase(Long deviceId, String portNumber);

    List<NetworkPort> findByPortStatus(PortStatus portStatus);

    @Query("SELECT p FROM NetworkPort p JOIN FETCH p.device d WHERE " +
           "LOWER(p.ipAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.macAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.hostnameOrUser) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.vlan) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.portNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY d.name ASC, p.portNumber ASC")
    List<NetworkPort> searchPorts(@Param("query") String query);

    @Query("SELECT p FROM NetworkPort p JOIN FETCH p.device d WHERE p.portStatus IN (:statuses) ORDER BY d.name ASC, p.portNumber ASC")
    List<NetworkPort> findByPortStatusIn(@Param("statuses") List<PortStatus> statuses);

    @Query("SELECT COUNT(p) FROM NetworkPort p WHERE p.portStatus = 'ACTIVE_CONNECTED'")
    long countActivePorts();

    @Query("SELECT COUNT(p) FROM NetworkPort p WHERE p.portStatus IN ('PROBLEMATIC', 'FLAPPING', 'LINK_DOWN')")
    long countDegradedPorts();
}
