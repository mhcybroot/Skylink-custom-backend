package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            Model model) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        Specification<EmployeeWorkOrder> spec = EmployeeWorkOrderSpecifications.withFilters(
                status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, client, contractor);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<EmployeeWorkOrder> workOrders = employeeWorkOrderRepository.findAll(spec, pageable);

        String filterName = "All Employee Work Orders";
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

        model.addAttribute("search", search);
        model.addAttribute("workType", workType);
        model.addAttribute("client", client);
        model.addAttribute("contractor", contractor);

        return "employee/work-order/list";
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
            Authentication authentication,
            Model model) {

        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        Specification<EmployeeWorkOrder> spec = EmployeeWorkOrderSpecifications.withFilters(
                status, clientInvoicePaid, contractorInvoicePaid, startDate, endDate, search, workType, client, contractor);

        List<EmployeeWorkOrder> reportData = employeeWorkOrderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        WorkOrderDashboardDTO stats = workOrderReportService.calculateEmployeeStatistics(reportData);

        String reportTitle = "Employee Work Order Report";
        if (startDate != null && endDate != null) {
            reportTitle += " (" + startDate + " - " + endDate + ")";
        }

        model.addAttribute("stats", stats);
        model.addAttribute("reportData", reportData);
        model.addAttribute("reportTitle", reportTitle);
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
}
