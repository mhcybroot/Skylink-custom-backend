package root.cyb.mh.attendancesystem.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.service.PhoneNotificationService;

@Controller
@RequestMapping("/admin/phone-notification-vault")
public class AdminPhoneNotificationController {

    @Autowired
    private PhoneNotificationService phoneNotificationService;

    @GetMapping
    public String viewVault(
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) String pkg,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("notifications", phoneNotificationService.searchNotifications(employee, pkg, search));
        
        // Pass distinct options for dropdowns
        model.addAttribute("employees", phoneNotificationService.getDistinctEmployeeUsernames());
        model.addAttribute("packages", phoneNotificationService.getDistinctPackageNames());
        
        // Maintain selected filter state in the view
        model.addAttribute("selectedEmployee", employee);
        model.addAttribute("selectedPackage", pkg);
        model.addAttribute("searchQuery", search);
        
        return "admin/phone-notification-vault";
    }
}
