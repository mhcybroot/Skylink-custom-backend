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

        // Contractor Scorecards
        List<Object[]> rawPerfData = workOrderRepository.findContractorPerformanceData();

        // Process data to compute metrics per contractor
        Map<String, List<Object[]>> groupedByContractor = rawPerfData.stream()
                .collect(Collectors.groupingBy(row -> (String) row[0]));

        List<WorkOrderDashboardDTO.ContractorScorecard> scorecards = new java.util.ArrayList<>();
        BigDecimal globalSumCost = BigDecimal.ZERO;
        double globalSumDays = 0;
        long globalCount = 0;

        for (Map.Entry<String, List<Object[]>> entry : groupedByContractor.entrySet()) {
            String name = entry.getKey();
            List<Object[]> rows = entry.getValue();
            long count = rows.size();

            BigDecimal sumCost = BigDecimal.ZERO;
            long sumDays = 0;

            for (Object[] row : rows) {
                BigDecimal cost = (BigDecimal) row[1];
                java.time.LocalDate dateReceived = (java.time.LocalDate) row[2];
                java.time.LocalDate invoiceDate = (java.time.LocalDate) row[3];

                if (cost != null)
                    sumCost = sumCost.add(cost);
                if (dateReceived != null && invoiceDate != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(dateReceived, invoiceDate);
                    sumDays += Math.max(0, days); // Ensure no negative days
                }
            }

            BigDecimal avgCost = count > 0
                    ? sumCost.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            double avgDays = count > 0 ? (double) sumDays / count : 0.0;

            scorecards.add(new WorkOrderDashboardDTO.ContractorScorecard(name, count, avgCost, avgDays));

            globalSumCost = globalSumCost.add(sumCost);
            globalSumDays += sumDays;
            globalCount += count;
        }

        // Compute Benchmark
        BigDecimal globalAvgCost = globalCount > 0
                ? globalSumCost.divide(BigDecimal.valueOf(globalCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double globalAvgDays = globalCount > 0 ? globalSumDays / globalCount : 0.0;

        // Sort scorecards by Volume desc
        scorecards.sort((a, b) -> Long.compare(b.getTotalWorkOrders(), a.getTotalWorkOrders()));

        stats.setContractorScorecards(scorecards);
        stats.setBenchmark(new WorkOrderDashboardDTO.ScorecardBenchmark(globalAvgCost, globalAvgDays));

        // Cycle Time Analysis
        // By Work Type
        List<Object[]> workTypeCycleData = workOrderRepository.findCycleTimeByWorkType();
        Map<String, List<Long>> byWorkTypeRaw = workTypeCycleData.stream().collect(Collectors.groupingBy(
                row -> (String) row[0],
                Collectors.mapping(row -> {
                    java.time.LocalDate dr = (java.time.LocalDate) row[1];
                    java.time.LocalDate inv = (java.time.LocalDate) row[2];
                    return java.time.temporal.ChronoUnit.DAYS.between(dr, inv);
                }, Collectors.toList())));
        Map<String, Double> byWorkType = new java.util.LinkedHashMap<>();
        byWorkTypeRaw.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue().stream().mapToLong(Long::longValue).average().orElse(0),
                        a.getValue().stream().mapToLong(Long::longValue).average().orElse(0)))
                .forEach(e -> byWorkType.put(e.getKey(),
                        e.getValue().stream().mapToLong(Long::longValue).average().orElse(0)));

        // By Contractor (from existing data)
        Map<String, Double> byContractor = new java.util.LinkedHashMap<>();
        scorecards.stream()
                .sorted((a, b) -> Double.compare(b.getAverageDaysToComplete(), a.getAverageDaysToComplete()))
                .forEach(s -> byContractor.put(s.getName(), s.getAverageDaysToComplete()));

        // Histogram Distribution
        Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        distribution.put("0-3 days", 0L);
        distribution.put("4-7 days", 0L);
        distribution.put("8-14 days", 0L);
        distribution.put("15-30 days", 0L);
        distribution.put("30+ days", 0L);

        rawPerfData.forEach(row -> {
            java.time.LocalDate dr = (java.time.LocalDate) row[2];
            java.time.LocalDate inv = (java.time.LocalDate) row[3];
            long days = java.time.temporal.ChronoUnit.DAYS.between(dr, inv);
            if (days <= 3)
                distribution.merge("0-3 days", 1L, Long::sum);
            else if (days <= 7)
                distribution.merge("4-7 days", 1L, Long::sum);
            else if (days <= 14)
                distribution.merge("8-14 days", 1L, Long::sum);
            else if (days <= 30)
                distribution.merge("15-30 days", 1L, Long::sum);
            else
                distribution.merge("30+ days", 1L, Long::sum);
        });

        stats.setCycleTimeAnalysis(new WorkOrderDashboardDTO.CycleTimeAnalysis(byWorkType, byContractor, distribution));

        // Profitability Analysis
        // By Client
        List<Object[]> marginByClientData = workOrderRepository.findMarginByClient();
        Map<String, BigDecimal> marginByClient = new java.util.LinkedHashMap<>();
        marginByClientData.stream()
                .map(row -> {
                    String name = (String) row[0];
                    BigDecimal rev = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal cost = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    return new java.util.AbstractMap.SimpleEntry<>(name, rev.subtract(cost));
                })
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(e -> marginByClient.put(e.getKey(), e.getValue()));

        // By State
        List<Object[]> marginByStateData = workOrderRepository.findMarginByState();
        Map<String, BigDecimal> marginByState = new java.util.LinkedHashMap<>();
        marginByStateData.stream()
                .map(row -> {
                    String state = (String) row[0];
                    BigDecimal rev = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal cost = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    return new java.util.AbstractMap.SimpleEntry<>(state, rev.subtract(cost));
                })
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> marginByState.put(e.getKey(), e.getValue()));

        stats.setProfitabilityAnalysis(new WorkOrderDashboardDTO.ProfitabilityAnalysis(marginByClient, marginByState));

        model.addAttribute("stats", stats);
        model.addAttribute("activeLink", "work-orders");
        return "work-order/dashboard";
    }
}
