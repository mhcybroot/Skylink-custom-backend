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

    @GetMapping
    public String viewDashboard(Model model) {
        DashboardStatsDTO stats = paymentDashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "payment-request/dashboard"; // Maps to resources/templates/payment-request/dashboard.html
    }
}
