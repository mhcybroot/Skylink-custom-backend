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

        // Work Orders Over Time
        List<Object[]> overTimeData = workOrderRepository.findWorkOrderCountsByMonth();
        Map<String, Long> overTimeMap = new java.util.LinkedHashMap<>();
        java.time.format.DateTimeFormatter monthYearFmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");

        for (Object[] row : overTimeData) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            Long count = (Long) row[2];
            if (year != null && month != null) {
                java.time.YearMonth ym = java.time.YearMonth.of(year, month);
                overTimeMap.put(ym.format(monthYearFmt), count);
            }
        }
        stats.setWorkOrdersOverTime(overTimeMap);

        // Top Clients by Revenue
        List<Object[]> topClientsData = workOrderRepository.findTopClientsByRevenue();
        List<WorkOrderDashboardDTO.ClientStat> topClients = topClientsData.stream()
                .limit(5)
                .map(row -> new WorkOrderDashboardDTO.ClientStat((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
        stats.setTopClients(topClients);

        // Margin by Work Type
        List<Object[]> workTypeData = workOrderRepository.findWorkTypeMargins();
        List<WorkOrderDashboardDTO.WorkTypeStat> margins = workTypeData.stream()
                .map(row -> {
                    String type = (String) row[0];
                    BigDecimal rev = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal cost = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    return new WorkOrderDashboardDTO.WorkTypeStat(type, rev.subtract(cost));
                })
                .sorted((a, b) -> b.getTotalMargin().compareTo(a.getTotalMargin()))
                .collect(Collectors.toList());
        stats.setWorkTypeMargins(margins);

        // State Distribution
        List<Object[]> stateData = workOrderRepository.findWorkOrderDistributionByState();
        List<WorkOrderDashboardDTO.StateStat> stateStats = stateData.stream()
                .map(row -> new WorkOrderDashboardDTO.StateStat((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
        stats.setStateDistribution(stateStats);

        model.addAttribute("stats", stats);
        model.addAttribute("activeLink", "work-orders");
        return "work-order/dashboard";
    }
}
