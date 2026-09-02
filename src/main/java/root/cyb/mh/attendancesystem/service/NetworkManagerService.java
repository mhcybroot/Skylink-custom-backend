package root.cyb.mh.attendancesystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.network.*;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.NetworkIssue;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.repository.NetworkDeviceRepository;
import root.cyb.mh.attendancesystem.repository.NetworkIssueRepository;
import root.cyb.mh.attendancesystem.repository.NetworkPortRepository;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkManagerService {

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkPortRepository portRepository;
    private final NetworkIssueRepository issueRepository;
    private final root.cyb.mh.attendancesystem.service.network.MikrotikSyncService mikrotikSyncService;
    private final root.cyb.mh.attendancesystem.service.network.MikrotikApiClient mikrotikApiClient;
    private final root.cyb.mh.attendancesystem.service.network.NetworkPortHistoryService portHistoryService;

    @Transactional(readOnly = true)
    public List<NetworkDevice> getAllDevices() {
        return deviceRepository.findAllWithPorts();
    }

    @Transactional(readOnly = true)
    public NetworkDevice getDeviceById(Long id) {
        return deviceRepository.findByIdWithPorts(id)
                .orElseThrow(() -> new IllegalArgumentException("Network Device not found with ID: " + id));
    }

    @Transactional
    public NetworkDevice saveDevice(DeviceSaveRequest req) {
        NetworkDevice device;
        boolean isNew = (req.getId() == null);
        if (!isNew) {
            device = deviceRepository.findById(req.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Network Device not found with ID: " + req.getId()));
        } else {
            device = new NetworkDevice();
        }

        device.setName(req.getName());
        device.setDeviceType(req.getDeviceType() != null ? req.getDeviceType() : NetworkDeviceType.MANAGED_SWITCH);
        device.setIpAddress(req.getIpAddress());
        device.setMacAddress(req.getMacAddress());
        device.setLocation(req.getLocation());
        device.setTotalPorts(req.getTotalPorts() > 0 ? req.getTotalPorts() : 24);
        device.setStatus(req.getStatus() != null ? req.getStatus() : NetworkDeviceStatus.ONLINE);
        device.setManagementUrl(req.getManagementUrl());
        device.setModelVendor(req.getModelVendor());
        device.setSerialNumber(req.getSerialNumber());
        device.setNotes(req.getNotes());

        if (req.getSnmpCommunity() != null && !req.getSnmpCommunity().isBlank()) {
            device.setSnmpCommunity(req.getSnmpCommunity().trim());
        }
        if (req.getSnmpPort() != null && req.getSnmpPort() > 0) {
            device.setSnmpPort(req.getSnmpPort());
        }
        if (req.getSnmpVersion() != null && !req.getSnmpVersion().isBlank()) {
            device.setSnmpVersion(req.getSnmpVersion().trim());
        }
        if (req.getSshPort() != null && req.getSshPort() > 0) {
            device.setSshPort(req.getSshPort());
        }
        if (req.getSshUsername() != null && !req.getSshUsername().isBlank()) {
            device.setSshUsername(req.getSshUsername().trim());
        }
        if (req.getSshPassword() != null && !req.getSshPassword().isBlank() && !req.getSshPassword().equals("••••••••")) {
            device.setSshPassword(req.getSshPassword());
        }

        device = deviceRepository.save(device);

        // If new switch/router, auto-create port slots if none exist
        if (isNew && (device.getPorts() == null || device.getPorts().isEmpty())) {
            createDefaultPortsForDevice(device, device.getTotalPorts());
        }

        return deviceRepository.findByIdWithPorts(device.getId()).orElse(device);
    }

    @Transactional
    public void createDefaultPortsForDevice(NetworkDevice device, int totalPorts) {
        List<NetworkPort> portList = new ArrayList<>();
        String prefix = (device.getDeviceType() == NetworkDeviceType.ROUTER_MIKROTIK) ? "ether" : "GE";
        for (int i = 1; i <= totalPorts; i++) {
            String portNum = (prefix.equals("GE")) ? String.format("GE%02d", i) : (prefix + i);
            NetworkPort port = NetworkPort.builder()
                    .device(device)
                    .portNumber(portNum)
                    .portMode(PortMode.ACCESS)
                    .portStatus(PortStatus.EMPTY_DISABLED)
                    .speedNegotiation("1 Gbps")
                    .build();
            portList.add(port);
        }
        portRepository.saveAll(portList);
    }

    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public NetworkPort getPortById(Long id) {
        return portRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Port not found with ID: " + id));
    }

    @Transactional
    public NetworkPort updatePort(PortUpdateRequest req) {
        NetworkPort port = portRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Port not found with ID: " + req.getId()));

        String oldVlan = port.getVlan();
        PortStatus oldStatus = port.getPortStatus();

        if (req.getPortNumber() != null && !req.getPortNumber().isBlank()) {
            port.setPortNumber(req.getPortNumber().trim());
        }
        if (req.getPortMode() != null) {
            port.setPortMode(req.getPortMode());
        }
        port.setVlan(req.getVlan());
        port.setIpAddress(req.getIpAddress());
        port.setMacAddress(req.getMacAddress());
        port.setHostnameOrUser(req.getHostnameOrUser());
        port.setDeviceCategory(req.getDeviceCategory());
        if (req.getSpeedNegotiation() != null && !req.getSpeedNegotiation().isBlank()) {
            port.setSpeedNegotiation(req.getSpeedNegotiation());
        }
        if (req.getPortStatus() != null) {
            port.setPortStatus(req.getPortStatus());
        } else {
            if (port.getHostnameOrUser() != null && !port.getHostnameOrUser().isBlank()) {
                port.setPortStatus(PortStatus.ACTIVE_CONNECTED);
            } else {
                port.setPortStatus(PortStatus.EMPTY_DISABLED);
            }
        }
        port.setUplink(req.isUplink());
        port.setTrunk(req.isTrunk() || port.getPortMode() == PortMode.TRUNK);
        port.setTargetDeviceName(req.getTargetDeviceName());
        port.setTargetPortName(req.getTargetPortName());
        port.setNotes(req.getNotes());

        NetworkPort saved = portRepository.save(port);

        // Record history event for manual configuration
        if (req.getVlan() != null && !req.getVlan().equals(oldVlan)) {
            portHistoryService.recordEvent(saved.getDevice(), saved, root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType.VLAN_CHANGED,
                    oldVlan, req.getVlan(), "VLAN modified manually to " + req.getVlan(), "Admin Web Console");
        } else {
            portHistoryService.recordEvent(saved.getDevice(), saved, root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType.CONFIG_UPDATE,
                    String.valueOf(oldStatus), String.valueOf(saved.getPortStatus()), "Port configuration updated via Web UI", "Admin Web Console");
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NetworkPort> searchPorts(String query) {
        if (query == null || query.trim().isBlank()) {
            return portRepository.findAll();
        }
        return portRepository.searchPorts(query.trim());
    }

    @Transactional(readOnly = true)
    public List<NetworkPort> getDegradedPorts() {
        return portRepository.findByPortStatusIn(List.of(PortStatus.PROBLEMATIC, PortStatus.FLAPPING, PortStatus.LINK_DOWN));
    }

    @Transactional
    public NetworkIssue logIssue(IssueLogRequest req) {
        NetworkDevice device = deviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + req.getDeviceId()));

        NetworkPort port = null;
        if (req.getPortId() != null) {
            port = portRepository.findById(req.getPortId()).orElse(null);
        }

        NetworkIssue issue = NetworkIssue.builder()
                .device(device)
                .port(port)
                .issueType(req.getIssueType() != null ? req.getIssueType() : IssueType.OTHER)
                .severity(req.getSeverity() != null ? req.getSeverity() : IssueSeverity.MEDIUM)
                .status(req.getStatus() != null ? req.getStatus() : IssueStatus.OPEN)
                .title(req.getTitle())
                .description(req.getDescription())
                .reportedBy(req.getReportedBy())
                .reportedAt(LocalDateTime.now())
                .build();

        issue = issueRepository.save(issue);

        // Optionally mark port as PROBLEMATIC / FLAPPING
        if (port != null && req.isMarkPortProblematic()) {
            if (req.getIssueType() == IssueType.PORT_FLAPPING) {
                port.setPortStatus(PortStatus.FLAPPING);
            } else if (req.getIssueType() == IssueType.DOWNTIME_DISCONNECTED) {
                port.setPortStatus(PortStatus.LINK_DOWN);
            } else {
                port.setPortStatus(PortStatus.PROBLEMATIC);
            }
            portRepository.save(port);
        }

        return issue;
    }

    @Transactional
    public NetworkIssue resolveIssue(IssueResolveRequest req) {
        NetworkIssue issue = issueRepository.findById(req.getIssueId())
                .orElseThrow(() -> new IllegalArgumentException("Issue not found with ID: " + req.getIssueId()));

        issue.setStatus(req.getStatus() != null ? req.getStatus() : IssueStatus.RESOLVED);
        issue.setResolvedBy(req.getResolvedBy());
        issue.setResolvedAt(LocalDateTime.now());
        issue.setRootCause(req.getRootCause());
        issue.setResolutionNotes(req.getResolutionNotes());

        issue = issueRepository.save(issue);

        // Optionally restore port status
        if (issue.getPort() != null && req.isRestorePortStatus()) {
            NetworkPort port = issue.getPort();
            port.setPortStatus(req.getNewPortStatus() != null ? req.getNewPortStatus() : PortStatus.ACTIVE_CONNECTED);
            portRepository.save(port);
        }

        return issue;
    }

    @Transactional(readOnly = true)
    public List<NetworkIssue> getAllIssues(String filterStatus) {
        if (filterStatus == null || filterStatus.isBlank() || "ALL".equalsIgnoreCase(filterStatus)) {
            return issueRepository.findAllWithDetails();
        } else if ("ACTIVE".equalsIgnoreCase(filterStatus)) {
            return issueRepository.findActiveIssues();
        } else {
            try {
                IssueStatus status = IssueStatus.valueOf(filterStatus.toUpperCase());
                return issueRepository.findByStatusOrderByReportedAtDesc(status);
            } catch (Exception e) {
                return issueRepository.findAllWithDetails();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NetworkIssue> searchIssues(String query) {
        if (query == null || query.trim().isBlank()) {
            return issueRepository.findAllWithDetails();
        }
        return issueRepository.searchIssues(query.trim());
    }

    @Transactional(readOnly = true)
    public NetworkStatsDto getStats() {
        long totalDevices = deviceRepository.count();
        long totalPorts = portRepository.count();
        long activePorts = portRepository.countActivePorts();
        long degradedPorts = portRepository.countDegradedPorts();
        long openIssues = issueRepository.countOpenIssues();
        long resolvedIssues = issueRepository.count() - openIssues;

        long managedSwitches = deviceRepository.findByDeviceType(NetworkDeviceType.MANAGED_SWITCH).size();
        long unmanagedSwitches = deviceRepository.findByDeviceType(NetworkDeviceType.UNMANAGED_SWITCH).size();
        long routers = deviceRepository.findByDeviceType(NetworkDeviceType.ROUTER_MIKROTIK).size();
        long accessPoints = deviceRepository.findByDeviceType(NetworkDeviceType.ACCESS_POINT).size();

        return NetworkStatsDto.builder()
                .totalDevices(totalDevices)
                .totalPorts(totalPorts)
                .activeConnectedPorts(activePorts)
                .degradedPorts(degradedPorts)
                .openIssues(openIssues)
                .resolvedIssues(resolvedIssues)
                .managedSwitches(managedSwitches)
                .unmanagedSwitches(unmanagedSwitches)
                .routers(routers)
                .accessPoints(accessPoints)
                .build();
    }

    public PingResultDto checkReachability(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return PingResultDto.builder().targetIp(ipAddress).reachable(false).message("Empty IP Address").build();
        }

        String target = ipAddress.trim();

        // 1. If target is a private LAN IP (e.g. 10.x.x.x, 192.168.x.x, 172.16-31.x.x) and MikroTik is configured in DB, ping via MikroTik!
        if (mikrotikSyncService != null) {
            root.cyb.mh.attendancesystem.model.MikrotikSetting s = mikrotikSyncService.getPersistentSetting();
            if (s != null && s.getHost() != null && !s.getHost().isBlank()) {
                if (target.startsWith("10.") || target.startsWith("192.168.") || target.startsWith("172.")) {
                    PingResultDto routerPing = mikrotikApiClient.pingViaRouter(
                            s.getHost(),
                            s.getPort(),
                            s.getUsername(),
                            s.getPassword() != null ? s.getPassword() : "",
                            Boolean.TRUE.equals(s.getUseSsl()),
                            target
                    );
                    if (routerPing != null) {
                        return routerPing;
                    }
                }
            }
        }

        // 2. Fallback to direct ICMP check
        long start = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(target);
            boolean reachable = address.isReachable(1500);
            long timeMs = System.currentTimeMillis() - start;
            return PingResultDto.builder()
                    .targetIp(target)
                    .reachable(reachable)
                    .responseTimeMs(timeMs)
                    .message(reachable ? "Online (" + timeMs + "ms)" : "Host unreachable / ICMP filtered")
                    .build();
        } catch (Exception e) {
            long timeMs = System.currentTimeMillis() - start;
            return PingResultDto.builder()
                    .targetIp(target)
                    .reachable(false)
                    .responseTimeMs(timeMs)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }
}
