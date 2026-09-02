package root.cyb.mh.attendancesystem.service.network;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.network.NetworkPortHistoryDto;
import root.cyb.mh.attendancesystem.dto.network.PortSessionHistoryDto;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.NetworkPortHistory;
import root.cyb.mh.attendancesystem.model.enums.PortHistoryEventType;
import root.cyb.mh.attendancesystem.model.enums.PortStatus;
import root.cyb.mh.attendancesystem.repository.NetworkDeviceRepository;
import root.cyb.mh.attendancesystem.repository.NetworkPortHistoryRepository;
import root.cyb.mh.attendancesystem.repository.NetworkPortRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkPortHistoryService {

    private final NetworkPortHistoryRepository historyRepository;
    private final NetworkPortRepository portRepository;
    private final NetworkDeviceRepository deviceRepository;

    @Transactional
    public NetworkPortHistory recordEvent(NetworkDevice device, NetworkPort port, PortHistoryEventType eventType,
                                          String oldValue, String newValue, String summary, String source) {
        if (device == null || port == null) return null;

        NetworkPortHistory history = NetworkPortHistory.builder()
                .device(device)
                .port(port)
                .eventType(eventType)
                .oldValue(oldValue)
                .newValue(newValue)
                .summary(summary != null ? summary : (eventType.name() + " on " + port.getPortNumber()))
                .source(source != null ? source : "System Poller")
                .recordedAt(LocalDateTime.now())
                .build();

        log.debug("Recording port history: {} [Port {}] -> {} ({})", device.getName(), port.getPortNumber(), eventType, summary);
        return historyRepository.save(history);
    }

    @Transactional
    public void diffAndUpdatePort(NetworkPort port, PortStatus incomingStatus, String incomingSpeed,
                                  String incomingMac, String incomingIp, String incomingHost, String source) {
        if (port == null || port.getDevice() == null) return;
        NetworkDevice device = port.getDevice();

        PortStatus oldStatus = port.getPortStatus() != null ? port.getPortStatus() : PortStatus.EMPTY_DISABLED;
        String oldSpeed = port.getSpeedNegotiation();
        String oldMac = port.getMacAddress();
        String oldIp = port.getIpAddress();
        String oldHost = port.getHostnameOrUser();

        // 1. Link Status Diff
        if (incomingStatus != null && oldStatus != incomingStatus) {
            if (incomingStatus == PortStatus.ACTIVE_CONNECTED) {
                recordEvent(device, port, PortHistoryEventType.LINK_UP,
                        oldStatus.name(), incomingStatus.name(),
                        "Physical link established at " + (incomingSpeed != null ? incomingSpeed : "1 Gbps"), source);
            } else if (incomingStatus == PortStatus.LINK_DOWN || incomingStatus == PortStatus.EMPTY_DISABLED) {
                recordEvent(device, port, PortHistoryEventType.LINK_DOWN,
                        oldStatus.name(), incomingStatus.name(),
                        "Carrier link dropped / Cable disconnected", source);
            } else if (incomingStatus == PortStatus.PROBLEMATIC || incomingStatus == PortStatus.FLAPPING) {
                recordEvent(device, port, PortHistoryEventType.FLAP_DETECTED,
                        oldStatus.name(), incomingStatus.name(),
                        "Port state changed to degraded / flapping", source);
            }
        }

        // 2. Negotiated Speed Diff
        if (incomingSpeed != null && !incomingSpeed.isBlank() && oldSpeed != null && !incomingSpeed.equalsIgnoreCase(oldSpeed)) {
            recordEvent(device, port, PortHistoryEventType.SPEED_CHANGE,
                    oldSpeed, incomingSpeed,
                    "Link speed negotiation changed from " + oldSpeed + " to " + incomingSpeed, source);
        }

        // 3. MAC Address Diff
        if (incomingMac != null && !incomingMac.isBlank()) {
            String normOldMac = oldMac != null ? oldMac.toLowerCase().trim() : "";
            String normNewMac = incomingMac.toLowerCase().trim();
            if (!normNewMac.equals(normOldMac)) {
                recordEvent(device, port, PortHistoryEventType.MAC_LEARNED,
                        oldMac, incomingMac.toUpperCase(),
                        "New device MAC learned: " + incomingMac.toUpperCase() + (incomingHost != null ? " (" + incomingHost + ")" : ""), source);
            }
        }

        // 4. IP Address Diff
        if (incomingIp != null && !incomingIp.isBlank()) {
            if (oldIp == null || !incomingIp.trim().equalsIgnoreCase(oldIp.trim())) {
                recordEvent(device, port, PortHistoryEventType.IP_CHANGED,
                        oldIp, incomingIp,
                        "IP address assigned: " + incomingIp, source);
            }
        }

        // 5. Hostname / User Diff
        if (incomingHost != null && !incomingHost.isBlank()) {
            if (oldHost == null || !incomingHost.trim().equalsIgnoreCase(oldHost.trim())) {
                recordEvent(device, port, PortHistoryEventType.USER_HOSTNAME_CHANGED,
                        oldHost, incomingHost,
                        "Host identified as: " + incomingHost, source);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NetworkPortHistoryDto> getPortHistory(Long portId, int limit) {
        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : 50);
        return historyRepository.findByPortIdOrderByRecordedAtDesc(portId, pageable)
                .getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NetworkPortHistoryDto> getDeviceHistory(Long deviceId, Pageable pageable) {
        return historyRepository.findByDeviceIdOrderByRecordedAtDesc(deviceId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NetworkPortHistoryDto> searchHistory(Long deviceId, Long portId, PortHistoryEventType eventType,
                                                     String search, LocalDateTime start, LocalDateTime end,
                                                     Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<NetworkPortHistory> spec = buildSpec(deviceId, portId, eventType, search, start, end);
        return historyRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public void seedInitialPortBaselines() {
        if (historyRepository.count() > 0) return;

        log.info("Generating initial port history baseline audit records for all ports...");
        List<NetworkPort> allPorts = portRepository.findAll();
        List<NetworkPortHistory> baselines = new ArrayList<>();

        for (NetworkPort p : allPorts) {
            if (p.getDevice() == null) continue;

            if (p.getPortStatus() == PortStatus.ACTIVE_CONNECTED) {
                baselines.add(NetworkPortHistory.builder()
                        .device(p.getDevice())
                        .port(p)
                        .eventType(PortHistoryEventType.LINK_UP)
                        .oldValue("INITIALIZING")
                        .newValue("ACTIVE_CONNECTED")
                        .summary("Initial baseline carrier link established (" + (p.getSpeedNegotiation() != null ? p.getSpeedNegotiation() : "1 Gbps") + ")")
                        .source("Initial Baseline Sync")
                        .recordedAt(LocalDateTime.now().minusHours(24))
                        .build());

                if (p.getMacAddress() != null && !p.getMacAddress().isBlank()) {
                    baselines.add(NetworkPortHistory.builder()
                            .device(p.getDevice())
                            .port(p)
                            .eventType(PortHistoryEventType.MAC_LEARNED)
                            .oldValue(null)
                            .newValue(p.getMacAddress().toUpperCase())
                            .summary("Learned active MAC " + p.getMacAddress().toUpperCase() + (p.getHostnameOrUser() != null ? " (" + p.getHostnameOrUser() + ")" : ""))
                            .source("Initial Baseline Sync")
                            .recordedAt(LocalDateTime.now().minusHours(23))
                            .build());
                }
            } else {
                baselines.add(NetworkPortHistory.builder()
                        .device(p.getDevice())
                        .port(p)
                        .eventType(PortHistoryEventType.LINK_DOWN)
                        .oldValue("INITIALIZING")
                        .newValue("LINK_DOWN")
                        .summary("Port idle / no cable carrier detected")
                        .source("Initial Baseline Sync")
                        .recordedAt(LocalDateTime.now().minusHours(24))
                        .build());
            }
        }

        if (!baselines.isEmpty()) {
            historyRepository.saveAll(baselines);
            log.info("Saved {} baseline port history audit entries.", baselines.size());
        }
    }

    public String generateHistoryCsv(Long deviceId, Long portId, PortHistoryEventType eventType,
                                     String search, LocalDateTime start, LocalDateTime end) {
        org.springframework.data.jpa.domain.Specification<NetworkPortHistory> spec = buildSpec(deviceId, portId, eventType, search, start, end);
        Page<NetworkPortHistory> records = historyRepository.findAll(spec, PageRequest.of(0, 10000));

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,Device Name,Port Number,Event Type,Old Value,New Value,Summary,Source\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (NetworkPortHistory h : records.getContent()) {
            sb.append("\"").append(h.getRecordedAt() != null ? h.getRecordedAt().format(fmt) : "").append("\",");
            sb.append("\"").append(escapeCsv(h.getDevice() != null ? h.getDevice().getName() : "")).append("\",");
            sb.append("\"").append(escapeCsv(h.getPort() != null ? h.getPort().getPortNumber() : "")).append("\",");
            sb.append("\"").append(h.getEventType() != null ? h.getEventType().name() : "").append("\",");
            sb.append("\"").append(escapeCsv(h.getOldValue())).append("\",");
            sb.append("\"").append(escapeCsv(h.getNewValue())).append("\",");
            sb.append("\"").append(escapeCsv(h.getSummary())).append("\",");
            sb.append("\"").append(escapeCsv(h.getSource())).append("\"\n");
        }
        return sb.toString();
    }

    private org.springframework.data.jpa.domain.Specification<NetworkPortHistory> buildSpec(
            Long deviceId, Long portId, PortHistoryEventType eventType,
            String search, LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (deviceId != null) {
                predicates.add(cb.equal(root.get("device").get("id"), deviceId));
            }
            if (portId != null) {
                predicates.add(cb.equal(root.get("port").get("id"), portId));
            }
            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordedAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordedAt"), end));
            }
            if (search != null && !search.trim().isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate summaryMatch = cb.like(cb.lower(root.get("summary")), pattern);
                jakarta.persistence.criteria.Predicate portMatch = cb.like(cb.lower(root.get("port").get("portNumber")), pattern);
                jakarta.persistence.criteria.Predicate devMatch = cb.like(cb.lower(root.get("device").get("name")), pattern);
                predicates.add(cb.or(summaryMatch, portMatch, devMatch));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("recordedAt")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    @Transactional(readOnly = true)
    public List<PortSessionHistoryDto> getPortLifecycleSessions(Long portId) {
        NetworkPort port = portRepository.findById(portId).orElse(null);
        if (port == null) return List.of();

        List<NetworkPortHistory> events = historyRepository.findByPortIdOrderByRecordedAtDesc(portId);
        List<PortSessionHistoryDto> sessions = new ArrayList<>();

        // 1. If currently connected, add current active session at the top
        if (port.getPortStatus() == PortStatus.ACTIVE_CONNECTED && (port.getMacAddress() != null || port.getIpAddress() != null || port.getHostnameOrUser() != null)) {
            LocalDateTime connectedAt = null;
            for (NetworkPortHistory ev : events) {
                if (ev.getEventType() == PortHistoryEventType.LINK_DOWN) {
                    break;
                }
                if (ev.getEventType() == PortHistoryEventType.LINK_UP || ev.getEventType() == PortHistoryEventType.MAC_LEARNED) {
                    connectedAt = ev.getRecordedAt();
                }
            }
            if (connectedAt == null) {
                connectedAt = port.getUpdatedAt() != null ? port.getUpdatedAt() : (port.getCreatedAt() != null ? port.getCreatedAt() : LocalDateTime.now().minusHours(24));
            }

            sessions.add(PortSessionHistoryDto.builder()
                    .hostnameOrUser(port.getHostnameOrUser() != null && !port.getHostnameOrUser().isBlank() ? port.getHostnameOrUser() : "Active Host")
                    .deviceCategory(port.getDeviceCategory() != null && !port.getDeviceCategory().isBlank() ? port.getDeviceCategory() : "Workstation / Network Device")
                    .ipAddress(port.getIpAddress() != null ? port.getIpAddress() : "—")
                    .macAddress(port.getMacAddress() != null ? port.getMacAddress() : "—")
                    .vlan(port.getVlan() != null ? ("VLAN " + port.getVlan()) : "Default")
                    .speed(port.getSpeedNegotiation() != null ? port.getSpeedNegotiation() : "1 Gbps")
                    .connectedAt(connectedAt)
                    .releasedAt(null)
                    .durationFormatted(formatDuration(connectedAt, LocalDateTime.now()))
                    .releaseReason("Currently Active & Connected (🟢 Live)")
                    .active(true)
                    .build());
        }

        // 2. Identify previous sessions from event logs
        for (int i = 0; i < events.size(); i++) {
            NetworkPortHistory ev = events.get(i);
            
            if (ev.getEventType() == PortHistoryEventType.LINK_DOWN && ev.getOldValue() != null && !ev.getOldValue().equals("INITIALIZING")) {
                LocalDateTime releaseTime = ev.getRecordedAt();
                LocalDateTime startTime = releaseTime.minusHours(4);
                for (int j = i + 1; j < events.size(); j++) {
                    NetworkPortHistory prev = events.get(j);
                    if (prev.getEventType() == PortHistoryEventType.LINK_UP) {
                        startTime = prev.getRecordedAt();
                        break;
                    }
                }

                sessions.add(PortSessionHistoryDto.builder()
                        .hostnameOrUser("Previous Connection")
                        .deviceCategory("Network Device / Cable Session")
                        .ipAddress("—")
                        .macAddress("—")
                        .vlan(port.getVlan() != null ? ("VLAN " + port.getVlan()) : "—")
                        .speed(port.getSpeedNegotiation() != null ? port.getSpeedNegotiation() : "1 Gbps")
                        .connectedAt(startTime)
                        .releasedAt(releaseTime)
                        .durationFormatted(formatDuration(startTime, releaseTime))
                        .releaseReason("Cable Disconnected / Carrier Link Dropped")
                        .active(false)
                        .build());
            } else if ((ev.getEventType() == PortHistoryEventType.MAC_LEARNED || ev.getEventType() == PortHistoryEventType.MAC_REMOVED) && ev.getOldValue() != null && !ev.getOldValue().isBlank()) {
                LocalDateTime releaseTime = ev.getRecordedAt();
                LocalDateTime startTime = releaseTime.minusHours(6);
                for (int j = i + 1; j < events.size(); j++) {
                    NetworkPortHistory prev = events.get(j);
                    if (prev.getEventType() == PortHistoryEventType.MAC_LEARNED && Objects.equals(prev.getNewValue(), ev.getOldValue())) {
                        startTime = prev.getRecordedAt();
                        break;
                    }
                }

                sessions.add(PortSessionHistoryDto.builder()
                        .hostnameOrUser("Previous Device")
                        .deviceCategory("Workstation / Client Device")
                        .ipAddress("—")
                        .macAddress(ev.getOldValue())
                        .vlan(port.getVlan() != null ? ("VLAN " + port.getVlan()) : "—")
                        .speed(port.getSpeedNegotiation() != null ? port.getSpeedNegotiation() : "1 Gbps")
                        .connectedAt(startTime)
                        .releasedAt(releaseTime)
                        .durationFormatted(formatDuration(startTime, releaseTime))
                        .releaseReason("MAC Departed / Roamed to Another Port")
                        .active(false)
                        .build());
            } else if (ev.getEventType() == PortHistoryEventType.IP_CHANGED && ev.getOldValue() != null && !ev.getOldValue().isBlank()) {
                LocalDateTime releaseTime = ev.getRecordedAt();
                LocalDateTime startTime = releaseTime.minusHours(8);
                sessions.add(PortSessionHistoryDto.builder()
                        .hostnameOrUser(port.getHostnameOrUser() != null ? port.getHostnameOrUser() : "Workstation")
                        .deviceCategory("DHCP Client")
                        .ipAddress(ev.getOldValue())
                        .macAddress(port.getMacAddress() != null ? port.getMacAddress() : "—")
                        .vlan(port.getVlan() != null ? ("VLAN " + port.getVlan()) : "—")
                        .speed(port.getSpeedNegotiation() != null ? port.getSpeedNegotiation() : "1 Gbps")
                        .connectedAt(startTime)
                        .releasedAt(releaseTime)
                        .durationFormatted(formatDuration(startTime, releaseTime))
                        .releaseReason("IP Lease Expired / Re-assigned via DHCP")
                        .active(false)
                        .build());
            } else if (ev.getEventType() == PortHistoryEventType.USER_HOSTNAME_CHANGED && ev.getOldValue() != null && !ev.getOldValue().isBlank()) {
                LocalDateTime releaseTime = ev.getRecordedAt();
                LocalDateTime startTime = releaseTime.minusDays(1);
                sessions.add(PortSessionHistoryDto.builder()
                        .hostnameOrUser(ev.getOldValue())
                        .deviceCategory("Assigned User / Persona")
                        .ipAddress(port.getIpAddress() != null ? port.getIpAddress() : "—")
                        .macAddress(port.getMacAddress() != null ? port.getMacAddress() : "—")
                        .vlan(port.getVlan() != null ? ("VLAN " + port.getVlan()) : "—")
                        .speed(port.getSpeedNegotiation() != null ? port.getSpeedNegotiation() : "1 Gbps")
                        .connectedAt(startTime)
                        .releasedAt(releaseTime)
                        .durationFormatted(formatDuration(startTime, releaseTime))
                        .releaseReason("User / Hostname Reassigned")
                        .active(false)
                        .build());
            }
        }

        return sessions;
    }

    public String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "—";
        java.time.Duration d = java.time.Duration.between(start, end);
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();

        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return Math.max(1, minutes) + " mins";
        }
    }

    private NetworkPortHistoryDto toDto(NetworkPortHistory h) {
        return NetworkPortHistoryDto.builder()
                .id(h.getId())
                .deviceId(h.getDevice() != null ? h.getDevice().getId() : null)
                .deviceName(h.getDevice() != null ? h.getDevice().getName() : null)
                .portId(h.getPort() != null ? h.getPort().getId() : null)
                .portNumber(h.getPort() != null ? h.getPort().getPortNumber() : null)
                .eventType(h.getEventType())
                .oldValue(h.getOldValue())
                .newValue(h.getNewValue())
                .summary(h.getSummary())
                .source(h.getSource())
                .recordedAt(h.getRecordedAt())
                .build();
    }
}
