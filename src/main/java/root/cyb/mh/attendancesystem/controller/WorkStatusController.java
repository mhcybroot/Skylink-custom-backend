package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.model.WorkStatus;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/employee/work-status")
public class WorkStatusController {

    @Autowired
    private EmployeeDailyWorkStatusRepository statusRepository;

    @PostMapping("/start-work")
    public String startWork(Principal principal, RedirectAttributes redirectAttributes) {
        String employeeId = principal.getName();
        LocalDate today = LocalDate.now();
        EmployeeDailyWorkStatus status = statusRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElse(new EmployeeDailyWorkStatus(employeeId, today));

        if (status.getStatus() == WorkStatus.LOGGED_IN || status.getStatus() == WorkStatus.ENTERED_OFFICE) {
            status.setStatus(WorkStatus.WORKING);
            status.setWorkStartTime(LocalDateTime.now());
            statusRepository.save(status);
            redirectAttributes.addFlashAttribute("success", "Work started successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot start work from current status: " + status.getStatus());
        }

        return "redirect:/employee/dashboard";
    }

    @PostMapping("/start-break")
    public String startBreak(Principal principal, RedirectAttributes redirectAttributes) {
        String employeeId = principal.getName();
        LocalDate today = LocalDate.now();
        EmployeeDailyWorkStatus status = statusRepository.findByEmployeeIdAndDate(employeeId, today).orElse(null);

        if (status != null && status.getStatus() == WorkStatus.WORKING) {
            status.setStatus(WorkStatus.ON_BREAK);
            status.setCurrentBreakStartTime(LocalDateTime.now());
            statusRepository.save(status);
            redirectAttributes.addFlashAttribute("success", "Break started.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot start break now.");
        }

        return "redirect:/employee/dashboard";
    }

    @PostMapping("/restart-work")
    public String restartWork(Principal principal, RedirectAttributes redirectAttributes) {
        String employeeId = principal.getName();
        LocalDate today = LocalDate.now();
        EmployeeDailyWorkStatus status = statusRepository.findByEmployeeIdAndDate(employeeId, today).orElse(null);

        if (status != null && status.getStatus() == WorkStatus.ON_BREAK) {
            long breakMins = ChronoUnit.MINUTES.between(status.getCurrentBreakStartTime(), LocalDateTime.now());
            status.setTotalBreakMinutes(status.getTotalBreakMinutes() + (int) breakMins);
            status.setCurrentBreakStartTime(null);
            status.setStatus(WorkStatus.WORKING);
            statusRepository.save(status);
            redirectAttributes.addFlashAttribute("success", "Work restarted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot restart work now.");
        }

        return "redirect:/employee/dashboard";
    }

    @PostMapping("/end-work")
    public String endWork(Principal principal, RedirectAttributes redirectAttributes) {
        String employeeId = principal.getName();
        LocalDate today = LocalDate.now();
        EmployeeDailyWorkStatus status = statusRepository.findByEmployeeIdAndDate(employeeId, today).orElse(null);

        if (status != null && (status.getStatus() == WorkStatus.WORKING || status.getStatus() == WorkStatus.ON_BREAK)) {
            // If they were on break while ending work, silently add the break time
            if (status.getStatus() == WorkStatus.ON_BREAK && status.getCurrentBreakStartTime() != null) {
                long breakMins = ChronoUnit.MINUTES.between(status.getCurrentBreakStartTime(), LocalDateTime.now());
                status.setTotalBreakMinutes(status.getTotalBreakMinutes() + (int) breakMins);
                status.setCurrentBreakStartTime(null);
            }
            status.setStatus(WorkStatus.ENDED_WORK);
            status.setWorkEndTime(LocalDateTime.now());
            statusRepository.save(status);
            redirectAttributes.addFlashAttribute("success",
                    "Work ended. Please punch out at the attendance machine within 30 minutes.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot end work from current state.");
        }

        return "redirect:/employee/dashboard";
    }
}
