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

    public void exportPaymentRequestsToCsv(PrintWriter writer, List<PaymentRequest> requests) throws IOException {
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "ID", "Date", "Work Order", "Requester", "Contractor", "Client", "Amount", "Priority", "Status",
                "Payment Status"));

        for (PaymentRequest p : requests) {
            String requester = p.getRequester() != null ? p.getRequester().getUsername()
                    : (p.getEmployeeRequester() != null ? p.getEmployeeRequester().getName() : "Unknown");

            printer.printRecord(
                    p.getId(),
                    p.getRequestDate(),
                    p.getWorkOrderNumber(),
                    requester,
                    p.getContractor() != null ? p.getContractor().getName() : "",
                    p.getClient() != null ? p.getClient().getCode() : "",
                    p.getAmount(),
                    p.getPriority(),
                    p.getStatus(),
                    p.getPaymentStatus());
        }
        printer.flush();
    }

    public void exportPaymentRequestsToPdf(java.io.OutputStream out, List<PaymentRequest> requests, String title) {
        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Title
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Paragraph titlePara = new com.lowagie.text.Paragraph(title, titleFont);
            titlePara.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            // Table
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 2.5f, 3f, 3f, 3f, 2f, 2f, 2f }); // Relative widths

            // Header
            String[] headers = { "ID", "Date", "Work Order", "Requester", "Contractor", "Amount", "Status",
                    "Pay Status" };
            com.lowagie.text.Font headFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10);

            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(h, headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Rows
            com.lowagie.text.Font bodyFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 9);
            for (PaymentRequest p : requests) {
                String requester = p.getRequester() != null ? p.getRequester().getUsername()
                        : (p.getEmployeeRequester() != null ? p.getEmployeeRequester().getName() : "Unknown");

                addCell(table, String.valueOf(p.getId()), bodyFont);
                addCell(table, p.getRequestDate().toString(), bodyFont);
                addCell(table, p.getWorkOrderNumber(), bodyFont);
                addCell(table, requester, bodyFont);
                addCell(table, p.getContractor() != null ? p.getContractor().getName() : "", bodyFont);
                addCell(table, "$" + p.getAmount(), bodyFont);
                addCell(table, p.getStatus().name(), bodyFont);
                addCell(table, p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "-", bodyFont);
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private void addCell(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(text != null ? text : "", font));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
