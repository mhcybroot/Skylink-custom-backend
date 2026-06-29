package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import root.cyb.mh.attendancesystem.model.SharedResource;
import root.cyb.mh.attendancesystem.repository.SharedResourceRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/extension")
public class ExtensionApiController {

    @Autowired
    private SharedResourceRepository sharedResourceRepository;

    @GetMapping("/credentials")
    public ResponseEntity<List<SharedResource>> getMyCredentials(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        // For Employees, the principal name is their ID (e.g. EMP001).
        // For Admins/HR, it is their username. 
        // SharedResource uses employeeId. If an admin wants to use the extension for testing, 
        // they might need an employee ID, but typically this is for employees.
        String currentUserId = authentication.getName();
        
        List<SharedResource> resources = sharedResourceRepository.findByEmployeeId(currentUserId);
        
        return ResponseEntity.ok(resources);
    }
}
