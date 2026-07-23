package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.SharedResource;

import java.util.List;

@Repository
public interface SharedResourceRepository extends JpaRepository<SharedResource, Long> {
    List<SharedResource> findByEmployeeId(String employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM SharedResource r WHERE r.employee.id = :employeeId OR r.folder.id IN :folderIds")
    List<SharedResource> findByEmployeeIdOrFolderIdIn(@org.springframework.data.repository.query.Param("employeeId") String employeeId, @org.springframework.data.repository.query.Param("folderIds") List<Long> folderIds);
}
