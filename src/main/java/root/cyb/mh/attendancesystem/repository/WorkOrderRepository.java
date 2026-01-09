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
}
