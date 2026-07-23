package root.cyb.mh.attendancesystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MonitorViewController {

    @GetMapping("/admin/live-monitor")
    public String liveMonitor(@RequestParam(required = false) String employeeId, Model model) {
        model.addAttribute("targetEmployeeId", employeeId != null ? employeeId : "");
        return "admin-live-monitor";
    }
}
