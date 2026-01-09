package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.repository.WorkOrderRepository;
import root.cyb.mh.attendancesystem.model.WorkOrder;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/work-orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @GetMapping
    public String listWorkOrders(@RequestParam(required = false) String status, Model model) {
        List<WorkOrder> workOrders;
        String filterName = "All Work Orders";

        if ("closed".equalsIgnoreCase(status)) {
            workOrders = workOrderRepository.findClosedWorkOrders();
            filterName = "Closed / Complete Work Orders";
        } else if ("cancelled".equalsIgnoreCase(status)) {
            workOrders = workOrderRepository.findCancelledWorkOrders();
            filterName = "Cancelled Work Orders";
        } else if ("open".equalsIgnoreCase(status)) {
            workOrders = workOrderRepository.findOpenWorkOrders();
            filterName = "Open / In Progress Work Orders";
        } else {
            workOrders = workOrderRepository.findAll();
        }

        model.addAttribute("workOrders", workOrders);
        model.addAttribute("activeLink", "work-orders");
        model.addAttribute("currentFilter", filterName);
        return "work-order/list";
    }

    @GetMapping("/dashboard")
    public String workOrderDashboard(Model model) {
        WorkOrderDashboardDTO stats = new WorkOrderDashboardDTO();

        // Financials
        BigDecimal totalRev = workOrderRepository.sumClientInvoiceTotal();
        BigDecimal totalCost = workOrderRepository.sumContractorInvoiceTotal();
        stats.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);
        stats.setTotalCost(totalCost != null ? totalCost : BigDecimal.ZERO);
        stats.setTotalMargin(stats.getTotalRevenue().subtract(stats.getTotalCost()));

        // Counts
        List<Object[]> statusCounts = workOrderRepository.countByStatus();
        long total = 0;
        long open = 0;
        long closed = 0;
        long cancelled = 0;
        Map<String, Long> dist = new HashMap<>();

        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            if (status == null)
                status = "Unknown";

            total += count;
            dist.put(status, count);

            if ("Complete".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status)) {
                closed += count;
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                cancelled += count;
            } else {
                open += count;
            }
        }
        stats.setTotalWorkOrders(total);
        stats.setOpenWorkOrders(open);
        stats.setClosedWorkOrders(closed);
        stats.setCancelledWorkOrders(cancelled);
        stats.setStatusDistribution(dist);

        // Averages
        if (total > 0) {
            BigDecimal divisor = new BigDecimal(total);
            stats.setAvgRevenue(stats.getTotalRevenue().divide(divisor, 2, java.math.RoundingMode.HALF_UP));
            stats.setAvgCost(stats.getTotalCost().divide(divisor, 2, java.math.RoundingMode.HALF_UP));
            stats.setAvgMargin(stats.getTotalMargin().divide(divisor, 2, java.math.RoundingMode.HALF_UP));
        } else {
            stats.setAvgRevenue(BigDecimal.ZERO);
            stats.setAvgCost(BigDecimal.ZERO);
            stats.setAvgMargin(BigDecimal.ZERO);
        }

        // Top Contractors
        List<Object[]> topContractors = workOrderRepository.findTopContractors();
        List<WorkOrderDashboardDTO.ContractorStat> top5 = topContractors.stream()
                .limit(5)
                .map(row -> new WorkOrderDashboardDTO.ContractorStat((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
        stats.setTopContractors(top5);

        model.addAttribute("stats", stats);
        model.addAttribute("activeLink", "work-orders");
        return "work-order/dashboard";
    }
}
