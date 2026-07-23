package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.model.EmployeeBrowseHistory;
import root.cyb.mh.attendancesystem.repository.EmployeeBrowseHistoryRepository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import root.cyb.mh.attendancesystem.repository.BrowseHistorySpecifications;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class BrowseHistoryController {

    @Autowired
    private EmployeeBrowseHistoryRepository historyRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.EmployeeRepository employeeRepository;

    @GetMapping("/admin/browse-history")
    public String getBrowseHistory(
            @RequestParam(required = false) String employeeSearch,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String sort,
            org.springframework.ui.Model model) {

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        Specification<EmployeeBrowseHistory> spec = Specification.where(BrowseHistorySpecifications.fetchEmployee())
                .and(BrowseHistorySpecifications.employeeSearch(employeeSearch))
                .and(BrowseHistorySpecifications.urlContains(url))
                .and(BrowseHistorySpecifications.domainContains(domain))
                .and(BrowseHistorySpecifications.titleContains(title))
                .and(BrowseHistorySpecifications.dateBetween(startDateTime, endDateTime));

        Sort sortObj = sort.equalsIgnoreCase("asc") ? Sort.by("timestamp").ascending() : Sort.by("timestamp").descending();
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<EmployeeBrowseHistory> historyPage = historyRepository.findAll(spec, pageable);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("activeLink", "browse-history");
        model.addAttribute("employees", employeeRepository.findAll());
        
        // Return filter values to the view
        model.addAttribute("employeeSearch", employeeSearch);
        model.addAttribute("urlSearch", url);
        model.addAttribute("domainSearch", domain);
        model.addAttribute("titleSearch", title);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("sortDirection", sort);
        model.addAttribute("size", size);
        
        return "admin-browse-history";
    }

    @GetMapping("/admin/api/browse-history/employee/{id}/dates")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<String> getEmployeeHistoryDates(@org.springframework.web.bind.annotation.PathVariable String id) {
        return historyRepository.findDistinctDatesByEmployeeId(id).stream()
                .map(java.sql.Date::toString)
                .toList();
    }

    @GetMapping("/admin/api/browse-history/domains")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, String>> searchDomains(@RequestParam(value = "q", required = false, defaultValue = "") String query) {
        java.util.List<String> domains = historyRepository.findDistinctDomainsContaining(query);
        return domains.stream().map(domain -> java.util.Map.of("id", domain, "text", domain)).toList();
    }

    @GetMapping("/admin/api/browse-history/urls")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, String>> searchUrls(@RequestParam(value = "q", required = false, defaultValue = "") String query) {
        java.util.List<String> urls = historyRepository.findDistinctUrlsContaining(query, PageRequest.of(0, 10));
        return urls.stream().map(url -> java.util.Map.of("id", url, "text", url)).toList();
    }

    @GetMapping("/admin/api/browse-history/titles")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, String>> searchTitles(@RequestParam(value = "q", required = false, defaultValue = "") String query) {
        java.util.List<String> titles = historyRepository.findDistinctTitlesContaining(query, PageRequest.of(0, 10));
        return titles.stream().map(title -> java.util.Map.of("id", title, "text", title)).toList();
    }
}
