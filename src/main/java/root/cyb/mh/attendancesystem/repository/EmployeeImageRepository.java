package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.EmployeeImage;

import java.util.List;

@Repository
public interface EmployeeImageRepository extends JpaRepository<EmployeeImage, Long> {

    @Query("SELECT DISTINCT i.employeeUsername FROM EmployeeImage i WHERE i.employeeUsername IS NOT NULL")
    List<String> findDistinctEmployeeUsernames();

    @Query("SELECT i FROM EmployeeImage i WHERE " +
           "(:employeeUsername IS NULL OR i.employeeUsername = :employeeUsername) " +
           "ORDER BY i.uploadedAt DESC")
    List<EmployeeImage> searchImages(@Param("employeeUsername") String employeeUsername);
}
