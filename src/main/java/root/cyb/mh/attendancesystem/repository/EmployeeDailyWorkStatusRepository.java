package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDailyWorkStatusRepository extends JpaRepository<EmployeeDailyWorkStatus, Long> {
    Optional<EmployeeDailyWorkStatus> findByEmployeeIdAndDate(String employeeId, LocalDate date);

    List<EmployeeDailyWorkStatus> findByStatus(root.cyb.mh.attendancesystem.model.WorkStatus status);

    List<EmployeeDailyWorkStatus> findByDate(LocalDate date);

    List<EmployeeDailyWorkStatus> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
