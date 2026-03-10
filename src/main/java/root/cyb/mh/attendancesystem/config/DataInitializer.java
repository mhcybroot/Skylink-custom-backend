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
                System.out.println("TESTING MODE ACTIVE: Injecting dummy Work Status records for all statuses...");
                try {
                    root.cyb.mh.attendancesystem.model.WorkStatus[] statuses = root.cyb.mh.attendancesystem.model.WorkStatus
                            .values();
                    java.util.List<root.cyb.mh.attendancesystem.model.Employee> employees = employeeRepository
                            .findAll();
                    java.time.LocalDate today = java.time.LocalDate.now();

                    int statusIndex = 1; // Start from 1 to skip NOT_ENTERED usually, or 0 to include it
                    for (int i = 0; i < employees.size() && statusIndex < statuses.length; i++) {
                        root.cyb.mh.attendancesystem.model.Employee emp = employees.get(i);
                        root.cyb.mh.attendancesystem.model.WorkStatus currentStatusToApply = statuses[statusIndex];

                        root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus dummyStatus = workStatusRepository
                                .findByEmployeeIdAndDate(emp.getId(), today)
                                .orElse(new root.cyb.mh.attendancesystem.model.EmployeeDailyWorkStatus(emp.getId(),
                                        today));

                        dummyStatus.setStatus(currentStatusToApply);

                        // Fake time depending on status
                        if (currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.WORKING) {
                            dummyStatus.setWorkStartTime(java.time.LocalDateTime.now().minusHours(4));
                        } else if (currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.ON_BREAK) {
                            dummyStatus.setWorkStartTime(java.time.LocalDateTime.now().minusHours(3));
                            dummyStatus.setCurrentBreakStartTime(java.time.LocalDateTime.now().minusMinutes(25));
                        } else if (currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.ENDED_WORK ||
                                currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.LEFT_WITHOUT_PUNCH
                                ||
                                currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.COMPLETED_DAY) {
                            dummyStatus.setWorkStartTime(java.time.LocalDateTime.now().minusHours(9));
                            dummyStatus.setTotalBreakMinutes(45);
                            dummyStatus.setWorkEndTime(java.time.LocalDateTime.now().minusMinutes(15));
                        } else if (currentStatusToApply == root.cyb.mh.attendancesystem.model.WorkStatus.INCOMPLETE_SHIFT) {
                            dummyStatus.setWorkStartTime(java.time.LocalDateTime.now().minusHours(6).minusMinutes(15));
                            dummyStatus.setTotalBreakMinutes(30);
                            dummyStatus.setWorkEndTime(java.time.LocalDateTime.now().minusMinutes(10));
                        }

                        workStatusRepository.save(dummyStatus);
                        System.out.println("Injected test status [" + currentStatusToApply + "] for Employee "
                                + emp.getName() + " (" + emp.getId() + ")");

                        statusIndex++;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to inject test work status data: " + e.getMessage());
                }
            } else {
                // Testing is false, so we are in production.
                // We should intentionally do nothing here to preserve today's active work
                // records
                // across application restarts.
            }
        };
    }
}
