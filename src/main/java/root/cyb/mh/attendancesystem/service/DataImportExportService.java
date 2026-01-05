package root.cyb.mh.attendancesystem.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.*;
import root.cyb.mh.attendancesystem.repository.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DataImportExportService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private WorkScheduleRepository workScheduleRepository;
    @Autowired
    private UserRepository userRepository;

    // --- EXPORT METODS ---

    public void exportEmployees(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer,
                CSVFormat.DEFAULT.withHeader("ID", "Name", "DepartmentID", "CardID"));
        for (Employee emp : employeeRepository.findAll()) {
            printer.printRecord(emp.getId(), emp.getName(),
                    emp.getDepartment() != null ? emp.getDepartment().getId() : "", emp.getCardId());
        }
        printer.flush();
    }

    public void exportDepartments(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("ID", "Name"));
        for (Department dept : departmentRepository.findAll()) {
            printer.printRecord(dept.getId(), dept.getName());
        }
        printer.flush();
    }

    public void exportLeaveRequests(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer,
                CSVFormat.DEFAULT.withHeader("ID", "EmployeeID", "StartDate", "EndDate", "Reason", "Status"));
        for (LeaveRequest lr : leaveRequestRepository.findAll()) {
            printer.printRecord(lr.getId(), lr.getEmployee().getId(), lr.getStartDate(), lr.getEndDate(),
                    lr.getReason(), lr.getStatus());
        }
        printer.flush();
    }

    public void exportDevices(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("ID", "Name", "IP", "Port", "Serial"));
        for (Device d : deviceRepository.findAll()) {
            printer.printRecord(d.getId(), d.getName(), d.getIpAddress(), d.getPort(), d.getSerialNumber());
        }
        printer.flush();
    }

    public void exportSettings(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("ID", "StartTime", "EndTime",
                "LateTolerance", "EarlyTolerance", "Weekends"));
        for (WorkSchedule ws : workScheduleRepository.findAll()) {
            printer.printRecord(ws.getId(), ws.getStartTime(), ws.getEndTime(), ws.getLateToleranceMinutes(),
                    ws.getEarlyLeaveToleranceMinutes(), ws.getWeekendDays());
        }
        printer.flush();
    }

    public void exportUsers(PrintWriter writer) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("ID", "Username", "Role"));
        for (User u : userRepository.findAll()) {
            printer.printRecord(u.getId(), u.getUsername(), u.getRole());
        }
        printer.flush();
    }

    // --- IMPORT METHODS ---

    public void importEmployees(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            String id = record.get("ID");
            Employee emp = employeeRepository.findById(id).orElse(new Employee());
            emp.setId(id);
            emp.setName(record.get("Name"));
            String deptId = record.get("DepartmentID");
            if (deptId != null && !deptId.isEmpty()) {
                departmentRepository.findById(Long.parseLong(deptId)).ifPresent(emp::setDepartment);
            }
            if (record.isMapped("CardID"))
                emp.setCardId(record.get("CardID"));
            employeeRepository.save(emp);
        }
    }

    public void importDepartments(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            Department dept = new Department();
            if (record.isMapped("ID") && !record.get("ID").isEmpty()) {
                Long id = Long.parseLong(record.get("ID"));
                if (departmentRepository.existsById(id))
                    dept.setId(id);
            }
            dept.setName(record.get("Name"));
            departmentRepository.save(dept);
        }
    }

    public void importLeaveRequests(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            LeaveRequest lr = new LeaveRequest();
            // Assuming new imports, or update if logic matches. Here simple insert for
            // simplicity/demo
            // or match by ID if present
            if (record.isMapped("ID") && !record.get("ID").isEmpty()) {
                Long id = Long.parseLong(record.get("ID"));
                leaveRequestRepository.findById(id).ifPresent(found -> lr.setId(found.getId()));
            }

            String empId = record.get("EmployeeID");
            employeeRepository.findById(empId).ifPresent(lr::setEmployee);

            lr.setStartDate(LocalDate.parse(record.get("StartDate")));
            lr.setEndDate(LocalDate.parse(record.get("EndDate")));
            lr.setReason(record.get("Reason"));
            if (record.isMapped("Status")) {
                lr.setStatus(LeaveRequest.Status.valueOf(record.get("Status")));
            } else {
                lr.setStatus(LeaveRequest.Status.PENDING);
            }
            leaveRequestRepository.save(lr);
        }
    }

    public void importDevices(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            Device d = new Device();
            if (record.isMapped("ID") && !record.get("ID").isEmpty()) {
                Long id = Long.parseLong(record.get("ID"));
                deviceRepository.findById(id).ifPresent(found -> d.setId(found.getId()));
            }
            d.setName(record.get("Name"));
            d.setIpAddress(record.get("IP"));
            d.setPort(Integer.parseInt(record.get("Port")));
            if (record.isMapped("Serial"))
                d.setSerialNumber(record.get("Serial"));
            deviceRepository.save(d);
        }
    }

    public void importSettings(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            WorkSchedule ws = workScheduleRepository.findAll().stream().findFirst().orElse(new WorkSchedule());
            ws.setStartTime(LocalTime.parse(record.get("StartTime")));
            ws.setEndTime(LocalTime.parse(record.get("EndTime")));
            ws.setLateToleranceMinutes(Integer.parseInt(record.get("LateTolerance")));
            ws.setEarlyLeaveToleranceMinutes(Integer.parseInt(record.get("EarlyTolerance")));
            ws.setWeekendDays(record.get("Weekends"));
            workScheduleRepository.save(ws);
        }
    }

    public void importUsers(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        for (CSVRecord record : records) {
            // Only update existing or add new if username unique
            String username = record.get("Username");
            User user = userRepository.findByUsername(username).orElse(new User());
            user.setUsername(username);

            if (record.isMapped("Role"))
                user.setRole(record.get("Role"));

            // Password not imported for security, or assumed handled otherwise.
            // If new user, might need default password.
            if (user.getId() == null) {
                user.setPassword("{noop}123456"); // Default/Temp password
            }

            userRepository.save(user);
        }
    }

    // --- PAYMENT REQUEST EXPORTS ---

    // Key -> Header Name
    private static final java.util.LinkedHashMap<String, String> EXPORT_COLUMNS = new java.util.LinkedHashMap<>();
    static {
        EXPORT_COLUMNS.put("date", "Date of Request");
        EXPORT_COLUMNS.put("requester", "Requested By");
        EXPORT_COLUMNS.put("workOrder", "Work Order");
        EXPORT_COLUMNS.put("amount", "Amount");
        EXPORT_COLUMNS.put("contractor", "Contractor");
        EXPORT_COLUMNS.put("method", "Method ID");
        EXPORT_COLUMNS.put("client", "Client Code");
        EXPORT_COLUMNS.put("priority", "Priority");
        EXPORT_COLUMNS.put("approval", "Approval Authority");
        EXPORT_COLUMNS.put("reason", "Reason");
        EXPORT_COLUMNS.put("status", "Status");
        EXPORT_COLUMNS.put("ppw", "PPW Update");
    }

    public void exportPaymentRequestsToCsv(PrintWriter writer, List<PaymentRequest> requests,
            List<String> selectedColumns) throws IOException {
        // Default to all if empty
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            selectedColumns = new java.util.ArrayList<>(EXPORT_COLUMNS.keySet());
        }

        // Build Header
        String[] headers = selectedColumns.stream()
                .map(key -> EXPORT_COLUMNS.getOrDefault(key, key))
                .toArray(String[]::new);

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build());

        for (PaymentRequest p : requests) {
            List<Object> record = new java.util.ArrayList<>();
            for (String col : selectedColumns) {
                record.add(getColumnValue(p, col));
            }
            printer.printRecord(record);
        }
        printer.flush();
        printer.close();
    }

    public void exportPaymentRequestsToPdf(java.io.OutputStream out, List<PaymentRequest> requests, String title,
            List<String> selectedColumns) {
        // Default to all if empty
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            selectedColumns = new java.util.ArrayList<>(EXPORT_COLUMNS.keySet());
        }

        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(),
                    10, 10, 10, 10);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Title
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Paragraph titlePara = new com.lowagie.text.Paragraph(title, titleFont);
            titlePara.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(10);
            document.add(titlePara);

            // Table
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(selectedColumns.size());
            table.setWidthPercentage(100);

            // Dynamic Widths
            float[] widths = new float[selectedColumns.size()];
            for (int i = 0; i < selectedColumns.size(); i++) {
                widths[i] = getColumnWidth(selectedColumns.get(i));
            }
            table.setWidths(widths);

            // Header
            com.lowagie.text.Font headFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 8);
            for (String col : selectedColumns) {
                String h = EXPORT_COLUMNS.getOrDefault(col, col);
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(h, headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                cell.setPadding(3);
                table.addCell(cell);
            }

            // Rows
            com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 7);
            for (PaymentRequest p : requests) {
                for (String col : selectedColumns) {
                    addCell(table, getColumnValue(p, col), bodyFont);
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private String getColumnValue(PaymentRequest p, String key) {
        switch (key) {
            case "date":
                return p.getRequestDate().toString();
            case "requester":
                return p.getRequester() != null ? p.getRequester().getUsername()
                        : (p.getEmployeeRequester() != null ? p.getEmployeeRequester().getName() : "Unknown");
            case "workOrder":
                return p.getWorkOrderNumber();
            case "amount":
                return "$" + p.getAmount(); // CSV might prefer raw number, but stick to string for consistency
            case "contractor":
                return p.getContractor() != null ? p.getContractor().getName() : "";
            case "method":
                return p.getPaymentMethod() != null ? p.getPaymentMethod().getMethodName() : "";
            case "client":
                return p.getClient() != null ? p.getClient().getCode() : "";
            case "priority":
                return String.valueOf(p.getPriority());
            case "approval":
                return p.getApprovalAuthority() != null ? p.getApprovalAuthority().getUsername()
                        : (p.getApprovalEmployee() != null ? p.getApprovalEmployee().getName() : "");
            case "reason":
                return p.getReason();
            case "status":
                return p.getStatus().name();
            case "ppw":
                return p.getPpwUpdateStatus() != null ? p.getPpwUpdateStatus().name() : "-";
            default:
                return "";
        }
    }

    private float getColumnWidth(String key) {
        switch (key) {
            case "date":
                return 2.5f;
            case "requester":
                return 2.5f;
            case "workOrder":
                return 2.5f;
            case "amount":
                return 2f;
            case "contractor":
                return 3f;
            case "method":
                return 2.5f;
            case "client":
                return 1.5f;
            case "priority":
                return 2f;
            case "approval":
                return 2.5f;
            case "reason":
                return 3f;
            case "status":
                return 2f;
            case "ppw":
                return 2f;
            default:
                return 2f;
        }
    }

    private void addCell(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(text != null ? text : "", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    public void generateInvoicePdf(java.io.OutputStream out, PaymentRequest request) {
        try {
            // Setup Document
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 60,
                    60);
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // --- PAGE BORDER (Black) ---
            com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorStroke(java.awt.Color.BLACK);
            canvas.setLineWidth(2f);
            // Margin 20 from edges
            float borderMargin = 20;
            canvas.rectangle(borderMargin, borderMargin,
                    document.getPageSize().getWidth() - (borderMargin * 2),
                    document.getPageSize().getHeight() - (borderMargin * 2));
            canvas.stroke();

            // --- FONTS (Strict Black & White) ---
            java.awt.Color black = java.awt.Color.BLACK;

            com.lowagie.text.Font companyFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14, black);
            // Title: Large, Bold, Black
            com.lowagie.text.Font titleLabelFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 24, black);
            // Label: Small, Bold, Black (High contrast)
            com.lowagie.text.Font labelFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, black);
            com.lowagie.text.Font valueFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 10, black);
            com.lowagie.text.Font valueBoldFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10, black);
            com.lowagie.text.Font footerFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 8, black);

            // --- HEADER ---
            com.lowagie.text.pdf.PdfPTable headerTbl = new com.lowagie.text.pdf.PdfPTable(2);
            headerTbl.setWidthPercentage(100);
            headerTbl.setWidths(new float[] { 1.5f, 1f });

            // LEFT: Company Details
            com.lowagie.text.pdf.PdfPCell compCell = new com.lowagie.text.pdf.PdfPCell();
            compCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);

            String companyName = "Skylink Construction";
            String companyAddress = "123 Business Road\nDhaka, Bangladesh";
            String cPhone = null;
            String cEmail = null;

            if (request.getCompany() != null) {
                companyName = request.getCompany().getName();
                if (request.getCompany().getAddress() != null)
                    companyAddress = request.getCompany().getAddress();
                if (request.getCompany().getPhone() != null)
                    cPhone = request.getCompany().getPhone();
                if (request.getCompany().getEmail() != null)
                    cEmail = request.getCompany().getEmail();
            }

            compCell.addElement(new com.lowagie.text.Paragraph(companyName.toUpperCase(), companyFont));
            compCell.addElement(new com.lowagie.text.Paragraph(companyAddress, valueFont));
            if (cPhone != null)
                compCell.addElement(new com.lowagie.text.Paragraph(cPhone, valueFont));
            if (cEmail != null)
                compCell.addElement(new com.lowagie.text.Paragraph(cEmail, valueFont));

            headerTbl.addCell(compCell);

            // RIGHT: Title & Meta
            com.lowagie.text.pdf.PdfPCell metaCell = new com.lowagie.text.pdf.PdfPCell();
            metaCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);

            com.lowagie.text.Paragraph titleP = new com.lowagie.text.Paragraph("INVOICE", titleLabelFont);
            titleP.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            metaCell.addElement(titleP);

            metaCell.addElement(new com.lowagie.text.Paragraph(" ", valueFont)); // Spacer

            addMetaRow(metaCell, "INVOICE #", "REC-" + request.getId(), labelFont, valueBoldFont);
            addMetaRow(metaCell, "DATE", request.getRequestDate().toString(), labelFont, valueFont);

            String statusStr = request.getPaymentStatus() != null ? request.getPaymentStatus().name() : "PENDING";
            // Status in Black text, maybe uppercase
            addMetaRow(metaCell, "STATUS", statusStr, labelFont, valueBoldFont);

            headerTbl.addCell(metaCell);
            document.add(headerTbl);

            // Black Separator
            com.lowagie.text.Paragraph spacer = new com.lowagie.text.Paragraph(" ");
            spacer.setSpacingAfter(10);
            document.add(spacer);

            com.lowagie.text.pdf.draw.LineSeparator sep = new com.lowagie.text.pdf.draw.LineSeparator(1f, 100,
                    black, com.lowagie.text.Element.ALIGN_CENTER, -2);
            document.add(sep);
            document.add(spacer);

            // --- PAYMENT INFO (Columns) ---
            // Just like the Header, split 50/50 for "Payment To" and "Payment Details"
            // (or maybe a bit asymmetric if needed, but 50/50 is safe)
            com.lowagie.text.pdf.PdfPTable infoTbl = new com.lowagie.text.pdf.PdfPTable(2);
            infoTbl.setWidthPercentage(100);
            infoTbl.setWidths(new float[] { 1f, 1f }); // Equal width for "Payment To" and "Details"

            // LEFT: PAYMENT TO
            com.lowagie.text.pdf.PdfPCell toCell = new com.lowagie.text.pdf.PdfPCell();
            toCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);

            toCell.addElement(new com.lowagie.text.Paragraph("PAYMENT TO", labelFont));
            String rName = getColumnValue(request, "requester");
            String rDesc = "";
            if (request.getContractor() != null) {
                rName = request.getContractor().getName();
                if (request.getContractor().getDescription() != null)
                    rDesc = request.getContractor().getDescription();
            }
            toCell.addElement(new com.lowagie.text.Paragraph(rName,
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, black)));
            if (!rDesc.isEmpty()) {
                toCell.addElement(new com.lowagie.text.Paragraph(rDesc, valueFont));
            }
            infoTbl.addCell(toCell);

            // RIGHT: PAYMENT DETAILS
            // We can put other details here if "Payment To" doesn't need to take up allow
            // space?
            // Actually, based on User Request: "PAYMENT TO... nexusnetro... rst... zelle
            // (arstsraastr... this should devide into column"
            // It seems they want the Recipient on Left, and Payment Method on Right? Or
            // Recipient & Method split?
            // Let's assume Left = Recipient, Right = Method/Ref.
            com.lowagie.text.pdf.PdfPCell methodCell = new com.lowagie.text.pdf.PdfPCell();
            methodCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            methodCell.addElement(new com.lowagie.text.Paragraph("PAYMENT METHOD", labelFont));

            String method = request.getPaymentMethod() != null ? request.getPaymentMethod().getMethodName() : "N/A";
            String refInfo = request.getPaymentReferenceNumber() != null ? request.getPaymentReferenceNumber() : "";

            methodCell.addElement(new com.lowagie.text.Paragraph(method, valueBoldFont));
            if (!refInfo.isEmpty()) {
                methodCell.addElement(new com.lowagie.text.Paragraph("Ref: " + refInfo, valueFont));
            }
            infoTbl.addCell(methodCell);

            document.add(infoTbl);
            document.add(spacer);

            // --- DETAILS TABLE ---
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[] { 4f, 1f });
            table.setWidthPercentage(100);

            addMinimalHeader(table, "WORK ORDER", labelFont);
            addMinimalHeaderRight(table, "AMOUNT", labelFont);

            // User Request: Show ONLY Work Order Number. No "Work Order:" prefix, No
            // Reason.
            String rowContent = "";
            if (request.getWorkOrderNumber() != null) {
                rowContent = request.getWorkOrderNumber();
            }

            addMinimalRow(table, rowContent, "$" + request.getAmount(), valueFont, valueBoldFont, black);

            document.add(table);

            // --- TOTAL ---
            com.lowagie.text.pdf.PdfPTable totalTbl = new com.lowagie.text.pdf.PdfPTable(2);
            totalTbl.setWidthPercentage(40);
            totalTbl.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            totalTbl.setSpacingBefore(10);

            com.lowagie.text.pdf.PdfPCell tlCell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("TOTAL", labelFont));
            tlCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            tlCell.setPaddingTop(10);
            totalTbl.addCell(tlCell);

            com.lowagie.text.Font bigTotalFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16, black);
            com.lowagie.text.pdf.PdfPCell tvCell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("$" + request.getAmount(), bigTotalFont));
            tvCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            tvCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            tvCell.setPaddingTop(10);
            totalTbl.addCell(tvCell);

            document.add(totalTbl);

            // --- FOOTER (Centered at Bottom) ---
            // Just add abundant space to push it down, or use fixed positioning.
            // For simplicity in this `DataImportExportService` context (flow-based layout),
            // adding spacing is safer than absolute positioning which might overlap dynamic
            // content.
            com.lowagie.text.Paragraph bigSpace = new com.lowagie.text.Paragraph("\n\n\n\n\n");
            document.add(bigSpace);

            com.lowagie.text.pdf.PdfPTable footTbl = new com.lowagie.text.pdf.PdfPTable(1);
            footTbl.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell fCell = new com.lowagie.text.pdf.PdfPCell();
            fCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER); // No line above, just text?
            // "this should be center on buttom" -> Usually means center alignment.
            // User didn't ask for a line on top of footer, just the text.
            fCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);

            fCell.addElement(new com.lowagie.text.Paragraph("Approved & Authorized by Skylink Management", footerFont));
            fCell.addElement(
                    new com.lowagie.text.Paragraph("This is a computer-generated receipt.", footerFont));

            // Center the paragraphs themselves
            for (Object o : fCell.getCompositeElements()) {
                if (o instanceof com.lowagie.text.Paragraph) {
                    ((com.lowagie.text.Paragraph) o).setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                }
            }

            footTbl.addCell(fCell);
            document.add(footTbl);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Invoice PDF", e);
        }
    }

    private void addMetaRow(com.lowagie.text.pdf.PdfPCell parent, String label, String value,
            com.lowagie.text.Font lFont, com.lowagie.text.Font vFont) {
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
        p.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        p.add(new com.lowagie.text.Chunk(label + "  ", lFont));
        p.add(new com.lowagie.text.Chunk(value, vFont));
        parent.addElement(p);
    }

    private void addMinimalHeader(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        cell.setBorderColorBottom(java.awt.Color.BLACK);
        cell.setBorderWidthBottom(1.5f); // Thicker header line
        cell.setPaddingBottom(5);
        table.addCell(cell);
    }

    private void addMinimalHeaderRight(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        cell.setBorderColorBottom(java.awt.Color.BLACK);
        cell.setBorderWidthBottom(1.5f);
        cell.setPaddingBottom(5);
        table.addCell(cell);
    }

    private void addMinimalRow(com.lowagie.text.pdf.PdfPTable table, String desc, String price,
            com.lowagie.text.Font descFont, com.lowagie.text.Font priceFont, java.awt.Color lineColor) {
        com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(desc, descFont));
        c1.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        c1.setBorderColorBottom(lineColor);
        c1.setPaddingTop(10);
        c1.setPaddingBottom(10);
        table.addCell(c1);

        com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(price, priceFont));
        c2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        c2.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        c2.setBorderColorBottom(lineColor);
        c2.setPaddingTop(10);
        c2.setPaddingBottom(10);
        table.addCell(c2);
    }
}
