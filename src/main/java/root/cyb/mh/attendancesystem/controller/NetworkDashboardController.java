package root.cyb.mh.attendancesystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.NetworkIssue;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.service.NetworkManagerService;

import java.util.List;

@Controller
@RequestMapping("/network")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class NetworkDashboardController {

    private final NetworkManagerService networkService;
    private final root.cyb.mh.attendancesystem.service.network.MikrotikSyncService mikrotikSyncService;
    private final root.cyb.mh.attendancesystem.service.network.NetworkPortHistoryService portHistoryService;
    private final root.cyb.mh.attendancesystem.service.network.NetworkAdvancedTelemetryService telemetryService;
    private final root.cyb.mh.attendancesystem.repository.NetworkVlanRepository vlanRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String viewDashboard(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "deviceTab", required = false) Long activeDeviceId,
            Model model) {

        List<NetworkDevice> devices = networkService.getAllDevices();
        model.addAttribute("devices", devices);
        model.addAttribute("stats", networkService.getStats());
        model.addAttribute("wifiClients", telemetryService.getRealWifiClients(null));
        model.addAttribute("vlans", vlanRepository.findAllByOrderByVlanIdAsc());

        // Default to first managed switch if no activeDeviceId is passed
        if (activeDeviceId == null && !devices.isEmpty()) {
            activeDeviceId = devices.stream()
                    .filter(d -> d.getDeviceType() == NetworkDeviceType.MANAGED_SWITCH)
                    .map(NetworkDevice::getId)
                    .findFirst()
                    .orElse(devices.get(0).getId());
        }
        model.addAttribute("activeDeviceId", activeDeviceId);

        // Issues
        List<NetworkIssue> recentIssues = networkService.getAllIssues("ALL");
        model.addAttribute("recentIssues", recentIssues);

        // Degraded ports
        List<NetworkPort> degradedPorts = networkService.getDegradedPorts();
        model.addAttribute("degradedPorts", degradedPorts);

        // Enums for modals
        model.addAttribute("deviceTypes", NetworkDeviceType.values());
        model.addAttribute("portModes", PortMode.values());
        model.addAttribute("portStatuses", PortStatus.values());
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("issueSeverities", IssueSeverity.values());
        model.addAttribute("issueStatuses", IssueStatus.values());

        model.addAttribute("activeLink", "network-manager");
        return "network/dashboard";
    }

    @GetMapping("/ports/{portId}")
    public String viewPortProfile(@org.springframework.web.bind.annotation.PathVariable Long portId, Model model) {
        NetworkPort port = networkService.getPortById(portId);
        if (port == null) {
            return "redirect:/network";
        }

        NetworkDevice device = port.getDevice();
        List<NetworkPort> allDevicePorts = device != null ? device.getPorts() : List.of();

        // Calculate Prev / Next ports
        NetworkPort prevPort = null;
        NetworkPort nextPort = null;
        if (allDevicePorts != null && !allDevicePorts.isEmpty()) {
            for (int i = 0; i < allDevicePorts.size(); i++) {
                if (allDevicePorts.get(i).getId().equals(portId)) {
                    if (i > 0) prevPort = allDevicePorts.get(i - 1);
                    if (i < allDevicePorts.size() - 1) nextPort = allDevicePorts.get(i + 1);
                    break;
                }
            }
        }

        var sessions = portHistoryService.getPortLifecycleSessions(portId);
        var historyList = portHistoryService.getPortHistory(portId, 100);

        model.addAttribute("port", port);
        model.addAttribute("device", device);
        model.addAttribute("prevPort", prevPort);
        model.addAttribute("nextPort", nextPort);
        model.addAttribute("allDevicePorts", allDevicePorts);
        model.addAttribute("sessions", sessions);
        model.addAttribute("historyList", historyList);
        model.addAttribute("issues", port.getIssues() != null ? port.getIssues() : List.of());

        model.addAttribute("portModes", PortMode.values());
        model.addAttribute("portStatuses", PortStatus.values());
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("issueSeverities", IssueSeverity.values());
        model.addAttribute("issueStatuses", IssueStatus.values());

        model.addAttribute("activeLink", "network-manager");
        return "network/port-details";
    }

    @GetMapping("/mikrotik")
    public String viewMikrotikSettings(Model model) {
        model.addAttribute("config", mikrotikSyncService.getConfig());
        
        List<NetworkDevice> routers = networkService.getAllDevices().stream()
                .filter(d -> d.getDeviceType() == NetworkDeviceType.ROUTER_MIKROTIK)
                .toList();
        model.addAttribute("routerDevice", !routers.isEmpty() ? routers.get(0) : null);
        model.addAttribute("stats", networkService.getStats());
        model.addAttribute("activeLink", "network-manager");
        return "network/mikrotik-settings";
    }
}
