package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeImportLog;
import root.cyb.mh.attendancesystem.repository.EmployeeImportLogRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.service.EmployeeWorkOrderService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/employee/imports")
public class EmployeeImportHistoryController {

    @Autowired
    private EmployeeImportLogRepository employeeImportLogRepository;

    @Autowired
    private EmployeeWorkOrderService employeeWorkOrderService;

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
    public String listImports(Authentication authentication, Model model) {
        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<EmployeeImportLog> logs;
        if (isAdmin) {
            logs = employeeImportLogRepository.findAllByOrderByImportDateDesc();
        } else {
            Employee emp = getLoggedEmployee(authentication);
            logs = employeeImportLogRepository.findByImportedByOrderByImportDateDesc(emp);
        }

        model.addAttribute("logs", logs);
        model.addAttribute("activeLink", "employee-import-history");
        return "employee/import-history";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteImport(@PathVariable Long id, Authentication authentication) {
        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        employeeWorkOrderService.deleteImportBatch(id);
        return "redirect:/employee/imports?success=deleted";
    }

    @PostMapping("/cleanup")
    @Transactional
    public String cleanupLegacyData(Authentication authentication) {
        if (!checkAccess(authentication)) {
            return "redirect:/access-denied";
        }

        employeeWorkOrderService.cleanupLegacyData();
        return "redirect:/employee/imports?success=cleanup";
    }
}
