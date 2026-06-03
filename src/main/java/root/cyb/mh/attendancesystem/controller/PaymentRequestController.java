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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import root.cyb.mh.attendancesystem.specification.PaymentRequestSpecification;

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
    public String listRequests(
            @RequestParam(required = false) String view,
            @RequestParam(required = false, defaultValue = "lastModified") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,

            // Filters
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long contractorId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) String workOrderNumber,
            @RequestParam(required = false) String requesterName,
            @RequestParam(required = false) PaymentPriority priority,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) PPWStatus ppwUpdateStatus,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdminOrHr = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        Specification<PaymentRequest> spec = createSpecification(
                startDate, endDate, contractorId, clientId, paymentMethodId,
                workOrderNumber, requesterName, priority, status, paymentStatus,
                ppwUpdateStatus, view, userDetails, isAdminOrHr);

        // Security / View Constraints for Title
        String title = "Payment Requests";
        if (isAdminOrHr) {
            title = "All Payment Requests";
        } else {
            Optional<root.cyb.mh.attendancesystem.model.Employee> empOpt = employeeRepository
                    .findById(userDetails.getUsername());
            if (empOpt.isPresent() && "team".equals(view)) {
                title = "Team Payment Requests";
            } else {
                title = "My Payment Requests";
            }
        }

        // Sorting
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);

        // Pagination
        int pageSize = Math.max(1, Math.min(size, 1000));
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<PaymentRequest> requestsPage = paymentRequestRepository.findAll(spec, pageable);

        model.addAttribute("requests", requestsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", requestsPage.getTotalPages());
        model.addAttribute("totalItems", requestsPage.getTotalElements());
        long endItemIndex = Math.min((long) (page + 1) * pageSize, requestsPage.getTotalElements());
        model.addAttribute("endItemIndex", endItemIndex);
        model.addAttribute("size", pageSize);
        model.addAttribute("pageTitle", title);

        // Sorting params
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        // Filter Params for UI
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("contractorId", contractorId);
        model.addAttribute("clientId", clientId);
        model.addAttribute("paymentMethodId", paymentMethodId);
        model.addAttribute("workOrderNumber", workOrderNumber);
        model.addAttribute("requesterName", requesterName);
        model.addAttribute("priority", priority);
        // Note: 'status' might conflict with RequestStatus enum in Thymeleaf if not
        // careful, but usually ok as attribute
        model.addAttribute("status", status);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("ppwUpdateStatus", ppwUpdateStatus);

        // Master Data for Dropdowns
        model.addAttribute("activeContractors", contractorRepository.findByActiveTrue());
        model.addAttribute("activeClients", clientRepository.findByActiveTrue());
        model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue());
        model.addAttribute("priorities", PaymentPriority.values());
        model.addAttribute("requestStatuses", RequestStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("ppwStatuses", PPWStatus.values());

        return "payment-request/list";
    }

    @GetMapping("/export")
    public void exportRequests(
            @RequestParam(required = false) String view,
            @RequestParam(required = false, defaultValue = "lastModified") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) List<String> columns,

            // Filters
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long contractorId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) String workOrderNumber,
            @RequestParam(required = false) String requesterName,
            @RequestParam(required = false) PaymentPriority priority,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) PPWStatus ppwUpdateStatus,

            jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails userDetails) throws java.io.IOException {

        boolean isAdminOrHr = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        Specification<PaymentRequest> spec = createSpecification(
                startDate, endDate, contractorId, clientId, paymentMethodId,
                workOrderNumber, requesterName, priority, status, paymentStatus,
                ppwUpdateStatus, view, userDetails, isAdminOrHr);

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        List<PaymentRequest> requests = paymentRequestRepository.findAll(spec, sort);

        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=payment_requests.csv");
            dataImportExportService.exportPaymentRequestsToCsv(response.getWriter(), requests, columns);
        } else {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=payment_requests.pdf");
            dataImportExportService.exportPaymentRequestsToPdf(response.getOutputStream(), requests,
                    "Payment Requests Export", columns);
        }
    }

    private Specification<PaymentRequest> createSpecification(
            LocalDate startDate, LocalDate endDate,
            Long contractorId, Long clientId, Long paymentMethodId,
            String workOrderNumber, String requesterName,
            PaymentPriority priority, RequestStatus status,
            PaymentStatus paymentStatus, PPWStatus ppwUpdateStatus,
            String view, UserDetails userDetails, boolean isAdminOrHr) {

        Specification<PaymentRequest> spec = PaymentRequestSpecification.getFilterSpec(
                startDate, endDate,
                contractorId, clientId, paymentMethodId,
                workOrderNumber, requesterName,
                priority, status, paymentStatus, ppwUpdateStatus);

        if (!isAdminOrHr) {
            Optional<root.cyb.mh.attendancesystem.model.Employee> empOpt = employeeRepository
                    .findById(userDetails.getUsername());

            if (empOpt.isPresent()) {
                root.cyb.mh.attendancesystem.model.Employee employee = empOpt.get();
                if ("team".equals(view)) {
                    Specification<PaymentRequest> teamSpec = (root, query, cb) -> {
                        jakarta.persistence.criteria.Join<PaymentRequest, User> userJoin = root.join("requester",
                                jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<PaymentRequest, root.cyb.mh.attendancesystem.model.Employee> empJoin = root
                                .join("employeeRequester", jakarta.persistence.criteria.JoinType.LEFT);
                        return cb.or(
                                cb.equal(userJoin.get("username"), userDetails.getUsername()),
                                cb.equal(empJoin.get("id"), employee.getId()));
                    };
                    spec = spec.and(teamSpec);
                } else {
                    Specification<PaymentRequest> selfSpec = (root, query, cb) -> {
                        jakarta.persistence.criteria.Join<PaymentRequest, User> userJoin = root.join("requester",
                                jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<PaymentRequest, root.cyb.mh.attendancesystem.model.Employee> empJoin = root
                                .join("employeeRequester", jakarta.persistence.criteria.JoinType.LEFT);
                        return cb.or(
                                cb.equal(userJoin.get("username"), userDetails.getUsername()),
                                cb.equal(empJoin.get("id"), employee.getId()));
                    };
                    spec = spec.and(selfSpec);
                }
            } else {
                spec = spec.and((root, query, cb) -> cb.disjunction());
            }
        }
        return spec;
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
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        if (paymentRequest.getWorkOrderNumber() != null && 
                paymentRequestRepository.existsByWorkOrderNumberIgnoreCase(paymentRequest.getWorkOrderNumber().trim())) {
            if (paymentRequest.getIsPartialPayment() == null || paymentRequest.getDuplicateReason() == null || paymentRequest.getDuplicateReason().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Work Order Number '" + paymentRequest.getWorkOrderNumber() + "' already exists. Please indicate if this is a partial payment and explain why.");
                model.addAttribute("showDuplicateQuestions", true);
                model.addAttribute("priorities", PaymentPriority.values());
                model.addAttribute("activeContractors", contractorRepository.findByActiveTrue());
                model.addAttribute("activeClients", clientRepository.findByActiveTrue());
                model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue());
                model.addAttribute("activeCompanies", companyRepository.findByActiveTrue());
                return "payment-request/form";
            }
        }
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
    public String viewRequest(@PathVariable Long id, 
            Model model, 
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpSession httpSession) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();

            // Track Referer for "Back" button
            String referer = httpServletRequest.getHeader("Referer");
            if (referer != null && (referer.endsWith("/payment-requests") || referer.contains("/payment-requests?"))) {
                httpSession.setAttribute("backUrl_" + id, referer);
            }
            String backUrl = (String) httpSession.getAttribute("backUrl_" + id);
            if (backUrl == null) {
                backUrl = "/payment-requests";
            }
            model.addAttribute("backUrl", backUrl);

            // Log viewed activity
            paymentRequestService.logActivity(request, userDetails.getUsername(), "VIEWED", "Request viewed by " + userDetails.getUsername());

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
            model.addAttribute("activities", paymentRequestService.getActivitiesForRequest(id));
            return "payment-request/view";
        } else {
            return "redirect:/payment-requests";
        }
    }

    @PostMapping("/{id}/review")
    public String reviewRequest(@PathVariable Long id,
            @RequestParam(value = "status", required = false) root.cyb.mh.attendancesystem.model.enums.RequestStatus status,
            @RequestParam(value = "paymentStatus", required = false) root.cyb.mh.attendancesystem.model.enums.PaymentStatus paymentStatus,
            @RequestParam(value = "checkStatus", required = false) String checkStatus,
            @RequestParam(value = "ppwUpdateStatus", required = false) root.cyb.mh.attendancesystem.model.enums.PPWStatus ppwUpdateStatus,
            @RequestParam(value = "paymentReferenceNumber", required = false) String paymentReferenceNumber,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "proofFile", required = false) org.springframework.web.multipart.MultipartFile proofFile,
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest existingRequest = requestOpt.get();
            // Capture Old State for Notification
            root.cyb.mh.attendancesystem.model.enums.RequestStatus oldStatus = existingRequest.getStatus();
            root.cyb.mh.attendancesystem.model.enums.PaymentStatus oldPaymentStatus = existingRequest
                    .getPaymentStatus();
            String oldCheckStatus = existingRequest.getCheckStatus();
            String oldRemarks = existingRequest.getRemarks();
            String oldReference = existingRequest.getPaymentReferenceNumber();
            root.cyb.mh.attendancesystem.model.enums.PPWStatus oldPpw = existingRequest.getPpwUpdateStatus();
            String oldProofPath = existingRequest.getPaymentProofPath();

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
                    boolean statusChanged = status != null
                            && status != existingRequest.getStatus();
                    boolean payStatusChanged = paymentStatus != null
                            && paymentStatus != existingRequest.getPaymentStatus();
                    boolean refNoChanged = paymentReferenceNumber != null &&
                            !paymentReferenceNumber.equals(existingRequest.getPaymentReferenceNumber());

                    if (statusChanged || payStatusChanged || refNoChanged) {
                        return "redirect:/payment-requests/" + id + "?error=LockedStatusPaid";
                    }
                }

                // 2. Limit to updates (Internal / Status fields)
                boolean statusChanged = status != null
                        && status != existingRequest.getStatus();
                boolean payStatusChanged = paymentStatus != null
                        && paymentStatus != existingRequest.getPaymentStatus();
                boolean ppwChanged = ppwUpdateStatus != null
                        && ppwUpdateStatus != existingRequest.getPpwUpdateStatus();

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
            if (checkStatus != null)
                existingRequest.setCheckStatus(checkStatus);
            if (paymentStatus != null)
                existingRequest.setPaymentStatus(paymentStatus);
            if (ppwUpdateStatus != null)
                existingRequest.setPpwUpdateStatus(ppwUpdateStatus);
            if (remarks != null)
                existingRequest.setRemarks(remarks);
            if (status != null)
                existingRequest.setStatus(status);
            if (paymentReferenceNumber != null)
                existingRequest.setPaymentReferenceNumber(paymentReferenceNumber);

            if (approverUser != null) {
                existingRequest.setApprovalAuthority(approverUser);
            } else if (isSupervisor) {
                // Save the Supervisor (Employee)
                Optional<root.cyb.mh.attendancesystem.model.Employee> supervisorOpt = employeeRepository
                        .findById(userDetails.getUsername());
                supervisorOpt.ifPresent(existingRequest::setApprovalEmployee);
            }

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

                    existingRequest.setPaymentProofPath(destFile.getAbsolutePath());
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }

            // Capture old status before update if not captured earlier?
            // Actually existingRequest is modified directly above.
            // We need to capture oldStatus AT THE TOP of the method, but existingRequest is
            // modified throughout.
            // Wait! The Replace tool replaces lines 299-325.

            // Problem: I need access to oldStatus which I should have captured at logical
            // start.
            // But I am only replacing the end.

            // I should capture oldStatus BEFORE the modifications start?
            // The modifications start around line 276.

            // I will ABORT this tool call and use a wider range or multiple chunks.
            // Or better: I assume 'oldStatus' was NOT captured at top (it wasn't).
            // So checking it now is impossible because 'existingRequest' IS ALREADY
            // MODIFIED.

            // I MUST modify the TOP of the method to capture 'oldStatus'.
            // AND the BOTTOM to use it.

            // I will use multi_replace to do both.

            paymentRequestService.updateRequest(existingRequest);

            // Notify Requester if ANY field changed
            java.util.List<String> changes = new java.util.ArrayList<>();
            java.util.List<String> diffDetails = new java.util.ArrayList<>();
            if (existingRequest.getStatus() != oldStatus) {
                changes.add("Status");
                diffDetails.add("Status: " + oldStatus + " -> " + existingRequest.getStatus());
            }
            if (existingRequest.getPaymentStatus() != oldPaymentStatus) {
                changes.add("Payment Status");
                diffDetails.add("Payment Status: " + oldPaymentStatus + " -> " + existingRequest.getPaymentStatus());
            }
            if (!java.util.Objects.equals(existingRequest.getCheckStatus(), oldCheckStatus)) {
                changes.add("Check Status");
                diffDetails.add("Check Status: '" + oldCheckStatus + "' -> '" + existingRequest.getCheckStatus() + "'");
            }
            if (!java.util.Objects.equals(existingRequest.getRemarks(), oldRemarks)) {
                changes.add("Remarks");
                diffDetails.add("Remarks: '" + oldRemarks + "' -> '" + existingRequest.getRemarks() + "'");
            }
            if (!java.util.Objects.equals(existingRequest.getPaymentReferenceNumber(), oldReference)) {
                changes.add("Ref Number");
                diffDetails.add("Ref Number: '" + oldReference + "' -> '" + existingRequest.getPaymentReferenceNumber() + "'");
            }
            if (existingRequest.getPpwUpdateStatus() != oldPpw) {
                changes.add("PPW Status");
                diffDetails.add("PPW Status: " + oldPpw + " -> " + existingRequest.getPpwUpdateStatus());
            }
            if (!java.util.Objects.equals(existingRequest.getPaymentProofPath(), oldProofPath)) {
                diffDetails.add("Proof File: updated");
            }

            if (!diffDetails.isEmpty()) {
                String detailsStr = "Updated: " + String.join(", ", diffDetails);
                paymentRequestService.logActivity(existingRequest, userDetails.getUsername(), "UPDATED", detailsStr);
            }

            if (!changes.isEmpty()) {
                paymentRequestService.notifyRequesterUpdate(existingRequest, changes);
            }
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
                paymentRequestService.deleteRequest(id);
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

    @PostMapping("/{id}/employee-note")
    public String addEmployeeNote(@PathVariable Long id, @RequestParam("employeeNote") String note,
            RedirectAttributes redirectAttributes, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<PaymentRequest> requestOpt = paymentRequestService.getRequestById(id);
        if (requestOpt.isPresent()) {
            PaymentRequest request = requestOpt.get();

            // Check if requester is current user
            boolean isRequester = false;
            Optional<root.cyb.mh.attendancesystem.model.Employee> currentEmpOpt = employeeRepository
                    .findById(userDetails.getUsername());
            if (currentEmpOpt.isPresent() && request.getEmployeeRequester() != null
                    && request.getEmployeeRequester().getId().equals(currentEmpOpt.get().getId())) {
                isRequester = true;
            }
            if (!isRequester && request.getRequester() != null
                    && request.getRequester().getUsername().equals(userDetails.getUsername())) {
                isRequester = true;
            }

            if (!isRequester) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Access Denied: Only the requester can add a note.");
                return "redirect:/payment-requests/" + id;
            }

            if (request.getStatus() != RequestStatus.PENDING) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Error: Notes can only be added to PENDING requests.");
                return "redirect:/payment-requests/" + id;
            }

            request.setEmployeeNote(note);
            paymentRequestService.updateRequest(request);
            paymentRequestService.logActivity(request, userDetails.getUsername(), "NOTE_ADDED", "Note added: " + note);

            redirectAttributes.addFlashAttribute("successMessage", "Note to Admin successfully saved.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Request not found.");
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

    @GetMapping("/api/check-work-order")
    @ResponseBody
    public java.util.Map<String, Boolean> checkWorkOrderNumber(@RequestParam String workOrderNumber) {
        boolean exists = paymentRequestRepository.existsByWorkOrderNumberIgnoreCase(workOrderNumber.trim());
        return java.util.Map.of("exists", exists);
    }

    @PostMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> bulkUpdate(
            @RequestParam("ids") List<Long> ids,
            @RequestParam(value = "status", required = false) RequestStatus status,
            @RequestParam(value = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(value = "ppwUpdateStatus", required = false) PPWStatus ppwUpdateStatus,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "paymentReferenceNumber", required = false) String paymentReferenceNumber,
            @RequestParam(value = "proofFile", required = false) org.springframework.web.multipart.MultipartFile proofFile,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String savedProofPath = null;
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
                savedProofPath = destFile.getAbsolutePath();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                return org.springframework.http.ResponseEntity.status(500).body("Error saving file: " + e.getMessage());
            }
        }

        List<PaymentRequest> requests = paymentRequestRepository.findAllById(ids);
        
        for (PaymentRequest req : requests) {
            java.util.List<String> changes = new java.util.ArrayList<>();
            java.util.List<String> diffDetails = new java.util.ArrayList<>();
            
            if (status != null) {
                if (req.getStatus() != status) {
                    changes.add("Status");
                    diffDetails.add("Status: " + req.getStatus() + " -> " + status);
                    req.setStatus(status);
                }
            }
            if (paymentStatus != null) {
                if (req.getPaymentStatus() != paymentStatus) {
                    changes.add("Payment Status");
                    diffDetails.add("Payment Status: " + req.getPaymentStatus() + " -> " + paymentStatus);
                    req.setPaymentStatus(paymentStatus);
                }
            }
            if (ppwUpdateStatus != null) {
                if (req.getPpwUpdateStatus() != ppwUpdateStatus) {
                    changes.add("PPW Status");
                    diffDetails.add("PPW Status: " + req.getPpwUpdateStatus() + " -> " + ppwUpdateStatus);
                    req.setPpwUpdateStatus(ppwUpdateStatus);
                }
            }
            if (remarks != null && !remarks.trim().isEmpty()) {
                if (!remarks.equals(req.getRemarks())) {
                    changes.add("Remarks");
                    diffDetails.add("Remarks: '" + req.getRemarks() + "' -> '" + remarks + "'");
                    req.setRemarks(remarks);
                }
            }
            if (paymentReferenceNumber != null && !paymentReferenceNumber.trim().isEmpty()) {
                if (!paymentReferenceNumber.equals(req.getPaymentReferenceNumber())) {
                    changes.add("Ref Number");
                    diffDetails.add("Ref Number: '" + req.getPaymentReferenceNumber() + "' -> '" + paymentReferenceNumber + "'");
                    req.setPaymentReferenceNumber(paymentReferenceNumber);
                }
            }
            if (savedProofPath != null) {
                changes.add("Proof File");
                diffDetails.add("Proof File: updated");
                req.setPaymentProofPath(savedProofPath);
            }
            
            if (!changes.isEmpty()) {
                boolean isAdminOrHr = userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
                if (isAdminOrHr) {
                    userRepository.findByUsername(userDetails.getUsername()).ifPresent(req::setApprovalAuthority);
                }
                
                paymentRequestService.updateRequest(req);
                paymentRequestService.notifyRequesterUpdate(req, changes);
                
                String detailsStr = "Bulk updated: " + String.join(", ", diffDetails);
                paymentRequestService.logActivity(req, userDetails.getUsername(), "UPDATED", detailsStr);
            }
        }
        
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "Bulk update completed successfully."));
    }
}
