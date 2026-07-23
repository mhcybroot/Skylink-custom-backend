package root.cyb.mh.attendancesystem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.dto.RtvRecordRequest;
import root.cyb.mh.attendancesystem.model.RtvAuditLog;
import root.cyb.mh.attendancesystem.model.RtvRecord;
import root.cyb.mh.attendancesystem.model.enums.IssueFromEnum;
import root.cyb.mh.attendancesystem.model.enums.RtvStatusEnum;
import root.cyb.mh.attendancesystem.service.RtvSheetService;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RtvSheetController {

    private final RtvSheetService rtvSheetService;

    @GetMapping("/rtv-sheet")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public String renderRtvSheetPage(Model model) {
        model.addAttribute("activeLink", "rtv-sheet");
        model.addAttribute("employees", rtvSheetService.getAllEmployees());
        model.addAttribute("issueFromOptions", IssueFromEnum.values());
        model.addAttribute("rtvStatusOptions", RtvStatusEnum.values());
        return "rtv-sheet";
    }

    // --- REST API ENDPOINTS ---

    @GetMapping("/api/rtv-sheet")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<List<RtvRecord>> getAllRecords() {
        return ResponseEntity.ok(rtvSheetService.getAllRecords());
    }

    @PostMapping("/api/rtv-sheet")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<RtvRecord> createRecord(@RequestBody RtvRecordRequest req, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        return ResponseEntity.ok(rtvSheetService.createRecord(req, username));
    }

    @PutMapping("/api/rtv-sheet/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<RtvRecord> updateRecord(@PathVariable Long id, @RequestBody RtvRecordRequest req, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        return ResponseEntity.ok(rtvSheetService.updateRecord(id, req, username));
    }

    @DeleteMapping("/api/rtv-sheet/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        rtvSheetService.deleteRecord(id, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/rtv-sheet/{id}/history")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<List<RtvAuditLog>> getLifecycleHistory(@PathVariable Long id) {
        return ResponseEntity.ok(rtvSheetService.getLifecycleHistory(id));
    }

    @GetMapping("/api/rtv-sheet/deleted")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<List<RtvRecord>> getDeletedRecords() {
        return ResponseEntity.ok(rtvSheetService.getDeletedRecords());
    }

    @PostMapping("/api/rtv-sheet/{id}/restore")
    @ResponseBody
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'HR', 'ADMIN')")
    public ResponseEntity<RtvRecord> restoreRecord(@PathVariable Long id, Principal principal) {
        String username = (principal != null) ? principal.getName() : "system";
        return ResponseEntity.ok(rtvSheetService.restoreRecord(id, username));
    }
}
