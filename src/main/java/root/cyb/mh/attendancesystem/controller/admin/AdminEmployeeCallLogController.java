package root.cyb.mh.attendancesystem.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.service.EmployeeCallLogService;

@Controller
@RequestMapping("/admin/employee-call-logs-vault")
public class AdminEmployeeCallLogController {

    @Autowired
    private EmployeeCallLogService callLogService;

    @GetMapping
    public String viewVault(
            @RequestParam(required = false) String employee,
            Model model) {
        
        model.addAttribute("logs", callLogService.searchCallLogs(employee));
        model.addAttribute("employees", callLogService.getDistinctEmployeeUsernames());
        model.addAttribute("selectedEmployee", employee);
        
        return "admin/employee-call-logs-vault";
    }
}
