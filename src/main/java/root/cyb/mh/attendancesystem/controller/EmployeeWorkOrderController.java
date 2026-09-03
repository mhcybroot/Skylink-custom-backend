package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import root.cyb.mh.attendancesystem.dto.AgingSummaryDTO;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.Client;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.ClientRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;
import root.cyb.mh.attendancesystem.service.ClientDueAgingService;
import root.cyb.mh.attendancesystem.service.EmployeeWorkOrderService;
import root.cyb.mh.attendancesystem.service.WorkOrderReportService;
import root.cyb.mh.attendancesystem.specification.EmployeeWorkOrderSpecifications;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employee/work-orders")
public class EmployeeWorkOrderController {

    @Autowired
    private EmployeeWorkOrderRepository employeeWorkOrderRepository;

    @Autowired
    private EmployeeWorkOrderService employeeWorkOrderService;

    @Autowired
    private WorkOrderReportService workOrderReportService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientDueAgingService clientDueAgingService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.service.ExportService exportService;

    private boolean checkAccess(Authentication authentication) {
        if (authentication == null) return false;
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;
        Optional<Employee> empOpt = employeeRepository.findById(authentication.getName());
        return empOpt.isPresent() && empOpt.get().isCanAccessWorkOrders();
    }

    private Employee getLoggedEmployee(Authentication authentication) {
        if (authentication == null) return null;
        return employeeRepository.findById(authentication.getName()).orElse(null);
    }

    @GetMapping
    public String listWorkOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean clientInvoicePaid,
            @RequestParam(required = false) Boolean contractorInvoicePaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String contractor,
            @RequestParam(required = false) String dueBucket,
            @RequestParam(required = false) Integer series,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            Model model) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        // Calculate Aging Summary on all current orders
        List<EmployeeWorkOrder> allOrders = employeeWorkOrderRepository.findAll();
        AgingSummaryDTO agingSummary = clientDueAgingService.calculateAgingSummary(allOrders);
        model.addAttribute("agingSummary", agingSummary);

        // Load configs for bucket mapping
        ClientDueConfig defaultConfig = clientDueAgingService.getDefaultConfig();
        Map<String, ClientDueConfig> configMap = clientDueAgingService.getConfigMap();

        Specification<EmployeeWorkOrder> spec = EmployeeWorkOrderSpecifications.withFilters(
                status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, client, contractor, series);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<EmployeeWorkOrder> workOrders;

        // If dueBucket filter is active, filter based on client-specific aging calculations
        if (dueBucket != null && !dueBucket.trim().isEmpty()) {
            List<EmployeeWorkOrder> matching = employeeWorkOrderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
            List<EmployeeWorkOrder> filteredList = matching.stream().filter(wo -> {
                if (!clientDueAgingService.isUnpaid(wo)) {
                    return false;
                }
                String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);
                if ("all_unpaid".equalsIgnoreCase(dueBucket)) {
                    return true;
                }
                return dueBucket.equalsIgnoreCase(bucket);
            }).collect(Collectors.toList());

            int start = Math.min((int) pageable.getOffset(), filteredList.size());
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            workOrders = new PageImpl<>(filteredList.subList(start, end), pageable, filteredList.size());
        } else {
            workOrders = employeeWorkOrderRepository.findAll(spec, pageable);
        }

        String filterName = "All Employee Work Orders";
        if (dueBucket != null && !dueBucket.trim().isEmpty()) {
            switch (dueBucket.toLowerCase()) {
                case "critical":
                    filterName = "🔴 Critical Delinquent (60+ Days Due)";
                    break;
                case "overdue":
                    filterName = "🟠 Past Due (50–59 Days Due)";
                    break;
                case "standard":
                    filterName = "🟡 Standard Due (40–49 Days Due)";
                    break;
                case "within_terms":
                    filterName = "🟢 Within Terms (<40 Days Due)";
                    break;
                case "all_unpaid":
                    filterName = "📋 All Unpaid Work Orders";
                    break;
            }
        } else if (status != null && !status.isEmpty()) {
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

        if (series != null) {
            filterName = "📁 Series " + series + " (" + series + "–" + (series + 99) + ")";
        }

        if (search != null && !search.isEmpty()) {
            filterName += " | Search: " + search;
        }

        model.addAttribute("workOrders", workOrders);
        model.addAttribute("activeLink", "employee-work-orders");
        model.addAttribute("currentFilter", filterName);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", workOrders.getTotalPages());
        model.addAttribute("totalItems", workOrders.getTotalElements());
        model.addAttribute("size", size);

        model.addAttribute("status", status);
        model.addAttribute("clientInvoicePaid", clientInvoicePaid);
        model.addAttribute("contractorInvoicePaid", contractorInvoicePaid);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("dueBucket", dueBucket);
        model.addAttribute("series", series);

        model.addAttribute("search", search);
        model.addAttribute("workType", workType);
        model.addAttribute("client", client);
        model.addAttribute("contractor", contractor);

        // All active clients for dropdown in config modal
        List<Client> clients = clientRepository.findAll();
        model.addAttribute("allClients", clients);

        // Pass aging service and config map for row-level badge calculation
        model.addAttribute("agingService", clientDueAgingService);
        model.addAttribute("configMap", configMap);
        model.addAttribute("defaultConfig", defaultConfig);

        return "employee/work-order/list";
    }

    @PostMapping("/aging-config")
    public String saveAgingConfig(
            @RequestParam(required = false) String clientIdentifier,
            @RequestParam(required = false) String clientName,
            @RequestParam int normalDueDays,
            @RequestParam int overdueDays,
            @RequestParam int criticalDueDays,
            Authentication authentication) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        try {
            clientDueAgingService.saveOrUpdateConfig(clientIdentifier, clientName, normalDueDays, overdueDays, criticalDueDays, authentication.getName());
            return "redirect:/employee/work-orders?success=config_saved";
        } catch (IllegalArgumentException e) {
            return "redirect:/employee/work-orders?error=invalid_thresholds";
        }
    }

    @PostMapping("/aging-config/{id}/delete")
    public String deleteAgingConfig(@PathVariable Long id, Authentication authentication) {
        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        clientDueAgingService.deleteConfig(id);
        return "redirect:/employee/work-orders?success=config_deleted";
    }

    @GetMapping("/report")
    public String generateReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean clientInvoicePaid,
            @RequestParam(required = false) Boolean contractorInvoicePaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String contractor,
            @RequestParam(required = false) Integer series,
            Authentication authentication,
            Model model) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        Specification<EmployeeWorkOrder> spec = EmployeeWorkOrderSpecifications.withFilters(
                status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, client, contractor, series);

        List<EmployeeWorkOrder> reportData = employeeWorkOrderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        WorkOrderDashboardDTO stats = workOrderReportService.calculateEmployeeStatistics(reportData);

        String reportTitle = "Employee Work Order Report";
        if (series != null) {
            reportTitle += " - Series " + series + " (" + series + "–" + (series + 99) + ")";
        }
        if (startDate != null && endDate != null) {
            reportTitle += " (" + startDate + " - " + endDate + ")";
        }

        model.addAttribute("stats", stats);
        model.addAttribute("reportData", reportData);
        model.addAttribute("reportTitle", reportTitle);
        model.addAttribute("series", series);
        model.addAttribute("generatedDate", java.time.LocalDateTime.now());
        model.addAttribute("activeLink", "employee-work-orders");

        return "employee/work-order/report";
    }

    @GetMapping("/dashboard")
    public String workOrderDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication,
            Model model) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        List<EmployeeWorkOrder> allWorkOrders;
        if (startDate != null && endDate != null) {
            allWorkOrders = employeeWorkOrderRepository.findByDateReceivedBetween(startDate, endDate);
        } else {
            allWorkOrders = employeeWorkOrderRepository.findAll();
        }

        WorkOrderDashboardDTO stats = workOrderReportService.calculateEmployeeStatistics(allWorkOrders);

        model.addAttribute("stats", stats);
        model.addAttribute("activeLink", "employee-work-orders");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "employee/work-order/dashboard";
    }

    @PostMapping("/import")
    public String importWorkOrders(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        if (file.isEmpty()) {
            return "redirect:/employee/work-orders?error=emptyfile";
        }

        Employee employee = getLoggedEmployee(authentication);
        employeeWorkOrderService.importWorkOrders(file.getInputStream(), employee);

        return "redirect:/employee/work-orders?success=import";
    }

    @GetMapping("/aging/export")
    public org.springframework.http.ResponseEntity<byte[]> exportAgingPortfolio(
            @RequestParam(defaultValue = "excel") String format,
            Authentication authentication) throws IOException {

        if (!checkAccess(authentication)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        List<EmployeeWorkOrder> allOrders = employeeWorkOrderRepository.findAll();
        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(allOrders);

        byte[] data;
        String filename;
        String contentType;

        if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportClientAgingCsv(summary);
            filename = "AR_Aging_Portfolio_" + LocalDate.now() + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else {
            data = exportService.exportClientAgingExcel(summary);
            filename = "AR_Aging_Portfolio_" + LocalDate.now() + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    @GetMapping("/aging/export-series")
    public org.springframework.http.ResponseEntity<byte[]> exportSeriesAgingPortfolio(
            @RequestParam(defaultValue = "excel") String format,
            Authentication authentication) throws IOException {

        if (!checkAccess(authentication)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        List<EmployeeWorkOrder> allOrders = employeeWorkOrderRepository.findAll();
        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(allOrders);

        byte[] data;
        String filename;
        String contentType;

        if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportSeriesAgingCsv(summary);
            filename = "AR_Series_Aging_Portfolio_" + LocalDate.now() + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else {
            data = exportService.exportSeriesAgingExcel(summary);
            filename = "AR_Series_Aging_Portfolio_" + LocalDate.now() + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportFilteredWorkOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean clientInvoicePaid,
            @RequestParam(required = false) Boolean contractorInvoicePaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String contractor,
            @RequestParam(required = false) String dueBucket,
            @RequestParam(required = false) Integer series,
            @RequestParam(defaultValue = "excel") String format,
            Authentication authentication) throws IOException {

        if (!checkAccess(authentication)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        Specification<EmployeeWorkOrder> spec = EmployeeWorkOrderSpecifications.withFilters(
                status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, client, contractor, series);

        List<EmployeeWorkOrder> orders = employeeWorkOrderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "invoiceDate"));

        if (dueBucket != null && !dueBucket.trim().isEmpty()) {
            orders = clientDueAgingService.filterOrdersByDueBucket(orders, dueBucket.trim());
        }

        byte[] data;
        String filename;
        String contentType;

        String prefix = "Work_Orders";
        if (series != null) {
            prefix = "Series_" + series + "_Orders";
        } else if (dueBucket != null && !dueBucket.trim().isEmpty()) {
            prefix = "Due_Orders_" + dueBucket.trim();
        }

        if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportDueWorkOrdersCsv(orders);
            filename = prefix + "_" + LocalDate.now() + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else {
            data = exportService.exportDueWorkOrdersExcel(orders);
            filename = prefix + "_" + LocalDate.now() + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }
}
