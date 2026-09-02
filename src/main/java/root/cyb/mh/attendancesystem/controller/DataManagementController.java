package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import root.cyb.mh.attendancesystem.service.DataImportExportService;

import java.io.IOException;
import java.io.StringWriter;

@Controller
@RequestMapping("/data")
public class DataManagementController {

    @Autowired
    private DataImportExportService dataService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData(@RequestParam String type) throws IOException {
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        java.io.PrintWriter writer = new java.io.PrintWriter(stringWriter);

        switch (type) {
            case "employees":
                dataService.exportEmployees(writer);
                break;
            case "departments":
                dataService.exportDepartments(writer);
                break;
            case "leaves":
                dataService.exportLeaveRequests(writer);
                break;
            case "devices":
                dataService.exportDevices(writer);
                break;
            case "settings":
                dataService.exportSettings(writer);
                break;
            case "users":
                dataService.exportUsers(writer);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }
        writer.flush();

        byte[] csvBytes = stringWriter.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + "_export.csv\"")
                .body(csvBytes);
    }

    @PostMapping("/import")
    public String importData(@RequestParam String type, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "redirect:/settings?error=emptyfile";
        }

        switch (type) {
            case "employees":
                dataService.importEmployees(file.getInputStream());
                break;
            case "departments":
                dataService.importDepartments(file.getInputStream());
                break;
            case "leaves":
                dataService.importLeaveRequests(file.getInputStream());
                break;
            case "devices":
                dataService.importDevices(file.getInputStream());
                break;
            case "settings":
                dataService.importSettings(file.getInputStream());
                break;
            case "users":
                dataService.importUsers(file.getInputStream());
                break;
            case "workorders":
                dataService.importWorkOrders(file.getInputStream());
                return "redirect:/dashboard?success=import";
            default:
                return "redirect:/settings?error=unknowntype";
        }

        return "redirect:/settings?success=import";
    }
}
