package root.cyb.mh.attendancesystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.dto.network.*;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.NetworkIssue;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.service.NetworkManagerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class NetworkApiController {

    private final NetworkManagerService networkService;
    private final root.cyb.mh.attendancesystem.service.network.MikrotikSyncService mikrotikSyncService;
    private final root.cyb.mh.attendancesystem.service.network.CiscoSwitchSyncService ciscoSwitchSyncService;
    private final root.cyb.mh.attendancesystem.service.network.NetworkPortHistoryService portHistoryService;
    private final root.cyb.mh.attendancesystem.repository.NetworkVlanRepository vlanRepository;

    @GetMapping("/stats")
    public ResponseEntity<NetworkStatsDto> getStats() {
        return ResponseEntity.ok(networkService.getStats());
    }

    @GetMapping("/devices")
    public ResponseEntity<List<NetworkDevice>> getDevices() {
        return ResponseEntity.ok(networkService.getAllDevices());
    }

    @GetMapping("/devices/{id}")
    public ResponseEntity<NetworkDevice> getDevice(@PathVariable Long id) {
        return ResponseEntity.ok(networkService.getDeviceById(id));
    }

    @PostMapping("/devices")
    public ResponseEntity<Map<String, Object>> saveDevice(@RequestBody DeviceSaveRequest req) {
        NetworkDevice device = networkService.saveDevice(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Device saved successfully");
        resp.put("device", device);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable Long id) {
        networkService.deleteDevice(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Device deleted successfully");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/ports/{id}")
    public ResponseEntity<NetworkPort> getPort(@PathVariable Long id) {
        return ResponseEntity.ok(networkService.getPortById(id));
    }

    @PostMapping("/ports/{id}")
    @PutMapping("/ports/{id}")
    public ResponseEntity<Map<String, Object>> updatePort(@PathVariable Long id, @RequestBody PortUpdateRequest req) {
        req.setId(id);
        NetworkPort port = networkService.updatePort(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Port updated successfully");
        resp.put("port", port);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/search")
    public ResponseEntity<List<NetworkPort>> searchPorts(@RequestParam(value = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(networkService.searchPorts(query));
    }

    @GetMapping("/issues")
    public ResponseEntity<List<NetworkIssue>> getIssues(@RequestParam(value = "status", defaultValue = "ALL") String status) {
        return ResponseEntity.ok(networkService.getAllIssues(status));
    }

    @PostMapping("/issues")
    public ResponseEntity<Map<String, Object>> logIssue(@RequestBody IssueLogRequest req, Authentication auth) {
        if (req.getReportedBy() == null || req.getReportedBy().isBlank()) {
            req.setReportedBy(auth != null ? auth.getName() : "Admin");
        }
        NetworkIssue issue = networkService.logIssue(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Issue logged successfully");
        resp.put("issue", issue);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/issues/resolve")
    public ResponseEntity<Map<String, Object>> resolveIssue(@RequestBody IssueResolveRequest req, Authentication auth) {
        if (req.getResolvedBy() == null || req.getResolvedBy().isBlank()) {
            req.setResolvedBy(auth != null ? auth.getName() : "Admin");
        }
        NetworkIssue issue = networkService.resolveIssue(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Issue marked as resolved");
        resp.put("issue", issue);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/ping")
    public ResponseEntity<PingResultDto> ping(@RequestParam("ip") String ip) {
        return ResponseEntity.ok(networkService.checkReachability(ip));
    }

    @PostMapping("/mikrotik/sync")
    public ResponseEntity<root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikSyncResultDto> syncMikrotik() {
        return ResponseEntity.ok(mikrotikSyncService.syncLive());
    }

    @PostMapping("/mikrotik/test-connection")
    public ResponseEntity<Map<String, Object>> testMikrotikConnection(@RequestBody root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikConnectionTestRequest req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Map<String, Object> resource = mikrotikSyncService.testConnection(req);
            resp.put("success", true);
            resp.put("message", "Connection successful! RouterOS " + resource.get("version") + " (Uptime: " + resource.get("uptime") + ")");
            resp.put("resource", resource);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "Connection failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/mikrotik/config")
    public ResponseEntity<root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikConfigDto> getMikrotikConfig() {
        return ResponseEntity.ok(mikrotikSyncService.getConfig());
    }

    @PostMapping("/mikrotik/config")
    public ResponseEntity<Map<String, Object>> updateMikrotikConfig(@RequestBody root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikConfigDto dto) {
        mikrotikSyncService.updateConfig(dto);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "MikroTik configuration updated successfully");
        resp.put("config", mikrotikSyncService.getConfig());
        return ResponseEntity.ok(resp);
    }
    @GetMapping("/mikrotik/leases")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikEnrichedLeaseDto>> getMikrotikLeases(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(mikrotikSyncService.getEnrichedLeases(search));
    }

    @GetMapping("/mikrotik/arp")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikArpDto>> getMikrotikArp(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(mikrotikSyncService.getArpEntries(search));
    }

    @GetMapping("/mikrotik/interfaces")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikInterfaceDto>> getMikrotikInterfaces(
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(mikrotikSyncService.getCachedInterfaces(search));
    }

    @PostMapping("/mikrotik/map-lease")
    public ResponseEntity<Map<String, Object>> mapLeaseToPort(
            @RequestBody root.cyb.mh.attendancesystem.dto.network.mikrotik.MapLeaseRequest req) {
        NetworkPort updatedPort = mikrotikSyncService.mapLeaseToPort(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Lease successfully mapped to port " + updatedPort.getPortNumber());
        resp.put("port", updatedPort);
        return ResponseEntity.ok(resp);
    }

    // --- Cisco Switch Management & Sync Endpoints ---

    @PostMapping("/cisco/sync/{deviceId}")
    public ResponseEntity<root.cyb.mh.attendancesystem.dto.network.cisco.CiscoSyncResultDto> syncCiscoSwitch(
            @PathVariable("deviceId") Long deviceId) {
        return ResponseEntity.ok(ciscoSwitchSyncService.syncSwitch(deviceId));
    }

    @PostMapping("/cisco/sync-all")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.dto.network.cisco.CiscoSyncResultDto>> syncAllCiscoSwitches() {
        return ResponseEntity.ok(ciscoSwitchSyncService.syncAllManagedSwitches());
    }

    @PostMapping("/cisco/test-connection")
    public ResponseEntity<Map<String, Object>> testCiscoConnection(
            @RequestBody root.cyb.mh.attendancesystem.dto.network.cisco.CiscoTestConnectionRequest req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Map<String, Object> test = ciscoSwitchSyncService.testConnection(req);
            resp.put("success", Boolean.TRUE.equals(test.get("connected")));
            resp.put("message", Boolean.TRUE.equals(test.get("connected")) 
                    ? "Connection successful! " + test.getOrDefault("sysDescr", test.getOrDefault("rawVersion", "")) 
                    : String.valueOf(test.getOrDefault("error", "Switch unreachable")));
            resp.put("details", test);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "Connection failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/cisco/config/{deviceId}")
    public ResponseEntity<Map<String, Object>> updateCiscoConfig(
            @PathVariable("deviceId") Long deviceId,
            @RequestBody DeviceSaveRequest req) {
        req.setId(deviceId);
        NetworkDevice existing = networkService.getDeviceById(deviceId);
        if (req.getName() == null || req.getName().isBlank()) req.setName(existing.getName());
        if (req.getDeviceType() == null) req.setDeviceType(existing.getDeviceType());
        if (req.getLocation() == null) req.setLocation(existing.getLocation());
        if (req.getTotalPorts() <= 0) req.setTotalPorts(existing.getTotalPorts());
        if (req.getStatus() == null) req.setStatus(existing.getStatus());
        if (req.getModelVendor() == null) req.setModelVendor(existing.getModelVendor());
        NetworkDevice device = networkService.saveDevice(req);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Switch configuration saved successfully");
        resp.put("device", device);
        return ResponseEntity.ok(resp);
    }

    // --- Port History & Infrastructure Audit Endpoints ---

    @GetMapping("/ports/{id}/history")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.dto.network.NetworkPortHistoryDto>> getPortHistory(
            @PathVariable("id") Long id,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(portHistoryService.getPortHistory(id, limit));
    }

    @GetMapping("/devices/{deviceId}/history")
    public ResponseEntity<org.springframework.data.domain.Page<root.cyb.mh.attendancesystem.dto.network.NetworkPortHistoryDto>> getDeviceHistory(
            @PathVariable("deviceId") Long deviceId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(portHistoryService.getDeviceHistory(deviceId, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/history")
    public ResponseEntity<org.springframework.data.domain.Page<root.cyb.mh.attendancesystem.dto.network.NetworkPortHistoryDto>> searchHistory(
            @RequestParam(value = "deviceId", required = false) Long deviceId,
            @RequestParam(value = "portId", required = false) Long portId,
            @RequestParam(value = "eventType", required = false) root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType eventType,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        return ResponseEntity.ok(portHistoryService.searchHistory(deviceId, portId, eventType, search, startDate, endDate, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportHistoryCsv(
            @RequestParam(value = "deviceId", required = false) Long deviceId,
            @RequestParam(value = "portId", required = false) Long portId,
            @RequestParam(value = "eventType", required = false) root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType eventType,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        String csv = portHistoryService.generateHistoryCsv(deviceId, portId, eventType, search, startDate, endDate);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"port-history-audit-" + java.time.LocalDate.now() + ".csv\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    // ==================== VLAN DIRECTORY & MANAGEMENT ====================

    @GetMapping("/vlans")
    public ResponseEntity<List<root.cyb.mh.attendancesystem.model.NetworkVlan>> getAllVlans() {
        return ResponseEntity.ok(vlanRepository.findAllByOrderByVlanIdAsc());
    }

    @PostMapping("/vlans")
    public ResponseEntity<Map<String, Object>> saveOrUpdateVlan(@RequestBody VlanRequestDto req) {
        Map<String, Object> resp = new HashMap<>();
        if (req.getVlanId() == null || req.getVlanId() < 1 || req.getVlanId() > 4094) {
            resp.put("success", false);
            resp.put("message", "VLAN ID must be between 1 and 4094");
            return ResponseEntity.badRequest().body(resp);
        }
        if (req.getName() == null || req.getName().isBlank()) {
            resp.put("success", false);
            resp.put("message", "VLAN Name cannot be empty");
            return ResponseEntity.badRequest().body(resp);
        }

        root.cyb.mh.attendancesystem.model.NetworkVlan vlan;
        if (req.getId() != null) {
            vlan = vlanRepository.findById(req.getId()).orElse(null);
            if (vlan == null) {
                resp.put("success", false);
                resp.put("message", "VLAN not found with ID: " + req.getId());
                return ResponseEntity.badRequest().body(resp);
            }
        } else {
            vlan = vlanRepository.findByVlanId(req.getVlanId()).orElse(new root.cyb.mh.attendancesystem.model.NetworkVlan());
        }

        vlan.setVlanId(req.getVlanId());
        vlan.setName(req.getName().trim());
        vlan.setSubnet(req.getSubnet() != null ? req.getSubnet().trim() : null);
        vlan.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        vlan.setBadgeColor(req.getBadgeColor() != null && !req.getBadgeColor().isBlank() ? req.getBadgeColor().trim() : "primary");

        root.cyb.mh.attendancesystem.model.NetworkVlan saved = vlanRepository.save(vlan);
        resp.put("success", true);
        resp.put("message", "VLAN " + saved.getVlanId() + " saved successfully");
        resp.put("vlan", saved);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/vlans/{id}")
    public ResponseEntity<Map<String, Object>> deleteVlan(@PathVariable("id") Long id) {
        Map<String, Object> resp = new HashMap<>();
        if (!vlanRepository.existsById(id)) {
            resp.put("success", false);
            resp.put("message", "VLAN not found");
            return ResponseEntity.badRequest().body(resp);
        }
        vlanRepository.deleteById(id);
        resp.put("success", true);
        resp.put("message", "VLAN deleted successfully");
        return ResponseEntity.ok(resp);
    }
}
