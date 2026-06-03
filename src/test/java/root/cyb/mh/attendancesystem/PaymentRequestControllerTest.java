package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testListRequests() throws Exception {
        mockMvc.perform(get("/payment-requests"))
                .andExpect(status().isOk());
    }

    @Autowired
    private root.cyb.mh.attendancesystem.repository.PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "employee", roles = { "EMPLOYEE" })
    public void testNewRequestForm() throws Exception {
        mockMvc.perform(get("/payment-requests/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "employee", roles = { "EMPLOYEE" })
    public void testCheckWorkOrderNumber() throws Exception {
        mockMvc.perform(get("/payment-requests/api/check-work-order").param("workOrderNumber", "WO-TEST-999"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.exists").value(false));

        root.cyb.mh.attendancesystem.model.PaymentRequest request = new root.cyb.mh.attendancesystem.model.PaymentRequest();
        request.setWorkOrderNumber("WO-TEST-999");
        request.setAmount(new java.math.BigDecimal("100.00"));
        request.setRequestDate(java.time.LocalDate.now());
        request.setPriority(root.cyb.mh.attendancesystem.model.enums.PaymentPriority.REGULAR);
        paymentRequestRepository.save(request);

        try {
            mockMvc.perform(get("/payment-requests/api/check-work-order").param("workOrderNumber", "wo-test-999"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.exists").value(true));
        } finally {
            paymentRequestRepository.delete(request);
        }
    }

    @Test
    @WithMockUser(username = "employee", roles = { "EMPLOYEE" })
    public void testSubmitDuplicateWorkOrderNumber() throws Exception {
        root.cyb.mh.attendancesystem.model.PaymentRequest request = new root.cyb.mh.attendancesystem.model.PaymentRequest();
        request.setWorkOrderNumber("WO-DUPE-123");
        request.setAmount(new java.math.BigDecimal("100.00"));
        request.setRequestDate(java.time.LocalDate.now());
        request.setPriority(root.cyb.mh.attendancesystem.model.enums.PaymentPriority.REGULAR);
        paymentRequestRepository.save(request);

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/payment-requests")
                            .param("workOrderNumber", "WO-DUPE-123")
                            .param("amount", "200.00")
                            .param("priority", "REGULAR")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists("errorMessage"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("errorMessage", org.hamcrest.Matchers.containsString("already exists")));
        } finally {
            paymentRequestRepository.delete(request);
        }
    }

    @Test
    @WithMockUser(username = "employee", roles = { "EMPLOYEE" })
    public void testSubmitDuplicateWorkOrderNumberWithExplanation() throws Exception {
        root.cyb.mh.attendancesystem.model.PaymentRequest request = new root.cyb.mh.attendancesystem.model.PaymentRequest();
        request.setWorkOrderNumber("WO-DUPE-456");
        request.setAmount(new java.math.BigDecimal("100.00"));
        request.setRequestDate(java.time.LocalDate.now());
        request.setPriority(root.cyb.mh.attendancesystem.model.enums.PaymentPriority.REGULAR);
        paymentRequestRepository.save(request);

        root.cyb.mh.attendancesystem.model.User dummyUser = new root.cyb.mh.attendancesystem.model.User();
        dummyUser.setUsername("employee");
        dummyUser.setPassword("password");
        dummyUser.setRole("EMPLOYEE");
        dummyUser = userRepository.save(dummyUser);

        root.cyb.mh.attendancesystem.model.PaymentRequest createdRequest = null;

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/payment-requests")
                            .param("workOrderNumber", "WO-DUPE-456")
                            .param("amount", "200.00")
                            .param("priority", "REGULAR")
                            .param("isPartialPayment", "true")
                            .param("duplicateReason", "Installment 2")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().is3xxRedirection());

            java.util.List<root.cyb.mh.attendancesystem.model.PaymentRequest> requests = paymentRequestRepository.findAll();
            for (root.cyb.mh.attendancesystem.model.PaymentRequest r : requests) {
                if ("WO-DUPE-456".equals(r.getWorkOrderNumber()) && !r.getId().equals(request.getId())) {
                    createdRequest = r;
                    break;
                }
            }

            org.junit.jupiter.api.Assertions.assertNotNull(createdRequest);
            org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, createdRequest.getIsPartialPayment());
            org.junit.jupiter.api.Assertions.assertEquals("Installment 2", createdRequest.getDuplicateReason());

        } finally {
            paymentRequestRepository.delete(request);
            if (createdRequest != null) {
                paymentRequestRepository.delete(createdRequest);
            }
            userRepository.delete(dummyUser);
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void testBulkUpdate() throws Exception {
        root.cyb.mh.attendancesystem.model.PaymentRequest r1 = new root.cyb.mh.attendancesystem.model.PaymentRequest();
        r1.setWorkOrderNumber("WO-BULK-1");
        r1.setAmount(new java.math.BigDecimal("100.00"));
        r1.setRequestDate(java.time.LocalDate.now());
        r1.setPriority(root.cyb.mh.attendancesystem.model.enums.PaymentPriority.REGULAR);
        r1 = paymentRequestRepository.save(r1);

        root.cyb.mh.attendancesystem.model.PaymentRequest r2 = new root.cyb.mh.attendancesystem.model.PaymentRequest();
        r2.setWorkOrderNumber("WO-BULK-2");
        r2.setAmount(new java.math.BigDecimal("150.00"));
        r2.setRequestDate(java.time.LocalDate.now());
        r2.setPriority(root.cyb.mh.attendancesystem.model.enums.PaymentPriority.REGULAR);
        r2 = paymentRequestRepository.save(r2);

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "proofFile",
                "bulk_receipt.pdf",
                "application/pdf",
                "fake receipt content".getBytes()
        );

        String r1ProofPath = null;
        String r2ProofPath = null;

        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/payment-requests/bulk-update")
                            .file(mockFile)
                            .param("ids", r1.getId().toString())
                            .param("ids", r2.getId().toString())
                            .param("status", "APPROVED")
                            .param("paymentStatus", "PAID")
                            .param("ppwUpdateStatus", "UPDATED")
                            .param("remarks", "Updated in bulk")
                            .param("paymentReferenceNumber", "REF-BULK-12345")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isOk());

            root.cyb.mh.attendancesystem.model.PaymentRequest r1Updated = paymentRequestRepository.findById(r1.getId()).get();
            root.cyb.mh.attendancesystem.model.PaymentRequest r2Updated = paymentRequestRepository.findById(r2.getId()).get();

            r1ProofPath = r1Updated.getPaymentProofPath();
            r2ProofPath = r2Updated.getPaymentProofPath();

            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED, r1Updated.getStatus());
            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID, r1Updated.getPaymentStatus());
            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.PPWStatus.UPDATED, r1Updated.getPpwUpdateStatus());
            org.junit.jupiter.api.Assertions.assertEquals("Updated in bulk", r1Updated.getRemarks());
            org.junit.jupiter.api.Assertions.assertEquals("REF-BULK-12345", r1Updated.getPaymentReferenceNumber());
            org.junit.jupiter.api.Assertions.assertNotNull(r1ProofPath);
            org.junit.jupiter.api.Assertions.assertTrue(r1ProofPath.endsWith("bulk_receipt.pdf"));

            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.RequestStatus.APPROVED, r2Updated.getStatus());
            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.PaymentStatus.PAID, r2Updated.getPaymentStatus());
            org.junit.jupiter.api.Assertions.assertEquals(root.cyb.mh.attendancesystem.model.enums.PPWStatus.UPDATED, r2Updated.getPpwUpdateStatus());
            org.junit.jupiter.api.Assertions.assertEquals("Updated in bulk", r2Updated.getRemarks());
            org.junit.jupiter.api.Assertions.assertEquals("REF-BULK-12345", r2Updated.getPaymentReferenceNumber());
            org.junit.jupiter.api.Assertions.assertNotNull(r2ProofPath);
            org.junit.jupiter.api.Assertions.assertTrue(r2ProofPath.endsWith("bulk_receipt.pdf"));

        } finally {
            paymentRequestRepository.delete(r1);
            paymentRequestRepository.delete(r2);
            if (r1ProofPath != null) {
                try {
                    new java.io.File(r1ProofPath).delete();
                } catch (Exception e) {}
            }
            if (r2ProofPath != null && !r2ProofPath.equals(r1ProofPath)) {
                try {
                    new java.io.File(r2ProofPath).delete();
                } catch (Exception e) {}
            }
        }
    }
}
