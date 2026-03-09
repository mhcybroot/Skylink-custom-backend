package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.model.WorkStatus;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkStatusLifecycleService {

    @Autowired
    private EmployeeDailyWorkStatusRepository statusRepository;

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void sweepMissingPunchOuts() {
        List<EmployeeDailyWorkStatus> endedStatuses = statusRepository.findByStatus(WorkStatus.ENDED_WORK);
        LocalDateTime now = LocalDateTime.now();

        for (EmployeeDailyWorkStatus status : endedStatuses) {
            if (status.getWorkEndTime() != null && now.isAfter(status.getWorkEndTime().plusMinutes(30))) {
                status.setStatus(WorkStatus.LEFT_WITHOUT_PUNCH);
                statusRepository.save(status);
                System.out.println("Flagged Employee " + status.getEmployeeId()
                        + " as LEFT_WITHOUT_PUNCH for failing to punch within 30 mins.");
            }
        }
    }
}
