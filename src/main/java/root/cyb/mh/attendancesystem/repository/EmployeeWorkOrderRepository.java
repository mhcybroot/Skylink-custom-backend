package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeWorkOrderRepository extends JpaRepository<EmployeeWorkOrder, Long>, JpaSpecificationExecutor<EmployeeWorkOrder> {

    Optional<EmployeeWorkOrder> findByWoNumber(String woNumber);

    void deleteByImportBatchId(Long importBatchId);

    void deleteByImportBatchIdIsNull();

    List<EmployeeWorkOrder> findByDateReceivedBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT w.status, COUNT(w) FROM EmployeeWorkOrder w GROUP BY w.status")
    List<Object[]> countByStatus();

    @Query("SELECT SUM(w.clientInvoiceTotal) FROM EmployeeWorkOrder w")
    BigDecimal sumClientInvoiceTotal();

    @Query("SELECT SUM(w.contractorInvoiceTotal) FROM EmployeeWorkOrder w")
    BigDecimal sumContractorInvoiceTotal();
}
