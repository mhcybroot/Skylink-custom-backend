package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.NetworkPortHistory;
import root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType;

import java.util.List;

@Repository
public interface NetworkPortHistoryRepository extends JpaRepository<NetworkPortHistory, Long>, JpaSpecificationExecutor<NetworkPortHistory> {

    List<NetworkPortHistory> findByPortIdOrderByRecordedAtDesc(Long portId);

    Page<NetworkPortHistory> findByPortIdOrderByRecordedAtDesc(Long portId, Pageable pageable);

    Page<NetworkPortHistory> findByDeviceIdOrderByRecordedAtDesc(Long deviceId, Pageable pageable);

    long countByPortId(Long portId);

    long countByDeviceId(Long deviceId);

    long countByEventType(PortHistoryEventType eventType);
}
