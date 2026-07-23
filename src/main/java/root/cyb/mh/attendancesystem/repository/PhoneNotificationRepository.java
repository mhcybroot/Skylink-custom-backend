package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.PhoneNotification;

import java.util.List;

@Repository
public interface PhoneNotificationRepository extends JpaRepository<PhoneNotification, Long> {
    List<PhoneNotification> findAllByOrderByInterceptedAtDesc();
    List<PhoneNotification> findByEmployeeUsernameOrderByInterceptedAtDesc(String employeeUsername);

    @Query("SELECT DISTINCT n.employeeUsername FROM PhoneNotification n WHERE n.employeeUsername IS NOT NULL")
    List<String> findDistinctEmployeeUsernames();

    @Query("SELECT DISTINCT n.packageName FROM PhoneNotification n WHERE n.packageName IS NOT NULL")
    List<String> findDistinctPackageNames();

    @Query("SELECT n FROM PhoneNotification n WHERE " +
           "(:employeeUsername IS NULL OR n.employeeUsername = :employeeUsername) AND " +
           "(:packageName IS NULL OR n.packageName = :packageName) AND " +
           "(:searchStr IS NULL OR LOWER(CAST(n.title AS string)) LIKE :searchStr OR LOWER(CAST(n.text AS string)) LIKE :searchStr) " +
           "ORDER BY n.interceptedAt DESC")
    List<PhoneNotification> searchNotifications(@Param("employeeUsername") String employeeUsername,
                                              @Param("packageName") String packageName,
                                              @Param("searchStr") String searchStr);
}
