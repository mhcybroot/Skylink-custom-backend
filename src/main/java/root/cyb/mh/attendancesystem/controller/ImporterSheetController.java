package root.cyb.mh.attendancesystem.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.dto.ImporterWorkOrderBulkRequest;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ImporterWorkOrder;
import root.cyb.mh.attendancesystem.model.ImporterWorkOrderHistory;
import root.cyb.mh.attendancesystem.model.DeletedImporterWorkOrder;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;
import root.cyb.mh.attendancesystem.repository.ImporterWorkOrderRepository;
import root.cyb.mh.attendancesystem.repository.ImporterWorkOrderHistoryRepository;
import root.cyb.mh.attendancesystem.repository.DeletedImporterWorkOrderRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/processing-sheet/importer")
public class ImporterSheetController {

    private final ImporterWorkOrderRepository importerWorkOrderRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeWorkOrderRepository employeeWorkOrderRepository;
    private final ImporterWorkOrderHistoryRepository historyRepository;
    private final DeletedImporterWorkOrderRepository deletedRepository;

    @Autowired
    public ImporterSheetController(ImporterWorkOrderRepository importerWorkOrderRepository,
                                  EmployeeRepository employeeRepository,
                                  EmployeeWorkOrderRepository employeeWorkOrderRepository,
                                  ImporterWorkOrderHistoryRepository historyRepository,
                                  DeletedImporterWorkOrderRepository deletedRepository) {
        this.importerWorkOrderRepository = importerWorkOrderRepository;
        this.employeeRepository = employeeRepository;
        this.employeeWorkOrderRepository = employeeWorkOrderRepository;
        this.historyRepository = historyRepository;
        this.deletedRepository = deletedRepository;
    }

    private UserContext resolveUser(Principal principal) {
        if (principal == null) {
            return new UserContext("system", "System User");
        }
        String principalName = principal.getName();
        Optional<Employee> empOpt = employeeRepository.findByUsername(principalName);
        if (empOpt.isEmpty()) {
            empOpt = employeeRepository.findById(principalName);
        }
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            return new UserContext(emp.getId(), emp.getName());
        }
        return new UserContext(principalName, principalName);
    }

    private static class UserContext {
        final String employeeId;
        final String name;

        UserContext(String employeeId, String name) {
            this.employeeId = employeeId;
            this.name = name;
        }
    }

    public static String resolveClientSeries(String clientCode) {
        if (clientCode == null || clientCode.trim().isEmpty()) {
            return "Unassigned Series";
        }
        String cleaned = clientCode.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\D*(\\d+)").matcher(cleaned);
        if (m.find()) {
            try {
                int num = Integer.parseInt(m.group(1));
                int seriesStart = (num / 100) * 100;
                int seriesEnd = seriesStart + 99;
                return seriesStart + " - " + seriesEnd + " Series";
            } catch (NumberFormatException ignored) {
            }
        }
        return "Special / Other Series";
    }

    @GetMapping
    public String importerDashboard(@RequestParam(required = false) String date,
                                    Model model,
                                    Principal principal) {
        LocalDate entryDate = (date != null && !date.trim().isEmpty())
                ? LocalDate.parse(date.trim())
                : LocalDate.now();
        model.addAttribute("currentDate", entryDate);

        UserContext user = resolveUser(principal);
        model.addAttribute("importerName", user.name);
        model.addAttribute("importerId", user.employeeId);

        List<ImporterWorkOrder> wos = importerWorkOrderRepository
                .findByEntryDateAndImporterEmployeeIdOrderByCreatedAtDesc(entryDate, user.employeeId);
        model.addAttribute("workOrders", wos);

        long totalWo = wos.size();
        BigDecimal totalValue = wos.stream()
                .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalWo", totalWo);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("activeLink", "importer-sheet");

        Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        types.addAll(List.of(
            "Allow Access", "BATF Allowables", "Bid Approval", "Bid Request", "Client Discount/Platform fee",
            "Corrective Action", "EOB Request", "Final Secure", "FTV Photo Documentation", "Grass Cut",
            "Grass Re-cut", "Initial Grass Cut", "Initial Secure", "Initial Services", "Inspection",
            "Maid Services", "Miscellaneous", "Occupancy Status", "Photo Request", "Pre Approved",
            "Pressure Test", "QC Photos", "Re-Securing", "Securing"
        ));
        try {
            List<String> dbTypes = employeeWorkOrderRepository.findDistinctWorkTypes();
            if (dbTypes != null) {
                dbTypes.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).forEach(types::add);
            }
        } catch (Exception ignored) {
        }
        model.addAttribute("workTypes", new ArrayList<>(types));

        return "admin-importer";
    }

    @PostMapping(value = "/api/bulk-add", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkAddWorkOrders(@RequestBody ImporterWorkOrderBulkRequest request,
                                                                 Principal principal) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No work orders provided."));
        }

        UserContext user = resolveUser(principal);

        LocalDate targetDate = LocalDate.now();
        if (request.getEntryDate() != null && !request.getEntryDate().trim().isEmpty()) {
            try {
                targetDate = LocalDate.parse(request.getEntryDate().trim());
            } catch (Exception ignored) {
            }
        }

        List<ImporterWorkOrder> toSave = new ArrayList<>();
        for (ImporterWorkOrderBulkRequest.Item item : request.getItems()) {
            if (item.getWoNumber() == null || item.getWoNumber().trim().isEmpty()) {
                continue;
            }

            ImporterWorkOrder wo = ImporterWorkOrder.builder()
                    .woNumber(item.getWoNumber().trim())
                    .clientCode(item.getClientCode() != null ? item.getClientCode().trim() : null)
                    .workType(item.getWorkType() != null ? item.getWorkType().trim() : null)
                    .value(item.getValue())
                    .importerName(user.name)
                    .importerEmployeeId(user.employeeId)
                    .entryDate(targetDate)
                    .build();

            toSave.add(wo);
        }

        if (toSave.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "All rows were empty or invalid."));
        }

        List<ImporterWorkOrder> savedList = importerWorkOrderRepository.saveAll(toSave);

        // Record Lifecycle History for creation
        List<ImporterWorkOrderHistory> historyLogs = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (ImporterWorkOrder item : savedList) {
            String summary = "WO: " + item.getWoNumber()
                    + (item.getClientCode() != null ? ", Client: " + item.getClientCode() : "")
                    + (item.getWorkType() != null ? ", Type: " + item.getWorkType() : "")
                    + (item.getValue() != null ? ", Value: $" + item.getValue() : "");

            historyLogs.add(ImporterWorkOrderHistory.builder()
                    .importerWorkOrderId(item.getId())
                    .woNumber(item.getWoNumber())
                    .actionType("CREATED")
                    .newValue(summary)
                    .changedByName(user.name)
                    .changedByEmployeeId(user.employeeId)
                    .changedAt(now)
                    .build());
        }
        historyRepository.saveAll(historyLogs);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", savedList.size(),
                "message", savedList.size() + " Work Orders added successfully!"
        ));
    }

    @PostMapping(value = "/api/update-wo", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateWorkOrder(@RequestBody Map<String, Object> payload,
                                                               Principal principal) {
        if (!payload.containsKey("id") || !payload.containsKey("field")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing required parameters"));
        }

        Long id = Long.valueOf(payload.get("id").toString());
        String field = (String) payload.get("field");
        Object rawVal = payload.get("value");
        String value = rawVal != null ? rawVal.toString().trim() : "";

        Optional<ImporterWorkOrder> woOpt = importerWorkOrderRepository.findById(id);
        if (woOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Work Order not found"));
        }

        ImporterWorkOrder wo = woOpt.get();
        UserContext user = resolveUser(principal);

        // Ownership check
        if (!wo.getImporterEmployeeId().equals(user.employeeId)) {
            // Check if admin
            boolean isAdmin = principal != null && (principal.getName().equalsIgnoreCase("admin"));
            if (!isAdmin) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Unauthorized"));
            }
        }

        String oldValue = "";
        String newValue = value;

        switch (field) {
            case "woNumber":
                if (value.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "WO # cannot be empty"));
                }
                oldValue = wo.getWoNumber();
                wo.setWoNumber(value);
                break;
            case "clientCode":
                oldValue = wo.getClientCode() != null ? wo.getClientCode() : "";
                wo.setClientCode(value);
                break;
            case "workType":
                oldValue = wo.getWorkType() != null ? wo.getWorkType() : "";
                wo.setWorkType(value);
                break;
            case "value":
                oldValue = wo.getValue() != null ? "$" + wo.getValue().toString() : "";
                try {
                    String clean = value.replace("$", "").replace(",", "").trim();
                    BigDecimal numVal = clean.isEmpty() ? null : new BigDecimal(clean);
                    wo.setValue(numVal);
                    newValue = numVal != null ? "$" + numVal.toString() : "";
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid value amount"));
                }
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unsupported field: " + field));
        }

        importerWorkOrderRepository.save(wo);

        // Record Update in Lifecycle History
        if (!Objects.equals(oldValue, newValue)) {
            historyRepository.save(ImporterWorkOrderHistory.builder()
                    .importerWorkOrderId(wo.getId())
                    .woNumber(wo.getWoNumber())
                    .actionType("UPDATED_" + field.toUpperCase())
                    .fieldName(field)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .changedByName(user.name)
                    .changedByEmployeeId(user.employeeId)
                    .changedAt(java.time.LocalDateTime.now())
                    .build());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Updated successfully"));
    }

    @PostMapping(value = "/api/delete-wo", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteWorkOrder(@RequestBody Map<String, Object> payload,
                                                               Principal principal) {
        if (!payload.containsKey("id")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing ID"));
        }

        Long id = Long.valueOf(payload.get("id").toString());
        Optional<ImporterWorkOrder> woOpt = importerWorkOrderRepository.findById(id);
        if (woOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Work Order not found"));
        }

        ImporterWorkOrder wo = woOpt.get();
        UserContext user = resolveUser(principal);

        if (!wo.getImporterEmployeeId().equals(user.employeeId)) {
            boolean isAdmin = principal != null && (principal.getName().equalsIgnoreCase("admin"));
            if (!isAdmin) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Unauthorized"));
            }
        }

        // Archive into DeletedImporterWorkOrder
        DeletedImporterWorkOrder deleted = DeletedImporterWorkOrder.builder()
                .originalId(wo.getId())
                .woNumber(wo.getWoNumber())
                .clientCode(wo.getClientCode())
                .workType(wo.getWorkType())
                .value(wo.getValue())
                .importerName(wo.getImporterName())
                .importerEmployeeId(wo.getImporterEmployeeId())
                .entryDate(wo.getEntryDate())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .deletedAt(java.time.LocalDateTime.now())
                .deletedByName(user.name)
                .deletedByEmployeeId(user.employeeId)
                .build();
        deletedRepository.save(deleted);

        // Record Lifecycle History for deletion
        historyRepository.save(ImporterWorkOrderHistory.builder()
                .importerWorkOrderId(wo.getId())
                .woNumber(wo.getWoNumber())
                .actionType("DELETED")
                .oldValue("WO: " + wo.getWoNumber() + (wo.getValue() != null ? ", Value: $" + wo.getValue() : ""))
                .changedByName(user.name)
                .changedByEmployeeId(user.employeeId)
                .changedAt(java.time.LocalDateTime.now())
                .build());

        importerWorkOrderRepository.delete(wo);
        return ResponseEntity.ok(Map.of("success", true, "message", "Deleted successfully"));
    }

    @GetMapping(value = "/api/lifeline", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ImporterWorkOrderHistory>> getLifeline(@RequestParam String woNumber) {
        List<ImporterWorkOrderHistory> history = historyRepository.findByWoNumberOrderByChangedAtDesc(woNumber);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/delete-history")
    public String deleteHistoryDashboard(Model model, Principal principal) {
        UserContext user = resolveUser(principal);
        model.addAttribute("importerName", user.name);
        model.addAttribute("importerId", user.employeeId);
        model.addAttribute("activeLink", "importer-sheet");

        boolean isAdmin = principal != null && ("admin".equalsIgnoreCase(principal.getName()));
        List<DeletedImporterWorkOrder> deletedOrders;
        if (isAdmin) {
            deletedOrders = deletedRepository.findAllByOrderByDeletedAtDesc();
        } else {
            deletedOrders = deletedRepository.findByDeletedByEmployeeIdOrderByDeletedAtDesc(user.employeeId);
        }
        model.addAttribute("workOrders", deletedOrders);

        return "admin-importer-delete-history";
    }

    @GetMapping("/dashboard")
    public String importerEntriesDashboard(@RequestParam(required = false, defaultValue = "all") String filter,
                                          @RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(required = false) String monthString,
                                          Model model,
                                          Principal principal) {
        UserContext user = resolveUser(principal);
        model.addAttribute("importerName", user.name);
        model.addAttribute("importerId", user.employeeId);
        model.addAttribute("activeLink", "importer-sheet");

        List<ImporterWorkOrder> wos;
        if ("monthly".equalsIgnoreCase(filter) && monthString != null && !monthString.trim().isEmpty()) {
            java.time.YearMonth ym = java.time.YearMonth.now();
            try {
                ym = java.time.YearMonth.parse(monthString.trim());
            } catch (Exception ignored) {
            }
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            wos = importerWorkOrderRepository.findByImporterEmployeeIdAndEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(user.employeeId, start, end);
            model.addAttribute("monthString", ym.toString());
        } else if ("custom".equalsIgnoreCase(filter) && startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            LocalDate start = LocalDate.parse(startDate.trim());
            LocalDate end = LocalDate.parse(endDate.trim());
            wos = importerWorkOrderRepository.findByImporterEmployeeIdAndEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(user.employeeId, start, end);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
        } else {
            filter = "all";
            wos = importerWorkOrderRepository.findByImporterEmployeeIdOrderByEntryDateDescCreatedAtDesc(user.employeeId);
        }

        model.addAttribute("filter", filter);
        model.addAttribute("workOrders", wos);

        // Calculate KPI Metrics
        long totalWo = wos.size();
        BigDecimal totalValue = wos.stream()
                .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeDays = wos.stream()
                .map(ImporterWorkOrder::getEntryDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal avgValue = totalWo > 0
                ? totalValue.divide(BigDecimal.valueOf(totalWo), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("totalWo", totalWo);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("activeDays", activeDays);
        model.addAttribute("avgValue", avgValue);

        // Group by Work Type
        Map<String, List<ImporterWorkOrder>> byWorkType = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned"));

        List<Map<String, Object>> workTypeStats = byWorkType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("count", e.getValue().size());
                    BigDecimal val = e.getValue().stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    map.put("totalValue", val);
                    double pct = totalWo > 0 ? (e.getValue().size() * 100.0 / totalWo) : 0.0;
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("workTypeStats", workTypeStats);

        // Group by Client Code
        Map<String, List<ImporterWorkOrder>> byClient = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getClientCode() != null && !w.getClientCode().isBlank()) ? w.getClientCode() : "No Client"));

        List<Map<String, Object>> clientStats = byClient.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("count", e.getValue().size());
                    BigDecimal val = e.getValue().stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    map.put("totalValue", val);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("clientStats", clientStats);

        // 1. Client Series Analysis
        Map<String, List<ImporterWorkOrder>> bySeries = wos.stream()
                .collect(Collectors.groupingBy(w -> resolveClientSeries(w.getClientCode())));

        List<Map<String, Object>> clientSeriesStats = bySeries.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    String seriesName = e.getKey();
                    List<ImporterWorkOrder> list = e.getValue();
                    long count = list.size();
                    BigDecimal val = list.stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = count > 0 ? val.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    double pct = totalWo > 0 ? (count * 100.0 / totalWo) : 0.0;

                    // Unique client codes
                    Set<String> clients = list.stream()
                            .map(ImporterWorkOrder::getClientCode)
                            .filter(c -> c != null && !c.isBlank())
                            .collect(Collectors.toCollection(TreeSet::new));

                    // Top work type in this series
                    String topWt = list.stream()
                            .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned", Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");

                    // Top importer in this series
                    String topImp = list.stream()
                            .collect(Collectors.groupingBy(w -> (w.getImporterName() != null && !w.getImporterName().isBlank()) ? w.getImporterName() : "Unknown", Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");

                    map.put("name", seriesName);
                    map.put("count", count);
                    map.put("totalValue", val);
                    map.put("avgValue", avg);
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    map.put("clientsCount", clients.size());
                    map.put("clientsList", String.join(", ", clients));
                    map.put("topWorkType", topWt);
                    map.put("topImporter", topImp);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("clientSeriesStats", clientSeriesStats);

        // 2. Work Type + Client Series Compare Matrix (Cross-tabulation)
        List<String> seriesHeaders = clientSeriesStats.stream()
                .map(m -> (String) m.get("name"))
                .sorted((s1, s2) -> {
                    try {
                        int n1 = Integer.parseInt(s1.replaceAll("\\D+", " ").trim().split(" ")[0]);
                        int n2 = Integer.parseInt(s2.replaceAll("\\D+", " ").trim().split(" ")[0]);
                        return Integer.compare(n1, n2);
                    } catch (Exception ex) {
                        return s1.compareTo(s2);
                    }
                })
                .collect(Collectors.toList());
        model.addAttribute("seriesHeaders", seriesHeaders);

        Map<String, List<ImporterWorkOrder>> byWtForMatrix = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned"));

        Map<String, Long> colCounts = new HashMap<>();
        Map<String, BigDecimal> colValues = new HashMap<>();
        for (String sh : seriesHeaders) {
            colCounts.put(sh, 0L);
            colValues.put(sh, BigDecimal.ZERO);
        }

        List<Map<String, Object>> compareMatrix = new ArrayList<>();
        for (Map.Entry<String, List<ImporterWorkOrder>> entry : byWtForMatrix.entrySet()) {
            String wtName = entry.getKey();
            List<ImporterWorkOrder> wtOrders = entry.getValue();

            Map<String, Object> row = new HashMap<>();
            row.put("workType", wtName);
            row.put("totalCount", (long) wtOrders.size());
            BigDecimal rowVal = wtOrders.stream()
                    .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("totalValue", rowVal);

            Map<String, List<ImporterWorkOrder>> seriesInWt = wtOrders.stream()
                    .collect(Collectors.groupingBy(w -> resolveClientSeries(w.getClientCode())));

            Map<String, Map<String, Object>> cells = new HashMap<>();
            for (String sh : seriesHeaders) {
                List<ImporterWorkOrder> match = seriesInWt.getOrDefault(sh, Collections.emptyList());
                long c = match.size();
                BigDecimal v = match.stream()
                        .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> cellData = new HashMap<>();
                cellData.put("count", c);
                cellData.put("value", v);
                cells.put(sh, cellData);

                colCounts.put(sh, colCounts.get(sh) + c);
                colValues.put(sh, colValues.get(sh).add(v));
            }
            row.put("cells", cells);
            compareMatrix.add(row);
        }

        compareMatrix.sort((r1, r2) -> Long.compare((Long) r2.get("totalCount"), (Long) r1.get("totalCount")));
        model.addAttribute("compareMatrix", compareMatrix);

        Map<String, Object> seriesColumnTotals = new HashMap<>();
        seriesColumnTotals.put("counts", colCounts);
        seriesColumnTotals.put("values", colValues);
        model.addAttribute("seriesColumnTotals", seriesColumnTotals);

        // Available Months for dropdown (last 12 months)
        List<Map<String, String>> availableMonths = new ArrayList<>();
        java.time.YearMonth currentYm = java.time.YearMonth.now();
        java.time.format.DateTimeFormatter monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy");
        for (int i = 0; i < 12; i++) {
            java.time.YearMonth m = currentYm.minusMonths(i);
            availableMonths.add(Map.of(
                    "value", m.toString(),
                    "label", m.format(monthFmt)
            ));
        }
        model.addAttribute("availableMonths", availableMonths);

        return "admin-importer-dashboard";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/admin-dashboard")
    public String adminOverviewDashboard(@RequestParam(required = false, defaultValue = "all") String filter,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(required = false) String importerId,
                                         @RequestParam(required = false) String clientCode,
                                         @RequestParam(required = false) String workType,
                                         Model model,
                                         Principal principal) {
        UserContext user = resolveUser(principal);
        model.addAttribute("loggedUserName", user.name);
        model.addAttribute("loggedUserId", user.employeeId);
        model.addAttribute("activeLink", "importer-admin-dashboard");

        LocalDate today = LocalDate.now();
        LocalDate queryStart = null;
        LocalDate queryEnd = null;

        if ("today".equalsIgnoreCase(filter)) {
            queryStart = today;
            queryEnd = today;
        } else if ("yesterday".equalsIgnoreCase(filter)) {
            queryStart = today.minusDays(1);
            queryEnd = today.minusDays(1);
        } else if ("this_week".equalsIgnoreCase(filter)) {
            queryStart = today.with(java.time.DayOfWeek.MONDAY);
            queryEnd = today;
        } else if ("this_month".equalsIgnoreCase(filter)) {
            queryStart = today.withDayOfMonth(1);
            queryEnd = today;
        } else if ("custom".equalsIgnoreCase(filter) && startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            try {
                queryStart = LocalDate.parse(startDate.trim());
                queryEnd = LocalDate.parse(endDate.trim());
            } catch (Exception ignored) {
            }
        }

        List<ImporterWorkOrder> wos;
        if (queryStart != null && queryEnd != null) {
            wos = importerWorkOrderRepository.findByEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(queryStart, queryEnd);
        } else {
            filter = "all";
            wos = importerWorkOrderRepository.findAllByOrderByEntryDateDescCreatedAtDesc();
        }

        // Apply secondary filters
        if (importerId != null && !importerId.trim().isEmpty()) {
            final String empId = importerId.trim();
            wos = wos.stream().filter(w -> empId.equals(w.getImporterEmployeeId())).collect(Collectors.toList());
        }
        if (clientCode != null && !clientCode.trim().isEmpty()) {
            final String cc = clientCode.trim();
            wos = wos.stream().filter(w -> cc.equalsIgnoreCase(w.getClientCode())).collect(Collectors.toList());
        }
        if (workType != null && !workType.trim().isEmpty()) {
            final String wt = workType.trim();
            wos = wos.stream().filter(w -> wt.equalsIgnoreCase(w.getWorkType())).collect(Collectors.toList());
        }

        model.addAttribute("filter", filter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedImporterId", importerId);
        model.addAttribute("selectedClientCode", clientCode);
        model.addAttribute("selectedWorkType", workType);
        model.addAttribute("workOrders", wos);

        // Compute KPIs
        long totalWo = wos.size();
        BigDecimal totalValue = wos.stream()
                .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeImporters = wos.stream()
                .map(ImporterWorkOrder::getImporterEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal avgValue = totalWo > 0
                ? totalValue.divide(BigDecimal.valueOf(totalWo), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double avgWoPerImporter = activeImporters > 0
                ? Math.round((double) totalWo / activeImporters * 10.0) / 10.0
                : 0.0;

        model.addAttribute("totalWo", totalWo);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("activeImporters", activeImporters);
        model.addAttribute("avgValue", avgValue);
        model.addAttribute("avgWoPerImporter", avgWoPerImporter);

        // Importer Performance Leaderboard
        Map<String, List<ImporterWorkOrder>> byImporter = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getImporterEmployeeId() != null ? w.getImporterEmployeeId() : "Unknown")));

        List<Map<String, Object>> importerLeaderboard = byImporter.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    String empId = e.getKey();
                    List<ImporterWorkOrder> list = e.getValue();
                    String name = list.isEmpty() ? empId : list.get(0).getImporterName();
                    long count = list.size();
                    BigDecimal val = list.stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = count > 0 ? val.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    double pct = totalWo > 0 ? (count * 100.0 / totalWo) : 0.0;

                    // Top work type for this importer
                    String topWt = list.stream()
                            .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "General", Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");

                    map.put("employeeId", empId);
                    map.put("name", name != null ? name : empId);
                    map.put("count", count);
                    map.put("totalValue", val);
                    map.put("avgValue", avg);
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    map.put("topWorkType", topWt);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("importerLeaderboard", importerLeaderboard);

        // Identify Top Performer
        if (!importerLeaderboard.isEmpty()) {
            model.addAttribute("topPerformer", importerLeaderboard.get(0));
        }

        // Work Type Breakdown
        Map<String, List<ImporterWorkOrder>> byWorkType = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned"));

        List<Map<String, Object>> workTypeStats = byWorkType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("count", e.getValue().size());
                    BigDecimal val = e.getValue().stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    map.put("totalValue", val);
                    double pct = totalWo > 0 ? (e.getValue().size() * 100.0 / totalWo) : 0.0;
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("workTypeStats", workTypeStats);

        // Client Code Breakdown
        Map<String, List<ImporterWorkOrder>> byClient = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getClientCode() != null && !w.getClientCode().isBlank()) ? w.getClientCode() : "No Client"));

        List<Map<String, Object>> clientStats = byClient.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("count", e.getValue().size());
                    BigDecimal val = e.getValue().stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    map.put("totalValue", val);
                    double pct = totalWo > 0 ? (e.getValue().size() * 100.0 / totalWo) : 0.0;
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("clientStats", clientStats);

        // 1. Client Series Analysis
        Map<String, List<ImporterWorkOrder>> bySeries = wos.stream()
                .collect(Collectors.groupingBy(w -> resolveClientSeries(w.getClientCode())));

        List<Map<String, Object>> clientSeriesStats = bySeries.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    String seriesName = e.getKey();
                    List<ImporterWorkOrder> list = e.getValue();
                    long count = list.size();
                    BigDecimal val = list.stream()
                            .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = count > 0 ? val.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    double pct = totalWo > 0 ? (count * 100.0 / totalWo) : 0.0;

                    // Unique client codes
                    Set<String> clients = list.stream()
                            .map(ImporterWorkOrder::getClientCode)
                            .filter(c -> c != null && !c.isBlank())
                            .collect(Collectors.toCollection(TreeSet::new));

                    // Top work type in this series
                    String topWt = list.stream()
                            .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned", Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");

                    // Top importer in this series
                    String topImp = list.stream()
                            .collect(Collectors.groupingBy(w -> (w.getImporterName() != null && !w.getImporterName().isBlank()) ? w.getImporterName() : "Unknown", Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("N/A");

                    map.put("name", seriesName);
                    map.put("count", count);
                    map.put("totalValue", val);
                    map.put("avgValue", avg);
                    map.put("percentage", Math.round(pct * 10.0) / 10.0);
                    map.put("clientsCount", clients.size());
                    map.put("clientsList", String.join(", ", clients));
                    map.put("topWorkType", topWt);
                    map.put("topImporter", topImp);
                    return map;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("count")).longValue(), ((Number) a.get("count")).longValue()))
                .collect(Collectors.toList());
        model.addAttribute("clientSeriesStats", clientSeriesStats);

        // 2. Work Type + Client Series Compare Matrix (Cross-tabulation)
        List<String> seriesHeaders = clientSeriesStats.stream()
                .map(m -> (String) m.get("name"))
                .sorted((s1, s2) -> {
                    try {
                        int n1 = Integer.parseInt(s1.replaceAll("\\D+", " ").trim().split(" ")[0]);
                        int n2 = Integer.parseInt(s2.replaceAll("\\D+", " ").trim().split(" ")[0]);
                        return Integer.compare(n1, n2);
                    } catch (Exception ex) {
                        return s1.compareTo(s2);
                    }
                })
                .collect(Collectors.toList());
        model.addAttribute("seriesHeaders", seriesHeaders);

        Map<String, List<ImporterWorkOrder>> byWtForMatrix = wos.stream()
                .collect(Collectors.groupingBy(w -> (w.getWorkType() != null && !w.getWorkType().isBlank()) ? w.getWorkType() : "Unassigned"));

        Map<String, Long> colCounts = new HashMap<>();
        Map<String, BigDecimal> colValues = new HashMap<>();
        for (String sh : seriesHeaders) {
            colCounts.put(sh, 0L);
            colValues.put(sh, BigDecimal.ZERO);
        }

        List<Map<String, Object>> compareMatrix = new ArrayList<>();
        for (Map.Entry<String, List<ImporterWorkOrder>> entry : byWtForMatrix.entrySet()) {
            String wtName = entry.getKey();
            List<ImporterWorkOrder> wtOrders = entry.getValue();

            Map<String, Object> row = new HashMap<>();
            row.put("workType", wtName);
            row.put("totalCount", (long) wtOrders.size());
            BigDecimal rowVal = wtOrders.stream()
                    .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.put("totalValue", rowVal);

            Map<String, List<ImporterWorkOrder>> seriesInWt = wtOrders.stream()
                    .collect(Collectors.groupingBy(w -> resolveClientSeries(w.getClientCode())));

            Map<String, Map<String, Object>> cells = new HashMap<>();
            for (String sh : seriesHeaders) {
                List<ImporterWorkOrder> match = seriesInWt.getOrDefault(sh, Collections.emptyList());
                long c = match.size();
                BigDecimal v = match.stream()
                        .map(w -> w.getValue() != null ? w.getValue() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> cellData = new HashMap<>();
                cellData.put("count", c);
                cellData.put("value", v);
                cells.put(sh, cellData);

                colCounts.put(sh, colCounts.get(sh) + c);
                colValues.put(sh, colValues.get(sh).add(v));
            }
            row.put("cells", cells);
            compareMatrix.add(row);
        }

        compareMatrix.sort((r1, r2) -> Long.compare((Long) r2.get("totalCount"), (Long) r1.get("totalCount")));
        model.addAttribute("compareMatrix", compareMatrix);

        Map<String, Object> seriesColumnTotals = new HashMap<>();
        seriesColumnTotals.put("counts", colCounts);
        seriesColumnTotals.put("values", colValues);
        model.addAttribute("seriesColumnTotals", seriesColumnTotals);

        // Dropdown Lists for Filter Bar
        List<Object[]> rawImporters = importerWorkOrderRepository.findDistinctImporters();
        List<Map<String, String>> importerList = new ArrayList<>();
        for (Object[] row : rawImporters) {
            if (row != null && row.length >= 2 && row[0] != null) {
                importerList.add(Map.of(
                        "id", row[0].toString(),
                        "name", row[1] != null ? row[1].toString() : row[0].toString()
                ));
            }
        }
        model.addAttribute("importerList", importerList);
        model.addAttribute("clientList", importerWorkOrderRepository.findDistinctClientCodes());
        model.addAttribute("workTypeList", importerWorkOrderRepository.findDistinctWorkTypes());

        return "admin-importer-admin-dashboard";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/admin-dashboard/export-csv")
    public void exportAdminCsv(@RequestParam(required = false, defaultValue = "all") String filter,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              @RequestParam(required = false) String importerId,
                              @RequestParam(required = false) String clientCode,
                              @RequestParam(required = false) String workType,
                              HttpServletResponse response) throws IOException {

        LocalDate today = LocalDate.now();
        LocalDate queryStart = null;
        LocalDate queryEnd = null;

        if ("today".equalsIgnoreCase(filter)) {
            queryStart = today;
            queryEnd = today;
        } else if ("yesterday".equalsIgnoreCase(filter)) {
            queryStart = today.minusDays(1);
            queryEnd = today.minusDays(1);
        } else if ("this_week".equalsIgnoreCase(filter)) {
            queryStart = today.with(java.time.DayOfWeek.MONDAY);
            queryEnd = today;
        } else if ("this_month".equalsIgnoreCase(filter)) {
            queryStart = today.withDayOfMonth(1);
            queryEnd = today;
        } else if ("custom".equalsIgnoreCase(filter) && startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            try {
                queryStart = LocalDate.parse(startDate.trim());
                queryEnd = LocalDate.parse(endDate.trim());
            } catch (Exception ignored) {
            }
        }

        List<ImporterWorkOrder> wos;
        if (queryStart != null && queryEnd != null) {
            wos = importerWorkOrderRepository.findByEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(queryStart, queryEnd);
        } else {
            wos = importerWorkOrderRepository.findAllByOrderByEntryDateDescCreatedAtDesc();
        }

        // Apply secondary filters
        if (importerId != null && !importerId.trim().isEmpty()) {
            final String empId = importerId.trim();
            wos = wos.stream().filter(w -> empId.equals(w.getImporterEmployeeId())).collect(Collectors.toList());
        }
        if (clientCode != null && !clientCode.trim().isEmpty()) {
            final String cc = clientCode.trim();
            wos = wos.stream().filter(w -> cc.equalsIgnoreCase(w.getClientCode())).collect(Collectors.toList());
        }
        if (workType != null && !workType.trim().isEmpty()) {
            final String wt = workType.trim();
            wos = wos.stream().filter(w -> wt.equalsIgnoreCase(w.getWorkType())).collect(Collectors.toList());
        }

        String filename = "importer_work_orders_" + LocalDate.now().toString() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter writer = response.getWriter();
        // Write UTF-8 BOM for Microsoft Excel compatibility
        writer.write('\uFEFF');

        // CSV Header
        writer.println("WO Number,Client Code,Client Series,Work Type,Value ($),Importer Name,Importer ID,Entry Date,Created At");

        for (ImporterWorkOrder wo : wos) {
            String woNum = escapeCsv(wo.getWoNumber());
            String cCode = escapeCsv(wo.getClientCode());
            String cSeries = escapeCsv(resolveClientSeries(wo.getClientCode()));
            String wType = escapeCsv(wo.getWorkType());
            String val = wo.getValue() != null ? wo.getValue().toString() : "0.00";
            String impName = escapeCsv(wo.getImporterName());
            String impId = escapeCsv(wo.getImporterEmployeeId());
            String entryDate = wo.getEntryDate() != null ? wo.getEntryDate().toString() : "";
            String createdAt = wo.getCreatedAt() != null ? wo.getCreatedAt().toString() : "";

            writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",\"%s\"",
                    woNum, cCode, cSeries, wType, val, impName, impId, entryDate, createdAt));
        }

        writer.flush();
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}
