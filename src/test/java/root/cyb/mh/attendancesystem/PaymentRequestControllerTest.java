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
}
