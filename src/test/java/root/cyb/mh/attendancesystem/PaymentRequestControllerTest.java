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

    @Test
    @WithMockUser(username = "employee", roles = { "EMPLOYEE" })
    public void testNewRequestForm() throws Exception {
        mockMvc.perform(get("/payment-requests/new"))
                .andExpect(status().isOk());
    }
}
