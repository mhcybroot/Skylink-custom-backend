package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;

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

    @GetMapping("/daily")
    public String dailyHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        if (date == null) {
            date = LocalDate.now();
        }
        List<PaymentRequest> requests = paymentRequestRepository.findByRequestDateOrderByLastModifiedDesc(date);
        calculateSummary(model, requests);

        model.addAttribute("selectedDate", date);
        model.addAttribute("pageTitle", "Daily Payment History");
        return "admin/history/daily";
    }

    @GetMapping("/weekly")
    public String weeklyHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            Model model) {
        if (startDate == null) {
            // Default to start of current week (Monday)
            startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        }
        LocalDate endDate = startDate.plusDays(6);

        List<PaymentRequest> requests = paymentRequestRepository
                .findByRequestDateBetweenOrderByRequestDateDesc(startDate, endDate);
        calculateSummary(model, requests);

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("pageTitle", "Weekly Payment History");
        return "admin/history/weekly";
    }

    @GetMapping("/monthly")
    public String monthlyHistory(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {
        LocalDate now = LocalDate.now();
        if (year == null)
            year = now.getYear();
        if (month == null)
            month = now.getMonthValue();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        List<PaymentRequest> requests = paymentRequestRepository
                .findByRequestDateBetweenOrderByRequestDateDesc(startDate, endDate);
        calculateSummary(model, requests);

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
}
