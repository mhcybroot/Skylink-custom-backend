package root.cyb.mh.attendancesystem.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import root.cyb.mh.attendancesystem.model.WorkStatus;
import root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus;
import root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private EmployeeDailyWorkStatusRepository employeeDailyWorkStatusRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_HR")) {
            response.sendRedirect("/dashboard");
        } else if (roles.contains("ROLE_EMPLOYEE")) {
            // Update WorkStatus to LOGGED_IN if it is currently ENTERED_OFFICE
            String employeeId = authentication.getName();
            java.time.LocalDate today = java.time.LocalDate.now();
            EmployeeDailyWorkStatus dailyStatus = employeeDailyWorkStatusRepository
                    .findByEmployeeIdAndDate(employeeId, today)
                    .orElse(new EmployeeDailyWorkStatus(employeeId, today));

            if (dailyStatus.getStatus() == WorkStatus.ENTERED_OFFICE) {
                dailyStatus.setStatus(WorkStatus.LOGGED_IN);
                employeeDailyWorkStatusRepository.save(dailyStatus);
            }

            response.sendRedirect("/employee/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}
