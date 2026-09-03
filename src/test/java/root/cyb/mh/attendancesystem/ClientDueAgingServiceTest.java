package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import root.cyb.mh.attendancesystem.dto.AgingSummaryDTO;
import root.cyb.mh.attendancesystem.model.Client;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.ClientDueConfigRepository;
import root.cyb.mh.attendancesystem.service.ClientDueAgingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientDueAgingServiceTest {

    @Mock
    private ClientDueConfigRepository clientDueConfigRepository;

    @InjectMocks
    private ClientDueAgingService clientDueAgingService;

    private ClientDueConfig defaultConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        defaultConfig = new ClientDueConfig();
        defaultConfig.setClientIdentifier("DEFAULT");
        defaultConfig.setNormalDueDays(40);
        defaultConfig.setOverdueDays(50);
        defaultConfig.setCriticalDueDays(60);

        when(clientDueConfigRepository.findByClientIdentifierIgnoreCase("DEFAULT")).thenReturn(Optional.of(defaultConfig));
        when(clientDueConfigRepository.findAll()).thenReturn(List.of(defaultConfig));
    }

    @Test
    void testAgingBucketStandardDueWithDefaultThresholds() {
        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(45)); // 45 days -> Standard Due (40-49)

        Map<String, ClientDueConfig> configMap = Map.of();
        String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);

        assertEquals(ClientDueAgingService.BUCKET_STANDARD, bucket);
    }

    @Test
    void testAgingBucketPastDueWithDefaultThresholds() {
        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(55)); // 55 days -> Past Due (50-59)

        Map<String, ClientDueConfig> configMap = Map.of();
        String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);

        assertEquals(ClientDueAgingService.BUCKET_OVERDUE, bucket);
    }

    @Test
    void testAgingBucketCriticalWithDefaultThresholds() {
        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(65)); // 65 days -> Critical (60+)

        Map<String, ClientDueConfig> configMap = Map.of();
        String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);

        assertEquals(ClientDueAgingService.BUCKET_CRITICAL, bucket);
    }

    @Test
    void testAgingBucketWithinTermsWithDefaultThresholds() {
        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(20)); // 20 days -> Within terms (<40)

        Map<String, ClientDueConfig> configMap = Map.of();
        String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);

        assertEquals(ClientDueAgingService.BUCKET_WITHIN_TERMS, bucket);
    }

    @Test
    void testClientOverrideThresholdsTakePrecedence() {
        Client client = new Client();
        client.setCode("C_CUSTOM");
        client.setName("Custom Client");

        ClientDueConfig customConfig = new ClientDueConfig();
        customConfig.setClientIdentifier("C_CUSTOM");
        customConfig.setNormalDueDays(25);
        customConfig.setOverdueDays(35);
        customConfig.setCriticalDueDays(45);

        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClient(client);
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(36)); // 36 days: under default (40) it would be within terms, but under custom (35-44) it is OVERDUE!

        Map<String, ClientDueConfig> configMap = Map.of("c_custom", customConfig);
        String bucket = clientDueAgingService.getAgingBucket(wo, configMap, defaultConfig);

        assertEquals(ClientDueAgingService.BUCKET_OVERDUE, bucket);
    }

    @Test
    void testCalculateAgingSummary() {
        EmployeeWorkOrder wo1 = new EmployeeWorkOrder();
        wo1.setClientInvoicePaid(false);
        wo1.setInvoiceDate(LocalDate.now().minusDays(45));
        wo1.setClientInvoiceTotal(new BigDecimal("100.00"));

        EmployeeWorkOrder wo2 = new EmployeeWorkOrder();
        wo2.setClientInvoicePaid(false);
        wo2.setInvoiceDate(LocalDate.now().minusDays(65));
        wo2.setClientInvoiceTotal(new BigDecimal("200.00"));

        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(List.of(wo1, wo2));

        assertEquals(2, summary.getTotalUnpaidCount());
        assertEquals(new BigDecimal("300.00"), summary.getTotalUnpaidAmount());
        assertEquals(1, summary.getStandardDueCount());
        assertEquals(new BigDecimal("100.00"), summary.getStandardDueAmount());
        assertEquals(1, summary.getCriticalDueCount());
        assertEquals(new BigDecimal("200.00"), summary.getCriticalDueAmount());
    }

    @Test
    void testCalculateAgingSummaryAlwaysUsesClientDiscountTotal() {
        EmployeeWorkOrder wo = new EmployeeWorkOrder();
        wo.setClientInvoicePaid(false);
        wo.setInvoiceDate(LocalDate.now().minusDays(45));
        wo.setClientInvoiceTotal(new BigDecimal("100.00")); // Gross invoice total
        wo.setClientDiscountTotal(new BigDecimal("80.00")); // Net discounted total (20% discount)

        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(List.of(wo));

        assertEquals(1, summary.getTotalUnpaidCount());
        assertEquals(new BigDecimal("80.00"), summary.getTotalUnpaidAmount(), "Should calculate based on clientDiscountTotal not gross invoice total");
        assertEquals(new BigDecimal("80.00"), summary.getStandardDueAmount());
    }

    @Test
    void testInvalidThresholdsValidationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            clientDueAgingService.saveOrUpdateConfig("DEFAULT", "Default", 50, 40, 60, "admin");
        });
    }
}
