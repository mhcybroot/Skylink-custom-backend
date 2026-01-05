package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import root.cyb.mh.attendancesystem.model.dto.DashboardStatsDTO;
import root.cyb.mh.attendancesystem.service.PaymentDashboardService;

@Controller
@RequestMapping("/admin/payment-dashboard")
public class PaymentDashboardController {

    @Autowired
    private PaymentDashboardService paymentDashboardService;

    @Autowired
    private root.cyb.mh.attendancesystem.service.SystemSettingService systemSettingService;

    @GetMapping
    public String viewDashboard(Model model) {
        DashboardStatsDTO stats = paymentDashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "payment-request/dashboard"; // Maps to resources/templates/payment-request/dashboard.html
    }

    @org.springframework.web.bind.annotation.PostMapping("/settings")
    public String updateSettings(
            @org.springframework.web.bind.annotation.RequestParam("highValueThreshold") String highValueThreshold) {
        systemSettingService.setValue("DASHBOARD_HIGH_VALUE_THRESHOLD", highValueThreshold,
                "Threshold for High Value Requests on Dashboard");
        return "redirect:/admin/payment-dashboard";
    }
}
