package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import root.cyb.mh.attendancesystem.model.PaymentRequest;
import root.cyb.mh.attendancesystem.model.User;
import root.cyb.mh.attendancesystem.repository.PaymentRequestRepository;
import root.cyb.mh.attendancesystem.service.PaymentRequestService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PaymentRequestServiceTest {

    @Autowired
    private PaymentRequestService paymentRequestService;

    @MockitoBean
    private PaymentRequestRepository paymentRequestRepository;

    @Test
    public void testCreateRequest() {
        PaymentRequest request = new PaymentRequest();
        User user = new User();
        user.setId(1L);

        when(paymentRequestRepository.save(any(PaymentRequest.class))).thenReturn(request);

        PaymentRequest created = paymentRequestService.createRequest(request, user);
        assertNotNull(created);
    }
}
