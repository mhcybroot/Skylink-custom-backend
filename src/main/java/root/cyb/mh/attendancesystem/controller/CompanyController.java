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
}
