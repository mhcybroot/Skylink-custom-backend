package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ResourceFolder;

import java.util.List;

@Repository
public interface ResourceFolderRepository extends JpaRepository<ResourceFolder, Long> {
    List<ResourceFolder> findByParentFolderIsNull();
    List<ResourceFolder> findByAllowedEmployeesContaining(Employee employee);
}
