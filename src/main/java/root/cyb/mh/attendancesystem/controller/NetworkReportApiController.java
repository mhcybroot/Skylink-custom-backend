package root.cyb.mh.attendancesystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import root.cyb.mh.attendancesystem.dto.network.EmployeeUsageSummaryDto;
import root.cyb.mh.attendancesystem.service.network.EmployeeReportExportService;
import root.cyb.mh.attendancesystem.service.network.EmployeeUsageAnalyticsService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/network/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class NetworkReportApiController {

    private final EmployeeUsageAnalyticsService analyticsService;
    private final EmployeeReportExportService exportService;

    @GetMapping("/usage")
    public ResponseEntity<EmployeeUsageSummaryDto> getUsageReport(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getUsageReport(range, date));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportUsageCsv(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        EmployeeUsageSummaryDto summary = analyticsService.getUsageReport(range, date);
        String csv = exportService.generateUsageCsv(summary);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee-network-usage-" + LocalDate.now() + ".csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    @GetMapping(value = "/export/print", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> exportPrintableHtml(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        EmployeeUsageSummaryDto summary = analyticsService.getUsageReport(range, date);
        String html = exportService.generatePrintableHtmlReport(summary);
        return ResponseEntity.ok(html);
    }
}
