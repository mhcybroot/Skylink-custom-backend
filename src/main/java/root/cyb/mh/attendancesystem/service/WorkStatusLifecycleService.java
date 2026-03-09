package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.model.WorkStatus;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkStatusLifecycleService {

    @Autowired
    private EmployeeDailyWorkStatusRepository statusRepository;

    // Run every 5 minutes — expire ENDED_WORK that didn't punch out within 30 mins
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

    // Run daily at 00:05 AM — expire any active statuses from YESTERDAY that were
    // never closed
    @Scheduled(cron = "0 5 0 * * *")
    public void expireStaleYesterdayStatuses() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<EmployeeDailyWorkStatus> allYesterday = statusRepository.findAll().stream()
                .filter(s -> s.getDate().equals(yesterday))
                .filter(s -> s.getStatus() == WorkStatus.WORKING
                        || s.getStatus() == WorkStatus.ON_BREAK
                        || s.getStatus() == WorkStatus.ENTERED_OFFICE
                        || s.getStatus() == WorkStatus.LOGGED_IN
                        || s.getStatus() == WorkStatus.ENDED_WORK)
                .collect(java.util.stream.Collectors.toList());

        for (EmployeeDailyWorkStatus status : allYesterday) {
            if (status.getStatus() == WorkStatus.ON_BREAK && status.getCurrentBreakStartTime() != null) {
                long breakMins = java.time.temporal.ChronoUnit.MINUTES
                        .between(status.getCurrentBreakStartTime(),
                                LocalDateTime.of(yesterday, java.time.LocalTime.of(23, 59)));
                status.setTotalBreakMinutes(status.getTotalBreakMinutes() + (int) breakMins);
                status.setCurrentBreakStartTime(null);
            }
            // If already ENDED_WORK, check active hours for COMPLETED or INCOMPLETE
            if (status.getStatus() == WorkStatus.ENDED_WORK && status.getWorkStartTime() != null
                    && status.getWorkEndTime() != null) {
                long totalMins = java.time.temporal.ChronoUnit.MINUTES.between(status.getWorkStartTime(),
                        status.getWorkEndTime());
                long activeMins = totalMins - status.getTotalBreakMinutes();
                status.setStatus(activeMins >= 480 ? WorkStatus.COMPLETED_DAY : WorkStatus.INCOMPLETE_SHIFT);
            } else {
                status.setStatus(WorkStatus.LEFT_WITHOUT_PUNCH);
            }
            statusRepository.save(status);
            System.out.println("Nightly reset: Expired stale status for Employee " + status.getEmployeeId());
        }
    }
}
