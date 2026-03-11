package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    java.util.List<Employee> findByDepartmentId(Long departmentId);

    // Check if an employee is a supervisor (either primary or assistant)
    boolean existsByReportsTo_IdOrReportsToAssistant_Id(String primaryId, String assistantId);

    // Find all subordinates for a supervisor (Primary OR Assistant)
    java.util.List<Employee> findByReportsTo_IdOrReportsToAssistant_Id(String primaryId, String assistantId);

    java.util.Optional<Employee> findByUsername(String username);

    @Query("SELECT e FROM Employee e LEFT JOIN e.department d WHERE " +
            "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.role) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.designation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.id) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Employee> searchEmployees(@Param("keyword") String keyword, Pageable pageable);
}
