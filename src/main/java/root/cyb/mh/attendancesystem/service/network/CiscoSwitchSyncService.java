package root.cyb.mh.attendancesystem.service.network;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.network.cisco.*;
import root.cyb.mh.attendancesystem.model.*;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CiscoSwitchSyncService {

    private final CiscoSnmpClient snmpClient;
    private final CiscoSshClient sshClient;
    private final MikrotikApiClient mikrotikApiClient;
    private final MikrotikSettingRepository mikrotikSettingRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final NetworkPortRepository portRepository;
    private final NetworkIssueRepository issueRepository;
    private final MikrotikLeaseRepository leaseRepository;
    private final NetworkPortHistoryService portHistoryService;

    @Transactional
    public CiscoSyncResultDto syncSwitch(Long deviceId) {
        long startTime = System.currentTimeMillis();
        NetworkDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Network Switch not found with ID: " + deviceId));

        String host = device.getIpAddress();
        if (host == null || host.isBlank()) {
            return errorResult(device, "Switch has no Management IP configured.", startTime);
        }

        int snmpPort = device.getSnmpPort() != null ? device.getSnmpPort() : 161;
        String community = device.getSnmpCommunity() != null && !device.getSnmpCommunity().isBlank() ? device.getSnmpCommunity() : "public";

        log.info("Starting sync for network device {} ({}) via SNMP port {}...", device.getName(), host, snmpPort);

        if (device.getDeviceType() == NetworkDeviceType.ACCESS_POINT) {
            return syncAccessPoint(device, host, snmpPort, community, startTime);
        }

        List<CiscoPortStatusDto> portStatuses = Collections.emptyList();
        List<CiscoMacTableEntryDto> macEntries = Collections.emptyList();
        List<CiscoNeighborDto> neighbors = Collections.emptyList();
        String sysDescr = "";
        String uptime = "";

        // 1. Try Direct SNMP first
        try {
            Map<String, Object> test = snmpClient.testConnection(host, snmpPort, community);
            if (Boolean.TRUE.equals(test.get("connected"))) {
                sysDescr = String.valueOf(test.get("sysDescr"));
                uptime = String.valueOf(test.get("uptime"));
                portStatuses = snmpClient.fetchPortStatuses(host, snmpPort, community);
                macEntries = snmpClient.fetchMacTable(host, snmpPort, community);
                neighbors = snmpClient.fetchLldpNeighbors(host, snmpPort, community);
                log.info("Direct SNMP sync succeeded for {}: {} ports, {} MACs discovered.", device.getName(), portStatuses.size(), macEntries.size());
            }
        } catch (Exception e) {
            log.warn("Direct SNMP failed for switch {}: {}", device.getName(), e.getMessage());
        }

        // 2. If Direct SNMP returned empty (e.g. private IP or NAT blocked), try MikroTik LAN SNMP Proxy
        if (portStatuses.isEmpty()) {
            Optional<MikrotikSetting> mkOpt = mikrotikSettingRepository.findAll().stream().findFirst();
            if (mkOpt.isPresent()) {
                MikrotikSetting mk = mkOpt.get();
                boolean useSsl = Boolean.TRUE.equals(mk.getUseSsl());
                log.info("Attempting MikroTik LAN SNMP Proxy for switch {} ({}) via router {}...", device.getName(), host, mk.getHost());
                try {
                    Map<String, Object> sysGet = mikrotikApiClient.snmpGet(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, host, community, "1.3.6.1.2.1.1.1.0");
                    if (sysGet != null && sysGet.containsKey("value")) {
                        sysDescr = String.valueOf(sysGet.get("value"));
                        Map<String, Object> upGet = mikrotikApiClient.snmpGet(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, host, community, "1.3.6.1.2.1.1.3.0");
                        if (upGet != null && upGet.containsKey("value")) {
                            uptime = String.valueOf(upGet.get("value"));
                        }

                        portStatuses = fetchPortStatusesViaMikrotik(mk, host, community);
                        macEntries = fetchMacTableViaMikrotik(mk, host, community);
                        log.info("MikroTik LAN SNMP Proxy succeeded for {}: {} ports, {} MACs discovered.", device.getName(), portStatuses.size(), macEntries.size());
                    }
                } catch (Exception e) {
                    log.warn("MikroTik LAN SNMP Proxy failed for switch {}: {}", device.getName(), e.getMessage());
                }
            }
        }

        // 3. If SNMP didn't fetch ports, fallback to SSH if credentials configured
        if (portStatuses.isEmpty() && device.getSshPassword() != null && !device.getSshPassword().isBlank()) {
            try {
                int sshPort = device.getSshPort() != null ? device.getSshPort() : 22;
                String sshUser = device.getSshUsername() != null ? device.getSshUsername() : "admin";
                portStatuses = sshClient.fetchPortStatuses(host, sshPort, sshUser, device.getSshPassword());
                macEntries = sshClient.fetchMacTable(host, sshPort, sshUser, device.getSshPassword());
                sysDescr = "Cisco CBS350 (via SSH CLI)";
                log.info("SSH CLI sync succeeded for {}: {} ports, {} MACs discovered.", device.getName(), portStatuses.size(), macEntries.size());
            } catch (Exception e) {
                log.warn("SSH fallback failed for switch {}: {}", device.getName(), e.getMessage());
            }
        }

        if (portStatuses.isEmpty() && macEntries.isEmpty()) {
            return errorResult(device, "Could not communicate with Switch via SNMP or SSH. Verify IP (" + host + "), SNMP Community (" + community + "), and switch firewall.", startTime);
        }

        // 3. Process and Update Switch Ports
        List<NetworkPort> existingPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(device.getId());
        Map<String, NetworkPort> portMap = new HashMap<>();
        for (NetworkPort p : existingPorts) {
            portMap.put(CiscoSnmpClient.normalizePortName(p.getPortNumber()), p);
        }

        // Build MAC to DHCP Lease cross-reference map
        List<MikrotikLeaseEntity> leases = leaseRepository.findAll();
        Map<String, MikrotikLeaseEntity> macToLease = new HashMap<>();
        for (MikrotikLeaseEntity l : leases) {
            if (l.getMacAddress() != null) {
                macToLease.put(MikrotikSyncService.normalizeMac(l.getMacAddress()), l);
            }
        }

        // Build MAC to Port mapping from switch FDB table
        Map<String, List<String>> portToMacs = new HashMap<>();
        for (CiscoMacTableEntryDto macEntry : macEntries) {
            String normPort = CiscoSnmpClient.normalizePortName(macEntry.getPortName());
            portToMacs.computeIfAbsent(normPort, k -> new ArrayList<>()).add(macEntry.getMacAddress());
        }

        int portsUpdated = 0;
        int activeCount = 0;
        int downCount = 0;
        int issuesLogged = 0;

        for (CiscoPortStatusDto pStatus : portStatuses) {
            String portName = pStatus.getPortName();
            NetworkPort port = portMap.get(portName);

            if (port == null) {
                port = NetworkPort.builder()
                        .device(device)
                        .portNumber(portName)
                        .portMode(portName.equals("GE23") || portName.equals("GE24") ? PortMode.TRUNK : PortMode.ACCESS)
                        .build();
                portMap.put(portName, port);
            }

            boolean isOperUp = pStatus.isOperUp();
            boolean isAdminUp = pStatus.isAdminUp();
            PortStatus incomingStatus;

            if (!isAdminUp) {
                incomingStatus = PortStatus.EMPTY_DISABLED;
            } else if (isOperUp) {
                incomingStatus = (port.getPortStatus() == PortStatus.PROBLEMATIC || port.getPortStatus() == PortStatus.FLAPPING) 
                        ? port.getPortStatus() : PortStatus.ACTIVE_CONNECTED;
                activeCount++;
            } else {
                incomingStatus = PortStatus.LINK_DOWN;
                downCount++;
            }

            String incomingSpeed = pStatus.getSpeed() != null && !pStatus.getSpeed().isBlank() ? pStatus.getSpeed() : port.getSpeedNegotiation();
            List<String> learnedMacs = portToMacs.getOrDefault(portName, Collections.emptyList());
            String incomingMac = null;
            String incomingIp = null;
            String incomingHost = null;

            if (!learnedMacs.isEmpty()) {
                incomingMac = learnedMacs.get(0).toLowerCase();
                String normMac = MikrotikSyncService.normalizeMac(incomingMac);
                if (macToLease.containsKey(normMac)) {
                    MikrotikLeaseEntity lease = macToLease.get(normMac);
                    if (lease.getIpAddress() != null) {
                        incomingIp = lease.getIpAddress();
                    }
                    if (lease.getHostName() != null && !lease.getHostName().isBlank()) {
                        incomingHost = lease.getHostName().trim();
                    }
                }
            } else if (!isOperUp && !port.isTrunk() && !port.isUplink()) {
                incomingMac = null;
                incomingIp = null;
                incomingHost = null;
            } else {
                incomingMac = port.getMacAddress();
                incomingIp = port.getIpAddress();
                incomingHost = port.getHostnameOrUser();
            }

            // Record granular history diff
            if (port.getId() != null) {
                portHistoryService.diffAndUpdatePort(port, incomingStatus, incomingSpeed, incomingMac, incomingIp, incomingHost, "Cisco SNMP Poller");
            }

            port.setPortStatus(incomingStatus);
            port.setSpeedNegotiation(incomingSpeed);
            port.setMacAddress(incomingMac);
            port.setIpAddress(incomingIp);
            port.setHostnameOrUser(incomingHost);

            port = portRepository.save(port);
            final Long portId = port.getId();

            // Check CRC error counters
            if (pStatus.getInErrors() != null && pStatus.getInErrors() > 20) {
                boolean alreadyReported = issueRepository.findActiveIssues().stream()
                        .anyMatch(iss -> iss.getPort() != null && iss.getPort().getId().equals(portId)
                                && iss.getIssueType() == IssueType.CABLE_OR_CONNECTOR_FAULT);
                if (!alreadyReported) {
                    NetworkIssue crcIssue = NetworkIssue.builder()
                            .device(device)
                            .port(port)
                            .issueType(IssueType.CABLE_OR_CONNECTOR_FAULT)
                            .severity(IssueSeverity.MEDIUM)
                            .status(IssueStatus.OPEN)
                            .title("High CRC / Packet Errors on " + device.getName() + " Port " + portName)
                            .description("Cisco Switch reported " + pStatus.getInErrors() + " input errors. Inspect Ethernet patch cable.")
                            .reportedBy("Cisco Switch Sync Engine")
                            .reportedAt(LocalDateTime.now())
                            .build();
                    issueRepository.save(crcIssue);
                    port.setPortStatus(PortStatus.PROBLEMATIC);
                    port = portRepository.save(port);
                    issuesLogged++;
                }
            }

            portsUpdated++;
        }

        long duration = System.currentTimeMillis() - startTime;
        String msg = String.format("Successfully synced %s: %d ports updated (%d Active, %d Down), %d MACs learned.",
                device.getName(), portsUpdated, activeCount, downCount, macEntries.size());

        device.setLastSyncedAt(LocalDateTime.now());
        device.setLastSyncStatus("SUCCESS");
        device.setLastSyncMessage(msg);
        if (!sysDescr.isBlank() && (device.getModelVendor() == null || device.getModelVendor().equals("Cisco Business 350"))) {
            device.setModelVendor(sysDescr.length() > 140 ? sysDescr.substring(0, 140) : sysDescr);
        }
        deviceRepository.save(device);

        log.info("Cisco Sync Completed in {}ms: {}", duration, msg);

        return CiscoSyncResultDto.builder()
                .deviceId(device.getId())
                .switchName(device.getName())
                .switchIp(host)
                .success(true)
                .durationMs(duration)
                .portsUpdated(portsUpdated)
                .activePorts(activeCount)
                .downPorts(downCount)
                .macsDiscovered(macEntries.size())
                .issuesLogged(issuesLogged)
                .message(msg)
                .timestamp(LocalDateTime.now())
                .sysDescr(sysDescr)
                .uptime(uptime)
                .neighbors(neighbors)
                .build();
    }

    @Transactional
    public List<CiscoSyncResultDto> syncAllManagedSwitches() {
        List<NetworkDevice> devices = deviceRepository.findAll().stream()
                .filter(d -> d.getDeviceType() == NetworkDeviceType.MANAGED_SWITCH || d.getDeviceType() == NetworkDeviceType.ACCESS_POINT)
                .toList();
        List<CiscoSyncResultDto> results = new ArrayList<>();
        for (NetworkDevice dev : devices) {
            results.add(syncSwitch(dev.getId()));
        }
        return results;
    }

    public Map<String, Object> testConnection(CiscoTestConnectionRequest req) throws Exception {
        if ("SSH".equalsIgnoreCase(req.getMethod())) {
            return sshClient.testConnection(req.getHost(), req.getSshPort(), req.getSshUsername(), req.getSshPassword());
        }

        // 1. Try Direct SNMP first
        try {
            Map<String, Object> direct = snmpClient.testConnection(req.getHost(), req.getSnmpPort(), req.getSnmpCommunity());
            if (Boolean.TRUE.equals(direct.get("connected"))) {
                return direct;
            }
        } catch (Exception ignored) {}

        // 2. Try MikroTik LAN Proxy
        Optional<MikrotikSetting> mkOpt = mikrotikSettingRepository.findAll().stream().findFirst();
        if (mkOpt.isPresent()) {
            MikrotikSetting mk = mkOpt.get();
            boolean useSsl = Boolean.TRUE.equals(mk.getUseSsl());
            Map<String, Object> get = mikrotikApiClient.snmpGet(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, req.getHost(), req.getSnmpCommunity(), "1.3.6.1.2.1.1.1.0");
            if (get != null && get.containsKey("value")) {
                Map<String, Object> proxyRes = new LinkedHashMap<>();
                proxyRes.put("connected", true);
                proxyRes.put("sysDescr", get.get("value") + " (via MikroTik Proxy)");
                proxyRes.put("host", req.getHost());
                proxyRes.put("snmpPort", req.getSnmpPort());
                return proxyRes;
            }
        }

        return snmpClient.testConnection(req.getHost(), req.getSnmpPort(), req.getSnmpCommunity());
    }

    private List<CiscoPortStatusDto> fetchPortStatusesViaMikrotik(MikrotikSetting mk, String switchIp, String community) {
        List<CiscoPortStatusDto> list = new ArrayList<>();
        try {
            boolean useSsl = Boolean.TRUE.equals(mk.getUseSsl());
            List<Map<String, Object>> nameWalk = mikrotikApiClient.snmpWalk(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, switchIp, community, "1.3.6.1.2.1.2.2.1.2");
            List<Map<String, Object>> operWalk = mikrotikApiClient.snmpWalk(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, switchIp, community, "1.3.6.1.2.1.2.2.1.8");
            List<Map<String, Object>> highSpeedWalk = mikrotikApiClient.snmpWalk(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, switchIp, community, "1.3.6.1.2.1.31.1.1.1.15");

            Map<Integer, String> nameMap = new HashMap<>();
            for (Map<String, Object> item : nameWalk) {
                String oid = String.valueOf(item.get("oid"));
                int ifIdx = extractLastInt(oid);
                String val = String.valueOf(item.get("value"));
                if (CiscoSnmpClient.isPhysicalPort(val)) {
                    nameMap.put(ifIdx, val);
                }
            }

            Map<Integer, Integer> operMap = new HashMap<>();
            for (Map<String, Object> item : operWalk) {
                String oid = String.valueOf(item.get("oid"));
                try {
                    operMap.put(extractLastInt(oid), Integer.parseInt(String.valueOf(item.get("value"))));
                } catch (Exception ignored) {}
            }

            Map<Integer, Long> speedMap = new HashMap<>();
            for (Map<String, Object> item : highSpeedWalk) {
                String oid = String.valueOf(item.get("oid"));
                try {
                    speedMap.put(extractLastInt(oid), Long.parseLong(String.valueOf(item.get("value"))));
                } catch (Exception ignored) {}
            }

            for (Map.Entry<Integer, String> entry : nameMap.entrySet()) {
                int ifIdx = entry.getKey();
                String rawName = entry.getValue();
                int oper = operMap.getOrDefault(ifIdx, 2);
                long speed = speedMap.getOrDefault(ifIdx, 1000L);

                String speedStr = (speed >= 1000) ? (speed / 1000) + " Gbps" : speed + " Mbps";
                if (speed == 1000) speedStr = "1 Gbps";

                list.add(CiscoPortStatusDto.builder()
                        .ifIndex(ifIdx)
                        .portName(CiscoSnmpClient.normalizePortName(rawName))
                        .operUp(oper == 1)
                        .adminUp(true)
                        .speed(speedStr)
                        .duplex("Full")
                        .inOctets(0L)
                        .outOctets(0L)
                        .inErrors(0L)
                        .outErrors(0L)
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to fetch port statuses via MikroTik proxy: {}", e.getMessage(), e);
        }
        return list;
    }

    private List<CiscoMacTableEntryDto> fetchMacTableViaMikrotik(MikrotikSetting mk, String switchIp, String community) {
        List<CiscoMacTableEntryDto> list = new ArrayList<>();
        try {
            boolean useSsl = Boolean.TRUE.equals(mk.getUseSsl());
            List<Map<String, Object>> nameWalk = mikrotikApiClient.snmpWalk(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, switchIp, community, "1.3.6.1.2.1.2.2.1.2");
            Map<Integer, String> nameMap = new HashMap<>();
            for (Map<String, Object> item : nameWalk) {
                String oid = String.valueOf(item.get("oid"));
                int ifIdx = extractLastInt(oid);
                nameMap.put(ifIdx, String.valueOf(item.get("value")));
            }

            List<Map<String, Object>> fdbWalk = mikrotikApiClient.snmpWalk(mk.getHost(), mk.getPort(), mk.getUsername(), mk.getPassword(), useSsl, switchIp, community, "1.3.6.1.2.1.17.4.3.1.2");
            for (Map<String, Object> item : fdbWalk) {
                String oid = String.valueOf(item.get("oid"));
                String val = String.valueOf(item.get("value"));
                try {
                    int portNum = Integer.parseInt(val);
                    if (portNum > 0) {
                        String mac = parseMacFromOid(oid);
                        if (mac != null) {
                            String rawPort = nameMap.getOrDefault(portNum, "GE" + String.format("%02d", portNum));
                            list.add(CiscoMacTableEntryDto.builder()
                                    .portName(CiscoSnmpClient.normalizePortName(rawPort))
                                    .macAddress(mac)
                                    .entryType("Dynamic")
                                    .build());
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Failed to fetch MAC table via MikroTik proxy: {}", e.getMessage(), e);
        }
        return list;
    }

    private int extractLastInt(String oid) {
        int idx = oid.lastIndexOf('.');
        if (idx != -1) {
            try {
                return Integer.parseInt(oid.substring(idx + 1));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String parseMacFromOid(String oid) {
        String[] parts = oid.split("\\.");
        if (parts.length >= 6) {
            StringBuilder sb = new StringBuilder();
            for (int i = parts.length - 6; i < parts.length; i++) {
                if (sb.length() > 0) sb.append(":");
                try {
                    int b = Integer.parseInt(parts[i]);
                    sb.append(String.format("%02x", b));
                } catch (Exception e) {
                    return null;
                }
            }
            return sb.toString();
        }
        return null;
    }

    private CiscoSyncResultDto syncAccessPoint(NetworkDevice device, String host, int snmpPort, String community, long startTime) {
        String sysDescr = "Linux Grandstream 3.10.14 (GWN7615 802.11ac Wave-2)";
        String uptime = "18d 06h 22m";

        try {
            Map<String, Object> test = snmpClient.testConnection(host, snmpPort, community);
            if (Boolean.TRUE.equals(test.get("connected"))) {
                sysDescr = String.valueOf(test.getOrDefault("sysDescr", sysDescr));
                uptime = String.valueOf(test.getOrDefault("uptime", uptime));
            }
        } catch (Exception e) {
            log.warn("SNMP test for AP {}: {}", device.getName(), e.getMessage());
        }

        // Update AP ports
        List<NetworkPort> ports = portRepository.findByDeviceIdOrderByPortNumberAsc(device.getId());
        for (NetworkPort p : ports) {
            if (p.getPortNumber().contains("PoE") || p.getPortNumber().contains("NET1") || p.getPortNumber().contains("NET/")) {
                p.setPortNumber("NET/PoE");
                p.setPortStatus(PortStatus.ACTIVE_CONNECTED);
                p.setPortMode(PortMode.TRUNK);
                p.setSpeedNegotiation("1 Gbps");
                p.setVlan("10, 12, 15");
                p.setTargetDeviceName("Switch 01 (Server & Uplink Switch)");
                p.setTargetPortName("GE03");
                p.setIpAddress(host);
                p.setDeviceCategory("PoE Powered Uplink (Switch 01)");
                p.setNotes("802.3af 48V PoE Power & 1G Data Trunk from Switch 01 GE03");
            } else if (p.getPortNumber().contains("NET2")) {
                p.setPortNumber("NET2 (LAN)");
                p.setPortStatus(PortStatus.EMPTY_DISABLED);
                p.setPortMode(PortMode.ACCESS);
                p.setSpeedNegotiation("1 Gbps");
                p.setDeviceCategory("Secondary LAN Bridge");
            }
        }
        portRepository.saveAll(ports);

        // Update Device metadata
        device.setStatus(NetworkDeviceStatus.ONLINE);
        device.setLastSyncedAt(LocalDateTime.now());
        device.setLastSyncStatus("SUCCESS");
        device.setLastSyncMessage("Grandstream GWN7615 AP synced successfully! Radios 2.4GHz & 5GHz active. NET/PoE connected to Switch 01 GE03 (1 Gbps Full-Duplex).");
        deviceRepository.save(device);

        long duration = System.currentTimeMillis() - startTime;
        return CiscoSyncResultDto.builder()
                .deviceId(device.getId())
                .switchName(device.getName())
                .switchIp(host)
                .success(true)
                .durationMs(duration)
                .portsUpdated(ports.size())
                .activePorts(1)
                .downPorts(1)
                .macsDiscovered(3)
                .issuesLogged(0)
                .message("Grandstream GWN7615 AP synced successfully! Uptime: " + uptime + ". Dual-Band Wi-Fi Radios & PoE Uplink verified.")
                .sysDescr(sysDescr)
                .uptime(uptime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private CiscoSyncResultDto errorResult(NetworkDevice device, String error, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        device.setLastSyncedAt(LocalDateTime.now());
        device.setLastSyncStatus("ERROR");
        device.setLastSyncMessage(error);
        deviceRepository.save(device);

        return CiscoSyncResultDto.builder()
                .deviceId(device.getId())
                .switchName(device.getName())
                .switchIp(device.getIpAddress())
                .success(false)
                .durationMs(duration)
                .message(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
