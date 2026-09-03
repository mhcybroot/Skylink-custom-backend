package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import root.cyb.mh.attendancesystem.config.CustomAuthenticationSuccessHandler;
import root.cyb.mh.attendancesystem.config.GlobalControllerAdvice;
import root.cyb.mh.attendancesystem.config.SecurityConfig;
import root.cyb.mh.attendancesystem.controller.EmployeeImportHistoryController;
import root.cyb.mh.attendancesystem.controller.EmployeeWorkOrderController;
import root.cyb.mh.attendancesystem.dto.AgingSummaryDTO;
import root.cyb.mh.attendancesystem.dto.WorkOrderDashboardDTO;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.ClientDueConfigRepository;
import root.cyb.mh.attendancesystem.repository.ClientRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeImportLogRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;
import root.cyb.mh.attendancesystem.service.ClientDueAgingService;
import root.cyb.mh.attendancesystem.service.EmployeeWorkOrderService;
import root.cyb.mh.attendancesystem.service.ExportService;
import root.cyb.mh.attendancesystem.service.WorkOrderReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private ClientDueAgingService clientDueAgingService;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @MockBean
    private ExportService exportService;

    private ClientDueConfig defaultConfig;
    private AgingSummaryDTO sampleSummary;

    @BeforeEach
    void setupCommonMocks() {
        defaultConfig = new ClientDueConfig();
        defaultConfig.setClientIdentifier("DEFAULT");
        defaultConfig.setNormalDueDays(40);
        defaultConfig.setOverdueDays(50);
        defaultConfig.setCriticalDueDays(60);

        sampleSummary = new AgingSummaryDTO();
        sampleSummary.setDefaultConfig(defaultConfig);
        sampleSummary.setTotalUnpaidCount(0L);
        sampleSummary.setTotalUnpaidAmount(BigDecimal.ZERO);

        when(clientDueAgingService.getDefaultConfig()).thenReturn(defaultConfig);
        when(clientDueAgingService.getConfigMap()).thenReturn(Map.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);
        when(clientRepository.findAll()).thenReturn(List.of());
    }

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
                .andExpect(view().name("employee/work-order/list"))
                .andExpect(model().attributeExists("agingSummary"));
    }

    @Test
    void filterByCriticalDueBucket() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP02");
        emp.setCanAccessWorkOrders(true);

        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(65));

        when(employeeRepository.findById("EMP02")).thenReturn(Optional.of(emp));
        when(employeeWorkOrderRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(wo));
        when(clientDueAgingService.isUnpaid(wo)).thenReturn(true);
        when(clientDueAgingService.getAgingBucket(eq(wo), any(), any())).thenReturn(ClientDueAgingService.BUCKET_CRITICAL);

        mockMvc.perform(get("/employee/work-orders?dueBucket=critical").with(user("EMP02").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/work-order/list"))
                .andExpect(model().attribute("dueBucket", "critical"));
    }

    @Test
    void saveAgingConfigEndpoint() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP02");
        emp.setCanAccessWorkOrders(true);
        when(employeeRepository.findById("EMP02")).thenReturn(Optional.of(emp));

        mockMvc.perform(post("/employee/work-orders/aging-config")
                .with(user("EMP02").roles("EMPLOYEE"))
                .with(csrf())
                .param("clientIdentifier", "DEFAULT")
                .param("clientName", "Global Default")
                .param("normalDueDays", "45")
                .param("overdueDays", "55")
                .param("criticalDueDays", "65"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/work-orders?success=config_saved"));

        verify(clientDueAgingService).saveOrUpdateConfig("DEFAULT", "Global Default", 45, 55, 65, "EMP02");
    }

    @Test
    void deleteAgingConfigEndpoint() throws Exception {
        Employee emp = new Employee();
        emp.setId("EMP02");
        emp.setCanAccessWorkOrders(true);
        when(employeeRepository.findById("EMP02")).thenReturn(Optional.of(emp));

        mockMvc.perform(post("/employee/work-orders/aging-config/5/delete")
                .with(user("EMP02").roles("EMPLOYEE"))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/work-orders?success=config_deleted"));

        verify(clientDueAgingService).deleteConfig(5L);
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
                "Grand Total", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
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

    @Test
    void exportAgingPortfolioExcel() throws Exception {
        when(employeeWorkOrderRepository.findAll()).thenReturn(List.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);
        when(exportService.exportClientAgingExcel(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/employee/work-orders/aging/export?format=excel").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("AR_Aging_Portfolio_")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportAgingPortfolioCsv() throws Exception {
        when(employeeWorkOrderRepository.findAll()).thenReturn(List.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);
        when(exportService.exportClientAgingCsv(any())).thenReturn("col1,col2".getBytes());

        mockMvc.perform(get("/employee/work-orders/aging/export?format=csv").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("AR_Aging_Portfolio_")))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }

    @Test
    void exportSeriesAgingPortfolioExcel() throws Exception {
        when(employeeWorkOrderRepository.findAll()).thenReturn(List.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);
        when(exportService.exportSeriesAgingExcel(any())).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/employee/work-orders/aging/export-series?format=excel").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("AR_Series_Aging_Portfolio_")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportSeriesAgingPortfolioCsv() throws Exception {
        when(employeeWorkOrderRepository.findAll()).thenReturn(List.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);
        when(exportService.exportSeriesAgingCsv(any())).thenReturn("series,range".getBytes());

        mockMvc.perform(get("/employee/work-orders/aging/export-series?format=csv").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("AR_Series_Aging_Portfolio_")))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }

    @Test
    void filterBySeriesEndpoint() throws Exception {
        when(employeeWorkOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<EmployeeWorkOrder>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);

        mockMvc.perform(get("/employee/work-orders?series=100").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("series", 100))
                .andExpect(view().name("employee/work-order/list"));
    }

    @Test
    void reportWithSeriesEndpoint() throws Exception {
        when(employeeWorkOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<EmployeeWorkOrder>>any(), any(Sort.class)))
                .thenReturn(List.of());
        WorkOrderDashboardDTO dto = new WorkOrderDashboardDTO();
        WorkOrderDashboardDTO.SeriesStat seriesStat = new WorkOrderDashboardDTO.SeriesStat(
                "Grand Total", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        dto.setGrandTotalSeries(seriesStat);
        dto.setSeriesStats(List.of());
        when(workOrderReportService.calculateEmployeeStatistics(any())).thenReturn(dto);

        mockMvc.perform(get("/employee/work-orders/report?series=100").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("series", 100))
                .andExpect(model().attribute("reportTitle", org.hamcrest.Matchers.containsString("Series 100")))
                .andExpect(view().name("employee/work-order/report"));
    }

    @Test
    void filterByPartialDueBucketEndpoint() throws Exception {
        when(employeeWorkOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<EmployeeWorkOrder>>any(), any(Sort.class)))
                .thenReturn(List.of());
        when(clientDueAgingService.filterOrdersByDueBucket(any(), any())).thenReturn(List.of());
        when(clientDueAgingService.calculateAgingSummary(any())).thenReturn(sampleSummary);

        mockMvc.perform(get("/employee/work-orders?dueBucket=partial").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dueBucket", "partial"))
                .andExpect(model().attribute("currentFilter", org.hamcrest.Matchers.containsString("Partially Paid Work Orders")))
                .andExpect(view().name("employee/work-order/list"));
    }

    @Test
    void exportPartialWorkOrdersEndpoint() throws Exception {
        when(employeeWorkOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<EmployeeWorkOrder>>any(), any(Sort.class)))
                .thenReturn(List.of());
        when(clientDueAgingService.filterOrdersByDueBucket(any(), any())).thenReturn(List.of());
        when(exportService.exportDueWorkOrdersExcel(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/employee/work-orders/export?dueBucket=partial&format=excel").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"Due_Orders_partial_")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
