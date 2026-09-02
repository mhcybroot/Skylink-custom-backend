package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.enums.NetworkDeviceType;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    Optional<NetworkDevice> findByNameIgnoreCase(String name);

    List<NetworkDevice> findByDeviceType(NetworkDeviceType deviceType);

    @Query("SELECT d FROM NetworkDevice d LEFT JOIN FETCH d.ports ORDER BY d.id ASC")
    List<NetworkDevice> findAllWithPorts();

    @Query("SELECT d FROM NetworkDevice d LEFT JOIN FETCH d.ports WHERE d.id = :id")
    Optional<NetworkDevice> findByIdWithPorts(Long id);
}
