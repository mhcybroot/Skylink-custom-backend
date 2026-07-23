package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.EmployeeCallLog;

import java.util.List;

@Repository
public interface EmployeeCallLogRepository extends JpaRepository<EmployeeCallLog, Long> {

    @Query("SELECT DISTINCT c.employeeUsername FROM EmployeeCallLog c WHERE c.employeeUsername IS NOT NULL")
    List<String> findDistinctEmployeeUsernames();

    @Query("SELECT c FROM EmployeeCallLog c WHERE " +
           "(:employeeUsername IS NULL OR c.employeeUsername = :employeeUsername) " +
           "ORDER BY c.callTimestamp DESC")
    List<EmployeeCallLog> searchCallLogs(@Param("employeeUsername") String employeeUsername);
}
