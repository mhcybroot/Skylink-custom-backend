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

    @Test
    void testWeightedAverageDaysAndRiskScoring() {
        EmployeeWorkOrder wo1 = new EmployeeWorkOrder();
        wo1.setClientInvoicePaid(false);
        wo1.setInvoiceDate(LocalDate.now().minusDays(20)); // 20 days old
        wo1.setClientInvoiceTotal(new BigDecimal("100.00")); // $100
        wo1.setOriginalClientString("Client A");

        EmployeeWorkOrder wo2 = new EmployeeWorkOrder();
        wo2.setClientInvoicePaid(false);
        wo2.setInvoiceDate(LocalDate.now().minusDays(80)); // 80 days old -> Critical
        wo2.setClientInvoiceTotal(new BigDecimal("300.00")); // $300
        wo2.setOriginalClientString("Client A");

        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(List.of(wo1, wo2));

        // Total unpaid = $400. Weighted days = (20*100 + 80*300) / 400 = (2000 + 24000) / 400 = 26000 / 400 = 65.0 days
        assertEquals(65.0, summary.getPortfolioAverageDays(), 0.1);

        AgingSummaryDTO.ClientAgingStat stat = summary.getClientStats().get(0);
        assertEquals(65.0, stat.getWeightedAverageDays(), 0.1);
        assertEquals("HIGH", stat.getRiskScore());
        assertEquals("High Risk", stat.getRiskScoreLabel());
    }

    @Test
    void testFilterOrdersByDueBucket() {
        EmployeeWorkOrder wo1 = new EmployeeWorkOrder();
        wo1.setClientInvoicePaid(false);
        wo1.setInvoiceDate(LocalDate.now().minusDays(20)); // Within Terms (<40)

        EmployeeWorkOrder wo2 = new EmployeeWorkOrder();
        wo2.setClientInvoicePaid(false);
        wo2.setInvoiceDate(LocalDate.now().minusDays(65)); // Critical (60+)

        List<EmployeeWorkOrder> criticalOrders = clientDueAgingService.filterOrdersByDueBucket(List.of(wo1, wo2), "critical");
        assertEquals(1, criticalOrders.size());
        assertEquals(wo2, criticalOrders.get(0));

        List<EmployeeWorkOrder> withinTerms = clientDueAgingService.filterOrdersByDueBucket(List.of(wo1, wo2), "within_terms");
        assertEquals(1, withinTerms.size());
        assertEquals(wo1, withinTerms.get(0));
    }

    @Test
    void testSeriesAgingPortfolioGrouping() {
        EmployeeWorkOrder wo100 = new EmployeeWorkOrder();
        wo100.setClientInvoicePaid(false);
        wo100.setInvoiceDate(LocalDate.now().minusDays(20)); // Within Terms (<40)
        wo100.setClientInvoiceTotal(new BigDecimal("150.00"));
        wo100.setOriginalClientString("105");

        EmployeeWorkOrder wo100b = new EmployeeWorkOrder();
        wo100b.setClientInvoicePaid(false);
        wo100b.setInvoiceDate(LocalDate.now().minusDays(45)); // Standard Due (40-49)
        wo100b.setClientInvoiceTotal(new BigDecimal("250.00"));
        wo100b.setOriginalClientString("120");

        EmployeeWorkOrder wo300 = new EmployeeWorkOrder();
        wo300.setClientInvoicePaid(false);
        wo300.setInvoiceDate(LocalDate.now().minusDays(70)); // Critical (60+)
        wo300.setClientInvoiceTotal(new BigDecimal("500.00"));
        wo300.setOriginalClientString("370");

        AgingSummaryDTO summary = clientDueAgingService.calculateAgingSummary(List.of(wo100, wo100b, wo300));

        assertNotNull(summary.getSeriesStats());
        assertEquals(2, summary.getSeriesStats().size());

        // Series 100
        AgingSummaryDTO.SeriesAgingStat s100 = summary.getSeriesStats().get(0);
        assertEquals("Series 100", s100.getSeriesName());
        assertEquals("100–199", s100.getSeriesRange());
        assertEquals(100, s100.getSeriesBase());
        assertEquals(2, s100.getClientCount());
        assertEquals(2, s100.getTotalUnpaidCount());
        assertEquals(new BigDecimal("400.00"), s100.getTotalUnpaidAmount());
        assertEquals(new BigDecimal("150.00"), s100.getWithinTermsAmount());
        assertEquals(new BigDecimal("250.00"), s100.getStandardDueAmount());

        // Series 300
        AgingSummaryDTO.SeriesAgingStat s300 = summary.getSeriesStats().get(1);
        assertEquals("Series 300", s300.getSeriesName());
        assertEquals("300–399", s300.getSeriesRange());
        assertEquals(300, s300.getSeriesBase());
        assertEquals(1, s300.getClientCount());
        assertEquals(1, s300.getTotalUnpaidCount());
        assertEquals(new BigDecimal("500.00"), s300.getTotalUnpaidAmount());
        assertEquals(new BigDecimal("500.00"), s300.getCriticalDueAmount());
        assertEquals("HIGH", s300.getRiskScore());
    }
}
