package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;
import root.cyb.mh.attendancesystem.model.enums.PPWStatus;
import root.cyb.mh.attendancesystem.model.enums.PaymentPriority;
import root.cyb.mh.attendancesystem.model.enums.PaymentStatus;
import root.cyb.mh.attendancesystem.model.enums.RequestStatus;
import root.cyb.mh.attendancesystem.repository.UserRepository;
import root.cyb.mh.attendancesystem.service.PaymentRequestService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/payment-requests")
public class PaymentRequestController {

    @Autowired
    private PaymentRequestService paymentRequestService;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.ContractorRepository contractorRepository;
    @Autowired
    private root.cyb.mh.attendancesystem.repository.ClientRepository clientRepository;
    @Autowired
    private root.cyb.mh.attendancesystem.repository.PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository;

    @GetMapping
    public String listRequests(@RequestParam(required = false) String view,
            @RequestParam(required = false, defaultValue = "lastModified") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdminOrHr = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        List<PaymentRequest> requests;
        String title = "Payment Requests";

        if (isAdminOrHr) {
            requests = paymentRequestService.getAllRequests();
            title = "All Payment Requests";
        } else {
            Optional<root.cyb.mh.attendancesystem.model.Employee> empOpt = employeeRepository
                    .findById(userDetails.getUsername());
            if (empOpt.isPresent()) {
                root.cyb.mh.attendancesystem.model.Employee employee = empOpt.get();
                if ("team".equals(view)) {
                    requests = paymentRequestService.getTeamRequests(employee);
                    title = "Team Payment Requests";
                    model.addAttribute("isTeamView", true);
                } else {
                    requests = paymentRequestService.getRequestsByRequester(employee);
                    title = "My Payment Requests";
                }
            } else {
                requests = List.of();
            }
        }

        // Apply Sorting
        paymentRequestService.sortRequests(requests, sortField, sortDir);

        model.addAttribute("requests", requests);
        model.addAttribute("pageTitle", title);

        // Sorting params for UI
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "payment-request/list";
    }

    @GetMapping("/new")
    public String newRequestForm(Model model) {
        model.addAttribute("paymentRequest", new PaymentRequest());
        model.addAttribute("priorities", PaymentPriority.values());

        // Master Data
        model.addAttribute("activeContractors", contractorRepository.findByActiveTrue());
        model.addAttribute("activeClients", clientRepository.findByActiveTrue());
        model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue());

        return "payment-request/form";
    }

    @PostMapping
    public String submitRequest(@ModelAttribute PaymentRequest paymentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        // 1. Try User
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            paymentRequestService.createRequest(paymentRequest, userOpt.get());
            return "redirect:/payment-requests";
        }

        // 2. Try Employee
        Optional<root.cyb.mh.attendancesystem.model.Employee> employeeOpt = employeeRepository.findById(username);
        if (employeeOpt.isPresent()) {
            paymentRequestService.createRequest(paymentRequest, employeeOpt.get());
            return "redirect:/payment-requests";
        }

        System.err.println("CRITICAL: User not found in database during submission: " + username);
        return "redirect:/login?logout";
    }

    @GetMapping("/{id}")
    public String viewRequest(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();

            // Check if Admin/HR
            boolean isAdminOrHr = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

            // Check if current user is the supervisor of the requester
            boolean isSupervisor = false;
            if (!isAdminOrHr) {
                Optional<root.cyb.mh.attendancesystem.model.Employee> currentEmpOpt = employeeRepository
                        .findById(userDetails.getUsername());
                if (currentEmpOpt.isPresent() && request.getEmployeeRequester() != null) {
                    root.cyb.mh.attendancesystem.model.Employee currentEmp = currentEmpOpt.get();
                    root.cyb.mh.attendancesystem.model.Employee requesterEmp = request.getEmployeeRequester();
                    // Check hierarchy
                    isSupervisor = (requesterEmp.getReportsTo() != null
                            && requesterEmp.getReportsTo().getId().equals(currentEmp.getId())) ||
                            (requesterEmp.getReportsToAssistant() != null
                                    && requesterEmp.getReportsToAssistant().getId().equals(currentEmp.getId()));
                }
            }

            boolean canReview = isAdminOrHr || isSupervisor;

            // Auto-update Check Status Logic
            if (canReview && (request.getCheckStatus() == null || request.getCheckStatus().isEmpty())) {
                String checkedBy = "Checked by " + userDetails.getUsername() + " on " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                request.setCheckStatus(checkedBy);
                paymentRequestService.updateRequest(request);
            }

            model.addAttribute("paymentRequest", request);
            model.addAttribute("paymentStatuses", PaymentStatus.values());
            model.addAttribute("requestStatuses", RequestStatus.values());
            model.addAttribute("ppwStatuses", PPWStatus.values());
            model.addAttribute("canReview", canReview);
            return "payment-request/view";
        } else {
            return "redirect:/payment-requests";
        }
    }

    @PostMapping("/{id}/review")
    public String reviewRequest(@PathVariable Long id,
            @ModelAttribute PaymentRequest formData,
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest existingRequest = requestOpt.get();

            boolean isAdminOrHr = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

            boolean isSupervisor = false;
            User approverUser = null; // If admin/hr

            if (isAdminOrHr) {
                Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
                if (userOpt.isPresent())
                    approverUser = userOpt.get();
            } else {
                // Check if supervisor
                Optional<root.cyb.mh.attendancesystem.model.Employee> currentEmpOpt = employeeRepository
                        .findById(userDetails.getUsername());
                if (currentEmpOpt.isPresent() && existingRequest.getEmployeeRequester() != null) {
                    root.cyb.mh.attendancesystem.model.Employee currentEmp = currentEmpOpt.get();
                    root.cyb.mh.attendancesystem.model.Employee requesterEmp = existingRequest.getEmployeeRequester();
                    if ((requesterEmp.getReportsTo() != null
                            && requesterEmp.getReportsTo().getId().equals(currentEmp.getId())) ||
                            (requesterEmp.getReportsToAssistant() != null
                                    && requesterEmp.getReportsToAssistant().getId().equals(currentEmp.getId()))) {
                        isSupervisor = true;
                    }
                }
            }

            if (!isAdminOrHr && !isSupervisor) {
                return "redirect:/access-denied";
            }

            // Update fields allowed for editing during review
            existingRequest.setCheckStatus(formData.getCheckStatus());
            existingRequest.setPaymentStatus(formData.getPaymentStatus());
            existingRequest.setPpwUpdateStatus(formData.getPpwUpdateStatus());
            existingRequest.setRemarks(formData.getRemarks());
            existingRequest.setStatus(formData.getStatus());

            if (approverUser != null) {
                existingRequest.setApprovalAuthority(approverUser);
            } else if (isSupervisor) {
                // Save the Supervisor (Employee)
                Optional<root.cyb.mh.attendancesystem.model.Employee> supervisorOpt = employeeRepository
                        .findById(userDetails.getUsername());
                supervisorOpt.ifPresent(existingRequest::setApprovalEmployee);
            }

            paymentRequestService.updateRequest(existingRequest);
        }
        return "redirect:/payment-requests/" + id;
    }
}
