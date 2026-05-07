package root.cyb.mh.attendancesystem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import root.cyb.mh.attendancesystem.config.CustomAuthenticationSuccessHandler;
import root.cyb.mh.attendancesystem.config.GlobalControllerAdvice;
import root.cyb.mh.attendancesystem.config.SecurityConfig;
import root.cyb.mh.attendancesystem.controller.WorkOrderController;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.WorkOrderRepository;
import root.cyb.mh.attendancesystem.service.WorkOrderReportService;

@WebMvcTest(controllers = WorkOrderController.class)
@Import({ SecurityConfig.class, GlobalControllerAdvice.class })
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkOrderController workOrderController;

    @MockBean
    private WorkOrderRepository workOrderRepository;

    @MockBean
    private WorkOrderReportService workOrderReportService;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @AfterEach
    void resetRestrictionFlag() {
        ReflectionTestUtils.setField(workOrderController, "workOrdersRestricted", false);
    }

    @Test
    void unauthenticatedUsersAreRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/work-orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void unrestrictedListViewStillRenders() throws Exception {
        when(employeeRepository.existsByReportsTo_IdOrReportsToAssistant_Id("admin", "admin")).thenReturn(false);
        when(workOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")), 0));

        mockMvc.perform(get("/admin/work-orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("work-order/list"));

        verify(workOrderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void restrictedListViewShowsUpgradePage() throws Exception {
        ReflectionTestUtils.setField(workOrderController, "workOrdersRestricted", true);
        when(employeeRepository.existsByReportsTo_IdOrReportsToAssistant_Id("admin", "admin")).thenReturn(false);

        mockMvc.perform(get("/admin/work-orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("work-order/upgrade"));

        verifyNoInteractions(workOrderRepository, workOrderReportService);
    }

    @Test
    void restrictedDashboardShowsUpgradePage() throws Exception {
        ReflectionTestUtils.setField(workOrderController, "workOrdersRestricted", true);
        when(employeeRepository.existsByReportsTo_IdOrReportsToAssistant_Id("admin", "admin")).thenReturn(false);

        mockMvc.perform(get("/admin/work-orders/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("work-order/upgrade"));

        verifyNoInteractions(workOrderRepository, workOrderReportService);
    }

    @Test
    void restrictedReportShowsUpgradePage() throws Exception {
        ReflectionTestUtils.setField(workOrderController, "workOrdersRestricted", true);
        when(employeeRepository.existsByReportsTo_IdOrReportsToAssistant_Id("admin", "admin")).thenReturn(false);

        mockMvc.perform(get("/admin/work-orders/report").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("work-order/upgrade"));

        verifyNoInteractions(workOrderRepository, workOrderReportService);
    }
}
