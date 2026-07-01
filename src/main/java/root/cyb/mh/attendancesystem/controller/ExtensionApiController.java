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
import root.cyb.mh.attendancesystem.dto.ExtensionVaultDto;
import root.cyb.mh.attendancesystem.dto.ExtensionVaultDto.FolderDto;
import root.cyb.mh.attendancesystem.dto.ExtensionVaultDto.ResourceDto;
import root.cyb.mh.attendancesystem.model.ResourceFolder;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;
import root.cyb.mh.attendancesystem.repository.ResourceFolderRepository;
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

    @Autowired
    private ResourceFolderRepository resourceFolderRepository;

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

    @GetMapping("/vault")
    public ResponseEntity<ExtensionVaultDto> getVault(Authentication authentication) {
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

        List<FolderDto> folderDtos = new ArrayList<>();
        if (!folderIds.isEmpty()) {
            List<ResourceFolder> folders = resourceFolderRepository.findAllById(folderIds);
            for (ResourceFolder folder : folders) {
                Long parentId = (folder.getParentFolder() != null) ? folder.getParentFolder().getId() : null;
                folderDtos.add(new FolderDto(folder.getId(), folder.getName(), parentId));
            }
        }

        List<ResourceDto> resourceDtos = new ArrayList<>();
        for (SharedResource resource : resources) {
            Long folderId = (resource.getFolder() != null) ? resource.getFolder().getId() : null;
            resourceDtos.add(new ResourceDto(
                resource.getId(),
                resource.getResourceName(),
                resource.getResourceLink(),
                resource.getLoginId(),
                resource.getPassword(),
                folderId
            ));
        }

        ExtensionVaultDto dto = new ExtensionVaultDto(folderDtos, resourceDtos);
        return ResponseEntity.ok(dto);
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
    @GetMapping("/attendance-history")
    public ResponseEntity<?> getAttendanceHistory(
            @org.springframework.web.bind.annotation.RequestParam(name = "year", required = false) Integer year,
            @org.springframework.web.bind.annotation.RequestParam(name = "month", required = false) Integer month,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String employeeId = authentication.getName();
        LocalDate now = LocalDate.now();

        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        root.cyb.mh.attendancesystem.dto.EmployeeRangeReportDto rangeData = reportService
                .getEmployeeRangeReport(employeeId, startDate, endDate);

        return ResponseEntity.ok(rangeData);
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.web.bind.annotation.PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
        }

        String employeeId = authentication.getName();
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);

        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        Employee employee = employeeOpt.get();

        if (!passwordEncoder.matches(oldPassword, employee.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect old password"));
        }

        employee.setUsername(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @Autowired
    private root.cyb.mh.attendancesystem.service.PhoneNotificationService phoneNotificationService;

    @org.springframework.web.bind.annotation.PostMapping("/phone-notifications")
    public ResponseEntity<?> receivePhoneNotification(
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String employeeId = authentication.getName();
        String packageName = payload.get("packageName");
        String title = payload.get("title");
        String text = payload.get("text");

        if (packageName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "packageName is required"));
        }

        root.cyb.mh.attendancesystem.model.PhoneNotification notification = new root.cyb.mh.attendancesystem.model.PhoneNotification();
        notification.setEmployeeUsername(employeeId);
        notification.setPackageName(packageName);
        notification.setTitle(title);
        notification.setText(text);
        
        phoneNotificationService.saveNotification(notification);

        return ResponseEntity.ok(Map.of("success", true));
    }
}

