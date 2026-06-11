package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ppw-mapping")
@CrossOrigin(origins = "*") // Allow the Chrome extension content script to call this
public class PPWApiController {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @PostMapping
    public ResponseEntity<?> updatePPWReportId(@RequestBody PPWMappingRequest request) {
        System.out.println("====== PPW SYNC REQUEST RECEIVED ======");
        System.out.println("Work Order: " + request.workOrderNumber);
        System.out.println("Report ID: " + request.reportId);
        System.out.println("=======================================");

        if (request.workOrderNumber == null || request.workOrderNumber.trim().isEmpty() ||
            request.reportId == null || request.reportId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "workOrderNumber and reportId are required"));
        }

        List<PaymentRequest> requests = paymentRequestRepository.findByWorkOrderNumber(request.workOrderNumber);
        
        int updated = 0;
        for (PaymentRequest pr : requests) {
            // Update even if already set, in case it changes or was incorrect
            pr.setPpwReportId(request.reportId);
            paymentRequestRepository.save(pr);
            updated++;
        }

        return ResponseEntity.ok(Map.of(
            "success", true, 
            "updatedCount", updated, 
            "workOrderNumber", request.workOrderNumber, 
            "reportId", request.reportId
        ));
    }

    public static class PPWMappingRequest {
        public String workOrderNumber;
        public String reportId;
    }
}
