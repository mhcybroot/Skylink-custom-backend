package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;
import root.cyb.mh.attendancesystem.repository.EmployeeBrowseHistoryRepository;

@Controller
public class BrowseHistoryController {

    @Autowired
    private EmployeeBrowseHistoryRepository historyRepository;

    @GetMapping("/admin/browse-history")
    public String getBrowseHistory(Model model, 
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EmployeeBrowseHistory> historyPage = historyRepository.findAllByOrderByTimestampDesc(pageable);
        
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("activeLink", "browse-history");
        
        return "admin-browse-history";
    }
}
