package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.WorkOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByWoNumber(String woNumber);

    @Query("SELECT w.status, COUNT(w) FROM WorkOrder w GROUP BY w.status")
    List<Object[]> countByStatus();

    @Query("SELECT SUM(w.clientInvoiceTotal) FROM WorkOrder w")
    BigDecimal sumClientInvoiceTotal();

    @Query("SELECT SUM(w.contractorInvoiceTotal) FROM WorkOrder w")
    BigDecimal sumContractorInvoiceTotal();

    @Query("SELECT w.contractor.name, COUNT(w) as cnt FROM WorkOrder w WHERE w.contractor IS NOT NULL GROUP BY w.contractor.name ORDER BY cnt DESC")
    List<Object[]> findTopContractors();

    @Query("SELECT w FROM WorkOrder w WHERE w.status IN ('Closed', 'Complete')")
    List<WorkOrder> findClosedWorkOrders();

    @Query("SELECT w FROM WorkOrder w WHERE w.status = 'Cancelled'")
    List<WorkOrder> findCancelledWorkOrders();

    @Query("SELECT w FROM WorkOrder w WHERE w.status NOT IN ('Closed', 'Complete', 'Cancelled') OR w.status IS NULL")
    List<WorkOrder> findOpenWorkOrders();

    @Query("SELECT YEAR(w.dateReceived), MONTH(w.dateReceived), COUNT(w) FROM WorkOrder w WHERE w.dateReceived IS NOT NULL GROUP BY YEAR(w.dateReceived), MONTH(w.dateReceived) ORDER BY YEAR(w.dateReceived), MONTH(w.dateReceived)")
    List<Object[]> findWorkOrderCountsByMonth();

    @Query("SELECT w.client.name, SUM(w.clientInvoiceTotal) as total FROM WorkOrder w WHERE w.client IS NOT NULL GROUP BY w.client.name ORDER BY total DESC")
    List<Object[]> findTopClientsByRevenue();

    @Query("SELECT w.workType, SUM(w.clientInvoiceTotal), SUM(w.contractorInvoiceTotal) FROM WorkOrder w WHERE w.workType IS NOT NULL GROUP BY w.workType")
    List<Object[]> findWorkTypeMargins();

    @Query("SELECT w.state, COUNT(w) as cnt FROM WorkOrder w WHERE w.state IS NOT NULL GROUP BY w.state ORDER BY cnt DESC")
    List<Object[]> findWorkOrderDistributionByState();

    @Query("SELECT w.contractor.name, w.contractorInvoiceTotal, w.dateReceived, w.invoiceDate FROM WorkOrder w WHERE w.contractor IS NOT NULL AND w.invoiceDate IS NOT NULL AND w.dateReceived IS NOT NULL")
    List<Object[]> findContractorPerformanceData();

    @Query("SELECT w.workType, w.dateReceived, w.invoiceDate FROM WorkOrder w WHERE w.workType IS NOT NULL AND w.invoiceDate IS NOT NULL AND w.dateReceived IS NOT NULL")
    List<Object[]> findCycleTimeByWorkType();

    @Query("SELECT w.client.name, SUM(w.clientInvoiceTotal), SUM(w.contractorInvoiceTotal) FROM WorkOrder w WHERE w.client IS NOT NULL GROUP BY w.client.name")
    List<Object[]> findMarginByClient();

    @Query("SELECT w.state, SUM(w.clientInvoiceTotal), SUM(w.contractorInvoiceTotal) FROM WorkOrder w WHERE w.state IS NOT NULL GROUP BY w.state")
    List<Object[]> findMarginByState();
}
