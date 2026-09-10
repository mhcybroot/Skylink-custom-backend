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

    @Query("SELECT DISTINCT w.originalClientString FROM EmployeeWorkOrder w WHERE w.originalClientString IS NOT NULL AND TRIM(w.originalClientString) <> '' ORDER BY w.originalClientString")
    List<String> findDistinctClientStrings();

    @Query("SELECT DISTINCT w.originalContractorString FROM EmployeeWorkOrder w WHERE w.originalContractorString IS NOT NULL AND TRIM(w.originalContractorString) <> '' ORDER BY w.originalContractorString")
    List<String> findDistinctContractorStrings();

    @Query("SELECT DISTINCT w.workType FROM EmployeeWorkOrder w WHERE w.workType IS NOT NULL AND TRIM(w.workType) <> '' ORDER BY w.workType")
    List<String> findDistinctWorkTypes();

    @Query("SELECT DISTINCT w.admin FROM EmployeeWorkOrder w WHERE w.admin IS NOT NULL AND TRIM(w.admin) <> '' ORDER BY w.admin")
    List<String> findDistinctAdmins();

    @Query("SELECT DISTINCT w.customerBank FROM EmployeeWorkOrder w WHERE w.customerBank IS NOT NULL AND TRIM(w.customerBank) <> '' ORDER BY w.customerBank")
    List<String> findDistinctCustomerBanks();

    @Query("SELECT DISTINCT w.state FROM EmployeeWorkOrder w WHERE w.state IS NOT NULL AND TRIM(w.state) <> '' ORDER BY w.state")
    List<String> findDistinctStates();
}
