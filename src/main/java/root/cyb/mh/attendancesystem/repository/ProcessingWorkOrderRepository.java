package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.ProcessingWorkOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingWorkOrderRepository
        extends JpaRepository<ProcessingWorkOrder, Long>, JpaSpecificationExecutor<ProcessingWorkOrder> {

    List<ProcessingWorkOrder> findByCreatedByEmployeeIdOrderByCreatedAtDesc(String createdByEmployeeId);

    List<ProcessingWorkOrder> findAllByOrderByCreatedAtDesc();

    Optional<ProcessingWorkOrder> findById(Long id);

    @Query("""
            SELECT pwo
            FROM ProcessingWorkOrder pwo
            WHERE (:woNumber IS NULL OR TRIM(:woNumber) = '' OR LOWER(pwo.woNumber) LIKE LOWER(CONCAT('%', :woNumber, '%')))
              AND (:analyst IS NULL OR TRIM(:analyst) = '' OR LOWER(pwo.analyst) LIKE LOWER(CONCAT('%', :analyst, '%')))
              AND (:status IS NULL OR TRIM(:status) = '' OR LOWER(pwo.status) = LOWER(:status))
              AND (:fromDate IS NULL OR pwo.workDate >= :fromDate)
              AND (:toDate IS NULL OR pwo.workDate <= :toDate)
            ORDER BY pwo.workDate DESC, pwo.createdAt DESC
            """)
    List<ProcessingWorkOrder> search(
            @Param("woNumber") String woNumber,
            @Param("analyst") String analyst,
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
