package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.model.WorkStatus;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/work-status")
// Protect these endpoints so only admins & HR can access them
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class AdminWorkStatusController {

    @Autowired
    private EmployeeDailyWorkStatusRepository workStatusRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/realtime")
    public List<RealtimeWorkStatusDto> getRealtimeStatus() {
        LocalDate today = LocalDate.now();
        List<Employee> allEmployees = employeeRepository.findAll();

        Map<String, EmployeeDailyWorkStatus> statusMap = workStatusRepository.findAll().stream()
                // Only todays records
                .filter(s -> s.getDate().equals(today))
                .collect(Collectors.toMap(EmployeeDailyWorkStatus::getEmployeeId, s -> s));

        return allEmployees.stream()
                .filter(emp -> !emp.isGuest())
                .map(emp -> {
                    RealtimeWorkStatusDto dto = new RealtimeWorkStatusDto();
                    dto.setEmployeeId(emp.getId());
                    dto.setName(emp.getName());
                    dto.setDepartment(emp.getDepartment() != null ? emp.getDepartment().getName() : "-");

                    String p = emp.getAvatarPath();
                    if (p == null && emp.getPhotoBase64() != null) {
                        p = "data:image/jpeg;base64," + emp.getPhotoBase64();
                    }
                    dto.setPhotoUrl(p);

                    EmployeeDailyWorkStatus status = statusMap.get(emp.getId());
                    if (status != null) {
                        dto.setStatus(status.getStatus().name());

                        LocalDateTime now = LocalDateTime.now();

                        // Accumulate current break if they are presently on break
                        int liveTotalBreak = status.getTotalBreakMinutes();
                        if (status.getStatus() == WorkStatus.ON_BREAK && status.getCurrentBreakStartTime() != null) {
                            liveTotalBreak += (int) ChronoUnit.MINUTES.between(status.getCurrentBreakStartTime(), now);
                        }
                        dto.setBreakDurationMins(liveTotalBreak);

                        // Accumulate work time
                        if (status.getWorkStartTime() != null) {
                            LocalDateTime endPoint = now;
                            if (status.getWorkEndTime() != null) {
                                endPoint = status.getWorkEndTime();
                            }
                            int totalLiveMins = (int) ChronoUnit.MINUTES.between(status.getWorkStartTime(), endPoint);
                            int actualWorkMins = Math.max(0, totalLiveMins - liveTotalBreak);
                            dto.setWorkDurationMins(actualWorkMins);
                        } else {
                            dto.setWorkDurationMins(0);
                        }

                    } else {
                        dto.setStatus("NOT_ENTERED");
                        dto.setWorkDurationMins(0);
                        dto.setBreakDurationMins(0);
                    }

                    return dto;
                })
                .sorted(Comparator.comparing(RealtimeWorkStatusDto::getName))
                .collect(Collectors.toList());
    }

    @PostMapping("/force-end/{employeeId}")
    public ResponseEntity<String> forceEndShift(@PathVariable String employeeId) {
        LocalDate today = LocalDate.now();
        EmployeeDailyWorkStatus status = workStatusRepository.findByEmployeeIdAndDate(employeeId, today).orElse(null);

        if (status != null && (status.getStatus() == WorkStatus.WORKING || status.getStatus() == WorkStatus.ON_BREAK)) {
            if (status.getStatus() == WorkStatus.ON_BREAK && status.getCurrentBreakStartTime() != null) {
                long breakMins = ChronoUnit.MINUTES.between(status.getCurrentBreakStartTime(), LocalDateTime.now());
                status.setTotalBreakMinutes(status.getTotalBreakMinutes() + (int) breakMins);
                status.setCurrentBreakStartTime(null);
            }
            status.setStatus(WorkStatus.ENDED_WORK);
            status.setWorkEndTime(LocalDateTime.now());
            workStatusRepository.save(status);
            return ResponseEntity.ok("Successfully ended shift for employee " + employeeId);
        }

        return ResponseEntity.badRequest().body("Employee is not currently working or on break.");
    }

    // DTO for returning flattened Realtime data
    public static class RealtimeWorkStatusDto {
        private String employeeId;
        private String name;
        private String department;
        private String photoUrl;
        private String status;
        private int workDurationMins;
        private int breakDurationMins;

        // Getters and Setters
        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getWorkDurationMins() {
            return workDurationMins;
        }

        public void setWorkDurationMins(int workDurationMins) {
            this.workDurationMins = workDurationMins;
        }

        public int getBreakDurationMins() {
            return breakDurationMins;
        }

        public void setBreakDurationMins(int breakDurationMins) {
            this.breakDurationMins = breakDurationMins;
        }
    }
}
