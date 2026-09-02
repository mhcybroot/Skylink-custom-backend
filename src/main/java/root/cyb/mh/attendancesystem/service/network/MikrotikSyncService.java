package root.cyb.mh.attendancesystem.service.network;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.network.mikrotik.*;
import root.cyb.mh.attendancesystem.model.*;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MikrotikSyncService {

    private final MikrotikApiClient apiClient;
    private final NetworkDeviceRepository deviceRepository;
    private final NetworkPortRepository portRepository;
    private final NetworkIssueRepository issueRepository;
    private final MikrotikSettingRepository settingRepository;
    private final MikrotikLeaseRepository leaseRepository;
    private final MikrotikArpRepository arpRepository;
    private final NetworkPortHistoryService portHistoryService;

    private final List<MikrotikInterfaceDto> cachedInterfaces = new CopyOnWriteArrayList<>();

    public static String normalizeMac(String mac) {
        if (mac == null) return "";
        return mac.replaceAll("[^a-fA-F0-9]", "").toLowerCase().trim();
    }

    @PostConstruct
    @Transactional
    public void initSettings() {
        if (settingRepository.count() == 0) {
            MikrotikSetting setting = MikrotikSetting.builder()
                    .host("116.206.59.142")
                    .port(8225)
                    .username("skylink-sync")
                    .password("")
                    .useSsl(true)
                    .autoSyncEnabled(true)
                    .pollCron("0 */5 * * * *")
                    .lastSyncStatus("NEVER")
                    .lastSyncMessage("No synchronization performed yet")
                    .lastLeasesCount(0)
                    .lastArpCount(0)
                    .lastInterfacesCount(0)
                    .build();
            settingRepository.save(setting);
            log.info("Initialized default MikroTik settings in database (Host: 116.206.59.142:8225)");
        }
    }

    public MikrotikSetting getPersistentSetting() {
        return settingRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    MikrotikSetting setting = MikrotikSetting.builder()
                            .host("116.206.59.142")
                            .port(8225)
                            .username("skylink-sync")
                            .useSsl(true)
                            .autoSyncEnabled(true)
                            .build();
                    return settingRepository.save(setting);
                });
    }

    @Transactional
    public MikrotikSyncResultDto syncLive() {
        long startTime = System.currentTimeMillis();
        MikrotikSetting setting = getPersistentSetting();
        String host = setting.getHost();
        int port = setting.getPort();
        String username = setting.getUsername();
        String password = setting.getPassword() != null ? setting.getPassword() : "";
        boolean useSsl = Boolean.TRUE.equals(setting.getUseSsl());

        log.info("Starting live MikroTik database synchronization with host: {}:{}", host, port);

        int leasesFound = 0;
        int portsUpdated = 0;
        int arpFound = 0;
        int interfacesSynced = 0;
        int issuesLogged = 0;

        try {
            // 1. Fetch DHCP leases from router & persist to DB
            List<MikrotikDhcpLeaseDto> leases = apiClient.fetchDhcpLeases(host, port, username, password, useSsl);
            leasesFound = leases.size();

            if (!leases.isEmpty()) {
                leaseRepository.deleteAllInBatch();
                List<MikrotikLeaseEntity> leaseEntities = leases.stream().map(dto -> MikrotikLeaseEntity.builder()
                        .mikrotikId(dto.getId())
                        .ipAddress(dto.getEffectiveIp())
                        .macAddress(dto.getEffectiveMac())
                        .hostName(dto.getEffectiveHostName())
                        .server(dto.getServer())
                        .status(dto.getStatus() != null ? dto.getStatus() : "bound")
                        .expiresAfter(dto.getExpiresAfter())
                        .dynamic(dto.getDynamic())
                        .disabled(dto.getDisabled())
                        .comment(dto.getComment())
                        .lastSeen(LocalDateTime.now())
                        .build()
                ).collect(Collectors.toList());
                leaseRepository.saveAll(leaseEntities);
            }

            // 2. Fetch ARP Table & persist to DB
            List<MikrotikArpDto> arps = apiClient.fetchArpTable(host, port, username, password, useSsl);
            arpFound = arps.size();

            if (!arps.isEmpty()) {
                arpRepository.deleteAllInBatch();
                List<MikrotikArpEntity> arpEntities = arps.stream().map(dto -> MikrotikArpEntity.builder()
                        .mikrotikId(dto.getId())
                        .ipAddress(dto.getAddress())
                        .macAddress(dto.getMacAddress())
                        .networkInterface(dto.getNetworkInterface())
                        .complete(dto.getComplete())
                        .dynamic(dto.getDynamic())
                        .comment(dto.getComment())
                        .lastSeen(LocalDateTime.now())
                        .build()
                ).collect(Collectors.toList());
                arpRepository.saveAll(arpEntities);
            }

            // Update Switch Ports using normalized matching
            List<NetworkPort> allPorts = portRepository.findAll();
            for (MikrotikDhcpLeaseDto lease : leases) {
                String leaseMac = lease.getEffectiveMac();
                String leaseIp = lease.getEffectiveIp();
                String leaseHost = lease.getEffectiveHostName();
                String normLeaseMac = normalizeMac(leaseMac);

                if (normLeaseMac.isEmpty() && (leaseIp == null || leaseIp.isBlank())) continue;

                for (NetworkPort p : allPorts) {
                    String normPortMac = normalizeMac(p.getMacAddress());
                    boolean matchMac = (!normLeaseMac.isEmpty() && !normPortMac.isEmpty() && normLeaseMac.equals(normPortMac));
                    boolean matchIp = (leaseIp != null && p.getIpAddress() != null &&
                            p.getIpAddress().trim().equalsIgnoreCase(leaseIp.trim()));

                    if (matchMac || matchIp) {
                        boolean updated = false;

                        // Auto-assign IP if missing
                        if (leaseIp != null && (p.getIpAddress() == null || p.getIpAddress().isBlank())) {
                            p.setIpAddress(leaseIp);
                            updated = true;
                        }

                        // Auto-assign MAC if missing
                        if (leaseMac != null && (p.getMacAddress() == null || p.getMacAddress().isBlank())) {
                            p.setMacAddress(leaseMac.toLowerCase());
                            updated = true;
                        }

                        // Update hostname
                        if (leaseHost != null && !leaseHost.isBlank()) {
                            if (p.getHostnameOrUser() == null || p.getHostnameOrUser().isBlank()) {
                                p.setHostnameOrUser(leaseHost);
                                updated = true;
                            } else if (!p.getHostnameOrUser().toLowerCase().contains(leaseHost.toLowerCase())) {
                                p.setHostnameOrUser(p.getHostnameOrUser() + " (" + leaseHost + ")");
                                updated = true;
                            }
                        }

                        // If port was previously empty/disabled, activate it
                        if ("bound".equalsIgnoreCase(lease.getStatus()) && p.getPortStatus() == PortStatus.EMPTY_DISABLED) {
                            p.setPortStatus(PortStatus.ACTIVE_CONNECTED);
                            updated = true;
                        }

                        if (updated) {
                            portRepository.save(p);
                            portsUpdated++;
                        }
                    }
                }
            }

            // 3. Sync Router Interfaces & Flap monitoring
            List<MikrotikInterfaceDto> interfaces = apiClient.fetchInterfaces(host, port, username, password, useSsl);
            cachedInterfaces.clear();
            cachedInterfaces.addAll(interfaces);
            interfacesSynced = interfaces.size();

            Optional<NetworkDevice> routerOpt = deviceRepository.findByDeviceType(NetworkDeviceType.ROUTER_MIKROTIK).stream().findFirst();
            if (routerOpt.isPresent()) {
                NetworkDevice router = routerOpt.get();
                List<NetworkPort> existingRouterPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(router.getId());
                Set<String> liveIfaceNames = new HashSet<>();

                for (MikrotikInterfaceDto iface : interfaces) {
                    if (iface.getName() == null) continue;
                    String ifName = iface.getName().trim();
                    liveIfaceNames.add(ifName.toLowerCase());

                    Optional<NetworkPort> routerPortOpt = portRepository.findByDeviceIdAndPortNumberIgnoreCase(router.getId(), ifName);
                    NetworkPort rPort;
                    if (routerPortOpt.isPresent()) {
                        rPort = routerPortOpt.get();
                    } else {
                        rPort = NetworkPort.builder()
                                .device(router)
                                .portNumber(ifName)
                                .portMode(PortMode.ACCESS)
                                .speedNegotiation("1 Gbps")
                                .build();
                    }

                    boolean isRunning = Boolean.TRUE.equals(iface.getRunning());
                    boolean isDisabled = Boolean.TRUE.equals(iface.getDisabled());
                    PortStatus incomingStatus;

                    if (isDisabled) {
                        incomingStatus = PortStatus.EMPTY_DISABLED;
                    } else if (isRunning) {
                        incomingStatus = (rPort.getPortStatus() == PortStatus.PROBLEMATIC || rPort.getPortStatus() == PortStatus.FLAPPING) 
                                ? rPort.getPortStatus() : PortStatus.ACTIVE_CONNECTED;
                    } else {
                        incomingStatus = PortStatus.LINK_DOWN;
                    }

                    String incomingMac = (iface.getMacAddress() != null && !iface.getMacAddress().isBlank()) 
                            ? iface.getMacAddress().toLowerCase() : rPort.getMacAddress();
                    String incomingHost = rPort.getHostnameOrUser();

                    // Enrich interface metadata based on actual RouterOS configuration
                    if (ifName.equalsIgnoreCase("ether10")) {
                        rPort.setPortMode(PortMode.TRUNK);
                        rPort.setTrunk(true);
                        rPort.setUplink(true);
                        rPort.setVlan("1,10,11,12,15,16");
                        incomingHost = "Trunk to Switch 01 (GE23)";
                        rPort.setDeviceCategory("Switch Trunk Uplink");
                        rPort.setTargetDeviceName("Switch 01 (Server & Uplink Switch)");
                        rPort.setTargetPortName("GE23");
                    } else if (ifName.toLowerCase().contains("wan") || ifName.toLowerCase().contains("aamra")) {
                        rPort.setPortMode(PortMode.WAN);
                        if (ifName.toLowerCase().contains("banani")) {
                            incomingHost = "Aamra Primary WAN (Banani)";
                        } else if (ifName.toLowerCase().contains("bashund")) {
                            incomingHost = "Aamra Secondary WAN (Bashundhara)";
                        }
                        rPort.setDeviceCategory("WAN / Internet Gateway");
                    } else if (ifName.equalsIgnoreCase("bridge-LAN")) {
                        incomingHost = "Main LAN Gateway Bridge";
                        rPort.setDeviceCategory("Bridge Interface");
                    } else if (ifName.startsWith("vlan")) {
                        rPort.setDeviceCategory("VLAN Sub-Interface");
                        rPort.setVlan(ifName.replace("vlan", ""));
                    } else if (ifName.startsWith("wg")) {
                        incomingHost = "WireGuard VPN Tunnel";
                        rPort.setDeviceCategory("VPN Interface");
                    }

                    if (rPort.getId() != null) {
                        portHistoryService.diffAndUpdatePort(rPort, incomingStatus, rPort.getSpeedNegotiation(), incomingMac, rPort.getIpAddress(), incomingHost, "MikroTik RouterOS");
                    }

                    rPort.setPortStatus(incomingStatus);
                    rPort.setMacAddress(incomingMac);
                    rPort.setHostnameOrUser(incomingHost);

                    portRepository.save(rPort);

                    // Check flap / error counts
                    if (iface.getLinkDowns() != null && iface.getLinkDowns() >= 3) {
                        boolean alreadyReported = issueRepository.findActiveIssues().stream()
                                .anyMatch(iss -> iss.getPort() != null && iss.getPort().getId().equals(rPort.getId())
                                        && iss.getIssueType() == IssueType.PORT_FLAPPING);
                        if (!alreadyReported) {
                            NetworkIssue flapIssue = NetworkIssue.builder()
                                    .device(router)
                                    .port(rPort)
                                    .issueType(IssueType.PORT_FLAPPING)
                                    .severity(IssueSeverity.HIGH)
                                    .status(IssueStatus.OPEN)
                                    .title("High Link Flap Count on Router Interface " + ifName)
                                    .description("MikroTik recorded " + iface.getLinkDowns() + " link-down transitions on interface " + ifName)
                                    .reportedBy("MikroTik Auto-Sync Engine")
                                    .reportedAt(LocalDateTime.now())
                                    .build();
                            issueRepository.save(flapIssue);
                            rPort.setPortStatus(PortStatus.FLAPPING);
                            portRepository.save(rPort);
                            issuesLogged++;
                        }
                    }
                }

                // Delete old dummy/orphan ports that don't exist on RouterOS
                for (NetworkPort oldPort : existingRouterPorts) {
                    if (!liveIfaceNames.contains(oldPort.getPortNumber().toLowerCase().trim())) {
                        log.info("Deleting orphan/dummy MikroTik port: {}", oldPort.getPortNumber());
                        portRepository.delete(oldPort);
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            String msg = String.format("Successfully synced from MikroTik (%s): %d DHCP leases, %d ARP entries, %d interfaces, %d ports updated.",
                    host, leasesFound, arpFound, interfacesSynced, portsUpdated);

            setting.setLastSyncTime(LocalDateTime.now());
            setting.setLastSyncStatus("SUCCESS");
            setting.setLastSyncMessage(msg);
            setting.setLastLeasesCount(leasesFound);
            setting.setLastArpCount(arpFound);
            setting.setLastInterfacesCount(interfacesSynced);
            settingRepository.save(setting);

            log.info("MikroTik Database Sync Completed in {}ms: {}", duration, msg);

            return MikrotikSyncResultDto.builder()
                    .success(true)
                    .durationMs(duration)
                    .leasesFound(leasesFound)
                    .portsUpdated(portsUpdated)
                    .arpEntriesFound(arpFound)
                    .interfacesSynced(interfacesSynced)
                    .issuesLogged(issuesLogged)
                    .message(msg)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = "MikroTik Sync failed: " + e.getMessage();
            setting.setLastSyncTime(LocalDateTime.now());
            setting.setLastSyncStatus("ERROR");
            setting.setLastSyncMessage(errorMsg);
            settingRepository.save(setting);

            log.error("MikroTik Sync encountered an error: {}", e.getMessage(), e);

            return MikrotikSyncResultDto.builder()
                    .success(false)
                    .durationMs(duration)
                    .leasesFound(0)
                    .portsUpdated(0)
                    .arpEntriesFound(0)
                    .interfacesSynced(0)
                    .issuesLogged(0)
                    .message(errorMsg)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<MikrotikEnrichedLeaseDto> getEnrichedLeases(String query) {
        List<MikrotikLeaseEntity> leases = (query != null && !query.isBlank())
                ? leaseRepository.searchLeases(query.trim())
                : leaseRepository.findAll();

        List<NetworkPort> allPorts = portRepository.findAll();
        Map<String, NetworkPort> macToPort = new HashMap<>();
        Map<String, NetworkPort> ipToPort = new HashMap<>();

        for (NetworkPort p : allPorts) {
            if (p.getMacAddress() != null && !p.getMacAddress().isBlank()) {
                macToPort.put(normalizeMac(p.getMacAddress()), p);
            }
            if (p.getIpAddress() != null && !p.getIpAddress().isBlank()) {
                ipToPort.put(p.getIpAddress().trim().toLowerCase(), p);
            }
        }

        List<MikrotikEnrichedLeaseDto> enrichedList = new ArrayList<>();
        for (MikrotikLeaseEntity lease : leases) {
            String ip = lease.getIpAddress();
            String mac = lease.getMacAddress();
            String host = lease.getHostName();
            String nMac = normalizeMac(mac);

            NetworkPort matchedPort = null;
            if (!nMac.isEmpty() && macToPort.containsKey(nMac)) {
                matchedPort = macToPort.get(nMac);
            } else if (ip != null && ipToPort.containsKey(ip.toLowerCase().trim())) {
                matchedPort = ipToPort.get(ip.toLowerCase().trim());
            }

            MikrotikEnrichedLeaseDto dto = MikrotikEnrichedLeaseDto.builder()
                    .id(String.valueOf(lease.getId()))
                    .ipAddress(ip)
                    .macAddress(mac)
                    .hostName(host)
                    .server(lease.getServer())
                    .status(lease.getStatus() != null ? lease.getStatus() : "bound")
                    .expiresAfter(lease.getExpiresAfter())
                    .dynamic(lease.getDynamic())
                    .disabled(lease.getDisabled())
                    .comment(lease.getComment())
                    .mapped(matchedPort != null)
                    .portId(matchedPort != null ? matchedPort.getId() : null)
                    .portNumber(matchedPort != null ? matchedPort.getPortNumber() : null)
                    .deviceId(matchedPort != null && matchedPort.getDevice() != null ? matchedPort.getDevice().getId() : null)
                    .deviceName(matchedPort != null && matchedPort.getDevice() != null ? matchedPort.getDevice().getName() : null)
                    .assignedUser(matchedPort != null ? matchedPort.getHostnameOrUser() : null)
                    .vlan(matchedPort != null ? matchedPort.getVlan() : null)
                    .build();

            enrichedList.add(dto);
        }

        return enrichedList;
    }

    @Transactional(readOnly = true)
    public List<MikrotikArpDto> getArpEntries(String query) {
        List<MikrotikArpEntity> list = (query != null && !query.isBlank())
                ? arpRepository.searchArp(query.trim())
                : arpRepository.findAll();

        return list.stream().map(a -> MikrotikArpDto.builder()
                .id(String.valueOf(a.getId()))
                .address(a.getIpAddress())
                .macAddress(a.getMacAddress())
                .networkInterface(a.getNetworkInterface())
                .complete(a.getComplete())
                .dynamic(a.getDynamic())
                .comment(a.getComment())
                .build()
        ).collect(Collectors.toList());
    }

    public List<MikrotikInterfaceDto> getCachedInterfaces(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(cachedInterfaces);
        }
        String q = query.toLowerCase().trim();
        return cachedInterfaces.stream()
                .filter(i -> (i.getName() != null && i.getName().toLowerCase().contains(q)) ||
                             (i.getType() != null && i.getType().toLowerCase().contains(q)) ||
                             (i.getMacAddress() != null && i.getMacAddress().toLowerCase().contains(q)) ||
                             (i.getComment() != null && i.getComment().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    @Transactional
    public NetworkPort mapLeaseToPort(MapLeaseRequest req) {
        NetworkPort port = portRepository.findById(req.getPortId())
                .orElseThrow(() -> new IllegalArgumentException("Port not found with ID: " + req.getPortId()));

        if (req.getIpAddress() != null && !req.getIpAddress().isBlank()) {
            port.setIpAddress(req.getIpAddress().trim());
        }
        if (req.getMacAddress() != null && !req.getMacAddress().isBlank()) {
            port.setMacAddress(req.getMacAddress().toLowerCase().trim());
        }
        if (req.getHostName() != null && !req.getHostName().isBlank()) {
            if (port.getHostnameOrUser() == null || port.getHostnameOrUser().isBlank() || port.getHostnameOrUser().equals("Workstation PC")) {
                port.setHostnameOrUser(req.getHostName());
            } else if (!port.getHostnameOrUser().toLowerCase().contains(req.getHostName().toLowerCase())) {
                port.setHostnameOrUser(port.getHostnameOrUser() + " (" + req.getHostName() + ")");
            }
        }
        if (req.getDeviceCategory() != null && !req.getDeviceCategory().isBlank()) {
            port.setDeviceCategory(req.getDeviceCategory());
        }
        port.setPortStatus(PortStatus.ACTIVE_CONNECTED);

        return portRepository.save(port);
    }

    public Map<String, Object> testConnection(MikrotikConnectionTestRequest req) throws Exception {
        return apiClient.testConnection(req.getHost(), req.getPort(), req.getUsername(), req.getPassword(), req.isUseSsl());
    }

    @Transactional(readOnly = true)
    public MikrotikConfigDto getConfig() {
        MikrotikSetting s = getPersistentSetting();
        return MikrotikConfigDto.builder()
                .host(s.getHost())
                .port(s.getPort())
                .username(s.getUsername())
                .password(s.getPassword() != null && !s.getPassword().isBlank() ? s.getPassword() : "")
                .useSsl(Boolean.TRUE.equals(s.getUseSsl()))
                .autoSyncEnabled(Boolean.TRUE.equals(s.getAutoSyncEnabled()))
                .pollIntervalCron(s.getPollCron())
                .lastSyncTime(s.getLastSyncTime())
                .lastSyncStatus(s.getLastSyncStatus())
                .lastSyncMessage(s.getLastSyncMessage())
                .build();
    }

    @Transactional
    public void updateConfig(MikrotikConfigDto dto) {
        MikrotikSetting s = getPersistentSetting();
        if (dto.getHost() != null && !dto.getHost().isBlank()) s.setHost(dto.getHost().trim());
        if (dto.getPort() > 0) s.setPort(dto.getPort());
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) s.setUsername(dto.getUsername().trim());
        if (dto.getPassword() != null && !dto.getPassword().isBlank() && !dto.getPassword().equals("********")) {
            s.setPassword(dto.getPassword());
        }
        s.setUseSsl(dto.isUseSsl());
        s.setAutoSyncEnabled(dto.isAutoSyncEnabled());
        if (dto.getPollIntervalCron() != null && !dto.getPollIntervalCron().isBlank()) {
            s.setPollCron(dto.getPollIntervalCron());
        }
        settingRepository.save(s);
    }

    @Scheduled(cron = "${app.network.mikrotik.poll-cron:0 */5 * * * *}")
    public void scheduledSync() {
        MikrotikSetting s = getPersistentSetting();
        if (Boolean.TRUE.equals(s.getAutoSyncEnabled())) {
            log.info("Triggering scheduled MikroTik database synchronization...");
            syncLive();
        }
    }
}
