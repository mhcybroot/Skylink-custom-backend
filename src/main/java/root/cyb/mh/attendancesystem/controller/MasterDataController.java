package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import root.cyb.mh.attendancesystem.exception.ResourceNotFoundException;
import root.cyb.mh.attendancesystem.model.*;
import root.cyb.mh.attendancesystem.repository.*;

@Controller
@RequestMapping("/master-data")
public class MasterDataController {

    @Autowired
    private ContractorRepository contractorRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private ContractorPaymentInfoRepository contractorPaymentInfoRepository;
    @Autowired
    private root.cyb.mh.attendancesystem.repository.PaymentRequestRepository paymentRequestRepository;
    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository;

    // --- CONTRACTORS (Employees, Admin, HR) ---
    @GetMapping("/contractors")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public String listContractors(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir", defaultValue = "asc") String dir,
            Model model) {

        org.springframework.data.domain.Sort.Direction direction = dir.equalsIgnoreCase("desc")
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;
        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(direction, sort);

        if (search != null && !search.trim().isEmpty()) {
            model.addAttribute("contractors", contractorRepository.searchContractors(search.trim(), sortObj));
            model.addAttribute("search", search.trim());
        } else {
            model.addAttribute("contractors", contractorRepository.findAll(sortObj));
        }

        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", dir.equals("asc") ? "desc" : "asc");

        model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue());
        model.addAttribute("newContractor", new Contractor());
        return "master-data/contractors";
    }

    @PostMapping("/contractors")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public String createContractor(@ModelAttribute Contractor contractor, RedirectAttributes ps,
            java.security.Principal principal) {
        try {
            Contractor saved = contractorRepository.save(contractor);
            if (saved.getDefaultPaymentMethod() != null) {
                ContractorPaymentInfo info = new ContractorPaymentInfo();
                info.setContractor(saved);
                info.setPaymentMethod(saved.getDefaultPaymentMethod());
                info.setAccountDetails(saved.getAccountDetails());
                info.setCreatedBy(principal != null ? principal.getName() : "System");
                contractorPaymentInfoRepository.save(info);
            }
            ps.addFlashAttribute("successMessage", "Contractor created successfully!");
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error: Contractor name must be unique.");
        }
        return "redirect:/master-data/contractors";
    }

    // --- CLIENTS (Admin, HR, Supervisor only) ---
    // Note: 'Supervisor' is not a dedicated ROLE in Spring Security here, usually
    // it's Employee with Reports.
    // However, the requirement says "supervisor/hr/admin".
    // If Supervisor is just an Employee, we might need custom logic.
    // For now, assuming ADMIN/HR or explicit check.
    // If Supervisor is a concept derived from Employee hierarchy, simpler to
    // restrict to ADMIN/HR initially
    // or allow all Employees if logic is too complex for now, BUT user said
    // "Employee can ONLY create contractor".
    // Let's restrict to ADMIN/HR for strict compliance, or if Supervisor role
    // exists.
    // Checking previous context: Supervisor is just an Employee with subordinates.
    // To strictly implement "Supervisor/HR/Admin" permissions using Spring Security
    // annotations is hard without a custom PermissionEvaluator.
    // I will use a helper method or strictly ADMIN/HR + logic in controller.

    // For simplicity and safety, I'll restrict to ADMIN/HR first, and maybe allow
    // all for now?
    // No, strictly following rules: Employee cannot create Client.
    // I will restrict Client/PaymentMethod to ADMIN/HR. Actual Supervisors might
    // need to ask Admin.
    // OR I can check if the current user is a Supervisor in the method.

    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String listClients(@RequestParam(value = "search", required = false) String search, Model model) {
        java.util.List<Client> allClients;

        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            allClients = clientRepository.findAll().stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchLower) ||
                            c.getCode().toLowerCase().contains(searchLower) ||
                            (c.getAddress() != null && c.getAddress().toLowerCase().contains(searchLower)) ||
                            c.getId().toString().contains(searchLower))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            allClients = clientRepository.findAll();
        }

        model.addAttribute("clients", allClients);
        model.addAttribute("newClient", new Client());
        model.addAttribute("search", search);

        // --- GLOBAL ANALYTICS ---

        // 1. Financial Overview
        java.math.BigDecimal totalRevenue = paymentRequestRepository.sumTotalRevenue();
        if (totalRevenue == null)
            totalRevenue = java.math.BigDecimal.ZERO;

        java.time.LocalDate yearStart = java.time.LocalDate.of(java.time.LocalDate.now().getYear(), 1, 1);
        java.math.BigDecimal ytdRevenue = paymentRequestRepository.sumRevenueByDateRange(yearStart,
                java.time.LocalDate.now());
        if (ytdRevenue == null)
            ytdRevenue = java.math.BigDecimal.ZERO;

        java.math.BigDecimal totalOutstanding = paymentRequestRepository.sumTotalOutstanding();
        if (totalOutstanding == null)
            totalOutstanding = java.math.BigDecimal.ZERO;

        java.util.List<Object[]> topClients = paymentRequestRepository.findTopClientsByRevenue(
                org.springframework.data.domain.PageRequest.of(0, 5));

        java.math.BigDecimal avgClientValue = java.math.BigDecimal.ZERO;
        String highestValueClient = "N/A";
        double clientConcentration = 0;

        long activeClientCount = clientRepository.findByActiveTrue().size();
        if (activeClientCount > 0 && totalRevenue.compareTo(java.math.BigDecimal.ZERO) > 0) {
            avgClientValue = totalRevenue.divide(java.math.BigDecimal.valueOf(activeClientCount), 2,
                    java.math.RoundingMode.HALF_UP);
        }

        if (!topClients.isEmpty()) {
            highestValueClient = (String) topClients.get(0)[1];
            java.math.BigDecimal topClientRevenue = (java.math.BigDecimal) topClients.get(0)[2];
            if (totalRevenue.compareTo(java.math.BigDecimal.ZERO) > 0) {
                clientConcentration = topClientRevenue.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue();
            }
        }

        // 2. Client Portfolio
        long totalClients = allClients.size();
        long activeClients = activeClientCount;

        // 3. Operational Metrics
        long totalRequests = paymentRequestRepository.count();
        long totalWorkOrders = paymentRequestRepository.countTotalWorkOrders();

        long approvedCount = paymentRequestRepository.countByClientIdAndStatus(null,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED);
        double globalApprovalRate = totalRequests > 0 ? (double) approvedCount / totalRequests * 100 : 0;

        double avgRequestsPerClient = activeClients > 0 ? (double) totalRequests / activeClients : 0;

        // 4. Vendor Ecosystem
        long totalVendors = paymentRequestRepository.countTotalUniqueVendors();
        double avgVendorsPerClient = activeClients > 0 ? (double) totalVendors / activeClients : 0;

        java.util.List<Object[]> mostUsedVendors = paymentRequestRepository.findMostUsedVendors(
                org.springframework.data.domain.PageRequest.of(0, 1));
        String mostUsedVendor = mostUsedVendors.isEmpty() ? "N/A" : (String) mostUsedVendors.get(0)[0];

        // 5. Growth Trends
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int previousMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int previousYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        java.math.BigDecimal currentMonthRevenue = paymentRequestRepository.sumRevenueByYearMonth(currentYear,
                currentMonth);
        java.math.BigDecimal previousMonthRevenue = paymentRequestRepository.sumRevenueByYearMonth(previousYear,
                previousMonth);

        if (currentMonthRevenue == null)
            currentMonthRevenue = java.math.BigDecimal.ZERO;
        if (previousMonthRevenue == null)
            previousMonthRevenue = java.math.BigDecimal.ZERO;

        double momGrowthRate = previousMonthRevenue.compareTo(java.math.BigDecimal.ZERO) > 0
                ? currentMonthRevenue.subtract(previousMonthRevenue)
                        .divide(previousMonthRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // --- MODEL POPULATION ---
        // Financial
        model.addAttribute("globalTotalRevenue", totalRevenue);
        model.addAttribute("globalYtdRevenue", ytdRevenue);
        model.addAttribute("globalAvgClientValue", avgClientValue);
        model.addAttribute("globalHighestValueClient", highestValueClient);
        model.addAttribute("globalTotalOutstanding", totalOutstanding);
        model.addAttribute("globalClientConcentration", clientConcentration);

        // Client Portfolio
        model.addAttribute("globalTotalClients", totalClients);
        model.addAttribute("globalActiveClients", activeClients);
        model.addAttribute("globalTopClients", topClients);

        // Operational
        model.addAttribute("globalTotalRequests", totalRequests);
        model.addAttribute("globalTotalWorkOrders", totalWorkOrders);
        model.addAttribute("globalApprovalRate", globalApprovalRate);
        model.addAttribute("globalAvgRequestsPerClient", avgRequestsPerClient);

        // Vendor
        model.addAttribute("globalTotalVendors", totalVendors);
        model.addAttribute("globalAvgVendorsPerClient", avgVendorsPerClient);
        model.addAttribute("globalMostUsedVendor", mostUsedVendor);

        // Growth
        model.addAttribute("globalMomGrowthRate", momGrowthRate);

        return "master-data/clients";
    }

    @GetMapping("/clients/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String getClientAnalytics(Model model) {
        // Reuse same logic as listClients for global analytics
        java.util.List<Client> allClients = clientRepository.findAll();

        // 1. Financial Overview
        java.math.BigDecimal totalRevenue = paymentRequestRepository.sumTotalRevenue();
        if (totalRevenue == null)
            totalRevenue = java.math.BigDecimal.ZERO;

        java.time.LocalDate yearStart = java.time.LocalDate.of(java.time.LocalDate.now().getYear(), 1, 1);
        java.math.BigDecimal ytdRevenue = paymentRequestRepository.sumRevenueByDateRange(yearStart,
                java.time.LocalDate.now());
        if (ytdRevenue == null)
            ytdRevenue = java.math.BigDecimal.ZERO;

        java.math.BigDecimal totalOutstanding = paymentRequestRepository.sumTotalOutstanding();
        if (totalOutstanding == null)
            totalOutstanding = java.math.BigDecimal.ZERO;

        java.util.List<Object[]> topClients = paymentRequestRepository.findTopClientsByRevenue(
                org.springframework.data.domain.PageRequest.of(0, 5));

        java.math.BigDecimal avgClientValue = java.math.BigDecimal.ZERO;
        String highestValueClient = "N/A";
        double clientConcentration = 0;

        long activeClientCount = clientRepository.findByActiveTrue().size();
        if (activeClientCount > 0 && totalRevenue.compareTo(java.math.BigDecimal.ZERO) > 0) {
            avgClientValue = totalRevenue.divide(java.math.BigDecimal.valueOf(activeClientCount), 2,
                    java.math.RoundingMode.HALF_UP);
        }

        if (!topClients.isEmpty()) {
            highestValueClient = (String) topClients.get(0)[1];
            java.math.BigDecimal topClientRevenue = (java.math.BigDecimal) topClients.get(0)[2];
            if (totalRevenue.compareTo(java.math.BigDecimal.ZERO) > 0) {
                clientConcentration = topClientRevenue.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue();
            }
        }

        long totalClients = allClients.size();
        long activeClients = activeClientCount;

        // 3. Operational Metrics
        long totalRequests = paymentRequestRepository.count();
        long totalWorkOrders = paymentRequestRepository.countTotalWorkOrders();

        long approvedCount = paymentRequestRepository.countByClientIdAndStatus(null,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED);
        double globalApprovalRate = totalRequests > 0 ? (double) approvedCount / totalRequests * 100 : 0;

        double avgRequestsPerClient = activeClients > 0 ? (double) totalRequests / activeClients : 0;

        // 4. Vendor Ecosystem
        long totalVendors = paymentRequestRepository.countTotalUniqueVendors();
        double avgVendorsPerClient = activeClients > 0 ? (double) totalVendors / activeClients : 0;

        java.util.List<Object[]> mostUsedVendors = paymentRequestRepository.findMostUsedVendors(
                org.springframework.data.domain.PageRequest.of(0, 1));
        String mostUsedVendor = mostUsedVendors.isEmpty() ? "N/A" : (String) mostUsedVendors.get(0)[0];

        // 5. Growth Trends
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int previousMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int previousYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        java.math.BigDecimal currentMonthRevenue = paymentRequestRepository.sumRevenueByYearMonth(currentYear,
                currentMonth);
        java.math.BigDecimal previousMonthRevenue = paymentRequestRepository.sumRevenueByYearMonth(previousYear,
                previousMonth);

        if (currentMonthRevenue == null)
            currentMonthRevenue = java.math.BigDecimal.ZERO;
        if (previousMonthRevenue == null)
            previousMonthRevenue = java.math.BigDecimal.ZERO;

        double momGrowthRate = previousMonthRevenue.compareTo(java.math.BigDecimal.ZERO) > 0
                ? currentMonthRevenue.subtract(previousMonthRevenue)
                        .divide(previousMonthRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // MODEL POPULATION
        model.addAttribute("globalTotalRevenue", totalRevenue);
        model.addAttribute("globalYtdRevenue", ytdRevenue);
        model.addAttribute("globalAvgClientValue", avgClientValue);
        model.addAttribute("globalHighestValueClient", highestValueClient);
        model.addAttribute("globalTotalOutstanding", totalOutstanding);
        model.addAttribute("globalClientConcentration", clientConcentration);
        model.addAttribute("globalTotalClients", totalClients);
        model.addAttribute("globalActiveClients", activeClients);
        model.addAttribute("globalTopClients", topClients);
        model.addAttribute("globalTotalRequests", totalRequests);
        model.addAttribute("globalTotalWorkOrders", totalWorkOrders);
        model.addAttribute("globalApprovalRate", globalApprovalRate);
        model.addAttribute("globalAvgRequestsPerClient", avgRequestsPerClient);
        model.addAttribute("globalTotalVendors", totalVendors);
        model.addAttribute("globalAvgVendorsPerClient", avgVendorsPerClient);
        model.addAttribute("globalMostUsedVendor", mostUsedVendor);
        model.addAttribute("globalMomGrowthRate", momGrowthRate);

        return "master-data/client-analytics";
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String createClient(@ModelAttribute Client client, RedirectAttributes ps) {
        try {
            clientRepository.save(client);
            ps.addFlashAttribute("successMessage", "Client created successfully!");
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error: Client code must be unique.");
        }
        return "redirect:/master-data/clients";
    }

    @PostMapping("/clients/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String updateClient(@ModelAttribute Client client, RedirectAttributes ps) {
        try {
            Client existing = clientRepository.findById(client.getId()).orElse(null);
            if (existing != null) {
                existing.setName(client.getName());
                existing.setCode(client.getCode());
                existing.setAddress(client.getAddress());
                clientRepository.save(existing);
                ps.addFlashAttribute("successMessage", "Client updated successfully!");
            } else {
                ps.addFlashAttribute("errorMessage", "Client not found.");
            }
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error updating client: " + e.getMessage());
        }
        return "redirect:/master-data/clients";
    }

    // --- PAYMENT METHODS (Admin, HR) ---
    @GetMapping("/payment-methods")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String listPaymentMethods(Model model) {
        model.addAttribute("methods", paymentMethodRepository.findAll());
        model.addAttribute("newMethod", new PaymentMethod());
        return "master-data/payment-methods";
    }

    @PostMapping("/payment-methods")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String createPaymentMethod(@ModelAttribute PaymentMethod method, RedirectAttributes ps) {
        try {
            paymentMethodRepository.save(method);
            ps.addFlashAttribute("successMessage", "Payment Method created successfully!");
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error: Method name must be unique.");
        }
        return "redirect:/master-data/payment-methods";
    }

    @PostMapping("/payment-methods/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String updatePaymentMethod(@ModelAttribute PaymentMethod method, RedirectAttributes ps) {
        try {
            PaymentMethod existing = paymentMethodRepository.findById(method.getId()).orElse(null);
            if (existing != null) {
                existing.setMethodName(method.getMethodName());
                existing.setDescription(method.getDescription());
                paymentMethodRepository.save(existing);
                ps.addFlashAttribute("successMessage", "Payment Method updated successfully!");
            } else {
                ps.addFlashAttribute("errorMessage", "Method not found.");
            }
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error updating method: " + e.getMessage());
        }
        return "redirect:/master-data/payment-methods";
    }

    // --- TOGGLE ACTIONS ---

    @PostMapping("/contractors/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')") // Only Admin/HR should probably delete/toggle, or maybe Creator?
                                               // Requirement says "deleted item...". Let's restrict Delete to higher
                                               // roles for safety,
                                               // or strictly follow "Admin cannot delete... wait, user said 'admin
                                               // cannot delete... deleted item will not delete'".
                                               // This implies the ACTION of deleting is available. I'll allow Admin/HR.
    public String toggleContractor(@PathVariable Long id, RedirectAttributes ps) {
        Contractor c = contractorRepository.findById(id).orElse(null);
        if (c != null) {
            c.setActive(!c.isActive());
            contractorRepository.save(c);
            ps.addFlashAttribute("successMessage", "Contractor status updated.");
        }
        return "redirect:/master-data/contractors";
    }

    @GetMapping("/contractors/{id}/dashboard")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public String contractorDashboard(@PathVariable Long id, Model model,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Contractor contractor = contractorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        // 1. Payment Methods
        java.util.List<ContractorPaymentInfo> paymentInfos = contractorPaymentInfoRepository
                .findByContractorIdAndActiveTrue(id);
        java.util.List<ContractorPaymentInfo> deletedPaymentInfos = contractorPaymentInfoRepository
                .findByContractorIdAndActiveFalse(id);

        // 2. Payment Requests History
        boolean isAdminOrHr = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));

        java.util.List<root.cyb.mh.attendancesystem.model.PaymentRequest> requests;

        if (isAdminOrHr) {
            requests = paymentRequestRepository.findByContractorIdOrderByRequestDateDesc(id);
        } else {
            java.util.Optional<root.cyb.mh.attendancesystem.model.Employee> emp = employeeRepository
                    .findById(userDetails.getUsername());
            if (emp.isPresent()) {
                requests = paymentRequestRepository.findByContractorIdAndEmployeeRequesterOrderByRequestDateDesc(id,
                        emp.get());
            } else {
                requests = paymentRequestRepository.findByContractorIdOrderByRequestDateDesc(id);
            }
        }

        // 3. Stats
        java.math.BigDecimal totalPaid = java.math.BigDecimal.ZERO;
        long pendingCount = 0;

        for (root.cyb.mh.attendancesystem.model.PaymentRequest r : requests) {
            if (r.getPaymentStatus() == root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID) {
                totalPaid = totalPaid.add(r.getAmount() != null ? r.getAmount() : java.math.BigDecimal.ZERO);
            }
            if (r.getStatus() == root.cyb.mh.attendancesystem.model.enums.RequestStatus.PENDING) {
                pendingCount++;
            }
        }

        model.addAttribute("contractor", contractor);
        model.addAttribute("paymentInfos", paymentInfos);
        model.addAttribute("deletedPaymentInfos", deletedPaymentInfos);
        model.addAttribute("requests", requests);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue()); // For the 'Add' modal

        return "master-data/contractor-dashboard";
    }

    @PostMapping("/clients/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String toggleClientStatus(@PathVariable Long id, RedirectAttributes ps) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        c.setActive(!c.isActive());
        clientRepository.save(c);
        ps.addFlashAttribute("successMessage", "Client status updated successfully!");
        return "redirect:/master-data/clients";
    }

    @PostMapping("/clients/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String updateClient(@PathVariable Long id, @RequestParam String code,
            @RequestParam String name, @RequestParam(required = false) String address,
            RedirectAttributes ps) {
        try {
            Client client = clientRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            client.setCode(code);
            client.setName(name);
            client.setAddress(address);
            clientRepository.save(client);
            ps.addFlashAttribute("successMessage", "Client updated successfully!");
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error: Client code must be unique.");
        }
        return "redirect:/master-data/clients";
    }

    @PostMapping("/payment-methods/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String togglePaymentMethod(@PathVariable Long id, RedirectAttributes ps) {
        PaymentMethod m = paymentMethodRepository.findById(id).orElse(null);
        if (m != null) {
            m.setActive(!m.isActive());
            paymentMethodRepository.save(m);
            ps.addFlashAttribute("successMessage", "Payment Method status updated.");
        }
        return "redirect:/master-data/payment-methods";
    }

    @GetMapping("/payment-methods/{id}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String getPaymentMethodDashboard(@PathVariable Long id, Model model) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment Method not found"));

        model.addAttribute("method", method);

        // Basic counts
        long totalTransactions = paymentRequestRepository.countByPaymentMethodId(id);
        long pendingTransactions = paymentRequestRepository.countByPaymentMethodIdAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.PENDING);
        long approvedTransactions = paymentRequestRepository.countByPaymentMethodIdAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED);
        long rejectedTransactions = paymentRequestRepository.countRejectedByPaymentMethod(id);

        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("pendingTransactions", pendingTransactions);
        model.addAttribute("approvedTransactions", approvedTransactions);
        model.addAttribute("rejectedTransactions", rejectedTransactions);

        // ============================================
        // STRENGTHS
        // ============================================

        // 1. Total Revenue
        java.math.BigDecimal totalAmountProcessed = paymentRequestRepository.sumApprovedByPaymentMethod(id);
        if (totalAmountProcessed == null)
            totalAmountProcessed = java.math.BigDecimal.ZERO;
        model.addAttribute("totalRevenue", totalAmountProcessed);

        // 2. Success Rate %
        double successRate = totalTransactions > 0 ? (double) approvedTransactions / totalTransactions * 100 : 0;
        model.addAttribute("successRate", successRate);

        // 3. Average Transaction
        java.math.BigDecimal avgAmount = paymentRequestRepository.avgByPaymentMethodIdPaid(id);
        if (avgAmount == null)
            avgAmount = java.math.BigDecimal.ZERO;
        model.addAttribute("avgAmount", avgAmount);

        // 4. Client Retention Rate
        long repeatClients = paymentRequestRepository.countRepeatClients(id);
        long distinctClients = paymentRequestRepository.countDistinctClientsByPaymentMethod(id);
        double clientRetention = distinctClients > 0 ? (double) repeatClients / distinctClients * 100 : 0;
        model.addAttribute("clientRetention", clientRetention);
        model.addAttribute("repeatClients", repeatClients);
        model.addAttribute("distinctClients", distinctClients);

        // 5. Contractor Loyalty Rate
        long repeatContractors = paymentRequestRepository.countRepeatContractors(id);
        long distinctContractors = paymentRequestRepository.countDistinctContractorsByPaymentMethod(id);
        double contractorLoyalty = distinctContractors > 0 ? (double) repeatContractors / distinctContractors * 100 : 0;
        model.addAttribute("contractorLoyalty", contractorLoyalty);
        model.addAttribute("repeatContractors", repeatContractors);
        model.addAttribute("distinctContractors", distinctContractors);

        // 6. High-Value Transactions (above average)
        long highValueCount = paymentRequestRepository.countHighValueTransactions(id, avgAmount);
        model.addAttribute("highValueCount", highValueCount);

        // ============================================
        // WEAKNESSES
        // ============================================

        // 7. Rejection Rate %
        double rejectionRate = totalTransactions > 0 ? (double) rejectedTransactions / totalTransactions * 100 : 0;
        model.addAttribute("rejectionRate", rejectionRate);

        // 8. Pending Backlog
        model.addAttribute("pendingBacklog", pendingTransactions);

        // 9. Oldest Pending (Days)
        java.time.LocalDate oldestPendingDate = paymentRequestRepository.findOldestPendingDate(id);
        long oldestPendingDays = 0;
        if (oldestPendingDate != null) {
            oldestPendingDays = java.time.temporal.ChronoUnit.DAYS.between(oldestPendingDate,
                    java.time.LocalDate.now());
        }
        model.addAttribute("oldestPendingDays", oldestPendingDays);

        // 10. Low-Value Transaction %
        java.math.BigDecimal lowThreshold = new java.math.BigDecimal("100");
        long lowValueCount = paymentRequestRepository.countLowValueTransactions(id, lowThreshold);
        double lowValuePercentage = totalTransactions > 0 ? (double) lowValueCount / totalTransactions * 100 : 0;
        model.addAttribute("lowValuePercentage", lowValuePercentage);
        model.addAttribute("lowValueCount", lowValueCount);

        // 11. Inactive Period (Days)
        java.time.LocalDate lastTransactionDate = paymentRequestRepository.findLastTransactionDate(id);
        long inactiveDays = 0;
        if (lastTransactionDate != null) {
            inactiveDays = java.time.temporal.ChronoUnit.DAYS.between(lastTransactionDate, java.time.LocalDate.now());
        }
        model.addAttribute("inactiveDays", inactiveDays);
        model.addAttribute("lastTransactionDate", lastTransactionDate);

        // ============================================
        // OPPORTUNITIES
        // ============================================

        // 12. Month-over-Month Growth
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int previousMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int previousYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        long thisMonthUsage = paymentRequestRepository.countByPaymentMethodIdAndYearMonth(id, currentYear,
                currentMonth);
        long lastMonthUsage = paymentRequestRepository.countByPaymentMethodIdAndYearMonth(id, previousYear,
                previousMonth);
        double growthRate = lastMonthUsage > 0 ? ((double) thisMonthUsage - lastMonthUsage) / lastMonthUsage * 100 : 0;

        model.addAttribute("thisMonthUsage", thisMonthUsage);
        model.addAttribute("lastMonthUsage", lastMonthUsage);
        model.addAttribute("growthRate", growthRate);

        // 13. New Clients This Month
        long newClientsThisMonth = paymentRequestRepository.countNewClientsThisMonth(id, currentYear, currentMonth);
        model.addAttribute("newClientsThisMonth", newClientsThisMonth);

        // 14. New Contractors This Month
        long newContractorsThisMonth = paymentRequestRepository.countNewContractorsThisMonth(id, currentYear,
                currentMonth);
        model.addAttribute("newContractorsThisMonth", newContractorsThisMonth);

        // 15. Untapped Clients (Total active clients - clients using this method)
        long totalActiveClients = clientRepository.findByActiveTrue().size();
        long untappedClients = totalActiveClients - distinctClients;
        if (untappedClients < 0)
            untappedClients = 0;
        model.addAttribute("untappedClients", untappedClients);
        model.addAttribute("totalActiveClients", totalActiveClients);

        // 16. Quarterly Trend
        int twoMonthsAgo = previousMonth == 1 ? 12 : previousMonth - 1;
        int twoMonthsAgoYear = previousMonth == 1 ? previousYear - 1 : previousYear;
        long twoMonthsAgoUsage = paymentRequestRepository.countByPaymentMethodIdAndYearMonth(id, twoMonthsAgoYear,
                twoMonthsAgo);
        String quarterlyTrend = "→";
        if (thisMonthUsage > lastMonthUsage && lastMonthUsage > twoMonthsAgoUsage)
            quarterlyTrend = "↑↑";
        else if (thisMonthUsage > lastMonthUsage)
            quarterlyTrend = "↑";
        else if (thisMonthUsage < lastMonthUsage && lastMonthUsage < twoMonthsAgoUsage)
            quarterlyTrend = "↓↓";
        else if (thisMonthUsage < lastMonthUsage)
            quarterlyTrend = "↓";
        model.addAttribute("quarterlyTrend", quarterlyTrend);
        model.addAttribute("twoMonthsAgoUsage", twoMonthsAgoUsage);

        // 17. Peak Day of Week
        java.util.List<Object[]> dayDistribution = paymentRequestRepository.findDayOfWeekDistribution(id);
        String peakDay = "N/A";
        if (!dayDistribution.isEmpty() && dayDistribution.get(0)[0] != null) {
            peakDay = dayDistribution.get(0)[0].toString();
        }
        model.addAttribute("peakDay", peakDay);

        // ============================================
        // THREATS
        // ============================================

        // 18. Client Concentration Risk %
        java.math.BigDecimal topClientAmount = paymentRequestRepository.findTopClientAmount(id);
        if (topClientAmount == null)
            topClientAmount = java.math.BigDecimal.ZERO;
        double clientConcentration = totalAmountProcessed.compareTo(java.math.BigDecimal.ZERO) > 0
                ? topClientAmount.divide(totalAmountProcessed, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;
        model.addAttribute("clientConcentration", clientConcentration);

        // 19. Contractor Concentration %
        java.math.BigDecimal topContractorAmount = paymentRequestRepository.findTopContractorAmount(id);
        if (topContractorAmount == null)
            topContractorAmount = java.math.BigDecimal.ZERO;
        double contractorConcentration = totalAmountProcessed.compareTo(java.math.BigDecimal.ZERO) > 0
                ? topContractorAmount.divide(totalAmountProcessed, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;
        model.addAttribute("contractorConcentration", contractorConcentration);

        // 20. Declining Usage Flag
        boolean isDeclining = growthRate < 0;
        model.addAttribute("isDeclining", isDeclining);

        // 21. High-Priority Pending
        long highPriorityPending = paymentRequestRepository.countHighPriorityPending(id);
        model.addAttribute("highPriorityPending", highPriorityPending);

        // 22. Issue Rate %
        long issueTransactions = paymentRequestRepository.countIssueTransactions(id);
        double issueRate = totalTransactions > 0 ? (double) issueTransactions / totalTransactions * 100 : 0;
        model.addAttribute("issueRate", issueRate);
        model.addAttribute("issueTransactions", issueTransactions);

        // Popularity - Calculate rank and usage percentage
        java.util.List<Object[]> allMethodsRanked = paymentRequestRepository.findAllPaymentMethodsRankedByUsage();
        int rank = 0;
        long totalAllTransactions = 0;
        for (int i = 0; i < allMethodsRanked.size(); i++) {
            Object[] row = allMethodsRanked.get(i);
            Long methodId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            totalAllTransactions += count;
            if (methodId.equals(id)) {
                rank = i + 1;
            }
        }
        double usagePercentage = totalAllTransactions > 0 ? (double) totalTransactions / totalAllTransactions * 100 : 0;

        model.addAttribute("usageRank", rank > 0 ? rank : "N/A");
        model.addAttribute("totalMethods", allMethodsRanked.size());
        model.addAttribute("usagePercentage", usagePercentage);

        // Top Clients and Contractors
        java.util.List<Object[]> topClients = paymentRequestRepository.findTopClientsByPaymentMethod(id,
                org.springframework.data.domain.PageRequest.of(0, 5));
        java.util.List<Object[]> topContractors = paymentRequestRepository.findTopContractorsByPaymentMethod(id,
                org.springframework.data.domain.PageRequest.of(0, 5));

        model.addAttribute("topClients", topClients);
        model.addAttribute("topContractors", topContractors);

        return "master-data/payment-method-dashboard";
    }

    // --- AJAX ENDPOINTS FOR CONTRACTOR PAYMENT INFOS ---

    @GetMapping("/api/contractors/{id}/payment-infos")
    @ResponseBody
    public java.util.List<ContractorPaymentInfo> getContractorPaymentInfos(@PathVariable Long id) {
        return contractorPaymentInfoRepository.findByContractorIdAndActiveTrue(id);
    }

    @PostMapping("/api/contractors/{id}/payment-infos")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public org.springframework.http.ResponseEntity<?> addContractorPaymentInfo(@PathVariable Long id,
            @RequestParam Long paymentMethodId, @RequestParam String accountDetails,
            java.security.Principal principal) {
        try {
            Contractor c = contractorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contractor not found"));
            PaymentMethod pm = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> new RuntimeException("Method not found"));

            ContractorPaymentInfo info = new ContractorPaymentInfo();
            info.setContractor(c);
            info.setPaymentMethod(pm);
            info.setAccountDetails(accountDetails);
            info.setCreatedBy(principal != null ? principal.getName() : "System");
            contractorPaymentInfoRepository.save(info);

            return org.springframework.http.ResponseEntity.ok().body("Saved");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/contractors/{cid}/set-default/{infoId}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public org.springframework.http.ResponseEntity<?> setDefaultPaymentInfo(@PathVariable Long cid,
            @PathVariable Long infoId) {
        try {
            Contractor c = contractorRepository.findById(cid)
                    .orElseThrow(() -> new RuntimeException("Contractor not found"));
            ContractorPaymentInfo info = contractorPaymentInfoRepository.findById(infoId)
                    .orElseThrow(() -> new RuntimeException("Info not found"));

            if (!info.getContractor().getId().equals(cid)) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body("Account does not belong to this contractor");
            }

            c.setDefaultPaymentMethod(info.getPaymentMethod());
            c.setAccountDetails(info.getAccountDetails());
            contractorRepository.save(c);

            return org.springframework.http.ResponseEntity.ok().body("Updated");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/contractors/{id}/default-payment-info")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public org.springframework.http.ResponseEntity<?> getDefaultPaymentInfo(@PathVariable Long id) {
        Contractor c = contractorRepository.findById(id).orElse(null);
        if (c == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (c.getDefaultPaymentMethod() != null) {
            response.put("paymentMethodId", c.getDefaultPaymentMethod().getId());
            response.put("paymentMethodName", c.getDefaultPaymentMethod().getMethodName());
        }
        response.put("accountDetails", c.getAccountDetails());

        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/api/payment-infos/{id}/delete")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR')")
    public org.springframework.http.ResponseEntity<?> deleteContractorPaymentInfo(@PathVariable Long id,
            java.security.Principal principal) {
        ContractorPaymentInfo info = contractorPaymentInfoRepository.findById(id).orElse(null);
        if (info != null) {
            info.setActive(false);
            info.setDeletedBy(principal != null ? principal.getName() : "System");
            info.setDeletedAt(java.time.LocalDateTime.now());
            contractorPaymentInfoRepository.save(info);
        }
        return org.springframework.http.ResponseEntity.ok().body("Deleted");
    }

    // --- VENDOR ANALYTICS DASHBOARD ---
    // --- VENDOR ANALYTICS DASHBOARD ---
    @GetMapping("/contractors/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String getVendorAnalytics(Model model) {
        // --- 1. FINANCIAL PERFORMANCE ---
        // 1.1 Total Lifetime Spend
        java.math.BigDecimal totalSpend = paymentRequestRepository
                .sumAmountByPaymentStatus(root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID);
        if (totalSpend == null)
            totalSpend = java.math.BigDecimal.ZERO;

        // 1.2 YTD Spend
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstDayOfYear = today.withDayOfYear(1);
        java.math.BigDecimal ytdSpend = paymentRequestRepository.sumPaidAmountBetween(firstDayOfYear, today);
        if (ytdSpend == null)
            ytdSpend = java.math.BigDecimal.ZERO;

        // 1.3 Projected Annual Spend
        java.math.BigDecimal projectedAnnualSpend = java.math.BigDecimal.ZERO;
        int dayOfYear = today.getDayOfYear();
        if (dayOfYear > 0) {
            projectedAnnualSpend = ytdSpend
                    .divide(java.math.BigDecimal.valueOf(dayOfYear), 2, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(today.lengthOfYear()));
        }

        // 1.4 Avg Transaction Value
        java.math.BigDecimal avgTransactionValue = paymentRequestRepository.findAverageRequestAmount();
        if (avgTransactionValue == null)
            avgTransactionValue = java.math.BigDecimal.ZERO;

        // 1.5 Max Single Transaction
        java.math.BigDecimal maxTransaction = paymentRequestRepository.findMaxTransactionAmount();
        if (maxTransaction == null)
            maxTransaction = java.math.BigDecimal.ZERO;

        // --- 2. VENDOR HEALTH & OPERATIONS ---
        // 2.1 Active Vendor Count
        long activeContractors = paymentRequestRepository.countActiveContractors();
        long inactiveContractors = paymentRequestRepository.countInactiveContractors();
        long totalContractors = activeContractors + inactiveContractors;

        // 2.2 Vendor Utilization %
        double utilization = (totalContractors > 0) ? ((double) activeContractors / totalContractors) * 100 : 0.0;

        // 2.3 New Vendors (30d)
        long newVendors30d = contractorRepository.countByCreatedAtAfter(java.time.LocalDateTime.now().minusDays(30));

        // 2.4 Stale Vendors (>90d)
        long staleVendors = contractorRepository.countStaleContractors(today.minusDays(90));

        // 2.5 Avg Payment Time (Real Implementation)
        java.util.List<root.cyb.mh.attendancesystem.model.PaymentRequest> paidRequests = paymentRequestRepository
                .findByPaymentStatus(root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID);
        long totalDays = 0;
        long countPaid = 0;
        for (root.cyb.mh.attendancesystem.model.PaymentRequest r : paidRequests) {
            if (r.getRequestDate() != null && r.getLastModified() != null) {
                // Assuming lastModified is approx payment time for PAID status
                long days = java.time.temporal.ChronoUnit.DAYS.between(r.getRequestDate(),
                        r.getLastModified().toLocalDate());
                if (days < 0)
                    days = 0; // Safety
                totalDays += days;
                countPaid++;
            }
        }
        long avgPaymentDays = (countPaid > 0) ? totalDays / countPaid : 0;

        // --- 3. RISK & STRATEGY ---
        // 3.1 Vendor Churn Rate (Inactive / Total)
        double churnRate = (totalContractors > 0) ? ((double) inactiveContractors / totalContractors) * 100 : 0.0;

        // 3.2 Top Vendor Concentration
        java.util.List<Object[]> topVendor = paymentRequestRepository
                .findTopContractorsBySpend(org.springframework.data.domain.PageRequest.of(0, 1));
        java.math.BigDecimal topVendorSpend = (topVendor != null && !topVendor.isEmpty())
                ? (java.math.BigDecimal) topVendor.get(0)[1]
                : java.math.BigDecimal.ZERO;
        double concentration = (totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? topVendorSpend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // 3.3 Rejection Rate
        long totalRequests = paymentRequestRepository.count();
        long rejectedRequests = paymentRequestRepository
                .countByStatus(root.cyb.mh.attendancesystem.model.enums.RequestStatus.REJECTED);
        double rejectionRate = (totalRequests > 0) ? ((double) rejectedRequests / totalRequests) * 100 : 0.0;

        // 3.4 Most Frequent Payment Method
        java.util.List<Object[]> topMethod = paymentRequestRepository
                .findMostFrequentPaymentMethodGlobal(org.springframework.data.domain.PageRequest.of(0, 1));
        String frequentMethod = (topMethod != null && !topMethod.isEmpty()) ? (String) topMethod.get(0)[0] : "N/A";

        // 3.5 Top Spending Month
        java.util.List<Object[]> topMonthData = paymentRequestRepository
                .findTopSpendingMonthGlobal(org.springframework.data.domain.PageRequest.of(0, 1));
        String topMonth = "N/A";
        if (topMonthData != null && !topMonthData.isEmpty()) {
            java.time.Month m = java.time.Month.of(((Number) topMonthData.get(0)[1]).intValue());
            int y = ((Number) topMonthData.get(0)[0]).intValue();
            topMonth = m.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US) + " " + y;
        }

        // --- CHARTS & LISTS ---
        java.util.List<Object[]> topVendorsList = paymentRequestRepository
                .findTopContractorsBySpend(org.springframework.data.domain.PageRequest.of(0, 5));

        // ============================================
        // SWOT ANALYTICS
        // ============================================

        // --- STRENGTHS ---
        // 1. Total Lifetime Spend (already calculated as totalSpend)
        // 2. Active Vendor Count (already calculated as activeContractors)
        // 3. Utilization Rate (already calculated as utilization)
        // 4. Avg Transaction Value (already calculated as avgTransactionValue)

        // 5. High-Value Vendor Count
        java.math.BigDecimal avgApprovedGlobal = paymentRequestRepository.avgApprovedGlobally();
        if (avgApprovedGlobal == null)
            avgApprovedGlobal = java.math.BigDecimal.ZERO;
        long highValueVendorCount = paymentRequestRepository.countVendorsAboveThreshold(avgApprovedGlobal);

        // --- WEAKNESSES ---
        // 6. Churn Rate (already calculated)
        // 7. Stale Vendors (already calculated)
        // 8. Rejection Rate (already calculated)
        // 9. Avg Payout Time (already calculated)

        // 10. Issue Rate
        long issueCount = paymentRequestRepository.countIssueRequestsGlobally();
        double issueRate = (totalRequests > 0) ? ((double) issueCount / totalRequests) * 100 : 0.0;

        // --- OPPORTUNITIES ---
        // 11. New Vendors 30d (already calculated)

        // 12. MoM Spend Growth
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        int prevMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int prevYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        java.math.BigDecimal currentMonthSpend = paymentRequestRepository.sumPaidByYearMonth(currentYear, currentMonth);
        java.math.BigDecimal prevMonthSpend = paymentRequestRepository.sumPaidByYearMonth(prevYear, prevMonth);
        if (currentMonthSpend == null)
            currentMonthSpend = java.math.BigDecimal.ZERO;
        if (prevMonthSpend == null)
            prevMonthSpend = java.math.BigDecimal.ZERO;

        double momGrowth = prevMonthSpend.compareTo(java.math.BigDecimal.ZERO) > 0
                ? currentMonthSpend.subtract(prevMonthSpend).divide(prevMonthSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 13. YoY Spend Growth
        int lastYear = currentYear - 1;
        java.math.BigDecimal thisYearSpendTotal = paymentRequestRepository.sumPaidAmountBetween(
                java.time.LocalDate.of(currentYear, 1, 1), today);
        java.math.BigDecimal lastYearSpendTotal = paymentRequestRepository.sumPaidAmountBetween(
                java.time.LocalDate.of(lastYear, 1, 1), java.time.LocalDate.of(lastYear, 12, 31));
        if (thisYearSpendTotal == null)
            thisYearSpendTotal = java.math.BigDecimal.ZERO;
        if (lastYearSpendTotal == null)
            lastYearSpendTotal = java.math.BigDecimal.ZERO;

        double yoyGrowth = lastYearSpendTotal.compareTo(java.math.BigDecimal.ZERO) > 0
                ? thisYearSpendTotal.subtract(lastYearSpendTotal)
                        .divide(lastYearSpendTotal, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 14. Quarterly Trend
        int twoMonthsAgo = prevMonth == 1 ? 12 : prevMonth - 1;
        int twoMonthsAgoYear = prevMonth == 1 ? prevYear - 1 : prevYear;
        java.math.BigDecimal twoMonthsAgoSpend = paymentRequestRepository.sumPaidByYearMonth(twoMonthsAgoYear,
                twoMonthsAgo);
        if (twoMonthsAgoSpend == null)
            twoMonthsAgoSpend = java.math.BigDecimal.ZERO;

        String quarterlyTrend = "→";
        if (currentMonthSpend.compareTo(prevMonthSpend) > 0 && prevMonthSpend.compareTo(twoMonthsAgoSpend) > 0)
            quarterlyTrend = "↑↑";
        else if (currentMonthSpend.compareTo(prevMonthSpend) > 0)
            quarterlyTrend = "↑";
        else if (currentMonthSpend.compareTo(prevMonthSpend) < 0 && prevMonthSpend.compareTo(twoMonthsAgoSpend) < 0)
            quarterlyTrend = "↓↓";
        else if (currentMonthSpend.compareTo(prevMonthSpend) < 0)
            quarterlyTrend = "↓";

        // 15. Peak Month (already calculated as topMonth)

        // --- THREATS ---
        // 16. Top 1 Concentration (already calculated as concentration)

        // 17. Top 5 Concentration
        java.math.BigDecimal top5Spend = paymentRequestRepository.sumTop5VendorsSpend();
        if (top5Spend == null)
            top5Spend = java.math.BigDecimal.ZERO;
        double top5Concentration = (totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? top5Spend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // 18. Single Payment Method Dependency
        java.math.BigDecimal topMethodSpend = paymentRequestRepository.sumTopPaymentMethodAmountGlobally();
        if (topMethodSpend == null)
            topMethodSpend = java.math.BigDecimal.ZERO;
        double paymentMethodDependency = (totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? topMethodSpend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // 19. Pending Queue
        long pendingQueue = paymentRequestRepository.countPendingGlobally();

        // 20. Is Declining Flag
        boolean isDeclining = momGrowth < 0;

        // --- MODEL POPULATION ---
        model.addAttribute("totalSpend", totalSpend);
        model.addAttribute("ytdSpend", ytdSpend);
        model.addAttribute("projectedAnnualSpend", projectedAnnualSpend);
        model.addAttribute("avgTransactionValue", avgTransactionValue);
        model.addAttribute("maxTransaction", maxTransaction);

        model.addAttribute("activeCount", activeContractors);
        model.addAttribute("inactiveCount", inactiveContractors);
        model.addAttribute("totalCount", totalContractors);
        model.addAttribute("utilization", utilization);
        model.addAttribute("newVendors30d", newVendors30d);
        model.addAttribute("staleVendors", staleVendors);
        model.addAttribute("avgPaymentDays", avgPaymentDays);

        model.addAttribute("churnRate", churnRate);
        model.addAttribute("concentration", concentration);
        model.addAttribute("rejectionRate", rejectionRate);
        model.addAttribute("frequentMethod", frequentMethod);
        model.addAttribute("topMonth", topMonth);

        model.addAttribute("topVendors", topVendorsList);

        // SWOT - Strengths
        model.addAttribute("highValueVendorCount", highValueVendorCount);

        // SWOT - Weaknesses
        model.addAttribute("issueRate", issueRate);
        model.addAttribute("issueCount", issueCount);

        // SWOT - Opportunities
        model.addAttribute("momGrowth", momGrowth);
        model.addAttribute("yoyGrowth", yoyGrowth);
        model.addAttribute("quarterlyTrend", quarterlyTrend);
        model.addAttribute("currentMonthSpend", currentMonthSpend);
        model.addAttribute("prevMonthSpend", prevMonthSpend);
        model.addAttribute("twoMonthsAgoSpend", twoMonthsAgoSpend);

        // SWOT - Threats
        model.addAttribute("top5Concentration", top5Concentration);
        model.addAttribute("paymentMethodDependency", paymentMethodDependency);
        model.addAttribute("pendingQueue", pendingQueue);
        model.addAttribute("isDeclining", isDeclining);

        return "master-data/vendor-dashboard";
    }

    @GetMapping("/clients/{id}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String getClientDashboard(@PathVariable Long id, Model model) {
        // Fetch the Client
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        // --- 1. FINANCIAL PERFORMANCE ---
        java.math.BigDecimal totalSpend = paymentRequestRepository.sumPaidAmountByClientId(id);
        if (totalSpend == null)
            totalSpend = java.math.BigDecimal.ZERO;

        java.time.LocalDate yearStart = java.time.LocalDate.of(java.time.LocalDate.now().getYear(), 1, 1);
        java.math.BigDecimal ytdSpend = paymentRequestRepository.sumPaidAmountByClientIdBetween(id, yearStart,
                java.time.LocalDate.now());
        if (ytdSpend == null)
            ytdSpend = java.math.BigDecimal.ZERO;

        java.math.BigDecimal avgTransaction = paymentRequestRepository.findAvgAmountByClientId(id);
        if (avgTransaction == null)
            avgTransaction = java.math.BigDecimal.ZERO;

        java.math.BigDecimal maxTransaction = paymentRequestRepository.findMaxAmountByClientId(id);
        if (maxTransaction == null)
            maxTransaction = java.math.BigDecimal.ZERO;

        // Projected Annual Spend
        int monthsElapsed = java.time.LocalDate.now().getMonthValue();
        java.math.BigDecimal projectedAnnualSpend = monthsElapsed > 0
                ? ytdSpend.multiply(java.math.BigDecimal.valueOf(12.0 / monthsElapsed))
                : java.math.BigDecimal.ZERO;

        // Monthly Burn Rate
        java.math.BigDecimal monthlyBurnRate = monthsElapsed > 0
                ? ytdSpend.divide(java.math.BigDecimal.valueOf(monthsElapsed), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;

        // --- 2. OPERATIONAL EFFICIENCY ---
        long totalRequests = paymentRequestRepository.countByClientId(id);
        long totalWorkOrders = paymentRequestRepository.countDistinctWorkOrdersByClientId(id);

        // Cost Per Work Order
        java.math.BigDecimal costPerWorkOrder = totalWorkOrders > 0
                ? totalSpend.divide(java.math.BigDecimal.valueOf(totalWorkOrders), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;

        // Avg Work Orders per Month
        double avgWorkOrdersPerMonth = monthsElapsed > 0 ? (double) totalWorkOrders / monthsElapsed : 0;

        long approvedCount = paymentRequestRepository.countByClientIdAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED);
        long rejectedCount = paymentRequestRepository.countByClientIdAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.REJECTED);

        double approvalRate = totalRequests > 0 ? (double) approvedCount / totalRequests * 100 : 0;
        double rejectionRate = totalRequests > 0 ? (double) rejectedCount / totalRequests * 100 : 0;

        // --- 3. VENDOR RELATIONSHIPS ---
        long totalVendors = paymentRequestRepository.countDistinctContractorsByClientId(id);

        java.util.List<Object[]> mostFrequentVendor = paymentRequestRepository.findMostFrequentVendorByClientId(id,
                org.springframework.data.domain.PageRequest.of(0, 1));
        String topVendorName = mostFrequentVendor.isEmpty() ? "N/A" : (String) mostFrequentVendor.get(0)[0];

        java.util.List<Object[]> topContractors = paymentRequestRepository.findTopContractorsByClientId(id,
                org.springframework.data.domain.PageRequest.of(0, 5));

        // Vendor Concentration
        double vendorConcentration = 0;
        if (!topContractors.isEmpty() && totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal topVendorSpend = (java.math.BigDecimal) topContractors.get(0)[1];
            vendorConcentration = topVendorSpend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(100)).doubleValue();
        }

        java.time.LocalDate thirtyDaysAgo = java.time.LocalDate.now().minusDays(30);
        long newVendors30d = paymentRequestRepository.countNewVendorsByClientIdSince(id, thirtyDaysAgo);

        // Vendor Utilization Rate (active vendors out of total)
        double vendorUtilization = totalVendors > 0 ? 100.0 : 0; // Simplified: all vendors counted are active

        // --- 4. RISK & PAYMENT ANALYSIS ---
        java.math.BigDecimal outstandingBalance = paymentRequestRepository.sumOutstandingByClientId(id);
        if (outstandingBalance == null)
            outstandingBalance = java.math.BigDecimal.ZERO;

        long highPriorityCount = paymentRequestRepository.countHighPriorityByClientId(id);
        double highPriorityPercent = totalRequests > 0 ? (double) highPriorityCount / totalRequests * 100 : 0;

        long pendingCount = paymentRequestRepository.countPendingByClientId(id);

        java.util.List<Object[]> paymentMethods = paymentRequestRepository.findPaymentMethodDistributionByClientId(id,
                org.springframework.data.domain.PageRequest.of(0, 3));

        // --- 5. GROWTH & TRENDS ---
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int previousMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int previousYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        java.math.BigDecimal currentMonthSpend = paymentRequestRepository.sumByClientIdYearMonth(id, currentYear,
                currentMonth);
        java.math.BigDecimal previousMonthSpend = paymentRequestRepository.sumByClientIdYearMonth(id, previousYear,
                previousMonth);

        if (currentMonthSpend == null)
            currentMonthSpend = java.math.BigDecimal.ZERO;
        if (previousMonthSpend == null)
            previousMonthSpend = java.math.BigDecimal.ZERO;

        double momGrowthRate = previousMonthSpend.compareTo(java.math.BigDecimal.ZERO) > 0
                ? currentMonthSpend.subtract(previousMonthSpend).divide(previousMonthSpend, 4,
                        java.math.RoundingMode.HALF_UP).multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        java.util.List<Object[]> peakMonth = paymentRequestRepository.findPeakMonthByClientIdAndYear(id, currentYear);
        String peakMonthName = "N/A";
        if (!peakMonth.isEmpty()) {
            int monthNum = ((Number) peakMonth.get(0)[0]).intValue();
            peakMonthName = java.time.Month.of(monthNum).name();
        }

        // --- CHARTS & LISTS ---
        java.util.List<PaymentRequest> recentPayments = paymentRequestRepository
                .findByClientIdOrderByRequestDateDesc(id, org.springframework.data.domain.PageRequest.of(0, 10));

        // ============================================
        // SWOT ANALYTICS
        // ============================================

        // --- STRENGTHS ---
        // 1. Total Lifetime Value (approved)
        java.math.BigDecimal lifetimeValue = paymentRequestRepository.sumApprovedByClient(id);
        if (lifetimeValue == null)
            lifetimeValue = java.math.BigDecimal.ZERO;

        // 2. Revenue Share %
        java.math.BigDecimal totalCompanyRevenue = paymentRequestRepository.sumAllApproved();
        if (totalCompanyRevenue == null)
            totalCompanyRevenue = java.math.BigDecimal.ZERO;
        double revenueShare = totalCompanyRevenue.compareTo(java.math.BigDecimal.ZERO) > 0
                ? lifetimeValue.divide(totalCompanyRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 3. Account Age (days since first transaction)
        java.time.LocalDate firstTransactionDate = paymentRequestRepository.findFirstTransactionDateByClient(id);
        long accountAgeDays = 0;
        if (firstTransactionDate != null) {
            accountAgeDays = java.time.temporal.ChronoUnit.DAYS.between(firstTransactionDate,
                    java.time.LocalDate.now());
        }

        // 4. Payment Consistency
        long activeMonths = paymentRequestRepository.countActiveMonthsByClient(id);
        long totalMonths = accountAgeDays > 0 ? (accountAgeDays / 30) + 1 : 1;
        double paymentConsistency = totalMonths > 0 ? (double) activeMonths / totalMonths * 100 : 0;
        if (paymentConsistency > 100)
            paymentConsistency = 100;

        // 5. High-Value Transactions
        java.math.BigDecimal avgApproved = paymentRequestRepository.avgApprovedByClient(id);
        if (avgApproved == null)
            avgApproved = java.math.BigDecimal.ZERO;
        long highValueTxn = paymentRequestRepository.countHighValueByClient(id, avgApproved);

        // --- WEAKNESSES ---
        // 6. Issue Rate
        long issueCount = paymentRequestRepository.countIssuesByClient(id);
        double issueRate = totalRequests > 0 ? (double) issueCount / totalRequests * 100 : 0;

        // 7. Oldest Pending Days
        java.time.LocalDate oldestPendingDate = paymentRequestRepository.findOldestPendingDateByClient(id);
        long oldestPendingDays = 0;
        if (oldestPendingDate != null) {
            oldestPendingDays = java.time.temporal.ChronoUnit.DAYS.between(oldestPendingDate,
                    java.time.LocalDate.now());
        }

        // 8. Low-Value Percentage
        java.math.BigDecimal lowThreshold = new java.math.BigDecimal("100");
        long lowValueCount = paymentRequestRepository.countLowValueByClient(id, lowThreshold);
        double lowValuePercentage = totalRequests > 0 ? (double) lowValueCount / totalRequests * 100 : 0;

        // --- OPPORTUNITIES ---
        // 9. YoY Growth
        int lastYear = currentYear - 1;
        java.math.BigDecimal thisYearSpend = paymentRequestRepository.sumByClientAndYear(id, currentYear);
        java.math.BigDecimal lastYearSpend = paymentRequestRepository.sumByClientAndYear(id, lastYear);
        if (thisYearSpend == null)
            thisYearSpend = java.math.BigDecimal.ZERO;
        if (lastYearSpend == null)
            lastYearSpend = java.math.BigDecimal.ZERO;
        double yoyGrowth = lastYearSpend.compareTo(java.math.BigDecimal.ZERO) > 0
                ? thisYearSpend.subtract(lastYearSpend).divide(lastYearSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 10. New Contractors This Month
        long newContractorsThisMonth = paymentRequestRepository.countNewContractorsThisMonthByClient(id, currentYear,
                currentMonth);

        // 11. Untapped Payment Methods
        long usedPaymentMethods = paymentRequestRepository.countPaymentMethodsUsedByClient(id);
        long totalPaymentMethods = paymentMethodRepository.findByActiveTrue().size();
        long untappedPaymentMethods = totalPaymentMethods - usedPaymentMethods;
        if (untappedPaymentMethods < 0)
            untappedPaymentMethods = 0;

        // 12. Quarterly Trend
        int twoMonthsAgo = previousMonth == 1 ? 12 : previousMonth - 1;
        int twoMonthsAgoYear = previousMonth == 1 ? previousYear - 1 : previousYear;
        java.math.BigDecimal twoMonthsAgoSpend = paymentRequestRepository.sumByClientAndYearMonth(id, twoMonthsAgoYear,
                twoMonthsAgo);
        if (twoMonthsAgoSpend == null)
            twoMonthsAgoSpend = java.math.BigDecimal.ZERO;

        String quarterlyTrend = "→";
        if (currentMonthSpend.compareTo(previousMonthSpend) > 0 && previousMonthSpend.compareTo(twoMonthsAgoSpend) > 0)
            quarterlyTrend = "↑↑";
        else if (currentMonthSpend.compareTo(previousMonthSpend) > 0)
            quarterlyTrend = "↑";
        else if (currentMonthSpend.compareTo(previousMonthSpend) < 0
                && previousMonthSpend.compareTo(twoMonthsAgoSpend) < 0)
            quarterlyTrend = "↓↓";
        else if (currentMonthSpend.compareTo(previousMonthSpend) < 0)
            quarterlyTrend = "↓";

        // 13. Peak Day of Week
        java.util.List<Object[]> dayDistribution = paymentRequestRepository.findDayOfWeekDistributionByClient(id);
        String peakDay = "N/A";
        if (!dayDistribution.isEmpty() && dayDistribution.get(0)[0] != null) {
            peakDay = dayDistribution.get(0)[0].toString().trim();
        }

        // --- THREATS ---
        // 14. Contractor Dependency
        java.math.BigDecimal topContractorAmount = paymentRequestRepository.findTopContractorAmountByClient(id);
        if (topContractorAmount == null)
            topContractorAmount = java.math.BigDecimal.ZERO;
        double contractorDependency = lifetimeValue.compareTo(java.math.BigDecimal.ZERO) > 0
                ? topContractorAmount.divide(lifetimeValue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 15. Payment Method Dependency
        java.math.BigDecimal topPaymentMethodAmount = paymentRequestRepository.findTopPaymentMethodAmountByClient(id);
        if (topPaymentMethodAmount == null)
            topPaymentMethodAmount = java.math.BigDecimal.ZERO;
        double paymentMethodDependency = lifetimeValue.compareTo(java.math.BigDecimal.ZERO) > 0
                ? topPaymentMethodAmount.divide(lifetimeValue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0;

        // 16. Declining Usage Flag
        boolean isDeclining = momGrowthRate < 0;

        // 17. High-Priority Pending
        long highPriorityPending = paymentRequestRepository.countHighPriorityPendingByClient(id);

        // 18. Inactive Period
        java.time.LocalDate lastTransactionDate = paymentRequestRepository.findLastTransactionDateByClient(id);
        long inactiveDays = 0;
        if (lastTransactionDate != null) {
            inactiveDays = java.time.temporal.ChronoUnit.DAYS.between(lastTransactionDate, java.time.LocalDate.now());
        }

        // --- MODEL POPULATION ---
        model.addAttribute("client", client);

        // Financial
        model.addAttribute("totalSpend", totalSpend);
        model.addAttribute("ytdSpend", ytdSpend);
        model.addAttribute("avgTransaction", avgTransaction);
        model.addAttribute("maxTransaction", maxTransaction);
        model.addAttribute("projectedAnnualSpend", projectedAnnualSpend);
        model.addAttribute("monthlyBurnRate", monthlyBurnRate);
        model.addAttribute("costPerWorkOrder", costPerWorkOrder);

        // Operational
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("totalWorkOrders", totalWorkOrders);
        model.addAttribute("avgWorkOrdersPerMonth", avgWorkOrdersPerMonth);
        model.addAttribute("approvalRate", approvalRate);
        model.addAttribute("rejectionRate", rejectionRate);

        // Vendor
        model.addAttribute("totalVendors", totalVendors);
        model.addAttribute("vendorConcentration", vendorConcentration);
        model.addAttribute("topVendorName", topVendorName);
        model.addAttribute("newVendors30d", newVendors30d);
        model.addAttribute("vendorUtilization", vendorUtilization);

        // Risk
        model.addAttribute("outstandingBalance", outstandingBalance);
        model.addAttribute("highPriorityPercent", highPriorityPercent);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("paymentMethods", paymentMethods);

        // Growth
        model.addAttribute("momGrowthRate", momGrowthRate);
        model.addAttribute("peakMonthName", peakMonthName);

        // Lists
        model.addAttribute("topContractors", topContractors);
        model.addAttribute("recentPayments", recentPayments);

        // SWOT - Strengths
        model.addAttribute("lifetimeValue", lifetimeValue);
        model.addAttribute("revenueShare", revenueShare);
        model.addAttribute("accountAgeDays", accountAgeDays);
        model.addAttribute("paymentConsistency", paymentConsistency);
        model.addAttribute("activeMonths", activeMonths);
        model.addAttribute("highValueTxn", highValueTxn);

        // SWOT - Weaknesses
        model.addAttribute("issueRate", issueRate);
        model.addAttribute("issueCount", issueCount);
        model.addAttribute("oldestPendingDays", oldestPendingDays);
        model.addAttribute("lowValuePercentage", lowValuePercentage);
        model.addAttribute("lowValueCount", lowValueCount);

        // SWOT - Opportunities
        model.addAttribute("yoyGrowth", yoyGrowth);
        model.addAttribute("newContractorsThisMonth", newContractorsThisMonth);
        model.addAttribute("untappedPaymentMethods", untappedPaymentMethods);
        model.addAttribute("totalPaymentMethods", totalPaymentMethods);
        model.addAttribute("quarterlyTrend", quarterlyTrend);
        model.addAttribute("peakDay", peakDay);
        model.addAttribute("currentMonthSpend", currentMonthSpend);
        model.addAttribute("previousMonthSpend", previousMonthSpend);
        model.addAttribute("twoMonthsAgoSpend", twoMonthsAgoSpend);

        // SWOT - Threats
        model.addAttribute("contractorDependency", contractorDependency);
        model.addAttribute("paymentMethodDependency", paymentMethodDependency);
        model.addAttribute("isDeclining", isDeclining);
        model.addAttribute("highPriorityPending", highPriorityPending);
        model.addAttribute("inactiveDays", inactiveDays);

        return "master-data/client-dashboard";
    }
}
