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
}
