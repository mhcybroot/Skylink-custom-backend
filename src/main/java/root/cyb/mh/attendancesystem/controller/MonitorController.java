package root.cyb.mh.attendancesystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    @PostMapping("/activity")
    public ResponseEntity<?> receiveActivity(@RequestBody Map<String, Object> activityPayload) {
        // In a real application, we would save this to the database with the employee ID
        System.out.println("Received activity update: " + activityPayload);
        return ResponseEntity.ok().build();
    }
}
