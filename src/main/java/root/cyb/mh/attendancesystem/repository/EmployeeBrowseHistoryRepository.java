package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeBrowseHistoryRepository extends JpaRepository<EmployeeBrowseHistory, Long>, JpaSpecificationExecutor<EmployeeBrowseHistory> {

    @EntityGraph(attributePaths = {"employee"})
    Page<EmployeeBrowseHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT DISTINCT e.url FROM EmployeeBrowseHistory e WHERE LOWER(e.url) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctUrlsContaining(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    @Query(value = "SELECT DISTINCT REPLACE(SUBSTRING(url FROM 'https?://([^/]+)'), 'www.', '') AS domain " +
                   "FROM employee_browse_history " +
                   "WHERE url ILIKE CONCAT('%', :query, '%') " +
                   "AND SUBSTRING(url FROM 'https?://([^/]+)') IS NOT NULL " +
                   "LIMIT 10", nativeQuery = true)
    List<String> findDistinctDomainsContaining(@org.springframework.data.repository.query.Param("query") String query);

    @Query("SELECT DISTINCT e.pageTitle FROM EmployeeBrowseHistory e WHERE LOWER(e.pageTitle) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctTitlesContaining(@org.springframework.data.repository.query.Param("query") String query, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeBrowseHistory e WHERE e.timestamp < :threshold")
    void deleteOlderThan(LocalDateTime threshold);
}
