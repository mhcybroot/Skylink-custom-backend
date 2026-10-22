package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import root.cyb.mh.attendancesystem.dto.EmployeeLeaveQuotaDto;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.LeaveRequest;
import root.cyb.mh.attendancesystem.model.WorkSchedule;
import root.cyb.mh.attendancesystem.repository.*;
import root.cyb.mh.attendancesystem.service.LeaveService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class LeaveServiceTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private root.cyb.mh.attendancesystem.controller.LeaveController leaveController;

    @MockitoBean
    private WorkScheduleRepository workScheduleRepository;

    @MockitoBean
    private LeaveRequestRepository leaveRequestRepository;

    @MockitoBean
    private PublicHolidayRepository publicHolidayRepository;

    @MockitoBean
    private EmployeeDailyWorkStatusRepository employeeDailyWorkStatusRepository;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    public void testEmployeeQuotaModelDefaultsAndOverrides() {
        WorkSchedule globalSchedule = new WorkSchedule();
        globalSchedule.setDefaultSickLeaveQuota(6);
        globalSchedule.setDefaultCasualLeaveQuota(8);
        globalSchedule.setDefaultAnnualLeaveQuota(14);

        // Case 1: Employee with no custom quotas (uses global defaults)
        Employee emp1 = new Employee();
        emp1.setId("EMP001");
        emp1.setName("Alice");
        assertEquals(6, emp1.getEffectiveSickQuota(globalSchedule.getDefaultSickLeaveQuota()));
        assertEquals(8, emp1.getEffectiveCasualQuota(globalSchedule.getDefaultCasualLeaveQuota()));
        assertEquals(14, emp1.getEffectiveQuota(globalSchedule.getDefaultAnnualLeaveQuota()));

        // Case 2: Employee with custom quotas (overrides global defaults)
        Employee emp2 = new Employee();
        emp2.setId("EMP002");
        emp2.setName("Bob");
        emp2.setSickLeaveQuota(10);
        emp2.setCasualLeaveQuota(12);
        assertEquals(10, emp2.getEffectiveSickQuota(globalSchedule.getDefaultSickLeaveQuota()));
        assertEquals(12, emp2.getEffectiveCasualQuota(globalSchedule.getDefaultCasualLeaveQuota()));
        assertEquals(22, emp2.getEffectiveQuota(globalSchedule.getDefaultAnnualLeaveQuota()));

        // Case 3: Employee with only Sick quota set, Casual left default (Panir's case)
        Employee emp3 = new Employee();
        emp3.setId("EMP003");
        emp3.setName("Panir");
        emp3.setSickLeaveQuota(3);
        emp3.setCasualLeaveQuota(null);
        emp3.setAnnualLeaveQuota(null);
        assertEquals(3, emp3.getEffectiveSickQuota(6));
        assertEquals(8, emp3.getEffectiveCasualQuota(8)); // inherits global default casual (8)
        assertEquals(10, emp3.getEffectiveQuota(14)); // 3 + (14/2) = 10

        // Case 4: Employee with legacy annualLeaveQuota (e.g. 21 days) kept in Casual
        Employee emp4 = new Employee();
        emp4.setId("EMP004");
        emp4.setName("Sarah");
        emp4.setAnnualLeaveQuota(21);
        assertEquals(6, emp4.getEffectiveSickQuota(6)); // sick gets global default (6)
        assertEquals(21, emp4.getEffectiveCasualQuota(12)); // casual gets full legacy annual quota (21)
        assertEquals(28, emp4.getEffectiveQuota(14)); // 21 + (14/2) = 28
    }

    @Test
    public void testCalculateSeparateSickAndCasualQuotas() {
        WorkSchedule globalSchedule = new WorkSchedule();
        globalSchedule.setDefaultSickLeaveQuota(6);
        globalSchedule.setDefaultCasualLeaveQuota(6);
        globalSchedule.setDefaultAnnualLeaveQuota(12);
        globalSchedule.setWeekendDays("6,7");
        when(workScheduleRepository.findAll()).thenReturn(List.of(globalSchedule));
        when(publicHolidayRepository.findAll()).thenReturn(Collections.emptyList());
        when(employeeDailyWorkStatusRepository.findByDateBetween(any(), any())).thenReturn(Collections.emptyList());

        Employee emp = new Employee();
        emp.setId("EMP100");
        emp.setName("Charlie");

        // Approved Leaves for EMP100:
        // 1. Sick Leave: 4 days (within 6 days sick quota)
        LeaveRequest sickLeave = new LeaveRequest();
        sickLeave.setEmployee(emp);
        sickLeave.setLeaveType("Sick");
        sickLeave.setStartDate(LocalDate.of(2026, 3, 1));
        sickLeave.setEndDate(LocalDate.of(2026, 3, 4)); // 4 days

        // 2. Casual Leave: 8 days (exceeds 6 days casual quota: 6 paid, 2 unpaid)
        LeaveRequest casualLeave = new LeaveRequest();
        casualLeave.setEmployee(emp);
        casualLeave.setLeaveType("Casual");
        casualLeave.setStartDate(LocalDate.of(2026, 4, 1));
        casualLeave.setEndDate(LocalDate.of(2026, 4, 8)); // 8 days

        when(leaveRequestRepository.findApprovedLeavesInYear(any(), any()))
                .thenReturn(List.of(sickLeave, casualLeave));

        LocalDate today = LocalDate.of(2026, 10, 22);
        List<EmployeeLeaveQuotaDto> result = leaveService.calculateAllEmployeeLeaveQuotas(today, List.of(emp));

        assertNotNull(result);
        assertEquals(1, result.size());
        EmployeeLeaveQuotaDto dto = result.get(0);

        // Sick quota checks
        assertEquals(6, dto.getSickQuota());
        assertEquals(4, dto.getSickLeavesTaken());
        assertEquals(4, dto.getSickPaidTaken());
        assertEquals(0, dto.getSickUnpaidTaken());
        assertEquals(2, dto.getSickRemaining());

        // Casual quota checks
        assertEquals(6, dto.getCasualQuota());
        assertEquals(8, dto.getCasualLeavesTaken());
        assertEquals(6, dto.getCasualPaidTaken());
        assertEquals(2, dto.getCasualUnpaidTaken());
        assertEquals(0, dto.getCasualRemaining());

        // Combined checks
        assertEquals(12, dto.getAnnualQuota());
        assertEquals(12, dto.getYearlyLeavesTaken());
        assertEquals(10, dto.getPaidLeavesTaken());
        assertEquals(2, dto.getUnpaidLeavesTaken());
    }

    @Test
    public void testEmployeeLeavePageModelAttributes() {
        WorkSchedule globalSchedule = new WorkSchedule();
        globalSchedule.setDefaultSickLeaveQuota(6);
        globalSchedule.setDefaultCasualLeaveQuota(6);
        globalSchedule.setDefaultAnnualLeaveQuota(12);
        when(workScheduleRepository.findAll()).thenReturn(List.of(globalSchedule));
        when(publicHolidayRepository.findAll()).thenReturn(Collections.emptyList());
        when(employeeDailyWorkStatusRepository.findByDateBetween(any(), any())).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findApprovedLeavesInYear(any(), any())).thenReturn(Collections.emptyList());

        Employee emp = new Employee();
        emp.setId("EMP001");
        emp.setName("Alice");
        when(employeeRepository.findById("EMP001")).thenReturn(java.util.Optional.of(emp));
        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc("EMP001")).thenReturn(Collections.emptyList());

        org.springframework.ui.ConcurrentModel model = new org.springframework.ui.ConcurrentModel();
        java.security.Principal principal = () -> "EMP001";

        String view = leaveController.employeeLeavePage(model, principal);
        assertEquals("employee-leave", view);
        assertTrue(model.containsAttribute("quota"));
        EmployeeLeaveQuotaDto quota = (EmployeeLeaveQuotaDto) model.getAttribute("quota");
        assertNotNull(quota);
        assertEquals(6, quota.getSickQuota());
        assertEquals(6, quota.getCasualQuota());
        assertEquals(6, quota.getSickRemaining());
        assertEquals(6, quota.getCasualRemaining());
    }
}
