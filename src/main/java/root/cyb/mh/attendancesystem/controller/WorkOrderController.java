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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.specification.WorkOrderSpecifications;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/work-orders")
public class WorkOrderController {

        @Autowired
        private WorkOrderRepository workOrderRepository;

        @Autowired
        private root.cyb.mh.attendancesystem.service.WorkOrderReportService workOrderReportService;

        @GetMapping("/report")
        public String generateReport(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Boolean clientInvoicePaid,
                        @RequestParam(required = false) Boolean contractorInvoicePaid,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String workType,
                        @RequestParam(required = false) String client,
                        @RequestParam(required = false) String contractor,
                        Model model) {

                // 1. Build Specification (Reuse the existing powerful specification)
                Specification<WorkOrder> spec = WorkOrderSpecifications.withFilters(status, clientInvoicePaid,
                                contractorInvoicePaid, startDate, endDate, search, workType, client, contractor);

                // 2. Fetch Data (All records matching filter, unsorted or default sort)
                List<WorkOrder> reportData = workOrderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));

                // 3. Calculate Stats using the new Service
                WorkOrderDashboardDTO stats = workOrderReportService.calculateStatistics(reportData);

                // 4. Construct Filter Label
                String reportTitle = "Custom Work Order Report";
                if (startDate != null && endDate != null) {
                        reportTitle += " (" + startDate + " - " + endDate + ")";
                }

                model.addAttribute("stats", stats);
                model.addAttribute("reportData", reportData); // For the table list
                model.addAttribute("reportTitle", reportTitle);
                model.addAttribute("generatedDate", java.time.LocalDateTime.now());
                model.addAttribute("activeLink", "work-orders");

                return "work-order/report";
        }

        @GetMapping
        public String listWorkOrders(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Boolean clientInvoicePaid,
                        @RequestParam(required = false) Boolean contractorInvoicePaid,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String workType,
                        @RequestParam(required = false) String client,
                        @RequestParam(required = false) String contractor,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model) {

                // Build Specification
                Specification<WorkOrder> spec = WorkOrderSpecifications.withFilters(status, clientInvoicePaid,
                                contractorInvoicePaid, startDate, endDate, search, workType, client, contractor);

                // Pagination (Spring Data is 0-indexed)
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

                // Fetch Data
                Page<WorkOrder> workOrders = workOrderRepository.findAll(spec, pageable);

                // Construct Filter Name for Display
                String filterName = "All Work Orders";
                if (status != null && !status.isEmpty()) {
                        if ("closed".equalsIgnoreCase(status)) {
                                filterName = "Closed / Complete Work Orders";
                        } else if ("cancelled".equalsIgnoreCase(status)) {
                                filterName = "Cancelled Work Orders";
                        } else if ("open".equalsIgnoreCase(status)) {
                                filterName = "Open / In Progress Work Orders";
                        } else {
                                filterName = status + " Work Orders";
                        }
                } else if (clientInvoicePaid != null) {
                        filterName = clientInvoicePaid ? "Client Invoices Paid" : "Client Invoices Unpaid";
                } else if (contractorInvoicePaid != null) {
                        filterName = contractorInvoicePaid ? "Contractor Invoices Paid" : "Contractor Invoices Unpaid";
                }

                if (startDate != null && endDate != null) {
                        filterName += " (" + startDate + " to " + endDate + ")";
                }

                if (search != null && !search.isEmpty()) {
                        filterName += " | Search: " + search;
                }

                model.addAttribute("workOrders", workOrders);
                model.addAttribute("activeLink", "work-orders");
                model.addAttribute("currentFilter", filterName);

                // Pagination & Filter Params for UI
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", workOrders.getTotalPages());
                model.addAttribute("totalItems", workOrders.getTotalElements());
                model.addAttribute("size", size);

                // Pass back params to maintain state in pagination links
                model.addAttribute("status", status);
                model.addAttribute("clientInvoicePaid", clientInvoicePaid);
                model.addAttribute("contractorInvoicePaid", contractorInvoicePaid);
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);

                model.addAttribute("search", search);
                model.addAttribute("workType", workType);
                model.addAttribute("client", client);
                model.addAttribute("contractor", contractor);

                return "work-order/list";
        }

        @GetMapping("/dashboard")
        public String workOrderDashboard(
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                        Model model) {
                WorkOrderDashboardDTO stats = new WorkOrderDashboardDTO();

                // Fetch all work orders (filtered by date if provided)
                List<WorkOrder> allWorkOrders;
                if (startDate != null && endDate != null) {
                        allWorkOrders = workOrderRepository.findByDateReceivedBetween(startDate, endDate);
                } else {
                        allWorkOrders = workOrderRepository.findAll();
                }

                // Financials (from filtered list)
                BigDecimal totalRev = allWorkOrders.stream()
                                .map(WorkOrder::getClientInvoiceTotal)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCost = allWorkOrders.stream()
                                .map(WorkOrder::getContractorInvoiceTotal)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTotalRevenue(totalRev);
                stats.setTotalCost(totalCost);
                stats.setTotalMargin(totalRev.subtract(totalCost));

                // Detailed Financials
                stats.setTotalClientDiscount(allWorkOrders.stream()
                                .map(WorkOrder::getClientDiscountTotal)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                stats.setTotalContractorDiscount(BigDecimal.ZERO); // Not explicitly tracked as total amount, only
                                                                   // percent in DTO?
                // Ah, wait, we don't have contractor discount total in Entity, only percent.
                // Leaving as ZERO or calculating if needed.
                // Let's rely on what we have.

                stats.setTotalWriteOffs(allWorkOrders.stream()
                                .map(WorkOrder::getWriteOffAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                stats.setRealizedRevenue(allWorkOrders.stream()
                                .map(WorkOrder::getClientPaidAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                stats.setRealizedCost(allWorkOrders.stream()
                                .map(WorkOrder::getContractorPaidAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                stats.setUnrealizedRevenue(totalRev.subtract(stats.getRealizedRevenue()));

                // Status Counts (from filtered list)
                long total = allWorkOrders.size();
                Map<String, Long> dist = allWorkOrders.stream()
                                .collect(Collectors.groupingBy(
                                                w -> w.getStatus() != null ? w.getStatus() : "Unknown",
                                                Collectors.counting()));

                long closed = allWorkOrders.stream()
                                .filter(w -> "Complete".equalsIgnoreCase(w.getStatus())
                                                || "Closed".equalsIgnoreCase(w.getStatus()))
                                .count();
                long cancelled = allWorkOrders.stream()
                                .filter(w -> "Cancelled".equalsIgnoreCase(w.getStatus()))
                                .count();
                long open = total - closed - cancelled;

                stats.setTotalWorkOrders(total);
                stats.setOpenWorkOrders(open);
                stats.setClosedWorkOrders(closed);
                stats.setCancelledWorkOrders(cancelled);
                stats.setStatusDistribution(dist);

                // Invoice Payment Status Counts
                stats.setClientInvoicesPaid(allWorkOrders.stream()
                                .filter(w -> w.isClientInvoicePaid())
                                .count());
                stats.setClientInvoicesUnpaid(allWorkOrders.stream()
                                .filter(w -> !w.isClientInvoicePaid() && w.getClientInvoiceTotal() != null
                                                && w.getClientInvoiceTotal().compareTo(BigDecimal.ZERO) > 0)
                                .count());
                stats.setContractorInvoicesPaid(allWorkOrders.stream()
                                .filter(w -> w.isContractorInvoicePaid())
                                .count());
                stats.setContractorInvoicesUnpaid(allWorkOrders.stream()
                                .filter(w -> !w.isContractorInvoicePaid() && w.getContractorInvoiceTotal() != null
                                                && w.getContractorInvoiceTotal().compareTo(BigDecimal.ZERO) > 0)
                                .count());

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

                // Top Contractors (from filtered list)
                List<WorkOrderDashboardDTO.ContractorStat> top5 = allWorkOrders.stream()
                                .filter(w -> w.getContractor() != null)
                                .collect(Collectors.groupingBy(w -> w.getContractor().getName(), Collectors.counting()))
                                .entrySet().stream()
                                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                                .limit(5)
                                .map(e -> new WorkOrderDashboardDTO.ContractorStat(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                stats.setTopContractors(top5);

                // Top Banks (NEW)
                List<WorkOrderDashboardDTO.BankStat> topBanks = allWorkOrders.stream()
                                .filter(w -> w.getCustomerBank() != null && !w.getCustomerBank().isEmpty())
                                .collect(Collectors.groupingBy(WorkOrder::getCustomerBank))
                                .entrySet().stream()
                                .map(e -> {
                                        String bankName = e.getKey();
                                        Long count = e.getValue().stream().count();
                                        BigDecimal revenue = e.getValue().stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new WorkOrderDashboardDTO.BankStat(bankName, count, revenue);
                                })
                                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                                .limit(5)
                                .collect(Collectors.toList());
                stats.setTopBanks(topBanks);

                // Work Orders Over Time (Invoice Date as proxy since Recvd Date missing)
                java.time.format.DateTimeFormatter monthYearFmt = java.time.format.DateTimeFormatter
                                .ofPattern("MMM yyyy");
                Map<String, Long> overTimeMap = allWorkOrders.stream()
                                .filter(w -> w.getInvoiceDate() != null)
                                .collect(Collectors.groupingBy(
                                                w -> java.time.YearMonth.from(w.getInvoiceDate()),
                                                Collectors.counting()))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .collect(Collectors.toMap(
                                                e -> e.getKey().format(monthYearFmt),
                                                Map.Entry::getValue,
                                                (a, b) -> a,
                                                java.util.LinkedHashMap::new));
                stats.setWorkOrdersOverTime(overTimeMap);

                // Top Clients by Revenue (from filtered list)
                List<WorkOrderDashboardDTO.ClientStat> topClients = allWorkOrders.stream()
                                .filter(w -> w.getClient() != null && w.getClientInvoiceTotal() != null)
                                .collect(Collectors.groupingBy(
                                                w -> w.getClient().getName(),
                                                Collectors.reducing(BigDecimal.ZERO, WorkOrder::getClientInvoiceTotal,
                                                                BigDecimal::add)))
                                .entrySet().stream()
                                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                                .limit(5)
                                .map(e -> new WorkOrderDashboardDTO.ClientStat(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                stats.setTopClients(topClients);

                // Margin by Work Type (from filtered list)
                List<WorkOrderDashboardDTO.WorkTypeStat> margins = allWorkOrders.stream()
                                .filter(w -> w.getWorkType() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getWorkType))
                                .entrySet().stream()
                                .map(e -> {
                                        BigDecimal rev = e.getValue().stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal cost = e.getValue().stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new WorkOrderDashboardDTO.WorkTypeStat(e.getKey(), rev.subtract(cost));
                                })
                                .sorted((a, b) -> b.getTotalMargin().compareTo(a.getTotalMargin()))
                                .collect(Collectors.toList());
                stats.setWorkTypeMargins(margins);

                // State Distribution (from filtered list)
                List<WorkOrderDashboardDTO.StateStat> stateStats = allWorkOrders.stream()
                                .filter(w -> w.getState() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getState, Collectors.counting()))
                                .entrySet().stream()
                                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                                .map(e -> new WorkOrderDashboardDTO.StateStat(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                stats.setStateDistribution(stateStats);

                // Contractor Scorecards

                // Group by Contractor
                Map<String, List<WorkOrder>> groupedByContractor = allWorkOrders.stream()
                                .filter(w -> w.getContractor() != null)
                                .collect(Collectors.groupingBy(w -> w.getContractor().getName()));

                List<WorkOrderDashboardDTO.ContractorScorecard> scorecards = new java.util.ArrayList<>();
                BigDecimal globalSumCost = BigDecimal.ZERO;
                double globalSumDays = 0;
                long globalCount = 0;

                for (Map.Entry<String, List<WorkOrder>> entry : groupedByContractor.entrySet()) {
                        String name = entry.getKey();
                        List<WorkOrder> wos = entry.getValue();
                        long count = wos.size();

                        BigDecimal sumCost = wos.stream()
                                        .map(WorkOrder::getContractorInvoiceTotal)
                                        .filter(java.util.Objects::nonNull)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // New Cycle: Due -> Sent to Client (Invoicing Speed)
                        long sumDays = wos.stream()
                                        .filter(w -> w.getDateDueClient() != null && w.getSentToClientDate() != null)
                                        .mapToLong(w -> Math.max(0,
                                                        java.time.temporal.ChronoUnit.DAYS.between(w.getDateDueClient(),
                                                                        w.getSentToClientDate())))
                                        .sum();
                        long validDatesCount = wos.stream()
                                        .filter(w -> w.getDateDueClient() != null && w.getSentToClientDate() != null)
                                        .count();

                        BigDecimal avgCost = count > 0
                                        ? sumCost.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;
                        double avgDays = validDatesCount > 0 ? (double) sumDays / validDatesCount : 0.0;

                        scorecards.add(new WorkOrderDashboardDTO.ContractorScorecard(name, count, avgCost, avgDays));

                        globalSumCost = globalSumCost.add(sumCost);
                        globalSumDays += sumDays;
                        globalCount += validDatesCount; // Use valid count for global avg
                }

                BigDecimal globalAvgCost = allWorkOrders.size() > 0
                                ? globalSumCost.divide(BigDecimal.valueOf(allWorkOrders.size()), 2,
                                                java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                double globalAvgDays = globalCount > 0 ? globalSumDays / globalCount : 0.0;

                scorecards.sort((a, b) -> Long.compare(b.getTotalWorkOrders(), a.getTotalWorkOrders()));
                stats.setContractorScorecards(scorecards);
                stats.setBenchmark(new WorkOrderDashboardDTO.ScorecardBenchmark(globalAvgCost, globalAvgDays));

                // Cycle Time Analysis (NEW)

                // Days Due -> Invoiced
                double avgDaysDueToInv = allWorkOrders.stream()
                                .filter(w -> w.getDateDueClient() != null && w.getSentToClientDate() != null)
                                .mapToLong(w -> java.time.temporal.ChronoUnit.DAYS.between(w.getDateDueClient(),
                                                w.getSentToClientDate()))
                                .average()
                                .orElse(0.0);

                // Days Invoiced -> Paid
                double avgDaysInvToPay = allWorkOrders.stream()
                                .filter(w -> w.getSentToClientDate() != null && w.getClientPaidDate() != null)
                                .mapToLong(w -> java.time.temporal.ChronoUnit.DAYS.between(w.getSentToClientDate(),
                                                w.getClientPaidDate()))
                                .average()
                                .orElse(0.0);

                // Lag by Work Type (Due -> Invoiced)
                Map<String, Double> invoicingLagByWorkType = allWorkOrders.stream()
                                .filter(w -> w.getWorkType() != null && w.getDateDueClient() != null
                                                && w.getSentToClientDate() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getWorkType))
                                .entrySet().stream()
                                .map(e -> new java.util.AbstractMap.SimpleEntry<>(e.getKey(),
                                                e.getValue().stream()
                                                                .mapToLong(w -> java.time.temporal.ChronoUnit.DAYS
                                                                                .between(w.getDateDueClient(),
                                                                                                w.getSentToClientDate()))
                                                                .average().orElse(0)))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                stats.setCycleTimeAnalysis(new WorkOrderDashboardDTO.CycleTimeAnalysis(avgDaysDueToInv, avgDaysInvToPay,
                                invoicingLagByWorkType));

                // Profitability Analysis (from filtered list)
                // By Client
                Map<String, BigDecimal> marginByClient = allWorkOrders.stream()
                                .filter(w -> w.getClient() != null)
                                .collect(Collectors.groupingBy(w -> w.getClient().getName()))
                                .entrySet().stream()
                                .map(e -> {
                                        BigDecimal rev = e.getValue().stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal cost = e.getValue().stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new java.util.AbstractMap.SimpleEntry<>(e.getKey(), rev.subtract(cost));
                                })
                                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                                .limit(10)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                                                java.util.LinkedHashMap::new));

                // By State
                Map<String, BigDecimal> marginByState = allWorkOrders.stream()
                                .filter(w -> w.getState() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getState))
                                .entrySet().stream()
                                .map(e -> {
                                        BigDecimal rev = e.getValue().stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal cost = e.getValue().stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new java.util.AbstractMap.SimpleEntry<>(e.getKey(), rev.subtract(cost));
                                })
                                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                                                java.util.LinkedHashMap::new));

                // By Bank (NEW)
                Map<String, BigDecimal> marginByBank = allWorkOrders.stream()
                                .filter(w -> w.getCustomerBank() != null && !w.getCustomerBank().isEmpty())
                                .collect(Collectors.groupingBy(WorkOrder::getCustomerBank))
                                .entrySet().stream()
                                .map(e -> {
                                        BigDecimal rev = e.getValue().stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal cost = e.getValue().stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(java.util.Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new java.util.AbstractMap.SimpleEntry<>(e.getKey(), rev.subtract(cost));
                                })
                                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                                .limit(10)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                                                java.util.LinkedHashMap::new));

                stats.setProfitabilityAnalysis(
                                new WorkOrderDashboardDTO.ProfitabilityAnalysis(marginByClient, marginByState,
                                                marginByBank));

                model.addAttribute("stats", stats);
                model.addAttribute("activeLink", "work-orders");
                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
                return "work-order/dashboard";
        }
}
