package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.repository.*;
import root.cyb.mh.attendancesystem.specification.PaymentRequestSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Controller
@RequestMapping("/admin/history")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class PaymentHistoryController {

        @Autowired
        private PaymentRequestRepository paymentRequestRepository;
        @Autowired
        private ContractorRepository contractorRepository;
        @Autowired
        private ClientRepository clientRepository;
        @Autowired
        private PaymentMethodRepository paymentMethodRepository;
        @Autowired
        private root.cyb.mh.attendancesystem.service.DataImportExportService dataImportExportService;

        @RequestMapping("/export")
        public void exportHistory(
                        @RequestParam(defaultValue = "pdf") String format,
                        @RequestParam(required = false) List<String> columns,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) Integer month,
                        // Filters
                        @RequestParam(required = false) Long contractorId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long paymentMethodId,
                        @RequestParam(required = false) String workOrderNumber,
                        @RequestParam(required = false) String requesterName,
                        @RequestParam(required = false) PaymentPriority priority,
                        @RequestParam(required = false) RequestStatus status,
                        @RequestParam(required = false) PaymentStatus paymentStatus,
                        @RequestParam(required = false) PPWStatus ppwUpdateStatus,
                        jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

                // Determine Date Range based on parameters
                LocalDate rangeStart = null;
                LocalDate rangeEnd = null;
                String title = "Payment History";

                if (year != null && month != null) {
                        // Monthly
                        LocalDate start = LocalDate.of(year, month, 1);
                        rangeStart = start;
                        rangeEnd = start.with(TemporalAdjusters.lastDayOfMonth());
                        title = "Monthly Report - " + start.getMonth().name() + " " + year;
                } else if (startDate != null) {
                        // Weekly
                        rangeStart = startDate;
                        rangeEnd = startDate.plusDays(6);
                        title = "Weekly Report - " + startDate + " to " + rangeEnd;
                } else {
                        // Daily (Default)
                        LocalDate d = date != null ? date : LocalDate.now();
                        rangeStart = d;
                        rangeEnd = d;
                        title = "Daily Report - " + d;
                }

                Specification<PaymentRequest> spec = PaymentRequestSpecification.getFilterSpec(
                                rangeStart, rangeEnd,
                                contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName,
                                priority, status, paymentStatus, ppwUpdateStatus);

                List<PaymentRequest> requests = paymentRequestRepository.findAll(spec,
                                Sort.by(Sort.Direction.DESC, "requestDate", "lastModified"));

                if ("csv".equalsIgnoreCase(format)) {
                        response.setContentType("text/csv");
                        response.setHeader("Content-Disposition", "attachment; filename=\"payment_history.csv\"");
                        dataImportExportService.exportPaymentRequestsToCsv(response.getWriter(), requests, columns);
                } else {
                        response.setContentType("application/pdf");
                        response.setHeader("Content-Disposition", "attachment; filename=\"payment_history.pdf\"");
                        dataImportExportService.exportPaymentRequestsToPdf(response.getOutputStream(), requests, title,
                                        columns);
                }
        }

        @GetMapping("/daily")
        public String dailyHistory(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        // Filters
                        @RequestParam(required = false) Long contractorId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long paymentMethodId,
                        @RequestParam(required = false) String workOrderNumber,
                        @RequestParam(required = false) String requesterName,
                        @RequestParam(required = false) PaymentPriority priority,
                        @RequestParam(required = false) RequestStatus status,
                        @RequestParam(required = false) PaymentStatus paymentStatus,
                        @RequestParam(required = false) PPWStatus ppwUpdateStatus,
                        Model model) {

                if (date == null)
                        date = LocalDate.now();

                Specification<PaymentRequest> spec = PaymentRequestSpecification.getFilterSpec(
                                date, date,
                                contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName,
                                priority, status, paymentStatus, ppwUpdateStatus);

                List<PaymentRequest> requests = paymentRequestRepository.findAll(spec,
                                Sort.by(Sort.Direction.DESC, "requestDate", "lastModified"));

                calculateSummary(model, requests);
                populateModelAttributes(model, date, null, null, null, null, contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName, priority, status, paymentStatus, ppwUpdateStatus);

                model.addAttribute("selectedDate", date);
                model.addAttribute("pageTitle", "Daily Payment History");
                return "admin/history/daily";
        }

        @GetMapping("/weekly")
        public String weeklyHistory(
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        // Filters
                        @RequestParam(required = false) Long contractorId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long paymentMethodId,
                        @RequestParam(required = false) String workOrderNumber,
                        @RequestParam(required = false) String requesterName,
                        @RequestParam(required = false) PaymentPriority priority,
                        @RequestParam(required = false) RequestStatus status,
                        @RequestParam(required = false) PaymentStatus paymentStatus,
                        @RequestParam(required = false) PPWStatus ppwUpdateStatus,
                        Model model) {

                if (startDate == null) {
                        startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                }
                LocalDate endDate = startDate.plusDays(6);

                Specification<PaymentRequest> spec = PaymentRequestSpecification.getFilterSpec(
                                startDate, endDate,
                                contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName,
                                priority, status, paymentStatus, ppwUpdateStatus);

                List<PaymentRequest> requests = paymentRequestRepository.findAll(spec,
                                Sort.by(Sort.Direction.DESC, "requestDate", "lastModified"));

                calculateSummary(model, requests);
                populateModelAttributes(model, null, startDate, endDate, null, null, contractorId, clientId,
                                paymentMethodId,
                                workOrderNumber, requesterName, priority, status, paymentStatus, ppwUpdateStatus);

                model.addAttribute("startDate", startDate);
                model.addAttribute("endDate", endDate);
                model.addAttribute("pageTitle", "Weekly Payment History");
                return "admin/history/weekly";
        }

        @GetMapping("/monthly")
        public String monthlyHistory(
                        @RequestParam(required = false) Integer year,
                        @RequestParam(required = false) Integer month,
                        // Filters
                        @RequestParam(required = false) Long contractorId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) Long paymentMethodId,
                        @RequestParam(required = false) String workOrderNumber,
                        @RequestParam(required = false) String requesterName,
                        @RequestParam(required = false) PaymentPriority priority,
                        @RequestParam(required = false) RequestStatus status,
                        @RequestParam(required = false) PaymentStatus paymentStatus,
                        @RequestParam(required = false) PPWStatus ppwUpdateStatus,
                        Model model) {

                LocalDate now = LocalDate.now();
                if (year == null)
                        year = now.getYear();
                if (month == null)
                        month = now.getMonthValue();

                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

                Specification<PaymentRequest> spec = PaymentRequestSpecification.getFilterSpec(
                                startDate, endDate,
                                contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName,
                                priority, status, paymentStatus, ppwUpdateStatus);

                List<PaymentRequest> requests = paymentRequestRepository.findAll(spec,
                                Sort.by(Sort.Direction.DESC, "requestDate", "lastModified"));

                calculateSummary(model, requests);
                populateModelAttributes(model, null, null, null, year, month, contractorId, clientId, paymentMethodId,
                                workOrderNumber, requesterName, priority, status, paymentStatus, ppwUpdateStatus);

                model.addAttribute("selectedYear", year);
                model.addAttribute("selectedMonth", month);
                model.addAttribute("monthName", startDate.getMonth().name());
                model.addAttribute("pageTitle", "Monthly Payment History");
                return "admin/history/monthly";
        }

        private void calculateSummary(Model model, List<PaymentRequest> requests) {
                long totalCount = requests.size();
                BigDecimal totalAmount = requests.stream()
                                .map(PaymentRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                model.addAttribute("requests", requests);
                model.addAttribute("totalCount", totalCount);
                model.addAttribute("totalAmount", totalAmount);
        }

        private void populateModelAttributes(Model model,
                        LocalDate date, LocalDate wStart, LocalDate wEnd,
                        Integer year, Integer month,
                        Long contractorId, Long clientId, Long paymentMethodId,
                        String workOrderNumber, String requesterName,
                        PaymentPriority priority, RequestStatus status,
                        PaymentStatus paymentStatus, PPWStatus ppwUpdateStatus) {

                // Master Data for Dropdowns
                model.addAttribute("activeContractors", contractorRepository.findByActiveTrue());
                model.addAttribute("activeClients", clientRepository.findByActiveTrue());
                model.addAttribute("activePaymentMethods", paymentMethodRepository.findByActiveTrue());
                model.addAttribute("priorities", PaymentPriority.values());
                model.addAttribute("requestStatuses", RequestStatus.values());
                model.addAttribute("paymentStatuses", PaymentStatus.values());
                model.addAttribute("ppwStatuses", PPWStatus.values());

                // Selected Values
                model.addAttribute("contractorId", contractorId);
                model.addAttribute("clientId", clientId);
                model.addAttribute("paymentMethodId", paymentMethodId);
                model.addAttribute("workOrderNumber", workOrderNumber);
                model.addAttribute("requesterName", requesterName);
                model.addAttribute("priority", priority);
                model.addAttribute("status", status);
                model.addAttribute("paymentStatus", paymentStatus);
                model.addAttribute("ppwUpdateStatus", ppwUpdateStatus);
        }
}
