package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;

import java.time.LocalDateTime;

@Repository
public interface EmployeeBrowseHistoryRepository extends JpaRepository<EmployeeBrowseHistory, Long> {

    @EntityGraph(attributePaths = {"employee"})
    Page<EmployeeBrowseHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeBrowseHistory e WHERE e.timestamp < :threshold")
    void deleteOlderThan(LocalDateTime threshold);
}
