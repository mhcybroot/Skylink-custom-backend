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

    @Autowired
    private root.cyb.mh.attendancesystem.repository.AttendanceLogRepository attendanceLogRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_HR")) {
            response.sendRedirect("/dashboard");
        } else if (roles.contains("ROLE_EMPLOYEE")) {
            String employeeId = authentication.getName();
            java.time.LocalDate today = java.time.LocalDate.now();
            EmployeeDailyWorkStatus dailyStatus = employeeDailyWorkStatusRepository
                    .findByEmployeeIdAndDate(employeeId, today)
                    .orElse(new EmployeeDailyWorkStatus(employeeId, today));

            // Upgrade to LOGGED_IN if they already passed the office gate (ENTERED_OFFICE)
            // Or if they are NOT_ENTERED but actually have an ADMS punch today (healing
            // missed status)
            if (dailyStatus.getStatus() == WorkStatus.ENTERED_OFFICE) {
                dailyStatus.setStatus(WorkStatus.LOGGED_IN);
                employeeDailyWorkStatusRepository.save(dailyStatus);
            } else if (dailyStatus.getStatus() == WorkStatus.NOT_ENTERED) {
                java.time.LocalDateTime startOfDay = today.atStartOfDay();
                java.time.LocalDateTime endOfDay = today.atTime(java.time.LocalTime.MAX);
                boolean hasPunchedToday = !attendanceLogRepository.findByTimestampBetween(startOfDay, endOfDay)
                        .stream().filter(log -> log.getEmployeeId().equals(employeeId))
                        .collect(java.util.stream.Collectors.toList()).isEmpty();

                if (hasPunchedToday) {
                    dailyStatus.setStatus(WorkStatus.LOGGED_IN);
                    employeeDailyWorkStatusRepository.save(dailyStatus);
                }
            }

            response.sendRedirect("/employee/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}
