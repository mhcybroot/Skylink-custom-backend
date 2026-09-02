package root.cyb.mh.attendancesystem.service.network;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.network.DevicePingStatusDto;
import root.cyb.mh.attendancesystem.dto.network.TopBandwidthUserDto;
import root.cyb.mh.attendancesystem.dto.network.WatchdogAlertDto;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.enums.PortMode;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;
import root.cyb.mh.attendancesystem.repository.NetworkPortRepository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkWatchdogService {

    private final NetworkAdvancedTelemetryService telemetryService;
    private final NetworkPingService pingService;
    private final NetworkPortRepository portRepository;

    public List<WatchdogAlertDto> getActiveAlerts() {
        List<WatchdogAlertDto> alerts = new ArrayList<>();
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // 1. Check Offline Core Nodes / Switches / APs via Ping Monitor
        try {
            List<DevicePingStatusDto> pings = pingService.pingAllTargets();
            for (DevicePingStatusDto p : pings) {
                if (!p.isOnline() && !"WAN_GATEWAY".equals(p.getDeviceCategory())) {
                    alerts.add(WatchdogAlertDto.builder()
                            .id("offline-" + p.getIpAddress())
                            .alertType("OFFLINE_NODE")
                            .severity("CRITICAL")
                            .severityBadgeClass("bg-danger")
                            .title("Infrastructure Node Unreachable")
                            .message("Device " + p.getDeviceName() + " (" + p.getIpAddress() + ") is offline / non-responsive to ICMP.")
                            .targetDeviceOrUser(p.getDeviceName())
                            .ipAddress(p.getIpAddress())
                            .timestamp(timeStr)
                            .actionUrl("/network")
                            .build());
                } else if (p.getResponseTimeMs() > 100.0) {
                    alerts.add(WatchdogAlertDto.builder()
                            .id("latency-" + p.getIpAddress())
                            .alertType("LATENCY_SPIKE")
                            .severity("MEDIUM")
                            .severityBadgeClass("bg-warning text-dark")
                            .title("High Latency Detected")
                            .message("Response time to " + p.getDeviceName() + " is currently " + p.getResponseTimeMs() + " ms.")
                            .targetDeviceOrUser(p.getDeviceName())
                            .ipAddress(p.getIpAddress())
                            .timestamp(timeStr)
                            .actionUrl("/network")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error evaluating ping alerts: {}", e.getMessage());
        }

        // 2. Check Bandwidth & Socket Floods
        try {
            List<TopBandwidthUserDto> topUsers = telemetryService.getTopBandwidthUsers(10);
            for (TopBandwidthUserDto u : topUsers) {
                if (u.getActiveConnections() != null && u.getActiveConnections() >= 100) {
                    alerts.add(WatchdogAlertDto.builder()
                            .id("socket-flood-" + u.getIpAddress())
                            .alertType("BANDWIDTH_SPIKE")
                            .severity("HIGH")
                            .severityBadgeClass("bg-danger")
                            .title("Excessive Socket Load")
                            .message("Host " + u.getHostnameOrUser() + " has " + u.getActiveConnections() + " open firewall connections.")
                            .targetDeviceOrUser(u.getHostnameOrUser())
                            .ipAddress(u.getIpAddress())
                            .timestamp(timeStr)
                            .actionUrl("/network")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error evaluating bandwidth alerts: {}", e.getMessage());
        }

        // 3. Check Critical Trunk & Uplink Ports
        try {
            List<NetworkPort> allPorts = portRepository.findAll();
            for (NetworkPort port : allPorts) {
                if ((port.getPortMode() == PortMode.TRUNK || port.isUplink())
                        && (port.getPortStatus() == PortStatus.LINK_DOWN || port.getPortStatus() == PortStatus.PROBLEMATIC)) {
                    alerts.add(WatchdogAlertDto.builder()
                            .id("port-down-" + port.getId())
                            .alertType("PORT_DOWN")
                            .severity("HIGH")
                            .severityBadgeClass("bg-danger")
                            .title("Critical Uplink / Trunk Port Down")
                            .message("Port " + port.getPortNumber() + " on " + (port.getDevice() != null ? port.getDevice().getName() : "Switch") + " is DOWN.")
                            .targetDeviceOrUser(port.getHostnameOrUser() != null ? port.getHostnameOrUser() : port.getPortNumber())
                            .ipAddress(port.getIpAddress())
                            .timestamp(timeStr)
                            .actionUrl("/network")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Error evaluating port alerts: {}", e.getMessage());
        }

        return alerts;
    }
}
