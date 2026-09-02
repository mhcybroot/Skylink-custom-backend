package root.cyb.mh.attendancesystem.service.network;

import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.network.EmployeeUsageItemDto;
import root.cyb.mh.attendancesystem.dto.network.EmployeeUsageSummaryDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmployeeReportExportService {

    public String generateUsageCsv(EmployeeUsageSummaryDto summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rank,Employee / Hostname,Client OS,IP Address,MAC Address,Switch Port,Total Data Transferred,Work & Dev (MB/GB),Work %,Communication (MB/GB),Comm %,Entertainment & Media (MB/GB),Media %,General Web (MB/GB),General %,Productivity Score (%),Top Visited Service,Focus Category\n");

        if (summary != null && summary.getEmployeeUsageList() != null) {
            int rank = 1;
            for (EmployeeUsageItemDto item : summary.getEmployeeUsageList()) {
                sb.append(rank++).append(",")
                        .append(escapeCsv(item.getEmployeeName())).append(",")
                        .append(escapeCsv(item.getClientOs())).append(",")
                        .append(escapeCsv(item.getIpAddress())).append(",")
                        .append(escapeCsv(item.getMacAddress())).append(",")
                        .append(escapeCsv(item.getSwitchPort())).append(",")
                        .append(escapeCsv(item.getTotalFormatted())).append(",")
                        .append(escapeCsv(item.getWorkDevFormatted())).append(",")
                        .append(item.getWorkDevPercent()).append("%,")
                        .append(escapeCsv(item.getCommunicationFormatted())).append(",")
                        .append(item.getCommunicationPercent()).append("%,")
                        .append(escapeCsv(item.getMediaEntertainmentFormatted())).append(",")
                        .append(item.getMediaEntertainmentPercent()).append("%,")
                        .append(escapeCsv(item.getGeneralWebFormatted())).append(",")
                        .append(item.getGeneralWebPercent()).append("%,")
                        .append(item.getProductivityScore()).append("%,")
                        .append(escapeCsv(item.getTopVisitedDomain())).append(",")
                        .append(escapeCsv(item.getProductivityLabel()))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    public String generatePrintableHtmlReport(EmployeeUsageSummaryDto summary) {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"));
        StringBuilder sb = new StringBuilder();

        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Skylink Employee Internet Usage & Productivity Report</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 25px; background: #fff; color: #1e293b; }
                    .header-box { border-bottom: 2px solid #0284c7; padding-bottom: 15px; margin-bottom: 25px; }
                    .kpi-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 15px; text-align: center; }
                    .progress-stacked { height: 12px; border-radius: 6px; overflow: hidden; }
                    @media print { .no-print { display: none; } }
                </style>
            </head>
            <body>
                <div class="container-fluid">
                    <div class="d-flex justify-content-between align-items-center header-box">
                        <div>
                            <h2 class="fw-bold text-primary mb-1">SKYLINK ENTERPRISE NETWORK</h2>
                            <h5 class="text-secondary mb-0">Employee Internet Usage & Productivity Audit Report</h5>
                            <span class="text-muted small">Period: <strong>""").append(summary.getDateRangeLabel()).append("""
                            </strong> &bull; Generated: """).append(generatedAt).append("""
                        </div>
                        <div class="no-print">
                            <button onclick="window.print()" class="btn btn-primary btn-sm px-4 rounded-pill fw-bold">🖨️ Print / Save as PDF</button>
                        </div>
                    </div>

                    <!-- KPI Cards -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <div class="kpi-card">
                                <span class="text-muted small text-uppercase">Total Office Internet Usage</span>
                                <h3 class="fw-bold text-primary mb-0">""").append(summary.getTotalOfficeDataFormatted()).append("""
                                </h3>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="kpi-card">
                                <span class="text-muted small text-uppercase">Average Work Focus Index</span>
                                <h3 class="fw-bold text-success mb-0">""").append(summary.getAverageProductivityScore()).append("""
                                %</h3>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="kpi-card">
                                <span class="text-muted small text-uppercase">Top Bandwidth Workstation</span>
                                <h5 class="fw-bold text-dark mb-0 text-truncate">""").append(summary.getTopWorkstation()).append("""
                                </h5>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="kpi-card">
                                <span class="text-muted small text-uppercase">Primary Cloud Work Service</span>
                                <h5 class="fw-bold text-info mb-0 text-truncate">""").append(summary.getTopWorkService()).append("""
                                </h5>
                            </div>
                        </div>
                    </div>

                    <!-- Category Breakdown Progress -->
                    <div class="card border-0 bg-light rounded-3 p-3 mb-4">
                        <div class="d-flex justify-content-between mb-1 small fw-bold">
                            <span class="text-success">💻 Work & Dev: """).append(summary.getOverallWorkDevPercent()).append("""
                            %</span>
                            <span class="text-primary">✉️ Communication: """).append(summary.getOverallCommunicationPercent()).append("""
                            %</span>
                            <span class="text-danger">🎬 Entertainment & Media: """).append(summary.getOverallMediaPercent()).append("""
                            %</span>
                            <span class="text-secondary">🌐 General Web: """).append(summary.getOverallGeneralWebPercent()).append("""
                            %</span>
                        </div>
                        <div class="progress progress-stacked">
                            <div class="progress-bar bg-success" style="width: """).append(summary.getOverallWorkDevPercent()).append("""
                            %"></div>
                            <div class="progress-bar bg-primary" style="width: """).append(summary.getOverallCommunicationPercent()).append("""
                            %"></div>
                            <div class="progress-bar bg-danger" style="width: """).append(summary.getOverallMediaPercent()).append("""
                            %"></div>
                            <div class="progress-bar bg-secondary" style="width: """).append(summary.getOverallGeneralWebPercent()).append("""
                            %"></div>
                        </div>
                    </div>

                    <!-- Detailed Table -->
                    <table class="table table-bordered align-middle small mb-0">
                        <thead class="table-dark">
                            <tr>
                                <th>#</th>
                                <th>Employee / Workstation</th>
                                <th>IP Address</th>
                                <th>Switch Port</th>
                                <th>Total Data</th>
                                <th>Work & Dev</th>
                                <th>Communication</th>
                                <th>Media & Streaming</th>
                                <th>Top Visited Service</th>
                                <th>Focus Index</th>
                            </tr>
                        </thead>
                        <tbody>
            """);

        int rank = 1;
        for (EmployeeUsageItemDto item : summary.getEmployeeUsageList()) {
            sb.append("<tr>")
                    .append("<td>").append(rank++).append("</td>")
                    .append("<td><strong>").append(item.getEmployeeName()).append("</strong><div class='text-muted small'>").append(item.getClientOs()).append("</div></td>")
                    .append("<td><code>").append(item.getIpAddress()).append("</code></td>")
                    .append("<td>").append(item.getSwitchPort()).append("</td>")
                    .append("<td><strong class='text-primary'>").append(item.getTotalFormatted()).append("</strong></td>")
                    .append("<td>").append(item.getWorkDevFormatted()).append(" (").append(item.getWorkDevPercent()).append("%)</td>")
                    .append("<td>").append(item.getCommunicationFormatted()).append(" (").append(item.getCommunicationPercent()).append("%)</td>")
                    .append("<td>").append(item.getMediaEntertainmentFormatted()).append(" (").append(item.getMediaEntertainmentPercent()).append("%)</td>")
                    .append("<td><code>").append(item.getTopVisitedDomain()).append("</code></td>")
                    .append("<td><span class='badge ").append(item.getProductivityBadgeClass()).append("'>").append(item.getProductivityScore()).append("% ").append(item.getProductivityLabel()).append("</span></td>")
                    .append("</tr>");
        }

        sb.append("""
                        </tbody>
                    </table>
                    <div class="mt-4 pt-3 border-top text-muted small text-center">
                        Report automatically audited and verified via MikroTik RouterOS Live Connection Tracker & Grandstream AP Telemetry.
                    </div>
                </div>
            </body>
            </html>
            """);

        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"—\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
