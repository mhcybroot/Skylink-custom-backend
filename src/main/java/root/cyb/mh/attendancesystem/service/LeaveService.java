package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.LeaveRequest;
import root.cyb.mh.attendancesystem.repository.LeaveRequestRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.Comparator;
import root.cyb.mh.attendancesystem.dto.EmployeeLeaveQuotaDto;
import root.cyb.mh.attendancesystem.dto.MonthlyLeaveBreakdownDto;
import java.time.YearMonth;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;

@Service
public class LeaveService {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.UserRepository userRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.WorkScheduleRepository workScheduleRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.PublicHolidayRepository publicHolidayRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository employeeDailyWorkStatusRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository;

    public LeaveRequest createRequest(Employee employee, LeaveRequest request) {
        request.setEmployee(employee);
        request.setStatus(LeaveRequest.Status.PENDING);
        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        // Notify Supervisor
        if (employee.getReportsTo() != null) {
            sendNewRequestNotification(employee.getReportsTo().getId(), savedRequest);
        }

        // Notify Assistant Supervisor
        if (employee.getReportsToAssistant() != null) {
            sendNewRequestNotification(employee.getReportsToAssistant().getId(), savedRequest);
        }

        // Notify HRs
        List<root.cyb.mh.attendancesystem.model.User> hrUsers = userRepository.findByRole("HR");
        for (root.cyb.mh.attendancesystem.model.User hr : hrUsers) {
            sendNewRequestNotification(hr.getUsername(), savedRequest);
        }

        return savedRequest;
    }

    private void sendNewRequestNotification(String recipientUsername, LeaveRequest request) {
        String title = "New Leave Request";
        String message = String.format("%s has requested leave from %s to %s.",
                request.getEmployee().getName(), request.getStartDate(), request.getEndDate());
        String link = "/leave/manage"; // Link for approvers to manage requests

        try {
            notificationService.sendNotification(
                    recipientUsername,
                    title,
                    message,
                    "LEAVE_NEW",
                    link);
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public List<LeaveRequest> getEmployeeHistory(String employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public List<LeaveRequest> getAllRequests() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<LeaveRequest> getRequestsForApprover(String approverId) {
        // Return requests where user is Primary OR Assistant
        return leaveRequestRepository.findByEmployee_ReportsTo_IdOrEmployee_ReportsToAssistant_Id(approverId,
                approverId);
    }

    public Optional<LeaveRequest> findById(Long id) {
        return leaveRequestRepository.findById(id);
    }

    public void updateStatus(Long requestId, LeaveRequest.Status newStatus, String comment, String reviewerRole,
            String reviewerEmail) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Request ID"));

        // HR Logic: Can only update if PENDING
        if ("ROLE_HR".equals(reviewerRole)) {
            if (request.getStatus() != LeaveRequest.Status.PENDING) {
                throw new IllegalStateException("HR cannot modify a request that is already processed.");
            }
        }

        // Admin Logic: Can override anything (no Check)

        request.setStatus(newStatus);
        request.setAdminComment(comment); // Overwrites previous comment if any
        request.setReviewedBy(reviewerEmail + " (" + reviewerRole + ")");

        leaveRequestRepository.save(request);

        // Notify Employee
        String title = "Leave Request Updated";
        String message = String.format("Your leave request for %s to %s has been %s by %s.",
                request.getStartDate(), request.getEndDate(), newStatus, reviewerRole);
        String link = "/leave/employee"; // Link to their leave history

        try {
            notificationService.sendNotification(
                    request.getEmployee().getId(), // Use ID as Principal
                    title,
                    message,
                    "LEAVE_UPDATE",
                    link);
        } catch (Exception e) {
            // Log error but don't fail transaction
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    public void deleteRequest(Long id) {
        leaveRequestRepository.deleteById(id);
    }

    public List<EmployeeLeaveQuotaDto> calculateAllEmployeeLeaveQuotas(LocalDate today, List<Employee> allEmployees) {
        // --- Annual Leave Quotas (Optimized Single Query) ---
        root.cyb.mh.attendancesystem.model.WorkSchedule globalSchedule = workScheduleRepository.findAll().stream().findFirst().orElse(new root.cyb.mh.attendancesystem.model.WorkSchedule());
        int defaultQuota = globalSchedule.getDefaultAnnualLeaveQuota() != null ? globalSchedule.getDefaultAnnualLeaveQuota() : 12;
        String weekendDays = globalSchedule.getWeekendDays() != null ? globalSchedule.getWeekendDays() : "";

        LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);
        
        // Pre-fetch public holidays
        List<root.cyb.mh.attendancesystem.model.PublicHoliday> publicHolidays = publicHolidayRepository.findAll();
        
        // Pre-fetch work statuses to find out which days employees were present
        List<root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus> yearStatuses = 
                        employeeDailyWorkStatusRepository.findByDateBetween(startOfYear, today);
        
        Map<String, Set<LocalDate>> presentDatesByEmployee = yearStatuses.stream()
                        .collect(Collectors.groupingBy(
                                        root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus::getEmployeeId,
                                        Collectors.mapping(root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus::getDate, Collectors.toSet())
                        ));

        List<root.cyb.mh.attendancesystem.model.LeaveRequest> allApprovedLeavesThisYear = leaveRequestRepository.findApprovedLeavesInYear(startOfYear, endOfYear);

        // Group leaves by employee ID
        Map<String, List<root.cyb.mh.attendancesystem.model.LeaveRequest>> leavesByEmployee = allApprovedLeavesThisYear.stream()
                .collect(Collectors.groupingBy(l -> l.getEmployee().getId()));

        return allEmployees.stream()
                .filter(e -> !e.isGuest())
                .map(emp -> {
                    int effectiveQuota = emp.getEffectiveQuota(defaultQuota);
                    List<root.cyb.mh.attendancesystem.model.LeaveRequest> empLeaves = leavesByEmployee.getOrDefault(emp.getId(), java.util.Collections.emptyList());
                    
                    int totalTaken = 0, sickTaken = 0, casualTaken = 0, otherTaken = 0;
                    
                    for (root.cyb.mh.attendancesystem.model.LeaveRequest req : empLeaves) {
                        // Cap to current year only
                        LocalDate start = req.getStartDate().isBefore(startOfYear) ? startOfYear : req.getStartDate();
                        LocalDate end = req.getEndDate().isAfter(endOfYear) ? endOfYear : req.getEndDate();
                        
                        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                        totalTaken += days;
                        
                        String type = req.getLeaveType() != null ? req.getLeaveType().toLowerCase() : "";
                        if (type.contains("sick")) {
                            sickTaken += days;
                        } else if (type.contains("casual")) {
                            casualTaken += days;
                        } else {
                            otherTaken += days;
                        }
                    }
                    
                    int paidTaken = Math.min(totalTaken, effectiveQuota);
                    int unpaidTaken = Math.max(0, totalTaken - effectiveQuota);
                    double percentage = effectiveQuota > 0 ? (totalTaken * 100.0 / effectiveQuota) : 0;
                    
                    // Calculate Absent Days
                    int absentDays = 0;
                    LocalDate startDate = emp.getJoiningDate() != null && emp.getJoiningDate().getYear() == today.getYear() 
                                    ? emp.getJoiningDate() : startOfYear;
                    LocalDate endDate = today; 
                    
                    Set<LocalDate> empPresentDates = presentDatesByEmployee.getOrDefault(emp.getId(), Set.of());
                    
                    LocalDate d = startDate;
                    while (!d.isAfter(endDate)) {
                            final LocalDate checkDate = d;
                            // 1. Skip Weekends
                            if (weekendDays.contains(String.valueOf(checkDate.getDayOfWeek().getValue()))) {
                                    d = d.plusDays(1);
                                    continue;
                            }
                            // 2. Skip Public Holidays
                            if (publicHolidays.stream().anyMatch(h -> h.getDate().equals(checkDate))) {
                                    d = d.plusDays(1);
                                    continue;
                            }
                            // 3. Skip if Present
                            if (empPresentDates.contains(checkDate)) {
                                    d = d.plusDays(1);
                                    continue;
                            }
                            // 4. Skip if on Approved Leave
                            boolean onLeave = empLeaves.stream().anyMatch(l -> 
                                            !checkDate.isBefore(l.getStartDate()) && !checkDate.isAfter(l.getEndDate())
                            );
                            if (onLeave) {
                                    d = d.plusDays(1);
                                    continue;
                            }
                            
                            // Otherwise, they were absent
                            absentDays++;
                            d = d.plusDays(1);
                    }
                    
                    EmployeeLeaveQuotaDto dto = new EmployeeLeaveQuotaDto();
                    dto.setEmployeeId(emp.getId());
                    dto.setEmployeeName(emp.getName());
                    dto.setAnnualQuota(effectiveQuota);
                    dto.setYearlyLeavesTaken(totalTaken);
                    dto.setPaidLeavesTaken(paidTaken);
                    dto.setUnpaidLeavesTaken(unpaidTaken);
                    dto.setAbsentDays(absentDays);
                    dto.setSickLeavesTaken(sickTaken);
                    dto.setCasualLeavesTaken(casualTaken);
                    dto.setOtherLeavesTaken(otherTaken);
                    dto.setUsagePercentage(percentage);
                    
                    return dto;
                })
                .sorted(Comparator.comparing(EmployeeLeaveQuotaDto::getUsagePercentage).reversed())
                .collect(Collectors.toList());
    }
    public List<MonthlyLeaveBreakdownDto> calculateMonthlyBreakdown(Employee employee, int year) {
        root.cyb.mh.attendancesystem.model.WorkSchedule globalSchedule = workScheduleRepository.findAll().stream().findFirst().orElse(new root.cyb.mh.attendancesystem.model.WorkSchedule());
        String weekendDays = globalSchedule.getWeekendDays() != null ? globalSchedule.getWeekendDays() : "";
        List<root.cyb.mh.attendancesystem.model.PublicHoliday> publicHolidays = publicHolidayRepository.findAll();
        
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        
        List<root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus> yearStatuses = 
                employeeDailyWorkStatusRepository.findByDateBetween(startOfYear, LocalDate.now());
        Set<LocalDate> presentDates = yearStatuses.stream()
                .filter(s -> s.getEmployeeId().equals(employee.getId()))
                .map(root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus::getDate)
                .collect(Collectors.toSet());
                
        List<LeaveRequest> empLeaves = leaveRequestRepository.findApprovedLeavesInYear(startOfYear, endOfYear).stream()
                .filter(l -> l.getEmployee().getId().equals(employee.getId()))
                .collect(Collectors.toList());

        List<MonthlyLeaveBreakdownDto> breakdown = new ArrayList<>();
        int effectiveQuota = employee.getEffectiveQuota(globalSchedule.getDefaultAnnualLeaveQuota() != null ? globalSchedule.getDefaultAnnualLeaveQuota() : 12);
        
        int cumulativeLeavesTaken = 0; // to calculate paid vs unpaid
        LocalDate joiningDate = employee.getJoiningDate() != null ? employee.getJoiningDate() : startOfYear;

        for (int m = 1; m <= 12; m++) {
            Month month = Month.of(m);
            MonthlyLeaveBreakdownDto dto = new MonthlyLeaveBreakdownDto();
            dto.setMonth(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            dto.setMonthValue(m);
            dto.setYear(year);
            
            LocalDate startOfMonth = LocalDate.of(year, m, 1);
            LocalDate endOfMonth = YearMonth.of(year, m).atEndOfMonth();
            
            int totalLeaves = 0;
            
            for (LeaveRequest req : empLeaves) {
                LocalDate reqStart = req.getStartDate().isBefore(startOfMonth) ? startOfMonth : req.getStartDate();
                LocalDate reqEnd = req.getEndDate().isAfter(endOfMonth) ? endOfMonth : req.getEndDate();
                
                if (!reqStart.isAfter(reqEnd)) {
                    LocalDate d = reqStart;
                    while (!d.isAfter(reqEnd)) {
                        totalLeaves++;
                        String type = req.getLeaveType() != null ? req.getLeaveType().toLowerCase() : "";
                        
                        // Construct HTML proof for tooltip
                        StringBuilder proofBuilder = new StringBuilder("<div class='text-start' style='min-width: 200px;'>");
                        proofBuilder.append("<strong>Type:</strong> ").append(req.getLeaveType() != null ? req.getLeaveType() : "N/A").append("<br/>");
                        proofBuilder.append("<strong>Applied:</strong> ").append(req.getCreatedAt() != null ? req.getCreatedAt().toLocalDate().toString() : "N/A").append("<br/>");
                        proofBuilder.append("<strong>Approver:</strong> ").append(req.getReviewedBy() != null ? req.getReviewedBy() : "Pending/Auto").append("<br/>");
                        proofBuilder.append("<strong>Reason:</strong> ").append(req.getReason() != null ? req.getReason() : "N/A").append("<br/>");
                        if (req.getAdminComment() != null && !req.getAdminComment().isEmpty()) {
                            proofBuilder.append("<strong>Admin Note:</strong> ").append(req.getAdminComment()).append("<br/>");
                        }
                        proofBuilder.append("</div>");
                        
                        String proof = proofBuilder.toString();
                        MonthlyLeaveBreakdownDto.DateProof dp = new MonthlyLeaveBreakdownDto.DateProof(d.toString(), proof);
                        
                        if (type.contains("sick")) dto.getSickDates().add(dp);
                        else if (type.contains("casual")) dto.getCasualDates().add(dp);
                        else dto.getOtherDates().add(dp);
                        
                        if (cumulativeLeavesTaken < effectiveQuota) {
                            dto.getPaidDates().add(dp);
                            cumulativeLeavesTaken++;
                        } else {
                            dto.getUnpaidDates().add(dp);
                        }
                        
                        d = d.plusDays(1);
                    }
                }
            }
            
            LocalDate calcStart = startOfMonth.isBefore(joiningDate) ? joiningDate : startOfMonth;
            LocalDate today = LocalDate.now();
            LocalDate calcEnd = endOfMonth.isAfter(today) ? today : endOfMonth;
            
            if (!calcStart.isAfter(calcEnd)) {
                LocalDate d = calcStart;
                while (!d.isAfter(calcEnd)) {
                    final LocalDate checkDate = d;
                    if (weekendDays.contains(String.valueOf(checkDate.getDayOfWeek().getValue()))) {
                        dto.getWeekendDates().add(new MonthlyLeaveBreakdownDto.DateProof(checkDate.toString(), "Weekend"));
                        d = d.plusDays(1); continue;
                    }
                    var optionalHoliday = publicHolidays.stream().filter(h -> h.getDate().equals(checkDate)).findFirst();
                    if (optionalHoliday.isPresent()) {
                        dto.getHolidayDates().add(new MonthlyLeaveBreakdownDto.DateProof(checkDate.toString(), "Holiday: " + optionalHoliday.get().getName()));
                        d = d.plusDays(1); continue;
                    }
                    if (presentDates.contains(checkDate)) {
                        d = d.plusDays(1); continue;
                    }
                    boolean onLeave = empLeaves.stream().anyMatch(l -> 
                            !checkDate.isBefore(l.getStartDate()) && !checkDate.isAfter(l.getEndDate())
                    );
                    if (onLeave) {
                        d = d.plusDays(1); continue;
                    }
                    dto.getAbsentDates().add(new MonthlyLeaveBreakdownDto.DateProof(checkDate.toString(), "Unexcused Absence"));
                    d = d.plusDays(1);
                }
            }
            
            dto.setPaidLeavesTaken(dto.getPaidDates().size());
            dto.setUnpaidLeavesTaken(dto.getUnpaidDates().size());
            dto.setAbsentDays(dto.getAbsentDates().size());
            dto.setSickLeavesTaken(dto.getSickDates().size());
            dto.setCasualLeavesTaken(dto.getCasualDates().size());
            dto.setOtherLeavesTaken(dto.getOtherDates().size());
            dto.setTotalDays(dto.getPaidLeavesTaken() + dto.getUnpaidLeavesTaken() + dto.getAbsentDays());
            
            breakdown.add(dto);
        }
        
        return breakdown;
    }

    public LeaveRequest createHrForceLeave(String targetEmployeeId, LocalDate startDate, LocalDate endDate,
            String leaveType, String reason, LeaveRequest.Status status, String creatorUsername, String creatorRole) {
        Employee employee = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + targetEmployeeId));

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setLeaveType(leaveType != null && !leaveType.isBlank() ? leaveType : "Force Leave Deduction By HR");
        request.setReason(reason);
        request.setStatus(status != null ? status : LeaveRequest.Status.APPROVED);
        request.setReviewedBy(creatorUsername + " (" + creatorRole + ")");
        request.setAdminComment("Assigned by " + creatorRole + " (" + creatorUsername + ")");
        request.setIsHrAction(true);

        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        // Notify Employee
        String title = "HR Action: Force Leave Deduction";
        String message = String.format("A leave (%s) has been assigned to you from %s to %s by %s.",
                savedRequest.getLeaveType(), startDate, endDate, creatorUsername);
        String link = "/employee/dashboard";

        try {
            notificationService.sendNotification(
                    employee.getId(),
                    title,
                    message,
                    "LEAVE_FORCE",
                    link);
        } catch (Exception e) {
            System.err.println("Failed to send notification for force leave: " + e.getMessage());
        }

        return savedRequest;
    }
}
