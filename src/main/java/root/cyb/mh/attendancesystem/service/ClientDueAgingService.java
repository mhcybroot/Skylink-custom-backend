package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.AgingSummaryDTO;
import root.cyb.mh.attendancesystem.model.BaseWorkOrder;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.ClientDueConfigRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientDueAgingService {

    public static final String BUCKET_CRITICAL = "critical";
    public static final String BUCKET_OVERDUE = "overdue";
    public static final String BUCKET_STANDARD = "standard";
    public static final String BUCKET_WITHIN_TERMS = "within_terms";
    public static final String BUCKET_ALL_UNPAID = "all_unpaid";

    @Autowired
    private ClientDueConfigRepository clientDueConfigRepository;

    @Transactional
    public ClientDueConfig getDefaultConfig() {
        return clientDueConfigRepository.findByClientIdentifierIgnoreCase("DEFAULT")
                .orElseGet(() -> {
                    ClientDueConfig cfg = new ClientDueConfig();
                    cfg.setClientIdentifier("DEFAULT");
                    cfg.setClientName("Global Default Configuration");
                    cfg.setNormalDueDays(40);
                    cfg.setOverdueDays(50);
                    cfg.setCriticalDueDays(60);
                    cfg.setUpdatedBy("SYSTEM");
                    cfg.setUpdatedAt(LocalDateTime.now());
                    return clientDueConfigRepository.save(cfg);
                });
    }

    public Map<String, ClientDueConfig> getConfigMap() {
        List<ClientDueConfig> allConfigs = clientDueConfigRepository.findAll();
        Map<String, ClientDueConfig> map = new HashMap<>();
        for (ClientDueConfig cfg : allConfigs) {
            if (cfg.getClientIdentifier() != null) {
                map.put(cfg.getClientIdentifier().trim().toLowerCase(), cfg);
            }
        }
        return map;
    }

    public ClientDueConfig getConfigForWorkOrder(BaseWorkOrder wo, Map<String, ClientDueConfig> configMap, ClientDueConfig defaultConfig) {
        if (wo == null) return defaultConfig;

        if (wo.getClient() != null) {
            if (wo.getClient().getCode() != null) {
                ClientDueConfig cfg = configMap.get(wo.getClient().getCode().trim().toLowerCase());
                if (cfg != null) return cfg;
            }
            if (wo.getClient().getName() != null) {
                ClientDueConfig cfg = configMap.get(wo.getClient().getName().trim().toLowerCase());
                if (cfg != null) return cfg;
            }
        }

        if (wo.getOriginalClientString() != null) {
            ClientDueConfig cfg = configMap.get(wo.getOriginalClientString().trim().toLowerCase());
            if (cfg != null) return cfg;
        }

        return defaultConfig;
    }

    public long getDaysElapsed(BaseWorkOrder wo) {
        if (wo == null || wo.getInvoiceDate() == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(wo.getInvoiceDate(), LocalDate.now());
    }

    public boolean isUnpaid(BaseWorkOrder wo) {
        if (wo == null) return false;
        if (wo.isClientInvoicePaid()) {
            return false;
        }
        if (wo.getClientPaidDate() != null) {
            return false;
        }
        String status = wo.getStatus();
        if (status != null && (status.equalsIgnoreCase("Closed") || status.equalsIgnoreCase("Complete") || status.equalsIgnoreCase("Cancelled"))) {
            // Cancelled work orders are not due
            if (status.equalsIgnoreCase("Cancelled")) return false;
        }
        return true;
    }

    public String getAgingBucket(BaseWorkOrder wo, Map<String, ClientDueConfig> configMap, ClientDueConfig defaultConfig) {
        if (!isUnpaid(wo)) {
            return "PAID";
        }
        if (wo.getInvoiceDate() == null) {
            return "NO_INVOICE_DATE";
        }

        long days = getDaysElapsed(wo);
        if (days < 0) {
            return BUCKET_WITHIN_TERMS;
        }

        ClientDueConfig cfg = getConfigForWorkOrder(wo, configMap, defaultConfig);
        if (days >= cfg.getCriticalDueDays()) {
            return BUCKET_CRITICAL;
        } else if (days >= cfg.getOverdueDays()) {
            return BUCKET_OVERDUE;
        } else if (days >= cfg.getNormalDueDays()) {
            return BUCKET_STANDARD;
        } else {
            return BUCKET_WITHIN_TERMS;
        }
    }

    public AgingSummaryDTO calculateAgingSummary(List<EmployeeWorkOrder> allOrders) {
        ClientDueConfig defaultConfig = getDefaultConfig();
        Map<String, ClientDueConfig> configMap = getConfigMap();

        AgingSummaryDTO summary = new AgingSummaryDTO();
        summary.setDefaultConfig(defaultConfig);

        List<ClientDueConfig> clientConfigs = clientDueConfigRepository.findAllByOrderByClientNameAsc()
                .stream()
                .filter(c -> !"DEFAULT".equalsIgnoreCase(c.getClientIdentifier()))
                .collect(Collectors.toList());
        summary.setClientConfigs(clientConfigs);

        Map<String, AgingSummaryDTO.ClientAgingStat> clientStatsMap = new LinkedHashMap<>();

        for (EmployeeWorkOrder wo : allOrders) {
            if (!isUnpaid(wo) || wo.getInvoiceDate() == null) {
                continue;
            }

            BigDecimal amount = wo.getEffectiveClientTotal();
            String bucket = getAgingBucket(wo, configMap, defaultConfig);

            summary.setTotalUnpaidCount(summary.getTotalUnpaidCount() + 1);
            summary.setTotalUnpaidAmount(summary.getTotalUnpaidAmount().add(amount));

            switch (bucket) {
                case BUCKET_CRITICAL:
                    summary.setCriticalDueCount(summary.getCriticalDueCount() + 1);
                    summary.setCriticalDueAmount(summary.getCriticalDueAmount().add(amount));
                    break;
                case BUCKET_OVERDUE:
                    summary.setPastDueCount(summary.getPastDueCount() + 1);
                    summary.setPastDueAmount(summary.getPastDueAmount().add(amount));
                    break;
                case BUCKET_STANDARD:
                    summary.setStandardDueCount(summary.getStandardDueCount() + 1);
                    summary.setStandardDueAmount(summary.getStandardDueAmount().add(amount));
                    break;
                case BUCKET_WITHIN_TERMS:
                    summary.setWithinTermsCount(summary.getWithinTermsCount() + 1);
                    summary.setWithinTermsAmount(summary.getWithinTermsAmount().add(amount));
                    break;
            }

            // Client Stats grouping
            String clientName = "Unknown Client";
            String clientIdentifier = "unknown";
            if (wo.getClient() != null && wo.getClient().getName() != null) {
                clientName = wo.getClient().getName();
                clientIdentifier = wo.getClient().getCode() != null ? wo.getClient().getCode() : clientName;
            } else if (wo.getOriginalClientString() != null) {
                clientName = wo.getOriginalClientString();
                clientIdentifier = clientName;
            }

            ClientDueConfig clientCfg = getConfigForWorkOrder(wo, configMap, defaultConfig);
            boolean isCustom = clientCfg != null && !"DEFAULT".equalsIgnoreCase(clientCfg.getClientIdentifier());

            AgingSummaryDTO.ClientAgingStat cStat = clientStatsMap.computeIfAbsent(clientIdentifier.toLowerCase(), k -> {
                AgingSummaryDTO.ClientAgingStat stat = new AgingSummaryDTO.ClientAgingStat();
                stat.setClientIdentifier(clientCfg != null ? clientCfg.getClientIdentifier() : k);
                stat.setClientName(clientCfg != null && clientCfg.getClientName() != null ? clientCfg.getClientName() : k);
                stat.setNormalDueDays(clientCfg != null ? clientCfg.getNormalDueDays() : defaultConfig.getNormalDueDays());
                stat.setOverdueDays(clientCfg != null ? clientCfg.getOverdueDays() : defaultConfig.getOverdueDays());
                stat.setCriticalDueDays(clientCfg != null ? clientCfg.getCriticalDueDays() : defaultConfig.getCriticalDueDays());
                stat.setCustomConfig(isCustom);
                return stat;
            });

            cStat.setTotalUnpaidCount(cStat.getTotalUnpaidCount() + 1);
            cStat.setTotalUnpaidAmount(cStat.getTotalUnpaidAmount().add(amount));

            switch (bucket) {
                case BUCKET_CRITICAL:
                    cStat.setCriticalDueCount(cStat.getCriticalDueCount() + 1);
                    cStat.setCriticalDueAmount(cStat.getCriticalDueAmount().add(amount));
                    break;
                case BUCKET_OVERDUE:
                    cStat.setPastDueCount(cStat.getPastDueCount() + 1);
                    cStat.setPastDueAmount(cStat.getPastDueAmount().add(amount));
                    break;
                case BUCKET_STANDARD:
                    cStat.setStandardDueCount(cStat.getStandardDueCount() + 1);
                    cStat.setStandardDueAmount(cStat.getStandardDueAmount().add(amount));
                    break;
                case BUCKET_WITHIN_TERMS:
                    cStat.setWithinTermsCount(cStat.getWithinTermsCount() + 1);
                    cStat.setWithinTermsAmount(cStat.getWithinTermsAmount().add(amount));
                    break;
            }
        }

        // Sort client stats by total unpaid amount descending
        List<AgingSummaryDTO.ClientAgingStat> sortedStats = new ArrayList<>(clientStatsMap.values());
        sortedStats.sort((a, b) -> b.getTotalUnpaidAmount().compareTo(a.getTotalUnpaidAmount()));
        summary.setClientStats(sortedStats);

        return summary;
    }

    @Transactional
    public ClientDueConfig saveOrUpdateConfig(String clientIdentifier, String clientName, int normalDueDays, int overdueDays, int criticalDueDays, String updatedBy) {
        if (normalDueDays <= 0 || overdueDays <= normalDueDays || criticalDueDays <= overdueDays) {
            throw new IllegalArgumentException("Thresholds must be strictly increasing positive integers: Standard < Past Due < Critical");
        }

        String cleanIdentifier = (clientIdentifier != null && !clientIdentifier.trim().isEmpty()) ? clientIdentifier.trim() : "DEFAULT";

        ClientDueConfig cfg = clientDueConfigRepository.findByClientIdentifierIgnoreCase(cleanIdentifier)
                .orElse(new ClientDueConfig());

        cfg.setClientIdentifier(cleanIdentifier);
        cfg.setClientName(clientName != null && !clientName.trim().isEmpty() ? clientName.trim() : cleanIdentifier);
        cfg.setNormalDueDays(normalDueDays);
        cfg.setOverdueDays(overdueDays);
        cfg.setCriticalDueDays(criticalDueDays);
        cfg.setUpdatedBy(updatedBy);
        cfg.setUpdatedAt(LocalDateTime.now());

        return clientDueConfigRepository.save(cfg);
    }

    @Transactional
    public void deleteConfig(Long id) {
        clientDueConfigRepository.findById(id).ifPresent(cfg -> {
            if ("DEFAULT".equalsIgnoreCase(cfg.getClientIdentifier())) {
                // Reset to standard defaults instead of deletion
                cfg.setNormalDueDays(40);
                cfg.setOverdueDays(50);
                cfg.setCriticalDueDays(60);
                cfg.setUpdatedAt(LocalDateTime.now());
                clientDueConfigRepository.save(cfg);
            } else {
                clientDueConfigRepository.delete(cfg);
            }
        });
    }
}
