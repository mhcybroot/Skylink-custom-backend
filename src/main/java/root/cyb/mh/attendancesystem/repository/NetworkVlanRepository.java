package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.NetworkVlan;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkVlanRepository extends JpaRepository<NetworkVlan, Long> {
    List<NetworkVlan> findAllByOrderByVlanIdAsc();
    Optional<NetworkVlan> findByVlanId(Integer vlanId);
    boolean existsByVlanId(Integer vlanId);
}
