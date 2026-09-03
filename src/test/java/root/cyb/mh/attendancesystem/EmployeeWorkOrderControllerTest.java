package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import root.cyb.mh.attendancesystem.config.CustomAuthenticationSuccessHandler;
import root.cyb.mh.attendancesystem.config.GlobalControllerAdvice;
import root.cyb.mh.attendancesystem.config.SecurityConfig;
import root.cyb.mh.attendancesystem.controller.EmployeeImportHistoryController;
import root.cyb.mh.attendancesystem.controller.EmployeeWorkOrderController;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.repository.EmployeeImportLogRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;
import root.cyb.mh.attendancesystem.service.EmployeeWorkOrderService;
import root.cyb.mh.attendancesystem.service.WorkOrderReportService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { EmployeeWorkOrderController.class, EmployeeImportHistoryController.class })
@Import({ SecurityConfig.class, GlobalControllerAdvice.class })
class EmployeeWorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeWorkOrderRepository employeeWorkOrderRepository;

    @MockBean
    private EmployeeImportLogRepository employeeImportLogRepository;

    @MockBean
    private EmployeeWorkOrderService employeeWorkOrderService;

    @MockBean
    private WorkOrderReportService workOrderReportService;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @Test
    void unauthenticatedUsersAreRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/employee/work-orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void employeeWithoutPermissionIsRedirectedToAccessDenied() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP01");
        emp.setCanAccessWorkOrders(false);

        when(employeeRepository.findById("EMP01")).thenReturn(Optional.of(emp));

        mockMvc.perform(get("/employee/work-orders").with(user("EMP01").roles("EMPLOYEE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    void employeeWithPermissionCanAccessWorkOrders() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP02");
        emp.setCanAccessWorkOrders(true);

        when(employeeRepository.findById("EMP02")).thenReturn(Optional.of(emp));
        when(employeeWorkOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")), 0));

        mockMvc.perform(get("/employee/work-orders").with(user("EMP02").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/work-order/list"));
    }

    @Test
    void adminCanAccessEmployeeWorkOrders() throws Exception {
        when(employeeWorkOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")), 0));

        mockMvc.perform(get("/employee/work-orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/work-order/list"));
    }

    @Test
    void employeeWithPermissionCanAccessReport() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP03");
        emp.setCanAccessWorkOrders(true);

        when(employeeRepository.findById("EMP03")).thenReturn(Optional.of(emp));
        when(employeeWorkOrderRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());
        WorkOrderDashboardDTO dto = new WorkOrderDashboardDTO();
        WorkOrderDashboardDTO.SeriesStat seriesStat = new WorkOrderDashboardDTO.SeriesStat(
                "Grand Total", java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                0L, 0L, 0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
        dto.setGrandTotalSeries(seriesStat);
        dto.setSeriesStats(List.of());
        when(workOrderReportService.calculateEmployeeStatistics(any())).thenReturn(dto);

        mockMvc.perform(get("/employee/work-orders/report").with(user("EMP03").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/work-order/report"));
    }

    @Test
    void employeeWithPermissionCanAccessImports() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP04");
        emp.setCanAccessWorkOrders(true);

        when(employeeRepository.findById("EMP04")).thenReturn(Optional.of(emp));
        when(employeeImportLogRepository.findByImportedByOrderByImportDateDesc(any())).thenReturn(List.of());

        mockMvc.perform(get("/employee/imports").with(user("EMP04").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/import-history"));
    }
}
