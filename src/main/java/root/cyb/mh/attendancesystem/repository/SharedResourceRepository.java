package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.SharedResource;

import java.util.List;

@Repository
public interface SharedResourceRepository extends JpaRepository<SharedResource, Long> {
    List<SharedResource> findByEmployeeId(String employeeId);
}
