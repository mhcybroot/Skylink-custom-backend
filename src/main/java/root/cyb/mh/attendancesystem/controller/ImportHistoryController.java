package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import root.cyb.mh.attendancesystem.model.ImportLog;
import root.cyb.mh.attendancesystem.repository.ImportLogRepository;
import root.cyb.mh.attendancesystem.repository.WorkOrderRepository;

import java.util.List;

@Controller
@RequestMapping("/admin/imports")
public class ImportHistoryController {

    @Autowired
    private ImportLogRepository importLogRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @GetMapping
    public String listImports(Model model) {
        List<ImportLog> logs = importLogRepository.findAllByOrderByImportDateDesc();
        model.addAttribute("logs", logs);
        model.addAttribute("activeLink", "import-history");
        return "admin/import-history";
    }

    @PostMapping("/{id}/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteImport(@PathVariable Long id) {
        // 1. Delete all WorkOrders with this batch ID
        workOrderRepository.deleteByImportBatchId(id);

        // 2. Delete the log entry
        importLogRepository.deleteById(id);

        return "redirect:/admin/imports?success=deleted";
    }

    @PostMapping("/cleanup")
    @org.springframework.transaction.annotation.Transactional
    public String cleanupLegacyData() {
        workOrderRepository.deleteByImportBatchIdIsNull();
        return "redirect:/admin/imports?success=cleanup";
    }
}
