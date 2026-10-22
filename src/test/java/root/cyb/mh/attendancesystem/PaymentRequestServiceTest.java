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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PaymentRequestServiceTest {

    @Autowired
    private PaymentRequestService paymentRequestService;

    @MockitoBean
    private PaymentRequestRepository paymentRequestRepository;

    @MockitoBean
    private root.cyb.mh.attendancesystem.repository.PaymentRequestActivityRepository paymentRequestActivityRepository;

    @Test
    public void testCreateRequest() {
        PaymentRequest request = new PaymentRequest();
        User user = new User();
        user.setId(1L);

        when(paymentRequestRepository.save(any(PaymentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest created = paymentRequestService.createRequest(request, user);
        assertNotNull(created);
        assertEquals(root.cyb.mh.attendancesystem.model.enums.PaymentStatus.UNPAID, created.getPaymentStatus());
        assertEquals(root.cyb.mh.attendancesystem.model.enums.PPWStatus.NOT_UPDATED, created.getPpwUpdateStatus());
    }

    @Test
    public void testDisplayClientCodeAndSorting() {
        PaymentRequest req1 = new PaymentRequest();
        req1.setId(1L);
        root.cyb.mh.attendancesystem.model.Client c1 = new root.cyb.mh.attendancesystem.model.Client();
        c1.setCode("C200");
        req1.setClient(c1);

        PaymentRequest req2 = new PaymentRequest();
        req2.setId(2L);
        req2.setClientCode("C100");

        PaymentRequest req3 = new PaymentRequest();
        req3.setId(3L);

        assertEquals("C200", req1.getDisplayClientCode());
        assertEquals("C100", req2.getDisplayClientCode());
        assertEquals("-", req3.getDisplayClientCode());

        java.util.List<PaymentRequest> list = new java.util.ArrayList<>(java.util.List.of(req1, req2, req3));
        paymentRequestService.sortRequests(list, "clientCode", "asc");
        assertEquals(3L, list.get(0).getId());
        assertEquals(2L, list.get(1).getId());
        assertEquals(1L, list.get(2).getId());
    }
}
