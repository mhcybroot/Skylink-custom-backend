package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;
import root.cyb.mh.attendancesystem.repository.EmployeeBrowseHistoryRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.SystemSettingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/extension")
@CrossOrigin(origins = "*")
public class BrowseHistoryApiController {

    @Autowired
    private EmployeeBrowseHistoryRepository historyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    // We assume the auth header validation is handled via an interceptor or we check it manually if needed.
    // However, the current extension logic sends Authorization header but for this demo, 
    // we'll extract employee from Authorization token (if it's simple ID) or we need to pass employeeId.
    // Let's assume the Authorization header holds the token which maps to an Employee.
    // In ExtensionApiController, how is auth handled? Let's check ExtensionApiController.

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        String syncInterval = systemSettingRepository.findById("browse_history_sync_interval")
                .map(root.cyb.mh.attendancesystem.model.SystemSetting::getValue).orElse("60");
        return ResponseEntity.ok(Map.of("syncInterval", Integer.parseInt(syncInterval)));
    }

    @PostMapping("/browse-history")
    public ResponseEntity<?> syncBrowseHistory(org.springframework.security.core.Authentication authentication,
                                               @RequestBody List<HistoryItemRequest> items) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String currentUserId = authentication.getName();
        Optional<Employee> optEmployee = employeeRepository.findById(currentUserId);
        
        if (optEmployee.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        }
        
        Employee employee = optEmployee.get();
        List<EmployeeBrowseHistory> historyList = new ArrayList<>();
        
        for (HistoryItemRequest item : items) {
            EmployeeBrowseHistory history = new EmployeeBrowseHistory();
            history.setEmployee(employee);
            history.setUrl(item.url);
            history.setPageTitle(item.title);
            try {
                history.setTimestamp(LocalDateTime.parse(item.timestamp, java.time.format.DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception e) {
                history.setTimestamp(LocalDateTime.now());
            }
            historyList.add(history);
        }
        
        historyRepository.saveAll(historyList);

        return ResponseEntity.ok(Map.of("success", true, "count", historyList.size()));
    }

    public static class HistoryItemRequest {
        public String url;
        public String title;
        public String timestamp; // ISO format
    }
}
