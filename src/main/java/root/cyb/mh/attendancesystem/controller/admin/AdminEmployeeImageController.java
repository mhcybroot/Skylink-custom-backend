package root.cyb.mh.attendancesystem.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.service.EmployeeImageService;

@Controller
@RequestMapping("/admin/employee-images-vault")
public class AdminEmployeeImageController {

    @Autowired
    private EmployeeImageService employeeImageService;

    @GetMapping
    public String viewVault(
            @RequestParam(required = false) String employee,
            Model model) {
        
        model.addAttribute("images", employeeImageService.searchImages(employee));
        model.addAttribute("employees", employeeImageService.getDistinctEmployeeUsernames());
        model.addAttribute("selectedEmployee", employee);
        
        return "admin/employee-images-vault";
    }
}
