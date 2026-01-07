package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import root.cyb.mh.attendancesystem.model.Company;
import root.cyb.mh.attendancesystem.repository.CompanyRepository;

@Controller
@RequestMapping("/master-data/companies")
@PreAuthorize("hasRole('ADMIN')")
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.service.PaymentDashboardService paymentDashboardService;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.ContractorRepository contractorRepository;

    @GetMapping
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyRepository.findAll());
        model.addAttribute("newCompany", new Company());
        return "company/list";
    }

    @PostMapping
    public String saveCompany(@ModelAttribute Company company, RedirectAttributes ps) {
        try {
            companyRepository.save(company);
            ps.addFlashAttribute("successMessage", "Company saved successfully!");
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error saving company.");
        }
        return "redirect:/master-data/companies";
    }

    @PostMapping("/update")
    public String updateCompany(@ModelAttribute Company company, RedirectAttributes ps) {
        try {
            Company existing = companyRepository.findById(company.getId()).orElse(null);
            if (existing != null) {
                existing.setName(company.getName());
                existing.setPhone(company.getPhone());
                existing.setEmail(company.getEmail());
                existing.setAddress(company.getAddress());
                existing.setSmtpHost(company.getSmtpHost());
                existing.setSmtpPort(company.getSmtpPort());
                existing.setSmtpUsername(company.getSmtpUsername());
                existing.setSmtpPassword(company.getSmtpPassword());
                companyRepository.save(existing);
                ps.addFlashAttribute("successMessage", "Company updated successfully!");
            } else {
                ps.addFlashAttribute("errorMessage", "Company not found.");
            }
        } catch (Exception e) {
            ps.addFlashAttribute("errorMessage", "Error updating company: " + e.getMessage());
        }
        return "redirect:/master-data/companies";
    }

    @PostMapping("/{id}/toggle")
    public String toggleCompany(@PathVariable Long id, RedirectAttributes ps) {
        Company c = companyRepository.findById(id).orElse(null);
        if (c != null) {
            c.setActive(!c.isActive());
            companyRepository.save(c);
            ps.addFlashAttribute("successMessage", "Company status updated.");
        }
        return "redirect:/master-data/companies";
    }

    @GetMapping("/{id}/dashboard")
    public String getCompanyDashboard(@PathVariable Long id, Model model) {
        Company company = companyRepository.findById(id).orElseThrow(
                () -> new root.cyb.mh.attendancesystem.exception.ResourceNotFoundException("Company not found"));
        model.addAttribute("company", company);

        // 1. Get Base Stats from Service (Legacy & BI)
        root.cyb.mh.attendancesystem.model.dto.DashboardStatsDTO stats = paymentDashboardService.getCompanyStats(id);
        model.addAttribute("stats", stats);

        // 2. SWOT Calculations
        java.time.LocalDate today = java.time.LocalDate.now();
        java.math.BigDecimal totalSpend = paymentRequestRepository.sumPaidAmountByCompanyId(id);
        if (totalSpend == null)
            totalSpend = java.math.BigDecimal.ZERO;

        long activeContractors = paymentRequestRepository.countActiveContractorsByCompany(id);
        long totalContractors = contractorRepository.count();
        double utilization = (totalContractors > 0) ? ((double) activeContractors / totalContractors) * 100 : 0.0;

        java.math.BigDecimal avgTx = paymentRequestRepository.avgApprovedByCompanyInfo(id);
        if (avgTx == null)
            avgTx = java.math.BigDecimal.ZERO;
        long highValueTx = paymentRequestRepository.countHighValueTransactionsByCompany(id, avgTx);

        // Weaknesses
        long pending = paymentRequestRepository.countPendingByCompanyId(id);
        long rejected = paymentRequestRepository.countByCompanyAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.REJECTED);
        long approved = paymentRequestRepository.countByCompanyAndStatus(id,
                root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED);
        long totalReq = pending + rejected + approved;
        double rejectionRate = (totalReq > 0) ? ((double) rejected / totalReq) * 100 : 0.0;

        java.time.LocalDate oldestDate = paymentRequestRepository.findOldestPendingDateByCompany(id);
        long oldestPendingDays = (oldestDate != null)
                ? java.time.temporal.ChronoUnit.DAYS.between(oldestDate, today)
                : 0;

        long issues = paymentRequestRepository.countIssueRequestsByCompany(id);
        double issueRate = (totalReq > 0) ? ((double) issues / totalReq) * 100 : 0.0;

        // Opportunities
        int cm = today.getMonthValue();
        int cy = today.getYear();
        int lm = cm == 1 ? 12 : cm - 1;
        int ly = cm == 1 ? cy - 1 : cy;

        java.math.BigDecimal cmSpend = paymentRequestRepository.sumPaidByCompanyIdYearMonth(id, cy, cm);
        java.math.BigDecimal lmSpend = paymentRequestRepository.sumPaidByCompanyIdYearMonth(id, ly, lm);
        if (cmSpend == null)
            cmSpend = java.math.BigDecimal.ZERO;
        if (lmSpend == null)
            lmSpend = java.math.BigDecimal.ZERO;

        double momGrowth = (lmSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? cmSpend.subtract(lmSpend).divide(lmSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // Threats
        java.math.BigDecimal topConSpend = paymentRequestRepository.sumTopContractorSpendByCompany(id);
        if (topConSpend == null)
            topConSpend = java.math.BigDecimal.ZERO;
        double concentration = (totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? topConSpend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        java.math.BigDecimal topMethodSpend = paymentRequestRepository.sumTopPaymentMethodAmountByCompany(id);
        if (topMethodSpend == null)
            topMethodSpend = java.math.BigDecimal.ZERO;
        double methodDep = (totalSpend.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? topMethodSpend.divide(totalSpend, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        boolean isDeclining = momGrowth < -10;
        long urgentPending = paymentRequestRepository.countHighPriorityPendingByCompany(id);

        // Add Attributes
        model.addAttribute("totalSpend", totalSpend);
        model.addAttribute("activeContractors", activeContractors);
        model.addAttribute("utilization", utilization);
        model.addAttribute("avgTransactionValue", avgTx);
        model.addAttribute("highValueTxCount", highValueTx);

        model.addAttribute("rejectionRate", rejectionRate);
        model.addAttribute("pendingRequests", pending);
        model.addAttribute("oldestPendingDays", oldestPendingDays);
        model.addAttribute("issueRate", issueRate);

        model.addAttribute("momGrowth", momGrowth);
        model.addAttribute("currentMonthSpend", cmSpend);
        model.addAttribute("prevMonthSpend", lmSpend);

        // Peak Month
        java.util.List<Object[]> peakRaw = paymentRequestRepository.findPeakMonthByCompanyIdAndYear(id, cy);
        String peakMonth = (!peakRaw.isEmpty())
                ? java.time.Month.of(((Number) peakRaw.get(0)[0]).intValue()).name()
                : "N/A";
        model.addAttribute("peakMonth", peakMonth);

        model.addAttribute("concentration", concentration);
        model.addAttribute("paymentMethodDependency", methodDep);
        model.addAttribute("isDeclining", isDeclining);
        model.addAttribute("urgentPending", urgentPending);

        // Fix Placeholders
        Double avgPayDays = paymentRequestRepository.findAvgPaymentDurationDaysByCompany(id);
        long avgPayDaysInt = (avgPayDays != null) ? (long) Math.ceil(avgPayDays) : 0;

        java.time.LocalDate thirtyDaysAgo = today.minusDays(30);
        long newContractors = paymentRequestRepository.countNewContractorsByCompanySince(id, thirtyDaysAgo);

        model.addAttribute("avgPaymentDays", avgPayDaysInt);
        model.addAttribute("newContractors", newContractors);

        return "company/dashboard";
    }
}
