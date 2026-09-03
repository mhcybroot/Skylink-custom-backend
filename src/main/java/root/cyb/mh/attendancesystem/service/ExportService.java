package root.cyb.mh.attendancesystem.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.DailyAttendanceDto;
import root.cyb.mh.attendancesystem.dto.MonthlySummaryDto;
import root.cyb.mh.attendancesystem.dto.WeeklyAttendanceDto;
import root.cyb.mh.attendancesystem.dto.EmployeeMonthlyDetailDto;
import root.cyb.mh.attendancesystem.dto.EmployeeRangeReportDto;
import root.cyb.mh.attendancesystem.dto.EmployeeWeeklyDetailDto;
import root.cyb.mh.attendancesystem.dto.AgingSummaryDTO;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    // --- Daily Report ---

    public byte[] exportDailyExcel(List<DailyAttendanceDto> report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Daily Report");

            Row headerRow = sheet.createRow(0);
            String[] columns = { "Employee ID", "Name", "Department", "In Time", "Out Time", "Status",
                    "Activity Status", "Active Work", "Break Time" };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            for (DailyAttendanceDto dto : report) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getEmployeeId());
                row.createCell(1).setCellValue(dto.getEmployeeName());
                row.createCell(2).setCellValue(dto.getDepartmentName());
                row.createCell(3).setCellValue(dto.getInTime() != null ? dto.getInTime().toString() : "-");
                row.createCell(4).setCellValue(dto.getOutTime() != null ? dto.getOutTime().toString() : "-");
                row.createCell(5).setCellValue(dto.getStatus());
                row.createCell(6).setCellValue(dto.getCurrentWorkStatus() != null ? dto.getCurrentWorkStatus() : "-");
                row.createCell(7).setCellValue(dto.getActiveWorkDuration() != null ? dto.getActiveWorkDuration() : "-");
                row.createCell(8).setCellValue(dto.getTotalBreakDuration() != null ? dto.getTotalBreakDuration() : "-");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportDailyCsv(List<DailyAttendanceDto> report) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Employee ID", "Name", "Department", "In Time", "Out Time", "Status", "Activity Status",
                            "Active Work", "Break Time")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (DailyAttendanceDto dto : report) {
                    printer.printRecord(
                            dto.getEmployeeId(),
                            dto.getEmployeeName(),
                            dto.getDepartmentName(),
                            dto.getInTime() != null ? dto.getInTime().toString() : "-",
                            dto.getOutTime() != null ? dto.getOutTime().toString() : "-",
                            dto.getStatus(),
                            dto.getCurrentWorkStatus() != null ? dto.getCurrentWorkStatus() : "-",
                            dto.getActiveWorkDuration() != null ? dto.getActiveWorkDuration() : "-",
                            dto.getTotalBreakDuration() != null ? dto.getTotalBreakDuration() : "-");
                }
            }
            return out.toByteArray();
        }
    }

    // --- Weekly Report ---

    public byte[] exportWeeklyExcel(List<WeeklyAttendanceDto> report, LocalDate startOfWeek) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Weekly Report");

            Row headerRow = sheet.createRow(0);
            // Dynamic headers for days
            String[] fixedHeaders = { "Employee ID", "Name", "Department" };
            String[] statsHeaders = { "Present", "Absent", "Late", "Early", "Leave" };

            int colIdx = 0;
            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            for (String h : fixedHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(h);
                cell.setCellStyle(boldStyle);
            }

            for (int i = 0; i < 7; i++) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(startOfWeek.plusDays(i).getDayOfWeek().toString().substring(0, 3));
                cell.setCellStyle(boldStyle);
            }

            for (String h : statsHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(h);
                cell.setCellStyle(boldStyle);
            }

            int rowIdx = 1;
            for (WeeklyAttendanceDto dto : report) {
                Row row = sheet.createRow(rowIdx++);
                colIdx = 0;
                row.createCell(colIdx++).setCellValue(dto.getEmployeeId());
                row.createCell(colIdx++).setCellValue(dto.getEmployeeName());
                row.createCell(colIdx++).setCellValue(dto.getDepartmentName());

                Map<LocalDate, String> dailyStatus = dto.getDailyStatus();
                for (int i = 0; i < 7; i++) {
                    String status = dailyStatus.getOrDefault(startOfWeek.plusDays(i), "-");
                    // Simplify status for Excel similar to PDF (P, A, etc or full word?) - Full
                    // word is better for Excel
                    // actually let's stick to short codes if it's too long, but Excel has space.
                    // Let's use short codes for readability in grid
                    String code = "-";
                    if (status.contains("PRESENT"))
                        code = "P";
                    else if (status.contains("ABSENT"))
                        code = "A";
                    else if (status.contains("WEEKEND"))
                        code = "W";
                    else if (status.contains("HOLIDAY"))
                        code = "H";
                    else if (status.contains("LATE"))
                        code = "L";
                    else if (status.contains("EARLY"))
                        code = "E";
                    else if (status.contains("LEAVE"))
                        code = "LV";

                    row.createCell(colIdx++).setCellValue(code);
                }

                row.createCell(colIdx++).setCellValue(dto.getPresentCount());
                row.createCell(colIdx++).setCellValue(dto.getAbsentCount());
                row.createCell(colIdx++).setCellValue(dto.getLateCount());
                row.createCell(colIdx++).setCellValue(dto.getEarlyLeaveCount());
                row.createCell(colIdx++).setCellValue(dto.getLeaveCount());
            }

            for (int i = 0; i < colIdx; i++)
                sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportWeeklyCsv(List<WeeklyAttendanceDto> report, LocalDate startOfWeek) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out)) {

            // Build header list
            java.util.List<String> headers = new java.util.ArrayList<>();
            headers.add("Employee ID");
            headers.add("Name");
            headers.add("Department");
            for (int i = 0; i < 7; i++)
                headers.add(startOfWeek.plusDays(i).toString());
            headers.add("Present");
            headers.add("Absent");
            headers.add("Late");
            headers.add("Early");
            headers.add("Leave");

            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (WeeklyAttendanceDto dto : report) {
                    java.util.List<Object> record = new java.util.ArrayList<>();
                    record.add(dto.getEmployeeId());
                    record.add(dto.getEmployeeName());
                    record.add(dto.getDepartmentName());

                    Map<LocalDate, String> dailyStatus = dto.getDailyStatus();
                    for (int i = 0; i < 7; i++) {
                        record.add(dailyStatus.getOrDefault(startOfWeek.plusDays(i), "-"));
                    }

                    record.add(dto.getPresentCount());
                    record.add(dto.getAbsentCount());
                    record.add(dto.getLateCount());
                    record.add(dto.getEarlyLeaveCount());
                    record.add(dto.getLeaveCount());

                    printer.printRecord(record);
                }
            }
            return out.toByteArray();
        }
    }

    // --- Monthly Report ---

    public byte[] exportMonthlyExcel(List<MonthlySummaryDto> report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Monthly Report");

            Row headerRow = sheet.createRow(0);
            String[] columns = { "ID", "Name", "Department", "Period", "Present", "Absent", "Late", "Early",
                    "Total Leave",
                    "Paid Leave", "Unpaid Leave", "Active Work", "Break Time" };

            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowIdx = 1;
            for (MonthlySummaryDto dto : report) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(dto.getEmployeeId());
                row.createCell(col++).setCellValue(dto.getEmployeeName());
                row.createCell(col++).setCellValue(dto.getDepartmentName());
                // Add Period
                String period = java.time.Month.of(dto.getMonth()).toString() + "-" + dto.getYear();
                row.createCell(col++).setCellValue(period);

                row.createCell(col++).setCellValue(dto.getPresentCount());
                row.createCell(col++).setCellValue(dto.getAbsentCount());
                row.createCell(col++).setCellValue(dto.getLateCount());
                row.createCell(col++).setCellValue(dto.getEarlyLeaveCount());
                row.createCell(col++).setCellValue(dto.getLeaveCount());
                row.createCell(col++).setCellValue(dto.getPaidLeaveCount());
                row.createCell(col++).setCellValue(dto.getUnpaidLeaveCount());
                row.createCell(col++).setCellValue(dto.getTotalActiveDuration());
                row.createCell(col++).setCellValue(dto.getTotalBreakDuration());
            }

            for (int i = 0; i < columns.length; i++)
                sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportMonthlyCsv(List<MonthlySummaryDto> report) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Name", "Department", "Period", "Present", "Absent", "Late", "Early",
                            "Total Leave",
                            "Paid Leave", "Unpaid Leave", "Active Work", "Break Time")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (MonthlySummaryDto dto : report) {
                    String period = java.time.Month.of(dto.getMonth()).toString() + "-" + dto.getYear();
                    printer.printRecord(
                            dto.getEmployeeId(),
                            dto.getEmployeeName(),
                            dto.getDepartmentName(),
                            period,
                            dto.getPresentCount(),
                            dto.getAbsentCount(),
                            dto.getLateCount(),
                            dto.getEarlyLeaveCount(),
                            dto.getLeaveCount(),
                            dto.getPaidLeaveCount(),
                            dto.getUnpaidLeaveCount(),
                            dto.getTotalActiveDuration(),
                            dto.getTotalBreakDuration());
                }
            }
            return out.toByteArray();
        }
    }

    // --- Employee Detail (No Range for now, simple monthly detail) ---
    // Can expand if needed
    // --- Single Employee Weekly Detail ---

    public byte[] exportEmployeeWeeklyDetailExcel(EmployeeWeeklyDetailDto report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employee Weekly Report");

            // Info Header
            Row infoRow = sheet.createRow(0);
            infoRow.createCell(0)
                    .setCellValue("Employee: " + report.getEmployeeName() + " (" + report.getEmployeeId() + ")");

            Row deptRow = sheet.createRow(1);
            deptRow.createCell(0).setCellValue("Department: " + report.getDepartmentName());

            Row headerRow = sheet.createRow(3);
            String[] columns = { "Date", "Day", "In Time", "Out Time", "Late", "Early", "Status" };

            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowIdx = 4;
            for (EmployeeWeeklyDetailDto.DailyDetail day : report.getDailyDetails()) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(day.getDate().toString());
                row.createCell(col++).setCellValue(day.getDayOfWeek().toString());
                row.createCell(col++).setCellValue(day.getInTime() != null ? day.getInTime().toString() : "-");
                row.createCell(col++).setCellValue(day.getOutTime() != null ? day.getOutTime().toString() : "-");
                row.createCell(col++)
                        .setCellValue(day.getLateDurationMinutes() > 0 ? day.getLateDurationMinutes() + " min" : "-");
                row.createCell(col++).setCellValue(
                        day.getEarlyLeaveDurationMinutes() > 0 ? day.getEarlyLeaveDurationMinutes() + " min" : "-");
                row.createCell(col++).setCellValue(day.getStatus());
            }

            // Summary Row
            rowIdx++;
            Row summaryRow = sheet.createRow(rowIdx);
            summaryRow.createCell(0).setCellValue("Summary:");
            summaryRow.createCell(1).setCellValue("P: " + report.getTotalPresent() + ", A: " + report.getTotalAbsent() +
                    ", L: " + report.getTotalLates() + ", E: " + report.getTotalEarlyLeaves() +
                    ", LV: " + report.getTotalLeaves());

            for (int i = 0; i < columns.length; i++)
                sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportEmployeeWeeklyDetailCsv(EmployeeWeeklyDetailDto report) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Date", "Day", "In Time", "Out Time", "Late", "Early", "Status")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                // Info as comment or first rows? CSV usually raw data. Let's keep it raw data
                // table.
                for (EmployeeWeeklyDetailDto.DailyDetail day : report.getDailyDetails()) {
                    printer.printRecord(
                            day.getDate(),
                            day.getDayOfWeek(),
                            day.getInTime() != null ? day.getInTime() : "-",
                            day.getOutTime() != null ? day.getOutTime() : "-",
                            day.getLateDurationMinutes() > 0 ? day.getLateDurationMinutes() + " min" : "-",
                            day.getEarlyLeaveDurationMinutes() > 0 ? day.getEarlyLeaveDurationMinutes() + " min" : "-",
                            day.getStatus());
                }
            }
            return out.toByteArray();
        }
    }

    // --- Single Employee Monthly Detail (and Range) ---

    public byte[] exportEmployeeMonthlyDetailExcel(EmployeeMonthlyDetailDto report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Monthly Sheet");
            // Create style here to pass
            CellStyle bold = workbook.createCellStyle();
            Font f = workbook.createFont();
            f.setBold(true);
            bold.setFont(f);

            createMonthlyDetailSheet(sheet, report, bold);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportEmployeeRangeReportExcel(EmployeeRangeReportDto report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create Styles Helper
            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            // Summary Sheet
            Sheet summarySheet = workbook.createSheet("Overall Summary");
            Row r0 = summarySheet.createRow(0);
            r0.createCell(0).setCellValue("Report for: " + report.getEmployeeName());
            Row r1 = summarySheet.createRow(1);
            r1.createCell(0).setCellValue("Period: " + report.getStartDate() + " to " + report.getEndDate());

            Row r3 = summarySheet.createRow(3);
            r3.createCell(0).setCellValue("Total Present: " + report.getTotalPresent());
            Row r4 = summarySheet.createRow(4);
            r4.createCell(0).setCellValue("Total Absent: " + report.getTotalAbsent());
            Row r5 = summarySheet.createRow(5);
            r5.createCell(0).setCellValue("Total Leaves: " + report.getTotalLeaves());

            // Individual Sheets for months
            int sheetCounter = 1;
            for (EmployeeMonthlyDetailDto monthly : report.getMonthlyReports()) {
                // Safe Sheet Name
                String safeName = monthly.getMonth() + "-" + monthly.getYear();
                // Ensure uniqueness if for some reason duplicates exist (though unlikely with
                // current logic)
                if (workbook.getSheet(safeName) != null) {
                    safeName = safeName + " (" + sheetCounter++ + ")";
                }

                // Create sheet with safe name
                Sheet mSheet = null;
                try {
                    mSheet = workbook.createSheet(safeName);
                } catch (IllegalArgumentException e) {
                    // Fallback for invalid chars
                    mSheet = workbook.createSheet("Month-" + sheetCounter++);
                }

                createMonthlyDetailSheet(mSheet, monthly, boldStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createMonthlyDetailSheet(Sheet sheet, EmployeeMonthlyDetailDto report, CellStyle boldStyle) {
        // Reuse passed style
        if (boldStyle == null) {
            boldStyle = sheet.getWorkbook().createCellStyle();
            Font f = sheet.getWorkbook().createFont();
            f.setBold(true);
            boldStyle.setFont(f);
        }

        Row h1 = sheet.createRow(0);
        h1.createCell(0).setCellValue("Month: " + report.getMonth() + "/" + report.getYear());

        Row headerRow = sheet.createRow(2);
        String[] columns = { "Date", "Day", "In Time", "Out Time", "Late", "Early", "Status" };

        for (int i = 0; i < columns.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(columns[i]);
            c.setCellStyle(boldStyle);
        }

        int idx = 3;
        for (EmployeeWeeklyDetailDto.DailyDetail day : report.getDailyDetails()) {
            Row row = sheet.createRow(idx++);
            int c = 0;
            row.createCell(c++).setCellValue(day.getDate().toString());
            row.createCell(c++).setCellValue(day.getDayOfWeek().toString());
            row.createCell(c++).setCellValue(day.getInTime() != null ? day.getInTime().toString() : "-");
            row.createCell(c++).setCellValue(day.getOutTime() != null ? day.getOutTime().toString() : "-");
            row.createCell(c++)
                    .setCellValue(day.getLateDurationMinutes() > 0 ? day.getLateDurationMinutes() + " min" : "-");
            row.createCell(c++).setCellValue(
                    day.getEarlyLeaveDurationMinutes() > 0 ? day.getEarlyLeaveDurationMinutes() + " min" : "-");
            row.createCell(c++).setCellValue(day.getStatus());
        }
        for (int i = 0; i < columns.length; i++)
            sheet.autoSizeColumn(i);
    }

    public byte[] exportEmployeeMonthlyDetailCsv(EmployeeMonthlyDetailDto report) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Date", "Day", "In Time", "Out Time", "Late", "Early", "Status")
                    .build();
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (EmployeeWeeklyDetailDto.DailyDetail day : report.getDailyDetails()) {
                    printer.printRecord(
                            day.getDate(), day.getDayOfWeek(),
                            day.getInTime() != null ? day.getInTime() : "-",
                            day.getOutTime() != null ? day.getOutTime() : "-",
                            day.getLateDurationMinutes() > 0 ? day.getLateDurationMinutes() + " min" : "-",
                            day.getEarlyLeaveDurationMinutes() > 0 ? day.getEarlyLeaveDurationMinutes() + " min" : "-",
                            day.getStatus());
                }
            }
            return out.toByteArray();
        }
    }

    public byte[] exportEmployeeRangeReportCsv(EmployeeRangeReportDto report) throws IOException {
        // Flatten all months into one CSV
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Month", "Year", "Date", "Day", "In Time", "Out Time", "Late", "Early", "Status")
                    .build();
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (EmployeeMonthlyDetailDto monthly : report.getMonthlyReports()) {
                    for (EmployeeWeeklyDetailDto.DailyDetail day : monthly.getDailyDetails()) {
                        printer.printRecord(
                                monthly.getMonth(), monthly.getYear(),
                                day.getDate(), day.getDayOfWeek(),
                                day.getInTime() != null ? day.getInTime() : "-",
                                day.getOutTime() != null ? day.getOutTime() : "-",
                                day.getLateDurationMinutes() > 0 ? day.getLateDurationMinutes() + " min" : "-",
                                day.getEarlyLeaveDurationMinutes() > 0 ? day.getEarlyLeaveDurationMinutes() + " min"
                                        : "-",
                                day.getStatus());
                    }
                }
            }
            return out.toByteArray();
        }
    }

    public byte[] exportBankAdviceExcel(List<root.cyb.mh.attendancesystem.model.Payslip> slips) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bank Advice");

            Row headerRow = sheet.createRow(0);
            String[] columns = { "Employee ID", "Name", "Bank Name", "Account Number", "Net Salary", "Payment Ref" };

            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowIdx = 1;
            for (root.cyb.mh.attendancesystem.model.Payslip p : slips) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(p.getEmployee().getId());
                row.createCell(col++).setCellValue(p.getEmployee().getName());
                row.createCell(col++).setCellValue(p.getEmployee() != null ? p.getEmployee().getBankName() : "");
                row.createCell(col++).setCellValue(p.getEmployee() != null ? p.getEmployee().getAccountNumber() : "");
                row.createCell(col++).setCellValue(p.getNetSalary() != null ? p.getNetSalary() : 0.0);
                row.createCell(col++).setCellValue("Salary " + p.getMonth());
            }

            for (int i = 0; i < columns.length; i++)
                sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // --- Accounts Receivable Aging & Work Orders Export ---

    public byte[] exportClientAgingExcel(AgingSummaryDTO agingSummary) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("AR Aging Portfolio");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));

            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            boldCurrencyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            boldCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle boldText = workbook.createCellStyle();
            boldText.setFont(boldFont);
            boldText.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            boldText.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Client Name", "Client Code", "Risk Rating", "Avg Age (Days)",
                    "Configured Thresholds", "Override Status", "Unpaid WOs", "Total Unpaid ($)",
                    "Current (<40d) ($)", "Standard Due ($)", "Past Due ($)", "Critical Delinquent ($)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            if (agingSummary != null && agingSummary.getClientStats() != null) {
                for (AgingSummaryDTO.ClientAgingStat stat : agingSummary.getClientStats()) {
                    Row row = sheet.createRow(rowIdx++);
                    int c = 0;
                    row.createCell(c++).setCellValue(stat.getClientName());
                    row.createCell(c++).setCellValue(stat.getClientIdentifier());
                    row.createCell(c++).setCellValue(stat.getRiskScoreLabel());
                    row.createCell(c++).setCellValue(stat.getWeightedAverageDays());
                    row.createCell(c++).setCellValue(stat.getNormalDueDays() + " / " + stat.getOverdueDays() + " / " + stat.getCriticalDueDays() + "d");
                    row.createCell(c++).setCellValue(stat.isCustomConfig() ? "Custom" : "Default");
                    row.createCell(c++).setCellValue(stat.getTotalUnpaidCount());

                    Cell cTot = row.createCell(c++);
                    cTot.setCellValue(stat.getTotalUnpaidAmount() != null ? stat.getTotalUnpaidAmount().doubleValue() : 0.0);
                    cTot.setCellStyle(currencyStyle);

                    Cell cCur = row.createCell(c++);
                    cCur.setCellValue(stat.getWithinTermsAmount() != null ? stat.getWithinTermsAmount().doubleValue() : 0.0);
                    cCur.setCellStyle(currencyStyle);

                    Cell cStd = row.createCell(c++);
                    cStd.setCellValue(stat.getStandardDueAmount() != null ? stat.getStandardDueAmount().doubleValue() : 0.0);
                    cStd.setCellStyle(currencyStyle);

                    Cell cPast = row.createCell(c++);
                    cPast.setCellValue(stat.getPastDueAmount() != null ? stat.getPastDueAmount().doubleValue() : 0.0);
                    cPast.setCellStyle(currencyStyle);

                    Cell cCrit = row.createCell(c++);
                    cCrit.setCellValue(stat.getCriticalDueAmount() != null ? stat.getCriticalDueAmount().doubleValue() : 0.0);
                    cCrit.setCellStyle(currencyStyle);
                }

                // Summary Totals Row
                Row totalRow = sheet.createRow(rowIdx);
                int c = 0;
                Cell t0 = totalRow.createCell(c++);
                t0.setCellValue("TOTAL PORTFOLIO");
                t0.setCellStyle(boldText);

                Cell t1 = totalRow.createCell(c++);
                t1.setCellValue("");
                t1.setCellStyle(boldText);

                Cell t2 = totalRow.createCell(c++);
                t2.setCellValue("");
                t2.setCellStyle(boldText);

                Cell tAvg = totalRow.createCell(c++);
                tAvg.setCellValue(agingSummary.getPortfolioAverageDays());
                tAvg.setCellStyle(boldText);

                Cell t4 = totalRow.createCell(c++);
                t4.setCellValue("Active Overrides: " + agingSummary.getClientConfigs().size());
                t4.setCellStyle(boldText);

                Cell t5 = totalRow.createCell(c++);
                t5.setCellValue("");
                t5.setCellStyle(boldText);

                Cell tCount = totalRow.createCell(c++);
                tCount.setCellValue(agingSummary.getTotalUnpaidCount());
                tCount.setCellStyle(boldText);

                Cell tTot = totalRow.createCell(c++);
                tTot.setCellValue(agingSummary.getTotalUnpaidAmount() != null ? agingSummary.getTotalUnpaidAmount().doubleValue() : 0.0);
                tTot.setCellStyle(boldCurrencyStyle);

                Cell tCur = totalRow.createCell(c++);
                tCur.setCellValue(agingSummary.getWithinTermsAmount() != null ? agingSummary.getWithinTermsAmount().doubleValue() : 0.0);
                tCur.setCellStyle(boldCurrencyStyle);

                Cell tStd = totalRow.createCell(c++);
                tStd.setCellValue(agingSummary.getStandardDueAmount() != null ? agingSummary.getStandardDueAmount().doubleValue() : 0.0);
                tStd.setCellStyle(boldCurrencyStyle);

                Cell tPast = totalRow.createCell(c++);
                tPast.setCellValue(agingSummary.getPastDueAmount() != null ? agingSummary.getPastDueAmount().doubleValue() : 0.0);
                tPast.setCellStyle(boldCurrencyStyle);

                Cell tCrit = totalRow.createCell(c++);
                tCrit.setCellValue(agingSummary.getCriticalDueAmount() != null ? agingSummary.getCriticalDueAmount().doubleValue() : 0.0);
                tCrit.setCellStyle(boldCurrencyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportClientAgingCsv(AgingSummaryDTO agingSummary) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Client Name", "Client Code", "Risk Rating", "Avg Age (Days)",
                            "Configured Thresholds", "Override Status", "Unpaid WOs", "Total Unpaid ($)",
                            "Current (<40d) ($)", "Standard Due ($)", "Past Due ($)", "Critical Delinquent ($)")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                if (agingSummary != null && agingSummary.getClientStats() != null) {
                    for (AgingSummaryDTO.ClientAgingStat stat : agingSummary.getClientStats()) {
                        printer.printRecord(
                                stat.getClientName(),
                                stat.getClientIdentifier(),
                                stat.getRiskScoreLabel(),
                                stat.getWeightedAverageDays(),
                                stat.getNormalDueDays() + " / " + stat.getOverdueDays() + " / " + stat.getCriticalDueDays() + "d",
                                stat.isCustomConfig() ? "Custom" : "Default",
                                stat.getTotalUnpaidCount(),
                                stat.getTotalUnpaidAmount(),
                                stat.getWithinTermsAmount(),
                                stat.getStandardDueAmount(),
                                stat.getPastDueAmount(),
                                stat.getCriticalDueAmount()
                        );
                    }
                    printer.printRecord(
                            "TOTAL PORTFOLIO",
                            "",
                            "",
                            agingSummary.getPortfolioAverageDays(),
                            "Active Overrides: " + agingSummary.getClientConfigs().size(),
                            "",
                            agingSummary.getTotalUnpaidCount(),
                            agingSummary.getTotalUnpaidAmount(),
                            agingSummary.getWithinTermsAmount(),
                            agingSummary.getStandardDueAmount(),
                            agingSummary.getPastDueAmount(),
                            agingSummary.getCriticalDueAmount()
                    );
                }
            }
            return out.toByteArray();
        }
    }

    public byte[] exportSeriesAgingExcel(AgingSummaryDTO agingSummary) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Series Aging Portfolio");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));

            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));
            boldCurrencyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            boldCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle boldText = workbook.createCellStyle();
            boldText.setFont(boldFont);
            boldText.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            boldText.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Series", "Code Range", "Clients Count", "Included Clients", "Risk Rating", "Avg Age (Days)",
                    "Unpaid WOs", "Total Unpaid ($)", "Current (<40d) ($)", "Standard Due ($)", "Past Due ($)", "Critical Delinquent ($)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            if (agingSummary != null && agingSummary.getSeriesStats() != null) {
                for (AgingSummaryDTO.SeriesAgingStat stat : agingSummary.getSeriesStats()) {
                    Row row = sheet.createRow(rowIdx++);
                    int c = 0;
                    row.createCell(c++).setCellValue(stat.getSeriesName());
                    row.createCell(c++).setCellValue(stat.getSeriesRange());
                    row.createCell(c++).setCellValue(stat.getClientCount());
                    row.createCell(c++).setCellValue(stat.getClientsSummary());
                    row.createCell(c++).setCellValue(stat.getRiskScoreLabel());
                    row.createCell(c++).setCellValue(stat.getWeightedAverageDays());
                    row.createCell(c++).setCellValue(stat.getTotalUnpaidCount());

                    Cell cTot = row.createCell(c++);
                    cTot.setCellValue(stat.getTotalUnpaidAmount() != null ? stat.getTotalUnpaidAmount().doubleValue() : 0.0);
                    cTot.setCellStyle(currencyStyle);

                    Cell cCur = row.createCell(c++);
                    cCur.setCellValue(stat.getWithinTermsAmount() != null ? stat.getWithinTermsAmount().doubleValue() : 0.0);
                    cCur.setCellStyle(currencyStyle);

                    Cell cStd = row.createCell(c++);
                    cStd.setCellValue(stat.getStandardDueAmount() != null ? stat.getStandardDueAmount().doubleValue() : 0.0);
                    cStd.setCellStyle(currencyStyle);

                    Cell cPast = row.createCell(c++);
                    cPast.setCellValue(stat.getPastDueAmount() != null ? stat.getPastDueAmount().doubleValue() : 0.0);
                    cPast.setCellStyle(currencyStyle);

                    Cell cCrit = row.createCell(c++);
                    cCrit.setCellValue(stat.getCriticalDueAmount() != null ? stat.getCriticalDueAmount().doubleValue() : 0.0);
                    cCrit.setCellStyle(currencyStyle);
                }

                // Summary Total Row
                Row totalRow = sheet.createRow(rowIdx);
                int c = 0;
                Cell t1 = totalRow.createCell(c++);
                t1.setCellValue("TOTAL PORTFOLIO");
                t1.setCellStyle(boldText);

                Cell t2 = totalRow.createCell(c++);
                t2.setCellValue("");
                t2.setCellStyle(boldText);

                Cell t3 = totalRow.createCell(c++);
                t3.setCellValue(agingSummary.getClientStats() != null ? agingSummary.getClientStats().size() : 0);
                t3.setCellStyle(boldText);

                Cell t4 = totalRow.createCell(c++);
                t4.setCellValue("Active Series: " + agingSummary.getSeriesStats().size());
                t4.setCellStyle(boldText);

                Cell t5 = totalRow.createCell(c++);
                t5.setCellValue("");
                t5.setCellStyle(boldText);

                Cell tDays = totalRow.createCell(c++);
                tDays.setCellValue(agingSummary.getPortfolioAverageDays());
                tDays.setCellStyle(boldText);

                Cell tCount = totalRow.createCell(c++);
                tCount.setCellValue(agingSummary.getTotalUnpaidCount());
                tCount.setCellStyle(boldText);

                Cell tTot = totalRow.createCell(c++);
                tTot.setCellValue(agingSummary.getTotalUnpaidAmount() != null ? agingSummary.getTotalUnpaidAmount().doubleValue() : 0.0);
                tTot.setCellStyle(boldCurrencyStyle);

                Cell tCur = totalRow.createCell(c++);
                tCur.setCellValue(agingSummary.getWithinTermsAmount() != null ? agingSummary.getWithinTermsAmount().doubleValue() : 0.0);
                tCur.setCellStyle(boldCurrencyStyle);

                Cell tStd = totalRow.createCell(c++);
                tStd.setCellValue(agingSummary.getStandardDueAmount() != null ? agingSummary.getStandardDueAmount().doubleValue() : 0.0);
                tStd.setCellStyle(boldCurrencyStyle);

                Cell tPast = totalRow.createCell(c++);
                tPast.setCellValue(agingSummary.getPastDueAmount() != null ? agingSummary.getPastDueAmount().doubleValue() : 0.0);
                tPast.setCellStyle(boldCurrencyStyle);

                Cell tCrit = totalRow.createCell(c++);
                tCrit.setCellValue(agingSummary.getCriticalDueAmount() != null ? agingSummary.getCriticalDueAmount().doubleValue() : 0.0);
                tCrit.setCellStyle(boldCurrencyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportSeriesAgingCsv(AgingSummaryDTO agingSummary) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Series", "Code Range", "Clients Count", "Included Clients", "Risk Rating", "Avg Age (Days)",
                            "Unpaid WOs", "Total Unpaid ($)", "Current (<40d) ($)", "Standard Due ($)", "Past Due ($)", "Critical Delinquent ($)")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                if (agingSummary != null && agingSummary.getSeriesStats() != null) {
                    for (AgingSummaryDTO.SeriesAgingStat stat : agingSummary.getSeriesStats()) {
                        printer.printRecord(
                                stat.getSeriesName(),
                                stat.getSeriesRange(),
                                stat.getClientCount(),
                                stat.getClientsSummary(),
                                stat.getRiskScoreLabel(),
                                stat.getWeightedAverageDays(),
                                stat.getTotalUnpaidCount(),
                                stat.getTotalUnpaidAmount(),
                                stat.getWithinTermsAmount(),
                                stat.getStandardDueAmount(),
                                stat.getPastDueAmount(),
                                stat.getCriticalDueAmount()
                        );
                    }
                    printer.printRecord(
                            "TOTAL PORTFOLIO",
                            "",
                            agingSummary.getClientStats() != null ? agingSummary.getClientStats().size() : 0,
                            "Active Series: " + agingSummary.getSeriesStats().size(),
                            "",
                            agingSummary.getPortfolioAverageDays(),
                            agingSummary.getTotalUnpaidCount(),
                            agingSummary.getTotalUnpaidAmount(),
                            agingSummary.getWithinTermsAmount(),
                            agingSummary.getStandardDueAmount(),
                            agingSummary.getPastDueAmount(),
                            agingSummary.getCriticalDueAmount()
                    );
                }
            }
            return out.toByteArray();
        }
    }

    public byte[] exportDueWorkOrdersExcel(List<EmployeeWorkOrder> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Due Work Orders");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("$#,##0.00"));

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Work Order #", "Client", "Invoice Date", "Days Elapsed", "Gross Invoice ($)",
                    "Discounted Net ($)", "Paid Amount ($)", "Work Type", "Category", "Address", "City", "State", "Zip"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            if (orders != null) {
                for (EmployeeWorkOrder wo : orders) {
                    Row row = sheet.createRow(rowIdx++);
                    int c = 0;
                    row.createCell(c++).setCellValue(wo.getWoNumber() != null ? wo.getWoNumber() : "-");
                    row.createCell(c++).setCellValue(wo.getClient() != null ? wo.getClient().getName() : (wo.getOriginalClientString() != null ? wo.getOriginalClientString() : "-"));
                    row.createCell(c++).setCellValue(wo.getInvoiceDate() != null ? wo.getInvoiceDate().toString() : "-");
                    row.createCell(c++).setCellValue(wo.getDaysElapsed());

                    Cell cGross = row.createCell(c++);
                    cGross.setCellValue(wo.getClientInvoiceTotal() != null ? wo.getClientInvoiceTotal().doubleValue() : 0.0);
                    cGross.setCellStyle(currencyStyle);

                    Cell cNet = row.createCell(c++);
                    cNet.setCellValue(wo.getEffectiveClientTotal() != null ? wo.getEffectiveClientTotal().doubleValue() : 0.0);
                    cNet.setCellStyle(currencyStyle);

                    Cell cPaid = row.createCell(c++);
                    cPaid.setCellValue(wo.getClientPaidAmount() != null ? wo.getClientPaidAmount().doubleValue() : 0.0);
                    cPaid.setCellStyle(currencyStyle);

                    row.createCell(c++).setCellValue(wo.getWorkType() != null ? wo.getWorkType() : "-");
                    row.createCell(c++).setCellValue(wo.getCategory() != null ? wo.getCategory() : "-");
                    row.createCell(c++).setCellValue(wo.getAddress() != null ? wo.getAddress() : "-");
                    row.createCell(c++).setCellValue(wo.getCity() != null ? wo.getCity() : "-");
                    row.createCell(c++).setCellValue(wo.getState() != null ? wo.getState() : "-");
                    row.createCell(c++).setCellValue(wo.getZip() != null ? wo.getZip() : "-");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportDueWorkOrdersCsv(List<EmployeeWorkOrder> orders) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(out)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("Work Order #", "Client", "Invoice Date", "Days Elapsed", "Gross Invoice ($)",
                            "Discounted Net ($)", "Paid Amount ($)", "Work Type", "Category", "Address", "City", "State", "Zip")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                if (orders != null) {
                    for (EmployeeWorkOrder wo : orders) {
                        printer.printRecord(
                                wo.getWoNumber() != null ? wo.getWoNumber() : "-",
                                wo.getClient() != null ? wo.getClient().getName() : (wo.getOriginalClientString() != null ? wo.getOriginalClientString() : "-"),
                                wo.getInvoiceDate() != null ? wo.getInvoiceDate().toString() : "-",
                                wo.getDaysElapsed(),
                                wo.getClientInvoiceTotal() != null ? wo.getClientInvoiceTotal() : BigDecimal.ZERO,
                                wo.getEffectiveClientTotal() != null ? wo.getEffectiveClientTotal() : BigDecimal.ZERO,
                                wo.getClientPaidAmount() != null ? wo.getClientPaidAmount() : BigDecimal.ZERO,
                                wo.getWorkType() != null ? wo.getWorkType() : "-",
                                wo.getCategory() != null ? wo.getCategory() : "-",
                                wo.getAddress() != null ? wo.getAddress() : "-",
                                wo.getCity() != null ? wo.getCity() : "-",
                                wo.getState() != null ? wo.getState() : "-",
                                wo.getZip() != null ? wo.getZip() : "-"
                        );
                    }
                }
            }
            return out.toByteArray();
        }
    }
}
