package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.MikrotikSetting;

@Repository
public interface MikrotikSettingRepository extends JpaRepository<MikrotikSetting, Long> {
}
