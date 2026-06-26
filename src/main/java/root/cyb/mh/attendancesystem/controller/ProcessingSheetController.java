package root.cyb.mh.attendancesystem.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import root.cyb.mh.attendancesystem.dto.ProcessingWorkOrderImportDTO;
import root.cyb.mh.attendancesystem.dto.ProcessingWorkOrderImportForm;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ProcessingWorkOrder;
import root.cyb.mh.attendancesystem.model.ProcessingWorkOrderHistory;
import root.cyb.mh.attendancesystem.model.DeletedProcessingWorkOrder;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.ProcessingWorkOrderRepository;
import root.cyb.mh.attendancesystem.repository.ProcessingWorkOrderHistoryRepository;
import root.cyb.mh.attendancesystem.repository.DeletedProcessingWorkOrderRepository;
import root.cyb.mh.attendancesystem.dto.AnalystPerformanceDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/processing-sheet")
@SessionAttributes("importForm")
public class ProcessingSheetController {

    private final ProcessingWorkOrderRepository processingWorkOrderRepository;
    private final EmployeeRepository employeeRepository;
    private final ProcessingWorkOrderHistoryRepository historyRepository;
    private final DeletedProcessingWorkOrderRepository deletedProcessingWorkOrderRepository;

    @Autowired
    public ProcessingSheetController(ProcessingWorkOrderRepository processingWorkOrderRepository, EmployeeRepository employeeRepository, ProcessingWorkOrderHistoryRepository historyRepository, DeletedProcessingWorkOrderRepository deletedProcessingWorkOrderRepository) {
        this.processingWorkOrderRepository = processingWorkOrderRepository;
        this.employeeRepository = employeeRepository;
        this.historyRepository = historyRepository;
        this.deletedProcessingWorkOrderRepository = deletedProcessingWorkOrderRepository;
    }

    @ModelAttribute("importForm")
    public ProcessingWorkOrderImportForm setUpImportForm() {
        return new ProcessingWorkOrderImportForm();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String processingDashboard(
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String monthString,
            Model model) {
        
        List<ProcessingWorkOrder> wos;
        
        if ("monthly".equals(filter)) {
            java.time.YearMonth ym = java.time.YearMonth.now();
            if (monthString != null && !monthString.trim().isEmpty()) {
                try {
                    ym = java.time.YearMonth.parse(monthString);
                } catch (Exception e) {
                    // Ignore and use current month
                }
            }
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            wos = processingWorkOrderRepository.findByEntryDateBetween(start, end);
            monthString = ym.toString();
            model.addAttribute("monthString", monthString);
        } else if ("custom".equals(filter) && startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            wos = processingWorkOrderRepository.findByEntryDateBetween(start, end);
        } else {
            // all
            wos = processingWorkOrderRepository.findAllByOrderByCreatedAtDesc();
            filter = "all"; // Ensure filter matches "all" for UI logic
        }

        // Metrics
        long totalWOs = wos.size();
        long duplicates = wos.stream().filter(wo -> wo.getWoNumber() != null && wo.getWoNumber().contains("-DUP")).count();
        long errors = wos.stream().filter(wo -> wo.getStatus() == null || !wo.getStatus().trim().equalsIgnoreCase("Submitted")).count();
        long unassigned = wos.stream().filter(wo -> wo.getAnalyst() == null || wo.getAnalyst().trim().isEmpty() || wo.getAnalyst().trim().equalsIgnoreCase("Unassigned") || wo.getAnalyst().trim().equalsIgnoreCase("N/A")).count();

        // Generate list of past 24 months for the dropdown
        List<Map<String, String>> availableMonths = new ArrayList<>();
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        for (int i = 0; i < 24; i++) {
            java.time.YearMonth ym = currentMonth.minusMonths(i);
            Map<String, String> monthMap = new HashMap<>();
            monthMap.put("value", ym.toString());
            String label = ym.getMonth().name().substring(0, 1) + ym.getMonth().name().substring(1).toLowerCase() + " " + ym.getYear();
            monthMap.put("label", label);
            availableMonths.add(monthMap);
        }
        model.addAttribute("availableMonths", availableMonths);
        
        if (monthString == null || monthString.trim().isEmpty()) {
            monthString = currentMonth.toString();
        }
        model.addAttribute("monthString", monthString);

        // Chart Data: Performance by Analyst
        java.util.Set<String> activeCategories = new java.util.TreeSet<>();
        Map<String, AnalystPerformanceDto> performanceByAnalystMap = new HashMap<>();
        for (ProcessingWorkOrder wo : wos) {
            String analyst = wo.getAnalyst();
            if (analyst != null && !analyst.trim().isEmpty()) {
                AnalystPerformanceDto dto = performanceByAnalystMap.computeIfAbsent(analyst, k -> {
                    AnalystPerformanceDto newDto = new AnalystPerformanceDto();
                    newDto.setAnalyst(k);
                    return newDto;
                });
                
                dto.incrementTotalAssignedWos();

                if ("Submitted".equalsIgnoreCase(wo.getStatus())) {
                    dto.incrementTotalWos();
                    dto.addBidCount(wo.getBidCount());
                    dto.addBidAmount(wo.getBidAmount());
                    dto.addGrossProfit(wo.getClientInvoice(), wo.getCrewInvoice());
                    
                    if (wo.getCategory() != null && !wo.getCategory().trim().isEmpty()) {
                        String cat = wo.getCategory().trim().toLowerCase();
                        if (cat.contains("preservation")) {
                            dto.incrementPreservationWo();
                        } else if (cat.contains("maintenance")) {
                            dto.incrementMaintenanceWo();
                        } else {
                            dto.incrementOthersWo();
                        }
                        
                        String formattedCat = "";
                        if (cat.contains("securering")) cat = cat.replace("securering", "securing");
                        for (String word : cat.split("\\s+")) {
                            if (!word.isEmpty()) {
                                formattedCat += Character.toUpperCase(word.charAt(0)) + (word.length() > 1 ? word.substring(1) : "") + " ";
                            }
                        }
                        formattedCat = formattedCat.trim();
                        if (formattedCat.isEmpty()) formattedCat = "Others";
                        dto.incrementCategory(formattedCat);
                        activeCategories.add(formattedCat);
                    } else {
                        dto.incrementCategory("Others");
                        activeCategories.add("Others");
                        dto.incrementOthersWo();
                    }

                    if (wo.getClientCode() != null) {
                        try {
                            String numStr = wo.getClientCode().replaceAll("[^0-9]", "");
                            if (!numStr.isEmpty()) {
                                int code = Integer.parseInt(numStr);
                                int series = (code / 100) * 100;
                                dto.incrementSeries(series, wo.getBidAmount(), wo.getClientInvoice(), wo.getCrewInvoice());
                            } else {
                                dto.incrementSeries(-1, wo.getBidAmount(), wo.getClientInvoice(), wo.getCrewInvoice());
                            }
                        } catch (Exception e) {
                            dto.incrementSeries(-1, wo.getBidAmount(), wo.getClientInvoice(), wo.getCrewInvoice());
                        }
                    } else {
                        dto.incrementSeries(-1, wo.getBidAmount(), wo.getClientInvoice(), wo.getCrewInvoice());
                    }
                }
            }
        }
        List<AnalystPerformanceDto> performanceByAnalyst = new ArrayList<>(performanceByAnalystMap.values());
        performanceByAnalyst.sort(Comparator.comparing(AnalystPerformanceDto::getAnalyst));
        
        // Chart Data: WOs by Category
        Map<String, Long> categoryCounts = wos.stream()
            .filter(wo -> wo.getCategory() != null && !wo.getCategory().trim().isEmpty() && "Submitted".equalsIgnoreCase(wo.getStatus()))
            .collect(Collectors.groupingBy(ProcessingWorkOrder::getCategory, Collectors.counting()));

        model.addAttribute("performanceByAnalyst", performanceByAnalyst);
        model.addAttribute("activeCategories", activeCategories);
        
        model.addAttribute("hasSeries100", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries100WoCount() > 0));
        model.addAttribute("hasSeries200", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries200WoCount() > 0));
        model.addAttribute("hasSeries300", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries300WoCount() > 0));
        model.addAttribute("hasSeries400", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries400WoCount() > 0));
        model.addAttribute("hasSeries500", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries500WoCount() > 0));
        model.addAttribute("hasSeries600", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries600WoCount() > 0));
        model.addAttribute("hasSeries700", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries700WoCount() > 0));
        model.addAttribute("hasSeries800", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries800WoCount() > 0));
        model.addAttribute("hasSeries900", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeries900WoCount() > 0));
        model.addAttribute("hasSeriesOthers", performanceByAnalyst.stream().anyMatch(dto -> dto.getSeriesOthersWoCount() > 0));
        model.addAttribute("categoryCount", categoryCounts);

        List<Employee> allAnalysts = employeeRepository.findByIsAnalystTrue();
        model.addAttribute("allAnalysts", allAnalysts);

        model.addAttribute("wos", wos);
        model.addAttribute("filter", filter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("totalWOs", totalWOs);
        model.addAttribute("duplicates", duplicates);
        model.addAttribute("errors", errors);
        model.addAttribute("unassigned", unassigned);

        model.addAttribute("activeLink", "admin-analyst-dashboard");
        return "admin-analyst-dashboard";
    }

    @GetMapping("/analyst")
    public String analystDashboard(@RequestParam(required = false) String date, Model model, Principal principal) {
        LocalDate entryDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("currentDate", entryDate);
        
        Optional<Employee> empOpt = employeeRepository.findById(principal.getName());
        if (empOpt.isEmpty()) {
            empOpt = employeeRepository.findByUsername(principal.getName());
        }
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            List<ProcessingWorkOrder> wos = processingWorkOrderRepository.findByEntryDateAndAssignedAnalystEmployeeId(entryDate, emp.getId());
            model.addAttribute("workOrders", wos);
        } else {
            model.addAttribute("workOrders", new ArrayList<>());
        }
        
        List<Employee> analysts = employeeRepository.findByIsAnalystTrue();
        model.addAttribute("analysts", analysts);
        
        return "admin-analyst";
    }

    @GetMapping("/analyst-controller")
    public String analystControllerDashboard(@RequestParam(required = false) String date, Model model) {
        LocalDate entryDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("currentDate", entryDate);
        
        List<ProcessingWorkOrder> wos = processingWorkOrderRepository.findByEntryDate(entryDate);
        model.addAttribute("workOrders", wos);
        
        // Calculate Global Metrics
        int totalWo = wos.size();
        int totalBidSubmitted = 0;
        int totalMaintenanceWo = 0;
        int totalPreservationWo = 0;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;
        
        // Analyst Metrics
        Map<String, Map<String, Object>> analystStats = new HashMap<>();
        
        for (ProcessingWorkOrder wo : wos) {
            // Global aggregates
            if (wo.getBidCount() != null) {
                totalBidSubmitted += wo.getBidCount();
            }
            if (wo.getCategory() != null) {
                String cat = wo.getCategory().toLowerCase();
                if (cat.contains("maintenance")) totalMaintenanceWo++;
                if (cat.contains("preservation")) totalPreservationWo++;
            }
            
            BigDecimal clientInv = wo.getClientInvoice() != null ? wo.getClientInvoice() : BigDecimal.ZERO;
            BigDecimal crewInv = wo.getCrewInvoice() != null ? wo.getCrewInvoice() : BigDecimal.ZERO;
            BigDecimal gp = clientInv.subtract(crewInv);
            
            totalGrossProfit = totalGrossProfit.add(gp);
            
            // Analyst aggregates
            String analyst = wo.getAnalyst() != null && !wo.getAnalyst().trim().isEmpty() ? wo.getAnalyst() : "Unassigned";
            analystStats.putIfAbsent(analyst, new HashMap<>(Map.of(
                "name", analyst,
                "bidCount", 0,
                "bidAmount", BigDecimal.ZERO,
                "grossProfit", BigDecimal.ZERO
            )));
            
            Map<String, Object> stats = analystStats.get(analyst);
            if (wo.getBidCount() != null) {
                stats.put("bidCount", (int) stats.get("bidCount") + wo.getBidCount());
            }
            if (wo.getBidAmount() != null) {
                stats.put("bidAmount", ((BigDecimal) stats.get("bidAmount")).add(wo.getBidAmount()));
            }
            
            stats.put("grossProfit", ((BigDecimal) stats.get("grossProfit")).add(gp));
        }
        
        model.addAttribute("totalWo", totalWo);
        model.addAttribute("totalBidSubmitted", totalBidSubmitted);
        model.addAttribute("totalMaintenanceWo", totalMaintenanceWo);
        model.addAttribute("totalPreservationWo", totalPreservationWo);
        model.addAttribute("totalGrossProfit", totalGrossProfit);
        model.addAttribute("analystStats", analystStats.values());
        
        List<Employee> allAnalysts = employeeRepository.findByIsAnalystTrue();
        model.addAttribute("allAnalysts", allAnalysts);
        
        return "admin-analyst-controller";
    }

    @PostMapping("/import/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file, @ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, Model model) {
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            List<ProcessingWorkOrderImportDTO> rows = new ArrayList<>();
            
            for (CSVRecord csvRecord : csvParser) {
                ProcessingWorkOrderImportDTO dto = new ProcessingWorkOrderImportDTO();
                dto.setStatus(csvRecord.isSet("Status") ? csvRecord.get("Status") : "");
                dto.setWoNumber(csvRecord.isSet("WO #") ? csvRecord.get("WO #") : "");
                
                String dateDueStr = csvRecord.isSet("Date Due") ? csvRecord.get("Date Due") : "";
                try {
                    dto.setDateDue(ProcessingWorkOrder.parseLocalDate(dateDueStr));
                } catch (Exception e) {
                    try {
                        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("MM-dd-yy");
                        dto.setDateDue(LocalDate.parse(dateDueStr, formatter1));
                    } catch (Exception ex) {
                        try {
                            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM-dd-yyyy");
                            dto.setDateDue(LocalDate.parse(dateDueStr, formatter2));
                        } catch (Exception ex2) {
                            dto.setDateDue(null);
                        }
                    }
                }
                
                dto.setClientCode(csvRecord.isSet("Client") ? csvRecord.get("Client") : "");
                dto.setCategory(csvRecord.isSet("Category") ? csvRecord.get("Category") : "");
                dto.setAddress(csvRecord.isSet("Address") ? csvRecord.get("Address") : "");
                dto.setCity(csvRecord.isSet("City") ? csvRecord.get("City") : "");
                dto.setState(csvRecord.isSet("State") ? csvRecord.get("State") : "");
                dto.setZip(csvRecord.isSet("Zip") ? csvRecord.get("Zip") : "");
                dto.setContractor(csvRecord.isSet("Contractor") ? csvRecord.get("Contractor") : "");
                dto.setAdmin(csvRecord.isSet("Admin") ? csvRecord.get("Admin") : "");
                dto.setIcons(csvRecord.isSet("Icons") ? csvRecord.get("Icons") : "");
                dto.setWorkType(csvRecord.isSet("Work Type") ? csvRecord.get("Work Type") : "");
                
                String photos = csvRecord.isSet("Photos") ? csvRecord.get("Photos") : "0";
                try {
                    dto.setPhotoCount(Integer.parseInt(photos));
                } catch (NumberFormatException e) {
                    dto.setPhotoCount(0);
                }
                
                // Check if WO already exists
                List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(dto.getWoNumber());
                if (!existingWOs.isEmpty()) {
                    dto.setExists(true);
                    dto.setResolution("SKIP"); // Default resolution
                } else {
                    dto.setExists(false);
                }
                
                rows.add(dto);
            }
            
            importForm.setRows(rows);
            importForm.setSkippedCategories(new HashSet<>()); // Reset
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/processing-sheet/analyst-controller?error=importFailed";
        }
        
        return "redirect:/processing-sheet/import/preview";
    }

    @GetMapping("/import/preview")
    public String showImportPreview(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, Model model) {
        if (importForm.getRows().isEmpty()) {
            return "redirect:/processing-sheet/analyst-controller";
        }
        
        Set<String> categories = importForm.getRows().stream()
                .map(ProcessingWorkOrderImportDTO::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
                
        model.addAttribute("categories", categories);
        return "admin-analyst-import-preview";
    }

    @PostMapping("/import/preview")
    public String processPreview(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm) {
        // Form binds resolutions and skipped categories back to importForm
        return "redirect:/processing-sheet/import/assign";
    }

    @GetMapping("/import/assign")
    public String showImportAssign(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, Model model) {
        if (importForm.getRows().isEmpty()) {
            return "redirect:/processing-sheet/analyst-controller";
        }
        
        for (ProcessingWorkOrderImportDTO row : importForm.getRows()) {
            if ("UPDATE".equals(row.getResolution()) && (row.getAssignedAnalystEmployeeId() == null || row.getAssignedAnalystEmployeeId().isEmpty())) {
                List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(row.getWoNumber());
                if (!existingWOs.isEmpty() && existingWOs.get(0).getAssignedAnalystEmployeeId() != null) {
                    row.setAssignedAnalystEmployeeId(existingWOs.get(0).getAssignedAnalystEmployeeId());
                }
            }
        }
        
        List<Employee> analysts = employeeRepository.findByIsAnalystTrue();
        model.addAttribute("analysts", analysts);
        
        return "admin-analyst-import-assign";
    }

    @PostMapping("/import/confirm")
    public String confirmImport(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, Model model, Principal principal) {
        String currentUserId = "";
        Optional<Employee> currentEmp = employeeRepository.findByUsername(principal.getName());
        if (currentEmp.isPresent()) {
            currentUserId = currentEmp.get().getId();
        }

        String currentUserName = principal.getName();
        if (currentEmp.isPresent()) {
            currentUserName = currentEmp.get().getName();
        }

        List<ProcessingWorkOrderHistory> dummyHistoryLogs = new ArrayList<>();
        List<ProcessingWorkOrder> previewWOs = buildWorkOrdersFromForm(importForm, currentUserId, currentUserName, dummyHistoryLogs);
        model.addAttribute("previewWOs", previewWOs);
        
        return "admin-analyst-import-confirm";
    }

    @PostMapping("/import/save")
    public String saveImport(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, SessionStatus sessionStatus, Principal principal) {
        String currentUserId = principal.getName();
        String currentUserName = principal.getName();
        Optional<Employee> currentEmp = employeeRepository.findByUsername(principal.getName());
        if (!currentEmp.isPresent()) {
            currentEmp = employeeRepository.findById(principal.getName());
        }
        if (currentEmp.isPresent()) {
            currentUserId = currentEmp.get().getId();
            currentUserName = currentEmp.get().getName();
        }

        List<ProcessingWorkOrderHistory> updateHistoryLogs = new ArrayList<>();
        List<ProcessingWorkOrder> toSave = buildWorkOrdersFromForm(importForm, currentUserId, currentUserName, updateHistoryLogs);
        
        Set<String> updatedWoNumbers = new HashSet<>();
        for (ProcessingWorkOrder wo : toSave) {
            if (wo.getId() != null) {
                updatedWoNumbers.add(wo.getWoNumber());
            }
        }
        
        processingWorkOrderRepository.saveAll(toSave);
        
        List<ProcessingWorkOrderHistory> finalLogs = new ArrayList<>(updateHistoryLogs);
        for (ProcessingWorkOrder wo : toSave) {
            if (!updatedWoNumbers.contains(wo.getWoNumber())) {
                String action = wo.getWoNumber().contains("-DUP") ? "IMPORTED_AS_DUPLICATE" : "IMPORTED";
                finalLogs.add(new ProcessingWorkOrderHistory(
                    wo.getId(), wo.getWoNumber(), action, null, null, currentUserName
                ));
            }
        }
        historyRepository.saveAll(finalLogs);
        sessionStatus.setComplete(); // Clear session attributes
        
        return "redirect:/processing-sheet/analyst-controller?success=true";
    }

    private void logIfChanged(List<ProcessingWorkOrderHistory> logs, ProcessingWorkOrder wo, String field, String oldVal, String newVal, String userName) {
        String safeOld = oldVal == null ? "" : oldVal;
        String safeNew = newVal == null ? "" : newVal;
        if (!safeOld.equals(safeNew)) {
            logs.add(new ProcessingWorkOrderHistory(
                wo.getId(), wo.getWoNumber(), "UPDATED_" + field.toUpperCase() + "_VIA_IMPORT", safeOld, safeNew, userName
            ));
        }
    }

    private List<ProcessingWorkOrder> buildWorkOrdersFromForm(ProcessingWorkOrderImportForm importForm, String currentUserId, String currentUserName, List<ProcessingWorkOrderHistory> historyLogs) {
        List<ProcessingWorkOrder> toSave = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Track new WO numbers assigned during this session to handle intra-csv duplicates
        Set<String> newlyAssignedWOs = new HashSet<>();
        
        for (ProcessingWorkOrderImportDTO row : importForm.getRows()) {
            // Skip if category is skipped
            if (importForm.getSkippedCategories() != null && importForm.getSkippedCategories().contains(row.getCategory())) {
                continue;
            }
            
            ProcessingWorkOrder wo = new ProcessingWorkOrder();
            
            boolean isUpdate = false;
            
            if (row.isExists()) {
                if ("SKIP".equals(row.getResolution())) {
                    continue;
                } else if ("UPDATE".equals(row.getResolution())) {
                    List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(row.getWoNumber());
                    if (!existingWOs.isEmpty()) {
                        wo = existingWOs.get(0); // Update the first one found
                        isUpdate = true;
                    }
                } else if ("DUPLICATE".equals(row.getResolution())) {
                    String baseWoNumber = row.getWoNumber();
                    int suffix = 1;
                    String newWoNumber = baseWoNumber + "-DUP" + suffix;
                    while (!processingWorkOrderRepository.findByWoNumber(newWoNumber).isEmpty() || newlyAssignedWOs.contains(newWoNumber)) {
                        suffix++;
                        newWoNumber = baseWoNumber + "-DUP" + suffix;
                    }
                    wo.setWoNumber(newWoNumber);
                    newlyAssignedWOs.add(newWoNumber);
                }
            } else {
                wo.setWoNumber(row.getWoNumber());
                newlyAssignedWOs.add(row.getWoNumber());
            }
            String oldWoType = wo.getWoType();
            String oldClientCode = wo.getClientCode();
            String oldAddress = wo.getAddress();
            Integer oldPhotoCountInt = wo.getPhotoCount();
            String oldPhotoCount = oldPhotoCountInt != null ? String.valueOf(oldPhotoCountInt) : null;
            String oldCategory = wo.getCategory();
            LocalDate oldDateDue = wo.getDateDue();
            String oldAnalyst = wo.getAnalyst();
            
            wo.setWoType(row.getWorkType());
            wo.setClientCode(row.getClientCode());
            wo.setAddress(row.getAddress() + ", " + row.getCity() + ", " + row.getState() + " " + row.getZip());
            wo.setPhotoCount(row.getPhotoCount());
            wo.setCategory(row.getCategory());
            wo.setDateDue(row.getDateDue());
            
            if (!isUpdate) {
                wo.setEntryDate(today);
                wo.setStatus("NEW");
                wo.setCreatedByEmployeeId(currentUserId);
            }
            
            if (wo.getDateDue() != null && wo.getEntryDate() != null) {
                long lateDays = ChronoUnit.DAYS.between(wo.getDateDue(), wo.getEntryDate());
                wo.setLateStatus(String.valueOf(lateDays));
            } else {
                wo.setLateStatus("0");
            }
            
            if (row.getAssignedAnalystEmployeeId() != null && !row.getAssignedAnalystEmployeeId().isEmpty()) {
                wo.setAssignedAnalystEmployeeId(row.getAssignedAnalystEmployeeId());
                Optional<Employee> a = employeeRepository.findById(row.getAssignedAnalystEmployeeId());
                if(a.isPresent()) {
                    wo.setAnalyst(a.get().getName());
                }
            }
            
            if (isUpdate) {
                String newPhotoCount = wo.getPhotoCount() != null ? String.valueOf(wo.getPhotoCount()) : null;
                logIfChanged(historyLogs, wo, "WORK_TYPE", oldWoType, wo.getWoType(), currentUserName);
                logIfChanged(historyLogs, wo, "CLIENT_CODE", oldClientCode, wo.getClientCode(), currentUserName);
                logIfChanged(historyLogs, wo, "ADDRESS", oldAddress, wo.getAddress(), currentUserName);
                logIfChanged(historyLogs, wo, "PHOTO_COUNT", oldPhotoCount, newPhotoCount, currentUserName);
                logIfChanged(historyLogs, wo, "CATEGORY", oldCategory, wo.getCategory(), currentUserName);
                logIfChanged(historyLogs, wo, "DATE_DUE", oldDateDue != null ? oldDateDue.toString() : null, wo.getDateDue() != null ? wo.getDateDue().toString() : null, currentUserName);
                logIfChanged(historyLogs, wo, "ANALYST", oldAnalyst, wo.getAnalyst(), currentUserName);
            }
            
            toSave.add(wo);
        }
        return toSave;
    }

    @GetMapping("/api/duplicates")
    @ResponseBody
    public List<Map<String, Object>> getDuplicates(@RequestParam("woNumber") String woNumber) {
        String baseWoNumber = woNumber.replaceAll("-DUP\\d+$", "");
        
        List<ProcessingWorkOrder> relatedWOs = processingWorkOrderRepository.findByWoNumberStartingWith(baseWoNumber);
        
        return relatedWOs.stream().map(wo -> {
            Map<String, Object> map = new HashMap<>();
            map.put("woNumber", wo.getWoNumber());
            map.put("status", wo.getStatus());
            map.put("analyst", wo.getAnalyst() != null ? wo.getAnalyst() : "Unassigned");
            map.put("entryDate", wo.getEntryDate() != null ? wo.getEntryDate().toString() : "");
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping(value = "/api/update-wo", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateWorkOrder(@RequestBody Map<String, String> payload, Principal principal) {
        String currentUserName = (principal != null) ? principal.getName() : "admin";
        Optional<Employee> emp = employeeRepository.findByUsername(currentUserName);
        if (emp.isPresent()) {
            currentUserName = emp.get().getName();
        }

        String woNumber = payload.get("woNumber");
        String field = payload.get("field");
        String value = payload.get("value");

        String principalNameDebug = (principal != null) ? principal.getName() : "admin";
        System.out.println("DEBUG updateWorkOrder - USER: " + principalNameDebug + ", WO: " + woNumber + ", FIELD: " + field + ", VALUE: " + value);

        if (woNumber == null || field == null) {
            System.out.println("DEBUG updateWorkOrder - Invalid request payload");
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid request"));
        }

        List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(woNumber);
        if (existingWOs.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Work Order not found"));
        }

        ProcessingWorkOrder wo = existingWOs.get(0);
        String oldValue = "";
        
        // Ensure analyst only updates their own WO (Optional security layer, assuming it's required)
        String principalName = (principal != null) ? principal.getName() : "admin";
        Optional<Employee> empOpt = employeeRepository.findById(principalName);
        if (empOpt.isEmpty()) {
            empOpt = employeeRepository.findByUsername(principalName);
        }
        
        System.out.println("DEBUG updateWorkOrder - empOpt present: " + empOpt.isPresent() + ", ID: " + (empOpt.isPresent() ? empOpt.get().getId() : "N/A"));
        
        if (empOpt.isPresent() && !empOpt.get().getId().equals(wo.getAssignedAnalystEmployeeId()) 
            && !empOpt.get().isAnalystController() && !"ADMIN".equalsIgnoreCase(empOpt.get().getRole())) {
            System.out.println("DEBUG updateWorkOrder - Unauthorized");
            return ResponseEntity.ok(Map.of("success", false, "message", "Unauthorized to update this Work Order"));
        }

        try {
            switch (field) {
                case "status":
                    oldValue = wo.getStatus();
                    wo.setStatus(value);
                    break;
                case "baFromWo":
                    oldValue = wo.getBaFromWo();
                    wo.setBaFromWo(value);
                    break;
                case "baByAnalyst":
                    oldValue = wo.getBaByAnalyst();
                    wo.setBaByAnalyst(value);
                    break;
                case "bidCount":
                    oldValue = wo.getBidCount() != null ? String.valueOf(wo.getBidCount()) : "";
                    wo.setBidCount(value == null || value.trim().isEmpty() ? null : Integer.valueOf(value.replace(",", "").trim()));
                    break;
                case "bidAmount":
                    oldValue = wo.getBidAmount() != null ? String.valueOf(wo.getBidAmount()) : "";
                    wo.setBidAmount(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "clientInvoice":
                    oldValue = wo.getClientInvoice() != null ? String.valueOf(wo.getClientInvoice()) : "";
                    wo.setClientInvoice(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "crewInvoice":
                    oldValue = wo.getCrewInvoice() != null ? String.valueOf(wo.getCrewInvoice()) : "";
                    wo.setCrewInvoice(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "notes":
                    oldValue = wo.getNotes();
                    wo.setNotes(value);
                    break;
                case "analyst":
                    oldValue = wo.getAnalyst();
                    if (value == null || "Unassigned".equalsIgnoreCase(value) || value.trim().isEmpty()) {
                        wo.setAnalyst("Unassigned");
                        wo.setAssignedAnalystEmployeeId(null);
                    } else {
                        wo.setAnalyst(value);
                        List<Employee> activeAnalysts = employeeRepository.findByIsAnalystTrue();
                        wo.setAssignedAnalystEmployeeId(null);
                        for (Employee e : activeAnalysts) {
                            if (e.getName().equalsIgnoreCase(value)) {
                                wo.setAssignedAnalystEmployeeId(e.getId());
                                break;
                            }
                        }
                    }
                    break;
                default:
                    return ResponseEntity.ok(Map.of("success", false, "message", "Invalid field"));
            }

            String newValue = value == null ? "" : value;
            String safeOldValue = oldValue == null ? "" : oldValue;
            System.out.println("DEBUG updateWorkOrder - oldValue: " + safeOldValue + ", newValue: " + newValue);
            if (!safeOldValue.equals(newValue)) {
                System.out.println("DEBUG updateWorkOrder - Saving to historyRepository");
                historyRepository.save(new ProcessingWorkOrderHistory(
                    wo.getId(), wo.getWoNumber(), "UPDATED_" + field.toUpperCase(), safeOldValue, newValue, currentUserName
                ));
            }
        } catch (NumberFormatException e) {
            System.out.println("DEBUG updateWorkOrder - NumberFormatException");
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid number format"));
        }

        System.out.println("DEBUG updateWorkOrder - Saving to processingWorkOrderRepository");
        processingWorkOrderRepository.save(wo);
        
        System.out.println("DEBUG updateWorkOrder - Success");
        return ResponseEntity.ok(Map.of("success", true, "message", "Updated successfully"));
    }

    @PostMapping(value = "/api/delete-wo", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteWorkOrder(@RequestBody Map<String, String> payload, Principal principal, org.springframework.security.core.Authentication auth) {
        String woNumber = payload.get("woNumber");
        if (woNumber == null || woNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing WO Number"));
        }

        List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(woNumber);
        if (existingWOs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Work Order not found"));
        }
        ProcessingWorkOrder wo = existingWOs.get(0);

        String currentUserName = principal.getName();
        String currentUserId = "";
        Optional<Employee> emp = employeeRepository.findByUsername(currentUserName);
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        boolean isAnalystController = false;

        if (emp.isPresent()) {
            Employee currentEmp = emp.get();
            currentUserName = currentEmp.getName();
            currentUserId = currentEmp.getId();
            isAnalystController = currentEmp.isAnalystController();
            if ("ADMIN".equalsIgnoreCase(currentEmp.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(currentEmp.getRole())) {
                isAdmin = true;
            }
        } else {
            // Fallback: check if the user is an Employee via ID
            emp = employeeRepository.findById(currentUserName);
            if (emp.isPresent()) {
                Employee currentEmp = emp.get();
                currentUserName = currentEmp.getName();
                currentUserId = currentEmp.getId();
                isAnalystController = currentEmp.isAnalystController();
                if ("ADMIN".equalsIgnoreCase(currentEmp.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(currentEmp.getRole())) {
                    isAdmin = true;
                }
            } else {
                currentUserId = principal.getName(); // Fallback to principal name
            }
        }

        if (!isAdmin) {
            if (!isAnalystController || !currentUserId.equals(wo.getCreatedByEmployeeId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "You do not have permission to delete this work order. (Created By: " + wo.getCreatedByEmployeeId() + ", Your ID: " + currentUserId + ", Admin: " + isAdmin + ")"));
            }
        }

        DeletedProcessingWorkOrder deletedWo = new DeletedProcessingWorkOrder();
        deletedWo.setOriginalId(wo.getId());
        deletedWo.setWoNumber(wo.getWoNumber());
        deletedWo.setWoType(wo.getWoType());
        deletedWo.setClientCode(wo.getClientCode());
        deletedWo.setAddress(wo.getAddress());
        deletedWo.setPhotoCount(wo.getPhotoCount());
        deletedWo.setWorkDate(wo.getWorkDate());
        deletedWo.setCategory(wo.getCategory());
        deletedWo.setLateStatus(wo.getLateStatus());
        deletedWo.setAnalyst(wo.getAnalyst());
        deletedWo.setStatus(wo.getStatus());
        deletedWo.setDateDue(wo.getDateDue());
        deletedWo.setEntryDate(wo.getEntryDate());
        deletedWo.setNotes(wo.getNotes());
        deletedWo.setBaFromWo(wo.getBaFromWo());
        deletedWo.setBaByAnalyst(wo.getBaByAnalyst());
        deletedWo.setBidCount(wo.getBidCount());
        deletedWo.setBidAmount(wo.getBidAmount());
        deletedWo.setClientInvoice(wo.getClientInvoice());
        deletedWo.setCrewInvoice(wo.getCrewInvoice());
        deletedWo.setAssignedAnalystEmployeeId(wo.getAssignedAnalystEmployeeId());
        deletedWo.setCreatedAt(wo.getCreatedAt());
        deletedWo.setUpdatedAt(wo.getUpdatedAt());
        deletedWo.setCreatedByEmployeeId(wo.getCreatedByEmployeeId());
        deletedWo.setLastUpdatedByEmployeeId(wo.getLastUpdatedByEmployeeId());
        
        deletedWo.setDeletedAt(LocalDateTime.now());
        deletedWo.setDeletedByEmployeeId(currentUserId);
        deletedWo.setDeletedByName(currentUserName);

        deletedProcessingWorkOrderRepository.save(deletedWo);
        
        historyRepository.save(new ProcessingWorkOrderHistory(
            wo.getId(), wo.getWoNumber(), "DELETED", null, null, currentUserName
        ));

        processingWorkOrderRepository.delete(wo);

        return ResponseEntity.ok(Map.of("success", true, "message", "Work order deleted successfully."));
    }

    @GetMapping("/delete-history")
    public String deleteHistoryDashboard(Model model) {
        List<DeletedProcessingWorkOrder> deletedWorkOrders = deletedProcessingWorkOrderRepository.findAllByOrderByDeletedAtDesc();
        model.addAttribute("workOrders", deletedWorkOrders);
        return "admin-analyst-delete-history";
    }

    @GetMapping("/api/lifeline")
    @ResponseBody
    public ResponseEntity<List<ProcessingWorkOrderHistory>> getLifeline(@RequestParam String woNumber) {
        List<ProcessingWorkOrderHistory> history = historyRepository.findByWoNumberOrderByChangedAtDesc(woNumber);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/lifeline/{woNumber}")
    public String getLifelinePage(@PathVariable String woNumber, Model model) {
        List<ProcessingWorkOrderHistory> history = historyRepository.findByWoNumberOrderByChangedAtDesc(woNumber);
        model.addAttribute("woNumber", woNumber);
        model.addAttribute("history", history);
        return "admin-analyst-lifeline";
    }

    @GetMapping("/api/address-duplicates")
    @ResponseBody
    public List<Map<String, Object>> getAddressDuplicates(@RequestParam("address") String address) {
        List<ProcessingWorkOrder> related = processingWorkOrderRepository.findByAddress(address);
        
        // Return only what's needed for the popover
        return related.stream().map(wo -> {
            Map<String, Object> map = new HashMap<>();
            map.put("woNumber", wo.getWoNumber());
            map.put("status", wo.getStatus());
            map.put("analyst", wo.getAnalyst() != null ? wo.getAnalyst() : "Unassigned");
            map.put("entryDate", wo.getEntryDate() != null ? wo.getEntryDate().toString() : "");
            return map;
        }).collect(Collectors.toList());
    }
}
