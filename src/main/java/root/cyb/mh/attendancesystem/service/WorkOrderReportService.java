package root.cyb.mh.attendancesystem.service;

import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.WorkOrder;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.ContractorStat;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.BankStat;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.WorkTypeStat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.LinkedHashMap;

@Service
public class WorkOrderReportService {

        public WorkOrderDashboardDTO calculateStatistics(List<WorkOrder> workOrders) {
                WorkOrderDashboardDTO stats = new WorkOrderDashboardDTO();

                // Base Counts
                stats.setTotalWorkOrders(workOrders.size());
                stats.setOpenWorkOrders(workOrders
                                .stream().filter(w -> !"Completed".equalsIgnoreCase(w.getStatus())
                                                && !"Closed".equalsIgnoreCase(w.getStatus())
                                                && !"Cancelled".equalsIgnoreCase(w.getStatus()))
                                .count());
                stats.setClosedWorkOrders(workOrders.stream()
                                .filter(w -> "Closed".equalsIgnoreCase(w.getStatus())
                                                || "Completed".equalsIgnoreCase(w.getStatus()))
                                .count());
                stats.setCancelledWorkOrders(
                                workOrders.stream().filter(w -> "Cancelled".equalsIgnoreCase(w.getStatus())).count());

                // Financials
                BigDecimal totalRevenue = workOrders.stream()
                                .map(WorkOrder::getClientInvoiceTotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCost = workOrders.stream()
                                .map(WorkOrder::getContractorInvoiceTotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                stats.setTotalRevenue(totalRevenue);
                stats.setTotalCost(totalCost);
                stats.setTotalMargin(totalRevenue.subtract(totalCost));

                // Realized vs Unrealized
                BigDecimal realizedRevenue = workOrders.stream()
                                .map(WorkOrder::getClientPaidAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setRealizedRevenue(realizedRevenue);
                stats.setUnrealizedRevenue(totalRevenue.subtract(realizedRevenue));

                stats.setRealizedCost(workOrders.stream()
                                .map(WorkOrder::getContractorPaidAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Discounts & Write-offs
                stats.setTotalClientDiscount(workOrders.stream()
                                .map(WorkOrder::getClientDiscountTotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                stats.setTotalWriteOffs(workOrders.stream()
                                .map(WorkOrder::getWriteOffAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Averages
                if (!workOrders.isEmpty()) {
                        stats.setAvgRevenue(
                                        totalRevenue.divide(BigDecimal.valueOf(workOrders.size()), 2,
                                                        java.math.RoundingMode.HALF_UP));
                        stats.setAvgCost(
                                        totalCost.divide(BigDecimal.valueOf(workOrders.size()), 2,
                                                        java.math.RoundingMode.HALF_UP));
                        stats.setAvgMargin(stats.getTotalMargin().divide(BigDecimal.valueOf(workOrders.size()), 2,
                                        java.math.RoundingMode.HALF_UP));
                } else {
                        stats.setAvgRevenue(BigDecimal.ZERO);
                        stats.setAvgCost(BigDecimal.ZERO);
                        stats.setAvgMargin(BigDecimal.ZERO);
                }

                // Global Gross Margin %
                if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                        stats.setGlobalGrossMarginPercent(stats.getTotalMargin()
                                        .divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100)));
                } else {
                        stats.setGlobalGrossMarginPercent(BigDecimal.ZERO);
                }

                // --- Aggregations ---

                // Status Distribution
                Map<String, Long> statusDist = workOrders.stream()
                                .collect(Collectors.groupingBy(
                                                w -> w.getStatus() != null ? w.getStatus() : "Unassigned",
                                                Collectors.counting()));
                stats.setStatusDistribution(statusDist);

                // Top Contractors (Volume)
                List<ContractorStat> topContractors = workOrders.stream()
                                .filter(w -> w.getOriginalContractorString() != null || w.getContractor() != null)
                                .collect(Collectors.groupingBy(
                                                w -> w.getContractor() != null ? w.getContractor().getName()
                                                                : w.getOriginalContractorString(),
                                                Collectors.counting()))
                                .entrySet().stream()
                                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                .limit(5)
                                .map(e -> new ContractorStat(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
                stats.setTopContractors(topContractors);

                // Top Banks (Revenue)
                List<BankStat> topBanks = workOrders.stream()
                                .filter(w -> w.getCustomerBank() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getCustomerBank))
                                .entrySet().stream()
                                .map(entry -> {
                                        String bankName = entry.getKey();
                                        List<WorkOrder> bankOrders = entry.getValue();
                                        BigDecimal rev = bankOrders.stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new BankStat(bankName, (long) bankOrders.size(), rev);
                                })
                                .sorted(Comparator.comparing(BankStat::getRevenue).reversed())
                                .limit(5)
                                .collect(Collectors.toList());
                stats.setTopBanks(topBanks);

                // Margin By Work Type
                List<WorkTypeStat> workTypeMargins = workOrders.stream()
                                .filter(w -> w.getWorkType() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getWorkType))
                                .entrySet().stream()
                                .map(entry -> {
                                        String type = entry.getKey();
                                        BigDecimal margin = entry.getValue().stream()
                                                        .map(w -> {
                                                                BigDecimal client = w.getClientInvoiceTotal() != null
                                                                                ? w.getClientInvoiceTotal()
                                                                                : BigDecimal.ZERO;
                                                                BigDecimal cont = w.getContractorInvoiceTotal() != null
                                                                                ? w.getContractorInvoiceTotal()
                                                                                : BigDecimal.ZERO;
                                                                return client.subtract(cont);
                                                        })
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new WorkTypeStat(type, margin);
                                })
                                .sorted(Comparator.comparing(WorkTypeStat::getTotalMargin).reversed())
                                .limit(10)
                                .collect(Collectors.toList());
                stats.setWorkTypeMargins(workTypeMargins);

                // --- Series (LLC) Analysis ---
                Map<String, List<WorkOrder>> seriesGroups = workOrders.stream()
                                .collect(Collectors.groupingBy(w -> {
                                        // Determine Series from Client Code (e.g. "C105" -> "Series 100")
                                        String code = "0";
                                        if (w.getClient() != null && w.getClient().getCode() != null) {
                                                code = w.getClient().getCode().replaceAll("[^0-9]", ""); // Extract
                                                                                                         // digits
                                        } else if (w.getOriginalClientString() != null) {
                                                // Try to parse from string if entity link missing?
                                                // Or just fallback to 'Unknown'
                                                // Let's rely on Client Entity Code primarily.
                                        }

                                        if (code.isEmpty())
                                                return "Unknown Series";

                                        try {
                                                int clientNum = Integer.parseInt(code);
                                                int seriesBase = (clientNum / 100) * 100;
                                                return "Series " + seriesBase;
                                        } catch (NumberFormatException e) {
                                                return "Unknown Series";
                                        }
                                }));

                List<root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat> seriesStats = seriesGroups
                                .entrySet().stream()
                                .map(entry -> {
                                        String seriesName = entry.getKey();
                                        List<WorkOrder> orders = entry.getValue();

                                        BigDecimal clientTotal = orders.stream()
                                                        .map(WorkOrder::getClientInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal contractorTotal = orders.stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal profit = clientTotal.subtract(contractorTotal);

                                        BigDecimal margin = BigDecimal.ZERO;
                                        if (clientTotal.compareTo(BigDecimal.ZERO) > 0) {
                                                margin = profit.divide(clientTotal, 4, java.math.RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100)); // Percentage
                                        }

                                        return new root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat(
                                                        seriesName, clientTotal, contractorTotal, profit, margin);
                                })
                                // Sort by Series Name (Series 100, Series 200...)
                                .sorted(Comparator.comparing(
                                                root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat::getSeriesName))
                                .collect(Collectors.toList());

                stats.setSeriesStats(seriesStats);

                // Grand Total Series
                BigDecimal grandClient = seriesStats.stream().map(
                                root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat::getClientInvoiceTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandContractor = seriesStats.stream().map(
                                root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat::getContractorInvoiceTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandProfit = grandClient.subtract(grandContractor);
                BigDecimal grandMargin = BigDecimal.ZERO;
                if (grandClient.compareTo(BigDecimal.ZERO) > 0) {
                        grandMargin = grandProfit.divide(grandClient, 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                }
                stats.setGrandTotalSeries(new root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat(
                                "Grand Total", grandClient, grandContractor, grandProfit, grandMargin));

                return stats;
        }
}
