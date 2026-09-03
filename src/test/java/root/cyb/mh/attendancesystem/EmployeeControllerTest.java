package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import root.cyb.mh.attendancesystem.config.CustomAuthenticationSuccessHandler;
import root.cyb.mh.attendancesystem.config.GlobalControllerAdvice;
import root.cyb.mh.attendancesystem.config.SecurityConfig;
import root.cyb.mh.attendancesystem.controller.EmployeeController;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.repository.DepartmentRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.SharedResourceRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = EmployeeController.class)
@Import({ SecurityConfig.class, GlobalControllerAdvice.class })
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private DepartmentRepository departmentRepository;

    @MockBean
    private SharedResourceRepository sharedResourceRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @Test
    void employeesPageRendersSuccessfullyWithNewProperties() throws Exception {
        Employee emp = new Employee();
        emp.setId("1");
        emp.setName("Panir");
        emp.setCanViewAllPaymentRequests(true);
        emp.setCanAccessWorkOrders(true);

        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(emp)));
        when(employeeRepository.findAll()).thenReturn(List.of(emp));
        when(departmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/employees").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("employees"));
    }
}
