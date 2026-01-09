package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import root.cyb.mh.attendancesystem.repository.WorkOrderRepository;

@Controller
@RequestMapping("/admin/work-orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @GetMapping
    public String listWorkOrders(Model model) {
        model.addAttribute("workOrders", workOrderRepository.findAll());
        model.addAttribute("activeLink", "work-orders");
        return "work-order/list";
    }
}
