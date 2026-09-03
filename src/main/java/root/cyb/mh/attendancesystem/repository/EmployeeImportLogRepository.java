package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeImportLog;

import java.util.List;

@Repository
public interface EmployeeImportLogRepository extends JpaRepository<EmployeeImportLog, Long> {
    List<EmployeeImportLog> findAllByOrderByImportDateDesc();
    List<EmployeeImportLog> findByImportedByOrderByImportDateDesc(Employee employee);
}
