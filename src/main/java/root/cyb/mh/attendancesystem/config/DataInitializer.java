package root.cyb.mh.attendancesystem.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import root.cyb.mh.attendancesystem.model.Device;
import root.cyb.mh.attendancesystem.model.User;
import root.cyb.mh.attendancesystem.repository.DeviceRepository;
import root.cyb.mh.attendancesystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @org.springframework.beans.factory.annotation.Value("${app.testing:false}")
    private boolean appTesting;

    @Bean
    public CommandLineRunner loadData(DeviceRepository deviceRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            root.cyb.mh.attendancesystem.repository.EmployeeDailyWorkStatusRepository workStatusRepository,
            root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository) {
        return args -> {
            try {
                // Fix for payment_requests schema (drop NOT NULL on requester_id)
                jdbcTemplate.execute("ALTER TABLE payment_requests ALTER COLUMN requester_id DROP NOT NULL");
                System.out.println("Schema Update: Dropped NOT NULL from requester_id in payment_requests");
            } catch (Exception e) {
                // Ignore if fails (e.g. table not found or already dropped)
                System.out.println("Schema Update Check: " + e.getMessage());
            }

            // Devices
            if (deviceRepository.count() == 0) {
                Device device = new Device();
                device.setName("Mb460");
                device.setIpAddress("10.10.15.3");
                device.setPort(4370);
                device.setSerialNumber("QWC5251100143");
                deviceRepository.save(device);
                System.out.println("Pre-loaded device: Mb460 (10.10.15.3)");
            }

            // Users
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("Created default ADMIN user: admin / admin123");
            }

            if (userRepository.findByUsername("hr").isEmpty()) {
                User hr = new User();
                hr.setUsername("hr");
                hr.setPassword(passwordEncoder.encode("hr123"));
                hr.setRole("HR");
                userRepository.save(hr);
                System.out.println("Created default HR user: hr / hr123");
            }

            // Test Data injection
            if (appTesting) {
                System.out.println("TESTING MODE ACTIVE: Injecting dummy Work Status records...");
                try {
                    // Just take any employee
                    employeeRepository.findAll().stream().findFirst().ifPresent(emp -> {
                        java.time.LocalDate today = java.time.LocalDate.now();
                        root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus dummyStatus = workStatusRepository
                                .findByEmployeeIdAndDate(emp.getId(), today)
                                .orElse(new root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus(emp.getId(),
                                        today));

                        // Simulate they entered office, started working 2 hours ago, and are currently
                        // on break
                        dummyStatus.setStatus(root.cyb.mh.attendancesystem.model.WorkStatus.ON_BREAK);
                        dummyStatus.setWorkStartTime(java.time.LocalDateTime.now().minusHours(2));
                        dummyStatus.setCurrentBreakStartTime(java.time.LocalDateTime.now().minusMinutes(15));
                        dummyStatus.setTotalBreakMinutes(0); // 15 mins currently accruing

                        workStatusRepository.save(dummyStatus);
                        System.out.println("Injected dummy WorkStatus for Employee " + emp.getId());
                    });
                } catch (Exception e) {
                    System.out.println("Failed to inject test work status data: " + e.getMessage());
                }
            } else {
                // Clean up test data if app.testing is false
                try {
                    employeeRepository.findAll().stream().findFirst().ifPresent(emp -> {
                        java.time.LocalDate today = java.time.LocalDate.now();
                        workStatusRepository.findByEmployeeIdAndDate(emp.getId(), today).ifPresent(s -> {
                            workStatusRepository.delete(s);
                            System.out.println("Removed test WorkStatus for Employee " + emp.getId());
                        });
                    });
                } catch (Exception e) {
                }
            }
        };
    }
}
