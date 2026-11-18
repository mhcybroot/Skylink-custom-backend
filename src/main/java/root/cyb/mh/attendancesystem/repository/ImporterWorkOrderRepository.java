package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.ImporterWorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImporterWorkOrderRepository extends JpaRepository<ImporterWorkOrder, Long> {

    List<ImporterWorkOrder> findByEntryDateAndImporterEmployeeIdOrderByCreatedAtDesc(LocalDate entryDate, String importerEmployeeId);

    List<ImporterWorkOrder> findByEntryDateOrderByCreatedAtDesc(LocalDate entryDate);

    @Query("SELECT COUNT(i) FROM ImporterWorkOrder i WHERE i.entryDate = :entryDate AND i.importerEmployeeId = :importerEmployeeId")
    long countByEntryDateAndImporterEmployeeId(@Param("entryDate") LocalDate entryDate, @Param("importerEmployeeId") String importerEmployeeId);

    @Query("SELECT COALESCE(SUM(i.value), 0) FROM ImporterWorkOrder i WHERE i.entryDate = :entryDate AND i.importerEmployeeId = :importerEmployeeId")
    BigDecimal sumValueByEntryDateAndImporterEmployeeId(@Param("entryDate") LocalDate entryDate, @Param("importerEmployeeId") String importerEmployeeId);

    List<ImporterWorkOrder> findByImporterEmployeeIdOrderByCreatedAtDesc(String importerEmployeeId);

    List<ImporterWorkOrder> findByImporterEmployeeIdOrderByEntryDateDescCreatedAtDesc(String importerEmployeeId);

    List<ImporterWorkOrder> findByImporterEmployeeIdAndEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(String importerEmployeeId, LocalDate startDate, LocalDate endDate);

    List<ImporterWorkOrder> findAllByOrderByEntryDateDescCreatedAtDesc();

    List<ImporterWorkOrder> findByEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT i.importerEmployeeId, i.importerName FROM ImporterWorkOrder i ORDER BY i.importerName ASC")
    List<Object[]> findDistinctImporters();

    @Query("SELECT DISTINCT i.clientCode FROM ImporterWorkOrder i WHERE i.clientCode IS NOT NULL AND i.clientCode <> '' ORDER BY i.clientCode ASC")
    List<String> findDistinctClientCodes();

    @Query("SELECT DISTINCT i.workType FROM ImporterWorkOrder i WHERE i.workType IS NOT NULL AND i.workType <> '' ORDER BY i.workType ASC")
    List<String> findDistinctWorkTypes();
}
