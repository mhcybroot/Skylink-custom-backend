package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.model.SharedResource;
import root.cyb.mh.attendancesystem.dto.EmployeeMonthlyDetailDto;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;
import root.cyb.mh.attendancesystem.repository.SharedResourceRepository;
import root.cyb.mh.attendancesystem.service.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/extension")
public class ExtensionApiController {

    @Autowired
    private SharedResourceRepository sharedResourceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeDailyWorkStatusRepository workStatusRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private root.cyb.mh.attendancesystem.service.ResourceFolderService resourceFolderService;

    @GetMapping("/credentials")
    public ResponseEntity<List<SharedResource>> getMyCredentials(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String currentUserId = authentication.getName();
        Optional<Employee> empOpt = employeeRepository.findById(currentUserId);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        List<Long> folderIds = resourceFolderService.getAllAccessibleFolderIdsForEmployee(empOpt.get());
        List<SharedResource> resources;
        if (folderIds.isEmpty()) {
            resources = sharedResourceRepository.findByEmployeeId(currentUserId);
        } else {
            resources = sharedResourceRepository.findByEmployeeIdOrFolderIdIn(currentUserId, folderIds);
        }
        
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/session-status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String currentUserId = authentication.getName();
        Optional<Employee> empOpt = employeeRepository.findById(currentUserId);

        if (empOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Employee emp = empOpt.get();
        if (emp.isExtensionForceLogout()) {
            // Admin requested force-logout: reset flag and tell extension to logout
            emp.setExtensionForceLogout(false);
            employeeRepository.save(emp);
            return ResponseEntity.ok(Map.of("active", false));
        }

        return ResponseEntity.ok(Map.of("active", true));
    }

    @GetMapping("/dashboard-status")
    public ResponseEntity<Map<String, Object>> getDashboardStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String employeeId = authentication.getName();

        // Use the JVM's default timezone (which is set to app.timezone in TimeZoneConfig)
        ZoneId serverZone = ZoneId.systemDefault();
        ZonedDateTime serverNow = ZonedDateTime.now(serverZone);
        LocalDate today = serverNow.toLocalDate();

        EmployeeDailyWorkStatus dailyStatus = workStatusRepository
                .findByEmployeeIdAndDate(employeeId, today)
                .orElse(new EmployeeDailyWorkStatus(employeeId, today));

        String status = dailyStatus.getStatus().name();
        int shiftDurationSeconds = 8 * 60 * 60; // 8-hour shift
        int totalBreakSeconds = dailyStatus.getTotalBreakSeconds();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("serverTimeISO", serverNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("shiftDurationSeconds", shiftDurationSeconds);
        result.put("totalBreakSeconds", totalBreakSeconds);

        if (dailyStatus.getWorkStartTime() != null) {
            ZonedDateTime workStart = dailyStatus.getWorkStartTime().atZone(serverZone);
            result.put("workStartISO", workStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            long elapsedTotal = ChronoUnit.SECONDS.between(workStart, serverNow);

            // If on break, subtract active break time
            long activeBreakSeconds = 0;
            if (dailyStatus.getCurrentBreakStartTime() != null && "ON_BREAK".equals(status)) {
                ZonedDateTime breakStart = dailyStatus.getCurrentBreakStartTime().atZone(serverZone);
                activeBreakSeconds = ChronoUnit.SECONDS.between(breakStart, serverNow);
                result.put("breakStartISO", breakStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }

            long elapsedWorkSeconds = Math.max(0, elapsedTotal - totalBreakSeconds - activeBreakSeconds);
            long remainingSeconds = Math.max(0, shiftDurationSeconds - elapsedWorkSeconds);
            double progressPercent = Math.min(100.0, (elapsedWorkSeconds * 100.0) / shiftDurationSeconds);

            result.put("elapsedWorkSeconds", elapsedWorkSeconds);
            result.put("remainingSeconds", remainingSeconds);
            result.put("progressPercent", Math.round(progressPercent * 10.0) / 10.0);
        } else {
            result.put("elapsedWorkSeconds", 0);
            result.put("remainingSeconds", shiftDurationSeconds);
            result.put("progressPercent", 0.0);
        }

        // Monthly Stats
        EmployeeMonthlyDetailDto monthlyReport = reportService.getEmployeeMonthlyReport(employeeId, today.getYear(), today.getMonthValue());
        if (monthlyReport != null) {
            result.put("daysPresent", monthlyReport.getTotalPresent());
            result.put("lateCount", monthlyReport.getTotalLates());
            result.put("earlyCount", monthlyReport.getTotalEarlyLeaves());
            result.put("leaveCount", monthlyReport.getTotalLeaves());
        } else {
            result.put("daysPresent", 0);
            result.put("lateCount", 0);
            result.put("earlyCount", 0);
            result.put("leaveCount", 0);
        }

        return ResponseEntity.ok(result);
    }
}
