package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.repository.EmployeeBrowseHistoryRepository;
import root.cyb.mh.attendancesystem.repository.SystemSettingRepository;

import java.time.LocalDateTime;

@Service
public class BrowseHistoryCleanupService {

    @Autowired
    private EmployeeBrowseHistoryRepository historyRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    // Run every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldHistory() {
        System.out.println("====== Running Browse History Cleanup ======");
        
        int retentionDays = 90; // Default
        try {
            String daysStr = systemSettingRepository.findById("browse_history_retention_days")
                    .map(root.cyb.mh.attendancesystem.model.SystemSetting::getValue).orElse("90");
            retentionDays = Integer.parseInt(daysStr);
        } catch (Exception e) {
            System.err.println("Failed to parse retention days, using default 90");
        }
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        
        try {
            historyRepository.deleteOlderThan(threshold);
            System.out.println("Successfully deleted browse history older than " + retentionDays + " days.");
        } catch (Exception e) {
            System.err.println("Failed to clean up browse history: " + e.getMessage());
        }
    }
}
