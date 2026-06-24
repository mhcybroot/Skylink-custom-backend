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
import root.cyb.mh.attendancesystem.dto.ProcessingWorkOrderImportDTO;
import root.cyb.mh.attendancesystem.dto.ProcessingWorkOrderImportForm;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ProcessingWorkOrder;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.ProcessingWorkOrderRepository;

import java.io.InputStreamReader;
import java.io.Reader;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/processing-sheet")
@SessionAttributes("importForm")
public class ProcessingSheetController {

    @Autowired
    private ProcessingWorkOrderRepository processingWorkOrderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @ModelAttribute("importForm")
    public ProcessingWorkOrderImportForm setUpImportForm() {
        return new ProcessingWorkOrderImportForm();
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

        List<ProcessingWorkOrder> previewWOs = buildWorkOrdersFromForm(importForm, currentUserId);
        model.addAttribute("previewWOs", previewWOs);
        
        return "admin-analyst-import-confirm";
    }

    @PostMapping("/import/save")
    public String saveImport(@ModelAttribute("importForm") ProcessingWorkOrderImportForm importForm, SessionStatus sessionStatus, Principal principal) {
        String currentUserId = "";
        Optional<Employee> currentEmp = employeeRepository.findByUsername(principal.getName());
        if (currentEmp.isPresent()) {
            currentUserId = currentEmp.get().getId();
        }

        List<ProcessingWorkOrder> toSave = buildWorkOrdersFromForm(importForm, currentUserId);
        
        processingWorkOrderRepository.saveAll(toSave);
        sessionStatus.setComplete(); // Clear session attributes
        
        return "redirect:/processing-sheet/analyst-controller?success=true";
    }

    private List<ProcessingWorkOrder> buildWorkOrdersFromForm(ProcessingWorkOrderImportForm importForm, String currentUserId) {
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
            
            if (row.isExists()) {
                if ("SKIP".equals(row.getResolution())) {
                    continue;
                } else if ("UPDATE".equals(row.getResolution())) {
                    List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(row.getWoNumber());
                    if (!existingWOs.isEmpty()) {
                        wo = existingWOs.get(0); // Update the first one found
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
            
            wo.setWoType(row.getWorkType());
            wo.setClientCode(row.getClientCode());
            wo.setAddress(row.getAddress() + ", " + row.getCity() + ", " + row.getState() + " " + row.getZip());
            wo.setPhotoCount(row.getPhotoCount());
            wo.setCategory(row.getCategory());
            wo.setDateDue(row.getDateDue());
            wo.setEntryDate(today);
            wo.setStatus("NEW");
            
            if (wo.getDateDue() != null) {
                long lateDays = ChronoUnit.DAYS.between(wo.getDateDue(), wo.getEntryDate());
                wo.setLateStatus(String.valueOf(lateDays));
            } else {
                wo.setLateStatus("0");
            }
            
            wo.setAssignedAnalystEmployeeId(row.getAssignedAnalystEmployeeId());
            if (row.getAssignedAnalystEmployeeId() != null && !row.getAssignedAnalystEmployeeId().isEmpty()) {
                Optional<Employee> a = employeeRepository.findById(row.getAssignedAnalystEmployeeId());
                if(a.isPresent()) {
                    wo.setAnalyst(a.get().getName());
                }
            }
            
            wo.setCreatedByEmployeeId(currentUserId);
            
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
            map.put("analyst", wo.getAnalyst());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping(value = "/api/update-wo", produces = "application/json")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Object>> updateWorkOrder(@RequestBody Map<String, String> payload, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        String woNumber = payload.get("woNumber");
        String field = payload.get("field");
        String value = payload.get("value");

        if (woNumber == null || field == null) {
            response.put("success", false);
            response.put("message", "Invalid request");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        List<ProcessingWorkOrder> existingWOs = processingWorkOrderRepository.findByWoNumber(woNumber);
        if (existingWOs.isEmpty()) {
            response.put("success", false);
            response.put("message", "Work Order not found");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        ProcessingWorkOrder wo = existingWOs.get(0);
        
        // Ensure analyst only updates their own WO (Optional security layer, assuming it's required)
        Optional<Employee> empOpt = employeeRepository.findById(principal.getName());
        if (empOpt.isEmpty()) {
            empOpt = employeeRepository.findByUsername(principal.getName());
        }
        if (empOpt.isPresent() && !empOpt.get().getId().equals(wo.getAssignedAnalystEmployeeId()) 
            && !empOpt.get().isAnalystController() && !"ADMIN".equalsIgnoreCase(empOpt.get().getRole())) {
            response.put("success", false);
            response.put("message", "Unauthorized to update this Work Order");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        try {
            switch (field) {
                case "status":
                    wo.setStatus(value);
                    break;
                case "baFromWo":
                    wo.setBaFromWo(value);
                    break;
                case "baByAnalyst":
                    wo.setBaByAnalyst(value);
                    break;
                case "bidCount":
                    wo.setBidCount(value == null || value.trim().isEmpty() ? null : Integer.valueOf(value.replace(",", "").trim()));
                    break;
                case "bidAmount":
                    wo.setBidAmount(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "clientInvoice":
                    wo.setClientInvoice(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "crewInvoice":
                    wo.setCrewInvoice(value == null || value.trim().isEmpty() ? null : new java.math.BigDecimal(value.replace("$", "").replace(",", "").trim()));
                    break;
                case "notes":
                    wo.setNotes(value);
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "Invalid field");
                    return org.springframework.http.ResponseEntity.ok(response);
            }
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Invalid number format");
            return org.springframework.http.ResponseEntity.ok(response);
        }

        processingWorkOrderRepository.save(wo);
        
        response.put("success", true);
        return org.springframework.http.ResponseEntity.ok(response);
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
