package root.cyb.mh.attendancesystem.service;

import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.WorkOrder;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.ContractorStat;

import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.WorkTypeStat;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.SeriesStat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.ArrayList;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO.StateStat;

@Service
public class WorkOrderReportService {

        // Helper for Effective Revenue (Net Amount)
        private BigDecimal getEffectiveRevenue(WorkOrder w) {
                if (w.getClientDiscountTotal() != null && w.getClientDiscountTotal().compareTo(BigDecimal.ZERO) > 0) {
                        return w.getClientDiscountTotal();
                }
                return w.getClientInvoiceTotal() != null ? w.getClientInvoiceTotal() : BigDecimal.ZERO;
        }

        public WorkOrderDashboardDTO calculateStatistics(List<WorkOrder> workOrders) {
                WorkOrderDashboardDTO stats = new WorkOrderDashboardDTO();

                // Base Counts
                System.out.println("DEBUG: calculateStatistics called with " + workOrders.size() + " orders.");
                long validDates = workOrders.stream().filter(w -> w.getInvoiceDate() != null).count();
                System.out.println("DEBUG: Orders with valid InvoiceDate: " + validDates);

                stats.setTotalWorkOrders(workOrders.size());
                stats.setOpenWorkOrders(workOrders
                                .stream().filter(w -> !"Completed".equalsIgnoreCase(w.getStatus())
                                                && !"Closed".equalsIgnoreCase(w.getStatus())
                                                && !"Cancelled".equalsIgnoreCase(w.getStatus())
                                                && !"Invoiced".equalsIgnoreCase(w.getStatus()))
                                .count());
                stats.setClosedWorkOrders(workOrders.stream()
                                .filter(w -> "Closed".equalsIgnoreCase(w.getStatus())
                                                || "Completed".equalsIgnoreCase(w.getStatus()))
                                .count());
                stats.setCancelledWorkOrders(
                                workOrders.stream().filter(w -> "Cancelled".equalsIgnoreCase(w.getStatus())).count());
                stats.setInvoicedWorkOrders(
                                workOrders.stream().filter(w -> "Invoiced".equalsIgnoreCase(w.getStatus())).count());

                // Financials
                BigDecimal totalRevenue = workOrders.stream()
                                .map(this::getEffectiveRevenue)
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

                // --- Client Code Analysis (Volume & Revenue) ---
                List<WorkOrderDashboardDTO.ClientStat> allClientStats = workOrders.stream()
                                .filter(w -> {
                                        boolean hasClient = w.getClient() != null;
                                        boolean hasLink = w.getOriginalClientString() != null;
                                        return hasClient || hasLink; // Include everything we can identify
                                })
                                .collect(Collectors.groupingBy(w -> {
                                        if (w.getClient() != null && w.getClient().getCode() != null) {
                                                return w.getClient().getCode(); // e.g. "C100"
                                        }
                                        if (w.getOriginalClientString() != null) {
                                                return w.getOriginalClientString(); // Fallback to raw string
                                        }
                                        return "Unknown";
                                }))
                                .entrySet().stream()
                                .map(entry -> {
                                        String code = entry.getKey();
                                        List<WorkOrder> list = entry.getValue();

                                        // Determine display name
                                        String name = list.stream()
                                                        .filter(w -> w.getClient() != null)
                                                        .findFirst()
                                                        .map(w -> w.getClient().getName())
                                                        .orElse(code); // Fallback to Code if Name not found

                                        BigDecimal rev = list.stream()
                                                        .map(this::getEffectiveRevenue)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        return new WorkOrderDashboardDTO.ClientStat(code, name, list.size(), rev);
                                })
                                .collect(Collectors.toList());

                // Top Clients by Volume
                stats.setTopClientsByVolume(allClientStats.stream()
                                .sorted(Comparator.comparingLong(WorkOrderDashboardDTO.ClientStat::getCount).reversed())
                                .limit(10)
                                .collect(Collectors.toList()));

                // Top Clients by Revenue
                stats.setTopClientsByRevenue(allClientStats.stream()
                                .sorted(Comparator.comparing(WorkOrderDashboardDTO.ClientStat::getTotalRevenue)
                                                .reversed())
                                .limit(10)
                                .collect(Collectors.toList()));

                // Margin By Work Type
                List<WorkTypeStat> workTypeMargins = workOrders.stream()
                                .filter(w -> w.getWorkType() != null)
                                .collect(Collectors.groupingBy(WorkOrder::getWorkType))
                                .entrySet().stream()
                                .map(entry -> {
                                        String type = entry.getKey();
                                        BigDecimal margin = entry.getValue().stream()
                                                        .map(w -> {
                                                                BigDecimal client = getEffectiveRevenue(w);
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

                List<SeriesStat> seriesStats = seriesGroups
                                .entrySet().stream()
                                .map(entry -> {
                                        String seriesName = entry.getKey();
                                        List<WorkOrder> orders = entry.getValue();

                                        BigDecimal clientTotal = orders.stream()
                                                        .map(this::getEffectiveRevenue)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal contractorTotal = orders.stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal totalContractorPaid = orders.stream()
                                                        .map(WorkOrder::getContractorPaidAmount)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal totalClientPaid = orders.stream()
                                                        .map(WorkOrder::getClientPaidAmount)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal totalWriteOffs = orders.stream()
                                                        .map(WorkOrder::getWriteOffAmount)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal totalClientDiscount = orders.stream()
                                                        .map(o -> {
                                                                if (o.getClientInvoiceTotal() != null && o
                                                                                .getClientDiscountPercent() != null) {
                                                                        return o.getClientInvoiceTotal().multiply(
                                                                                        o.getClientDiscountPercent())
                                                                                        .divide(BigDecimal.valueOf(100),
                                                                                                        2,
                                                                                                        java.math.RoundingMode.HALF_UP);
                                                                }
                                                                return BigDecimal.ZERO;
                                                        })
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal profit = clientTotal.subtract(contractorTotal);

                                        BigDecimal margin = BigDecimal.ZERO;
                                        if (clientTotal.compareTo(BigDecimal.ZERO) > 0) {
                                                margin = profit.divide(clientTotal, 4, java.math.RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100)); // Percentage
                                        }

                                        // Operational Metrics
                                        long openCount = orders.stream()
                                                        .filter(o -> !"Completed".equalsIgnoreCase(o.getStatus())
                                                                        && !"Closed".equalsIgnoreCase(o.getStatus())
                                                                        && !"Cancelled".equalsIgnoreCase(o.getStatus())
                                                                        && !"Invoiced".equalsIgnoreCase(o.getStatus()))
                                                        .count();

                                        long closedCount = orders.stream()
                                                        .filter(o -> "Closed".equalsIgnoreCase(o.getStatus()))
                                                        .count();

                                        long invoicedCount = orders.stream()
                                                        .filter(o -> "Invoiced".equalsIgnoreCase(o.getStatus()))
                                                        .count();

                                        BigDecimal avgRev = BigDecimal.ZERO;
                                        BigDecimal avgCost = BigDecimal.ZERO;
                                        BigDecimal avgMar = BigDecimal.ZERO;

                                        int totalCount = orders.size();
                                        if (totalCount > 0) {
                                                avgRev = clientTotal.divide(BigDecimal.valueOf(totalCount), 2,
                                                                java.math.RoundingMode.HALF_UP);
                                                avgCost = contractorTotal.divide(BigDecimal.valueOf(totalCount), 2,
                                                                java.math.RoundingMode.HALF_UP);
                                                avgMar = profit.divide(BigDecimal.valueOf(totalCount), 2,
                                                                java.math.RoundingMode.HALF_UP);
                                        }

                                        return new SeriesStat(
                                                        seriesName, clientTotal, contractorTotal, profit, margin,
                                                        openCount, closedCount, invoicedCount, avgCost, avgRev, avgMar,
                                                        totalContractorPaid, totalClientPaid, totalWriteOffs,
                                                        totalClientDiscount);
                                })
                                // Sort by Series Name (Series 100, Series 200...)
                                .sorted(Comparator.comparing(
                                                SeriesStat::getSeriesName))
                                .collect(Collectors.toList());

                stats.setSeriesStats(seriesStats);

                // Grand Total Series
                BigDecimal grandClient = seriesStats.stream().map(
                                SeriesStat::getClientInvoiceTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandContractor = seriesStats.stream().map(
                                SeriesStat::getContractorInvoiceTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandProfit = grandClient.subtract(grandContractor);
                BigDecimal grandMargin = BigDecimal.ZERO;
                if (grandClient.compareTo(BigDecimal.ZERO) > 0) {
                        grandMargin = grandProfit.divide(grandClient, 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                }

                // Grand Total Operational Metrics
                long grandOpen = seriesStats.stream().mapToLong(
                                SeriesStat::getOpenWorkOrders)
                                .sum();
                long grandClosed = seriesStats.stream().mapToLong(
                                SeriesStat::getClosedWorkOrders)
                                .sum();
                long grandInvoiced = seriesStats.stream().mapToLong(
                                SeriesStat::getInvoicedWorkOrders)
                                .sum();

                // Grand Total Financial Aggregates
                BigDecimal grandContractorPaid = seriesStats.stream().map(
                                SeriesStat::getTotalContractorPaid)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandClientPaid = seriesStats.stream().map(
                                SeriesStat::getTotalClientPaid)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandWriteOffs = seriesStats.stream().map(
                                SeriesStat::getTotalWriteOffs)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal grandClientDiscount = seriesStats.stream().map(
                                SeriesStat::getTotalClientDiscount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalSeriesCount = workOrders.size();

                BigDecimal grandAvgCost = BigDecimal.ZERO;
                BigDecimal grandAvgRev = BigDecimal.ZERO;
                BigDecimal grandAvgMar = BigDecimal.ZERO;

                if (totalSeriesCount > 0) {
                        grandAvgCost = grandContractor.divide(BigDecimal.valueOf(totalSeriesCount), 2,
                                        java.math.RoundingMode.HALF_UP);
                        grandAvgRev = grandClient.divide(BigDecimal.valueOf(totalSeriesCount), 2,
                                        java.math.RoundingMode.HALF_UP);
                        grandAvgMar = grandProfit.divide(BigDecimal.valueOf(totalSeriesCount), 2,
                                        java.math.RoundingMode.HALF_UP);
                }

                stats.setGrandTotalSeries(new SeriesStat(
                                "Grand Total", grandClient, grandContractor, grandProfit, grandMargin,
                                grandOpen, grandClosed, grandInvoiced, grandAvgCost, grandAvgRev, grandAvgMar,
                                grandContractorPaid, grandClientPaid, grandWriteOffs, grandClientDiscount));

                // 2. Monthly Comparison
                DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
                DateTimeFormatter sortFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

                List<WorkOrderDashboardDTO.MonthlyStat> monthlyStats = workOrders.stream()
                                // Removed .filter(w -> w.getInvoiceDate() != null) to debug
                                .collect(Collectors.groupingBy(w -> {
                                        if (w.getInvoiceDate() == null)
                                                return "Unknown";
                                        return w.getInvoiceDate().format(sortFormatter);
                                }))
                                .entrySet().stream()
                                .map(entry -> {
                                        String yearMonth = entry.getKey();
                                        List<WorkOrder> monthOrders = entry.getValue();

                                        // Determine display name from first order or parse yearMonth
                                        // Determine display name
                                        String displayMonth = "Unknown";
                                        if (!"Unknown".equals(yearMonth) && !monthOrders.isEmpty()
                                                        && monthOrders.get(0).getInvoiceDate() != null) {
                                                displayMonth = monthOrders.get(0).getInvoiceDate()
                                                                .format(monthFormatter);
                                        }

                                        BigDecimal mRevenue = monthOrders.stream()
                                                        .map(this::getEffectiveRevenue)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal mCost = monthOrders.stream()
                                                        .map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal mProfit = mRevenue.subtract(mCost);
                                        BigDecimal mMargin = BigDecimal.ZERO;
                                        if (mRevenue.compareTo(BigDecimal.ZERO) > 0) {
                                                mMargin = mProfit.divide(mRevenue, 4, java.math.RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100));
                                        }

                                        return new WorkOrderDashboardDTO.MonthlyStat(
                                                        displayMonth, yearMonth, monthOrders.size(), mRevenue, mCost,
                                                        mProfit, mMargin);
                                })
                                .sorted(Comparator.comparing(WorkOrderDashboardDTO.MonthlyStat::getYearMonth)
                                                .reversed()) // Newest first
                                .collect(Collectors.toList());

                stats.setMonthlyStats(monthlyStats);

                // 2b. Monthly Performance by Series
                // Multi-dimensional grouping: Month × Series
                List<WorkOrderDashboardDTO.MonthlySeriesStat> monthlySeriesPerf = workOrders.stream()
                                .collect(Collectors.groupingBy(w -> {
                                        if (w.getInvoiceDate() == null)
                                                return "Unknown";
                                        return w.getInvoiceDate().format(sortFormatter);
                                })) // Group
                                    // by
                                    // month
                                .entrySet().stream()
                                .flatMap(monthEntry -> {
                                        String yearMonth = monthEntry.getKey();
                                        String tempDisplayMonth = "Unknown";
                                        if (!"Unknown".equals(yearMonth) && !monthEntry.getValue().isEmpty()
                                                        && monthEntry.getValue().get(0).getInvoiceDate() != null) {
                                                tempDisplayMonth = monthEntry.getValue().get(0).getInvoiceDate()
                                                                .format(monthFormatter);
                                        }
                                        final String displayMonth = tempDisplayMonth;

                                        // Within each month, group by Series
                                        return monthEntry.getValue().stream()
                                                        .collect(Collectors.groupingBy(w -> {
                                                                // Determine Series from Client Code
                                                                String code = "0";
                                                                if (w.getClient() != null
                                                                                && w.getClient().getCode() != null) {
                                                                        code = w.getClient().getCode()
                                                                                        .replaceAll("[^0-9]", "");
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
                                                        }))
                                                        .entrySet().stream()
                                                        .map(seriesEntry -> {
                                                                String seriesName = seriesEntry.getKey();
                                                                List<WorkOrder> seriesOrders = seriesEntry.getValue();

                                                                BigDecimal rev = seriesOrders.stream()
                                                                                .map(this::getEffectiveRevenue)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                BigDecimal cost = seriesOrders.stream()
                                                                                .map(WorkOrder::getContractorInvoiceTotal)
                                                                                .filter(Objects::nonNull)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                BigDecimal profit = rev.subtract(cost);
                                                                BigDecimal margin = BigDecimal.ZERO;
                                                                if (rev.compareTo(BigDecimal.ZERO) > 0) {
                                                                        margin = profit.divide(rev, 4,
                                                                                        java.math.RoundingMode.HALF_UP)
                                                                                        .multiply(BigDecimal
                                                                                                        .valueOf(100));
                                                                }

                                                                return new WorkOrderDashboardDTO.MonthlySeriesStat(
                                                                                displayMonth, yearMonth, seriesName,
                                                                                seriesOrders.size(), rev, cost, profit,
                                                                                margin);
                                                        });
                                })
                                .sorted(Comparator.comparing(WorkOrderDashboardDTO.MonthlySeriesStat::getYearMonth)
                                                .reversed()
                                                .thenComparing(WorkOrderDashboardDTO.MonthlySeriesStat::getSeriesName))
                                .collect(Collectors.toList());

                stats.setMonthlySeriesPerformance(monthlySeriesPerf);

                // 3. Geographic Analysis
                // Helper to map groupings to StateStat
                // 3. Geographic Analysis
                // Helper to map groupings to StateStat
                List<StateStat> allStateStats = workOrders.stream()
                                .filter(w -> w.getState() != null && !w.getState().trim().isEmpty())
                                .collect(Collectors.groupingBy(w -> w.getState().trim().toUpperCase()))
                                .entrySet().stream()
                                .map(entry -> {
                                        String state = entry.getKey();
                                        List<WorkOrder> list = entry.getValue();

                                        BigDecimal rev = list.stream().map(this::getEffectiveRevenue)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal cost = list.stream().map(WorkOrder::getContractorInvoiceTotal)
                                                        .filter(Objects::nonNull)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        BigDecimal profit = rev.subtract(cost);
                                        BigDecimal margin = BigDecimal.ZERO;
                                        if (rev.compareTo(BigDecimal.ZERO) > 0) {
                                                margin = profit.divide(rev, 4, java.math.RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100));
                                        }

                                        return new StateStat(state, list.size(), rev, cost, profit, margin);
                                })
                                .collect(Collectors.toList());

                stats.setTopStatesByVolume(allStateStats.stream()
                                .sorted(Comparator.comparingLong(StateStat::getCount).reversed())
                                .limit(10)
                                .collect(Collectors.toList()));

                stats.setTopStatesByRevenue(allStateStats.stream()
                                .sorted(Comparator.comparing(StateStat::getRevenue).reversed())
                                .limit(10)
                                .collect(Collectors.toList()));

                // State Efficiency Snapshot
                // 1. High Margin: >= 50%
                stats.setHighMarginStates(allStateStats.stream()
                                .filter(s -> s.getMargin().compareTo(BigDecimal.valueOf(50)) >= 0)
                                .sorted(Comparator.comparing(StateStat::getMargin).reversed())
                                .limit(5)
                                .collect(Collectors.toList()));

                // 2. Moderate Margin / High Volume: 30% <= Margin < 50%
                // Prioritize Volume (Count) for this bucket as requested
                stats.setModerateMarginHighVolumeStates(allStateStats.stream()
                                .filter(s -> s.getMargin().compareTo(BigDecimal.valueOf(30)) >= 0
                                                && s.getMargin().compareTo(BigDecimal.valueOf(50)) < 0)
                                .sorted(Comparator.comparingLong(StateStat::getCount).reversed())
                                .limit(5)
                                .collect(Collectors.toList()));

                // 3. Low / Risk: Margin < 30%
                stats.setLowRiskStates(allStateStats.stream()
                                .filter(s -> s.getMargin().compareTo(BigDecimal.valueOf(30)) < 0)
                                .sorted(Comparator.comparing(StateStat::getMargin)) // Lowest margin first
                                .limit(5)
                                .collect(Collectors.toList()));

                // 4. Geographic Analysis by Series
                // State × Series breakdown
                List<WorkOrderDashboardDTO.GeographicSeriesStat> stateSeriesStats = workOrders.stream()
                                .filter(w -> w.getState() != null && !w.getState().trim().isEmpty()
                                                && w.getClient() != null)
                                .collect(Collectors.groupingBy(w -> w.getState().trim().toUpperCase()))
                                .entrySet().stream()
                                .flatMap(stateEntry -> {
                                        String state = stateEntry.getKey();

                                        // Within each state, group by Series
                                        return stateEntry.getValue().stream()
                                                        .collect(Collectors.groupingBy(w -> {
                                                                // Extract Series from Client Code
                                                                String code = "0";
                                                                if (w.getClient() != null
                                                                                && w.getClient().getCode() != null) {
                                                                        code = w.getClient().getCode()
                                                                                        .replaceAll("[^0-9]", "");
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
                                                        }))
                                                        .entrySet().stream()
                                                        .map(seriesEntry -> {
                                                                String seriesName = seriesEntry.getKey();
                                                                List<WorkOrder> orders = seriesEntry.getValue();

                                                                BigDecimal rev = orders.stream()
                                                                                .map(this::getEffectiveRevenue)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                BigDecimal cost = orders.stream()
                                                                                .map(WorkOrder::getContractorInvoiceTotal)
                                                                                .filter(Objects::nonNull)
                                                                                .reduce(BigDecimal.ZERO,
                                                                                                BigDecimal::add);

                                                                BigDecimal profit = rev.subtract(cost);
                                                                BigDecimal margin = BigDecimal.ZERO;
                                                                if (rev.compareTo(BigDecimal.ZERO) > 0) {
                                                                        margin = profit.divide(rev, 4,
                                                                                        java.math.RoundingMode.HALF_UP)
                                                                                        .multiply(BigDecimal
                                                                                                        .valueOf(100));
                                                                }

                                                                return new WorkOrderDashboardDTO.GeographicSeriesStat(
                                                                                state, seriesName, orders.size(), rev,
                                                                                cost, profit, margin);
                                                        });
                                })
                                .sorted(Comparator.comparing(WorkOrderDashboardDTO.GeographicSeriesStat::getLocation)
                                                .thenComparing(WorkOrderDashboardDTO.GeographicSeriesStat::getRevenue)
                                                .reversed())
                                .collect(Collectors.toList());

                stats.setStateSeriesBreakdown(stateSeriesStats);

                return stats;
        }
}
