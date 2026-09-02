package root.cyb.mh.attendancesystem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.dto.network.*;
import root.cyb.mh.attendancesystem.service.network.NetworkAdvancedTelemetryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkAdvancedApiController {

    private final NetworkAdvancedTelemetryService telemetryService;
    private final root.cyb.mh.attendancesystem.service.network.NetworkPingService pingService;
    private final root.cyb.mh.attendancesystem.service.network.NetworkWatchdogService watchdogService;

    @GetMapping("/telemetry/latency")
    public ResponseEntity<List<DevicePingStatusDto>> getDeviceLatency() {
        return ResponseEntity.ok(pingService.pingAllTargets());
    }

    @GetMapping("/telemetry/watchdog-alerts")
    public ResponseEntity<List<WatchdogAlertDto>> getWatchdogAlerts() {
        return ResponseEntity.ok(watchdogService.getActiveAlerts());
    }

    @GetMapping("/telemetry/hardware")
    public ResponseEntity<List<NetworkDeviceTelemetryDto>> getHardwareTelemetry() {
        return ResponseEntity.ok(telemetryService.getAllHardwareTelemetry());
    }

    @GetMapping("/telemetry/top-consumers")
    public ResponseEntity<List<TopBandwidthUserDto>> getTopConsumers(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(telemetryService.getTopBandwidthUsers(limit));
    }

    @GetMapping("/telemetry/topology")
    public ResponseEntity<NetworkTopologyGraphDto> getTopology() {
        return ResponseEntity.ok(telemetryService.getNetworkTopology());
    }

    @PostMapping("/ports/{id}/tdr-test")
    public ResponseEntity<CableDiagnosticResultDto> runTdrTest(@PathVariable Long id) {
        return ResponseEntity.ok(telemetryService.runCableDiagnostic(id));
    }

    @PostMapping("/ports/{id}/poe-cycle")
    public ResponseEntity<Map<String, Object>> cyclePoe(@PathVariable Long id) {
        return ResponseEntity.ok(telemetryService.cyclePoePower(id));
    }

    @GetMapping("/firewall/connections")
    public ResponseEntity<List<FirewallConnectionDto>> getFirewallConnections(@RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(telemetryService.getFirewallConnections(limit));
    }

    @GetMapping("/wifi/clients")
    public ResponseEntity<List<WifiClientDto>> getWifiClients(@RequestParam(required = false) String apIp) {
        return ResponseEntity.ok(telemetryService.getRealWifiClients(apIp));
    }

    @GetMapping("/telemetry/visited-urls")
    public ResponseEntity<List<LiveUrlActivityDto>> getVisitedUrls(
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(telemetryService.getLiveVisitedUrls(limit, search));
    }
}
