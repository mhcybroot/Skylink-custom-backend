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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/payment-requests")
public class PaymentRequestController {

    @Autowired
    private PaymentRequestService paymentRequestService;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

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

    @Autowired
    private root.cyb.mh.attendancesystem.service.DataImportExportService dataImportExportService;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.CompanyRepository companyRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.service.SystemSettingService systemSettingService;

    @Autowired
    private root.cyb.mh.attendancesystem.service.EmailService emailService;

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
        model.addAttribute("activeCompanies", companyRepository.findByActiveTrue());

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

            String limitStr = systemSettingService.getValue("PAYMENT_REVIEW_UPDATE_LIMIT", "3");
            model.addAttribute("reviewUpdateLimit", Integer.parseInt(limitStr));

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
            @RequestParam(value = "proofFile", required = false) org.springframework.web.multipart.MultipartFile proofFile,
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

            // --- RESTRICTION LOGIC ---
            boolean isPaid = existingRequest.getPaymentStatus() == PaymentStatus.PAID;
            boolean isHR = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_HR"));
            boolean isRestrictedUser = isHR || isSupervisor; // Non-Admin

            if (isRestrictedUser) {
                // 1. Lock if PAID
                if (isPaid) {
                    // Prevent changing major fields
                    boolean statusChanged = formData.getStatus() != null
                            && formData.getStatus() != existingRequest.getStatus();
                    boolean payStatusChanged = formData.getPaymentStatus() != null
                            && formData.getPaymentStatus() != existingRequest.getPaymentStatus();
                    boolean refNoChanged = formData.getPaymentReferenceNumber() != null &&
                            !formData.getPaymentReferenceNumber().equals(existingRequest.getPaymentReferenceNumber());

                    if (statusChanged || payStatusChanged || refNoChanged) {
                        return "redirect:/payment-requests/" + id + "?error=LockedStatusPaid";
                    }
                }

                // 2. Limit to updates (Internal / Status fields)
                boolean statusChanged = formData.getStatus() != null
                        && formData.getStatus() != existingRequest.getStatus();
                boolean payStatusChanged = formData.getPaymentStatus() != null
                        && formData.getPaymentStatus() != existingRequest.getPaymentStatus();
                boolean ppwChanged = formData.getPpwUpdateStatus() != null
                        && formData.getPpwUpdateStatus() != existingRequest.getPpwUpdateStatus();

                if (statusChanged || payStatusChanged || ppwChanged) {
                    String limitStr = systemSettingService.getValue("PAYMENT_REVIEW_UPDATE_LIMIT", "3");
                    int maxUpdates = Integer.parseInt(limitStr);
                    int currentCount = (existingRequest.getReviewUpdateCount() != null)
                            ? existingRequest.getReviewUpdateCount()
                            : 0;
                    if (currentCount >= maxUpdates) {
                        return "redirect:/payment-requests/" + id + "?error=UpdateLimitReached";
                    }
                    existingRequest.setReviewUpdateCount(currentCount + 1);
                }
            }
            // --- END RESTRICTION LOGIC ---

            // Update fields allowed for editing during review
            if (formData.getCheckStatus() != null)
                existingRequest.setCheckStatus(formData.getCheckStatus());
            if (formData.getPaymentStatus() != null)
                existingRequest.setPaymentStatus(formData.getPaymentStatus());
            if (formData.getPpwUpdateStatus() != null)
                existingRequest.setPpwUpdateStatus(formData.getPpwUpdateStatus());
            if (formData.getRemarks() != null)
                existingRequest.setRemarks(formData.getRemarks());
            if (formData.getStatus() != null)
                existingRequest.setStatus(formData.getStatus());
            if (formData.getPaymentReferenceNumber() != null)
                existingRequest.setPaymentReferenceNumber(formData.getPaymentReferenceNumber());

            if (approverUser != null) {
                existingRequest.setApprovalAuthority(approverUser);
            } else if (isSupervisor) {
                // Save the Supervisor (Employee)
                Optional<root.cyb.mh.attendancesystem.model.Employee> supervisorOpt = employeeRepository
                        .findById(userDetails.getUsername());
                supervisorOpt.ifPresent(existingRequest::setApprovalEmployee);
            }

            // Handle Proof File Upload
            if (proofFile != null && !proofFile.isEmpty()) {
                try {
                    String baseDir = System.getProperty("user.dir");
                    String uploadDir = baseDir + java.io.File.separator + "uploads" + java.io.File.separator + "proofs"
                            + java.io.File.separator;

                    java.io.File directory = new java.io.File(uploadDir);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    String fileName = System.currentTimeMillis() + "_" + proofFile.getOriginalFilename();
                    java.io.File destFile = new java.io.File(uploadDir + fileName);
                    proofFile.transferTo(destFile);

                    // Save relative path or absolute?
                    // Saving absolute path makes it easier to load later without worrying about
                    // working dir changes.
                    // But for portability, maybe relative is better.
                    // Let's save the absolute path since the controller uses Paths.get(path) later.
                    existingRequest.setPaymentProofPath(destFile.getAbsolutePath());
                } catch (java.io.IOException e) {
                    e.printStackTrace(); // In prod, log this
                }
            }

            paymentRequestService.updateRequest(existingRequest);
        }
        return "redirect:/payment-requests/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();
            if (request.getStatus() == root.cyb.mh.attendancesystem.model.enums.RequestStatus.REJECTED) {
                paymentRequestRepository.delete(request); // Using repository directly since service might not have
                                                          // delete
                redirectAttributes.addFlashAttribute("successMessage", "Payment request deleted successfully.");
                return "redirect:/payment-requests";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Only REJECTED requests can be deleted.");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Request not found.");
        }
        return "redirect:/payment-requests/" + id;
    }

    @GetMapping("/{id}/invoice")
    public void downloadInvoice(@PathVariable Long id,
            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails userDetails) throws java.io.IOException {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();

            // Check permissions
            boolean isAdminOrHr = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

            boolean isRequester = false;
            // Check if requester
            Optional<root.cyb.mh.attendancesystem.model.Employee> currentEmpOpt = employeeRepository
                    .findById(userDetails.getUsername());
            if (currentEmpOpt.isPresent() && request.getEmployeeRequester() != null
                    && request.getEmployeeRequester().getId().equals(currentEmpOpt.get().getId())) {
                isRequester = true;
            }
            // Also user requester
            if (request.getRequester() != null
                    && request.getRequester().getUsername().equals(userDetails.getUsername())) {
                isRequester = true;
            }

            if (!isAdminOrHr && !isRequester) {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                return;
            }

            // Check Status
            if (request.getPaymentStatus() != PaymentStatus.PAID) {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST,
                        "Invoice available only for PAID requests");
                return;
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"invoice_" + id + ".pdf\"");

            dataImportExportService.generateInvoicePdf(response.getOutputStream(), request);
        }
    }

    @PostMapping("/{id}/send-email")
    public String sendInvoiceEmail(@PathVariable Long id, @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
            if (requestOpt.isPresent()) {
                PaymentRequest request = requestOpt.get();
                emailService.sendInvoiceEmail(email, request);

                // Update specific fields without triggering full entity validation if possible,
                // or just save
                request.setLastEmailSentAt(java.time.LocalDateTime.now());
                request.setLastEmailSentTo(email);
                paymentRequestRepository.save(request);

                redirectAttributes.addFlashAttribute("successMessage", "Invoice sent successfully to " + email);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Request not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending email: " + e.getMessage());
        }
        return "redirect:/payment-requests/" + id;
    }

    @GetMapping("/{id}/proof")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> viewProof(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();
            // Basic Access Check (same as view)
            if (request.getPaymentProofPath() != null) {
                try {
                    java.nio.file.Path file = java.nio.file.Paths.get(request.getPaymentProofPath());
                    org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(
                            file.toUri());
                    if (resource.exists() || resource.isReadable()) {
                        String contentType = "application/octet-stream"; // Default
                        // Try to determine content type
                        try {
                            contentType = java.nio.file.Files.probeContentType(file);
                        } catch (Exception ex) {
                        }
                        if (contentType == null)
                            contentType = "application/octet-stream";

                        return org.springframework.http.ResponseEntity.ok()
                                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" + resource.getFilename() + "\"")
                                .body(resource);
                    }
                } catch (java.net.MalformedURLException e) {
                    // Log
                }
            }
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }
}
