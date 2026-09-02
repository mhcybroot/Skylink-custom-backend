package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.NetworkIssue;
import root.cyb.mh.attendancesystem.model.enums.IssueStatus;

import java.util.List;

@Repository
public interface NetworkIssueRepository extends JpaRepository<NetworkIssue, Long> {

    List<NetworkIssue> findByDeviceIdOrderByReportedAtDesc(Long deviceId);

    List<NetworkIssue> findByPortIdOrderByReportedAtDesc(Long portId);

    List<NetworkIssue> findByStatusOrderByReportedAtDesc(IssueStatus status);

    @Query("SELECT i FROM NetworkIssue i LEFT JOIN FETCH i.device d LEFT JOIN FETCH i.port p ORDER BY i.reportedAt DESC")
    List<NetworkIssue> findAllWithDetails();

    @Query("SELECT i FROM NetworkIssue i LEFT JOIN FETCH i.device d LEFT JOIN FETCH i.port p WHERE i.status IN ('OPEN', 'INVESTIGATING') ORDER BY i.severity DESC, i.reportedAt DESC")
    List<NetworkIssue> findActiveIssues();

    @Query("SELECT COUNT(i) FROM NetworkIssue i WHERE i.status IN ('OPEN', 'INVESTIGATING')")
    long countOpenIssues();

    @Query("SELECT i FROM NetworkIssue i LEFT JOIN FETCH i.device d LEFT JOIN FETCH i.port p WHERE " +
           "LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.portNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.hostnameOrUser) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY i.reportedAt DESC")
    List<NetworkIssue> searchIssues(@Param("query") String query);
}
