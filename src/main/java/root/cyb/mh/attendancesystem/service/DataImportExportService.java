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
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 50,
                    50);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Colors & Fonts
            java.awt.Color primaryColor = new java.awt.Color(44, 62, 80); // Dark Blue/Grey
            java.awt.Color accentColor = new java.awt.Color(236, 240, 241); // Light Grey
            java.awt.Color textColor = java.awt.Color.BLACK;

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 24, primaryColor);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, textColor);
            com.lowagie.text.Font tableHeaderFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 10, textColor);
            com.lowagie.text.Font subFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA,
                    9, java.awt.Color.GRAY);

            // --- HEADER SECTION (Company Info & Invoice Meta) ---
            com.lowagie.text.pdf.PdfPTable headerTable = new com.lowagie.text.pdf.PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[] { 1f, 1f });

            // 1. Left: Company Info
            com.lowagie.text.pdf.PdfPCell companyCell = new com.lowagie.text.pdf.PdfPCell();
            companyCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);

            String companyName = "Skylink Construction";
            String companyAddress = "123 Business Road\nDhaka, Bangladesh";
            String companyPhone = null;
            String companyEmail = null;

            if (request.getCompany() != null) {
                companyName = request.getCompany().getName();
                if (request.getCompany().getAddress() != null && !request.getCompany().getAddress().isEmpty()) {
                    companyAddress = request.getCompany().getAddress();
                }
                companyPhone = request.getCompany().getPhone();
                companyEmail = request.getCompany().getEmail();
            }

            companyCell.addElement(new com.lowagie.text.Paragraph(companyName, com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16, primaryColor)));
            for (String line : companyAddress.split("\n")) {
                companyCell.addElement(new com.lowagie.text.Paragraph(line.trim(), subFont));
            }
            if (companyPhone != null && !companyPhone.isEmpty())
                companyCell.addElement(new com.lowagie.text.Paragraph("Phone: " + companyPhone, subFont));
            if (companyEmail != null && !companyEmail.isEmpty())
                companyCell.addElement(new com.lowagie.text.Paragraph("Email: " + companyEmail, subFont));

            headerTable.addCell(companyCell);

            // 2. Right: Invoice Title & Meta
            com.lowagie.text.pdf.PdfPCell metaCell = new com.lowagie.text.pdf.PdfPCell();
            metaCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            metaCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);

            com.lowagie.text.Paragraph invoiceTitle = new com.lowagie.text.Paragraph("INVOICE",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 22,
                            new java.awt.Color(189, 195, 199)));
            invoiceTitle.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            metaCell.addElement(invoiceTitle);

            metaCell.addElement(createMetaRow("Receipt #:", "REC-" + request.getId(), headerFont, bodyFont));
            metaCell.addElement(createMetaRow("Date:", request.getRequestDate().toString(), headerFont, bodyFont));

            String status = request.getPaymentStatus() != null ? request.getPaymentStatus().toString() : "PENDING";
            com.lowagie.text.Paragraph statusPara = new com.lowagie.text.Paragraph(status,
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10,
                            status.equals("PAID") ? new java.awt.Color(39, 174, 96) : java.awt.Color.RED));
            statusPara.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            statusPara.setSpacingBefore(5);
            metaCell.addElement(statusPara);

            headerTable.addCell(metaCell);
            document.add(headerTable);

            // Separator
            com.lowagie.text.pdf.draw.LineSeparator line = new com.lowagie.text.pdf.draw.LineSeparator();
            line.setLineColor(java.awt.Color.LIGHT_GRAY);
            document.add(new com.lowagie.text.Paragraph(" "));

            // --- BILL TO / INFO SECTION ---
            com.lowagie.text.pdf.PdfPTable infoTable = new com.lowagie.text.pdf.PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);

            // Bill To
            com.lowagie.text.pdf.PdfPCell billToCell = new com.lowagie.text.pdf.PdfPCell();
            billToCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            billToCell.addElement(new com.lowagie.text.Paragraph("BILL TO / REQUESTER:", headerFont));
            billToCell.addElement(new com.lowagie.text.Paragraph(getColumnValue(request, "requester"), bodyFont));
            billToCell.addElement(new com.lowagie.text.Paragraph(request.getWorkOrderNumber(), subFont));
            infoTable.addCell(billToCell);

            // Payment Details
            com.lowagie.text.pdf.PdfPCell paymentInfoCell = new com.lowagie.text.pdf.PdfPCell();
            paymentInfoCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            paymentInfoCell.addElement(new com.lowagie.text.Paragraph("PAYMENT DETAILS:", headerFont));
            paymentInfoCell.addElement(new com.lowagie.text.Paragraph(
                    "Method: "
                            + (request.getPaymentMethod() != null ? request.getPaymentMethod().getMethodName() : "N/A"),
                    bodyFont));
            if (request.getPaymentReferenceNumber() != null) {
                paymentInfoCell.addElement(
                        new com.lowagie.text.Paragraph("Ref #: " + request.getPaymentReferenceNumber(), bodyFont));
            }
            infoTable.addCell(paymentInfoCell);

            document.add(infoTable);
            document.add(new com.lowagie.text.Paragraph(" "));
            document.add(new com.lowagie.text.Paragraph(" "));

            // --- DETAILS TABLE ---
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[] { 3f, 1f });
            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            // Header
            addTableHeader(table, "DESCRIPTION / REASON", tableHeaderFont, primaryColor);
            addTableHeader(table, "AMOUNT", tableHeaderFont, primaryColor);

            // Row 1: Main Reason
            addTableCell(table, request.getReason(), bodyFont, false);
            addTableCell(table, "$" + request.getAmount(), bodyFont, true);

            // Row 2: Contractor Info (as extra detail)
            if (request.getContractor() != null) {
                addTableCell(table, "Contractor: " + request.getContractor().getName(), subFont, false);
                addTableCell(table, "", subFont, true);
            }

            document.add(table);

            // --- TOTAL SECTION ---
            com.lowagie.text.pdf.PdfPTable totalTable = new com.lowagie.text.pdf.PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            totalTable.setSpacingBefore(5);

            // Spacer
            // totalTable.addCell(createNoBorderCell(" ", bodyFont));

            com.lowagie.text.pdf.PdfPCell totalLabel = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("Total Amount:", headerFont));
            totalLabel.setBorder(com.lowagie.text.Rectangle.TOP);
            totalLabel.setPadding(8);
            totalTable.addCell(totalLabel);

            com.lowagie.text.pdf.PdfPCell totalValue = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("$" + request.getAmount(), headerFont));
            totalValue.setBorder(com.lowagie.text.Rectangle.TOP);
            totalValue.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            totalValue.setPadding(8);
            totalTable.addCell(totalValue);

            document.add(totalTable);

            // --- FOOTER ---
            // Move to bottom
            // Simple text footer for now as positioning in simple OpenPDF is sequential
            com.lowagie.text.Paragraph footerSpace = new com.lowagie.text.Paragraph("\n\n\n\n");
            document.add(footerSpace);

            com.lowagie.text.pdf.PdfPTable footerTable = new com.lowagie.text.pdf.PdfPTable(2);
            footerTable.setWidthPercentage(100);

            com.lowagie.text.pdf.PdfPCell authCell = new com.lowagie.text.pdf.PdfPCell();
            authCell.setBorder(com.lowagie.text.Rectangle.TOP);
            authCell.setBorderWidthTop(1f);
            authCell.setBorderColorTop(java.awt.Color.BLACK);
            authCell.setPaddingTop(5);
            authCell.addElement(new com.lowagie.text.Paragraph("Authorized Signature", subFont));
            // Just a line
            com.lowagie.text.pdf.PdfPCell dummyCell = new com.lowagie.text.pdf.PdfPCell();
            dummyCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);

            // We want authorization line on the right maybe? Or left. Let's do Left.
            // Actually usually signature is right.
            footerTable.addCell(dummyCell);
            footerTable.addCell(authCell);

            document.add(footerTable);

            com.lowagie.text.Paragraph closing = new com.lowagie.text.Paragraph("Thank you for your business.",
                    subFont);
            closing.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            closing.setSpacingBefore(20);
            document.add(closing);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Invoice PDF", e);
        }
    }

    private com.lowagie.text.Paragraph createMetaRow(String label, String value, com.lowagie.text.Font labelFont,
            com.lowagie.text.Font valueFont) {
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
        p.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        p.add(new com.lowagie.text.Chunk(label + " ", labelFont));
        p.add(new com.lowagie.text.Chunk(value, valueFont));
        return p;
    }

    private void addTableHeader(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font,
            java.awt.Color bgColor) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addTableCell(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font,
            boolean alignRight) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(java.awt.Color.LIGHT_GRAY);
        if (alignRight)
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
