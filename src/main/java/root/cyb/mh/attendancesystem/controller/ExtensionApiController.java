package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.SharedResource;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.SharedResourceRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/extension")
public class ExtensionApiController {

    @Autowired
    private SharedResourceRepository sharedResourceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/credentials")
    public ResponseEntity<List<SharedResource>> getMyCredentials(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String currentUserId = authentication.getName();
        
        List<SharedResource> resources = sharedResourceRepository.findByEmployeeId(currentUserId);
        
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/session-status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String currentUserId = authentication.getName();
        Optional<Employee> empOpt = employeeRepository.findById(currentUserId);

        if (empOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("active", false));
        }

        Employee emp = empOpt.get();
        if (emp.isExtensionForceLogout()) {
            // Admin requested force-logout: reset flag and tell extension to logout
            emp.setExtensionForceLogout(false);
            employeeRepository.save(emp);
            return ResponseEntity.ok(Map.of("active", false));
        }

        return ResponseEntity.ok(Map.of("active", true));
    }
}
