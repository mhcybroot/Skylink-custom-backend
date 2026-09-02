package root.cyb.mh.attendancesystem.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.model.NetworkDevice;
import root.cyb.mh.attendancesystem.model.NetworkPort;
import root.cyb.mh.attendancesystem.model.NetworkVlan;
import root.cyb.mh.attendancesystem.model.enums.*;
import root.cyb.mh.attendancesystem.repository.NetworkDeviceRepository;
import root.cyb.mh.attendancesystem.repository.NetworkIssueRepository;
import root.cyb.mh.attendancesystem.repository.NetworkPortRepository;
import root.cyb.mh.attendancesystem.repository.NetworkVlanRepository;
import root.cyb.mh.attendancesystem.service.network.NetworkPortHistoryService;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class NetworkDataInitializer implements CommandLineRunner {

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkPortRepository portRepository;
    private final NetworkIssueRepository issueRepository;
    private final NetworkPortHistoryService portHistoryService;
    private final NetworkVlanRepository vlanRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking Network Infrastructure Configuration & Purging Demo Data...");

        // 0. Seed default VLAN directory if empty
        if (vlanRepository.count() == 0) {
            log.info("Seeding default network VLAN directory...");
            List<NetworkVlan> initialVlans = List.of(
                    NetworkVlan.builder().vlanId(1).name("Default / Native").subnet("10.10.1.0/24").description("Default Management & Native VLAN").badgeColor("secondary").build(),
                    NetworkVlan.builder().vlanId(10).name("Workstations").subnet("10.10.10.0/24").description("Corporate Workstations & Employee Desktops").badgeColor("success").build(),
                    NetworkVlan.builder().vlanId(11).name("Guest Wi-Fi").subnet("10.10.11.0/24").description("Isolated Guest Wi-Fi Network").badgeColor("warning").build(),
                    NetworkVlan.builder().vlanId(12).name("Security / Cameras").subnet("10.10.12.0/24").description("Cisco & Grandstream Security Cameras & IoT").badgeColor("danger").build(),
                    NetworkVlan.builder().vlanId(15).name("VoIP & Management").subnet("10.10.15.0/24").description("VoIP Phones & Network Device Management").badgeColor("primary").build(),
                    NetworkVlan.builder().vlanId(16).name("Server Infrastructure").subnet("10.10.16.0/24").description("Core Switches, Database & Backend Servers").badgeColor("purple").build()
            );
            vlanRepository.saveAll(initialVlans);
        }

        // 1. Remove all demo/dummy issues from the issue ledger
        issueRepository.deleteAll();

        // 2. Remove any fake demo devices (e.g., Unmanaged switch)
        List<NetworkDevice> allDevs = deviceRepository.findAll();
        for (NetworkDevice dev : allDevs) {
            if (dev.getDeviceType() == NetworkDeviceType.UNMANAGED_SWITCH ||
                dev.getName().contains("Unmanaged") ||
                dev.getName().contains("Lab") ||
                dev.getName().contains("Bench")) {
                log.info("Purging demo network device: {}", dev.getName());
                deviceRepository.delete(dev);
            }
        }

        // 3. Ensure MikroTik Core Gateway exists with real settings
        NetworkDevice mikrotik = deviceRepository.findByDeviceType(NetworkDeviceType.ROUTER_MIKROTIK)
                .stream().findFirst().orElse(null);

        if (mikrotik == null) {
            mikrotik = NetworkDevice.builder()
                    .name("Core Gateway (MikroTik Router)")
                    .deviceType(NetworkDeviceType.ROUTER_MIKROTIK)
                    .ipAddress("10.10.15.1")
                    .macAddress("d4:01:c3:1e:a7:68")
                    .location("Main Server Rack")
                    .totalPorts(18)
                    .status(NetworkDeviceStatus.ONLINE)
                    .managementUrl("https://116.206.59.142:8225")
                    .modelVendor("MikroTik CCR2004-16G-2S+")
                    .notes("Main Core Router & DHCP/VLAN Gateway")
                    .build();
            mikrotik = deviceRepository.save(mikrotik);
        } else {
            mikrotik.setManagementUrl("https://116.206.59.142:8225");
            deviceRepository.save(mikrotik);

            // Clean any old dummy ports (e.g. ether1, ether2, ether3) that were hardcoded
            List<NetworkPort> mPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(mikrotik.getId());
            for (NetworkPort mp : mPorts) {
                if (mp.getPortNumber().matches("^ether[1-8]$")) {
                    portRepository.delete(mp);
                }
            }
        }

        // 4. Ensure Switch 01 exists with real IP 10.10.16.2 & SNMP skylink-snmp
        ensureRealSwitch("Switch 01 (Server & Uplink Switch)", "10.10.16.2", "https://10.10.16.2/cs9912d753/cbs/home.htm", "CBS350-24P-4G PoE", "Server Rack 1 - Unit 12");

        // 5. Ensure Switch 02 exists with real IP 10.10.16.3 & SNMP skylink-snmp
        ensureRealSwitch("Switch 02 (User Access Switch)", "10.10.16.3", "https://10.10.16.3/cs68915b73/cbs/home.htm", "CBS350-24T-4G", "Floor Rack - West Wing");

        // 6. Ensure Switch 03 exists with real IP 10.10.16.4 & SNMP skylink-snmp
        ensureRealSwitch("Switch 03 (User Access Switch)", "10.10.16.4", "https://10.10.16.4/cs68915b73/cbs/home.htm#", "CBS350-24T-4G", "Floor Rack - East Wing");

        // 7. Ensure Grandstream GWN7615 APs (Master & Slave) exist
        ensureRealAccessPoint("Grandstream GWN7615 (Master AP)", "10.10.12.12", "https://10.10.12.12/service/snmp", "Grandstream GWN7615 Wave-2 802.11ac", "Main Office Ceiling", "GE03");
        ensureRealAccessPoint("Grandstream GWN7615 (Slave AP)", "10.10.12.13", "https://10.10.12.13/service/snmp", "Grandstream GWN7615 Wave-2 802.11ac", "Meeting Room / Zone 2", "GE04");

        // 8. Clean all hardcoded demo names/users on switch ports
        List<NetworkPort> allPorts = portRepository.findAll();
        for (NetworkPort p : allPorts) {
            String user = p.getHostnameOrUser();
            if (user != null && !user.isBlank()) {
                String cleaned = sanitizePortUser(user);
                p.setHostnameOrUser(cleaned);
            }
            if (p.getDevice() != null && p.getDevice().getDeviceType() == NetworkDeviceType.MANAGED_SWITCH) {
                // If the port has no MAC learned and is not trunk, default to empty/down until SNMP updates it
                if ((p.getMacAddress() == null || p.getMacAddress().isBlank()) && !p.isTrunk()) {
                    p.setPortStatus(PortStatus.EMPTY_DISABLED);
                    p.setHostnameOrUser(null);
                    p.setIpAddress(null);
                }
            }
        }
        portRepository.saveAll(allPorts);

        // 8. Seed initial port history baselines
        portHistoryService.seedInitialPortBaselines();

        log.info("Network Infrastructure initialized cleanly with ZERO demo data.");
    }

    private void ensureRealSwitch(String name, String ip, String url, String model, String location) {
        NetworkDevice sw = deviceRepository.findAll().stream()
                .filter(d -> d.getDeviceType() == NetworkDeviceType.MANAGED_SWITCH &&
                             (ip.equals(d.getIpAddress()) || d.getName().contains(name.substring(0, 9))))
                .findFirst().orElse(null);

        if (sw == null) {
            sw = NetworkDevice.builder()
                    .name(name)
                    .deviceType(NetworkDeviceType.MANAGED_SWITCH)
                    .ipAddress(ip)
                    .location(location)
                    .totalPorts(28)
                    .status(NetworkDeviceStatus.ONLINE)
                    .managementUrl(url)
                    .modelVendor(model)
                    .snmpCommunity("skylink-snmp")
                    .snmpPort(161)
                    .build();
            sw = deviceRepository.save(sw);
            createEmptyPortsForDevice(sw, 28, "GE");
        } else {
            sw.setName(name);
            sw.setIpAddress(ip);
            sw.setManagementUrl(url);
            sw.setModelVendor(model);
            sw.setSnmpCommunity("skylink-snmp");
            sw.setSnmpPort(161);
            sw.setLocation(location);
            deviceRepository.save(sw);

            // Ensure 28 ports exist (24 RJ45 + 4 SFP)
            List<NetworkPort> existingPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(sw.getId());
            if (existingPorts.size() < 28) {
                createEmptyPortsForDevice(sw, 28, "GE");
            }
        }
    }

    private void ensureRealAccessPoint(String name, String ip, String url, String model, String location, String switchUplinkPort) {
        NetworkDevice ap = deviceRepository.findAll().stream()
                .filter(d -> (d.getIpAddress() != null && d.getIpAddress().equals(ip)) || (d.getName() != null && d.getName().equalsIgnoreCase(name)))
                .findFirst().orElse(null);

        if (ap == null) {
            ap = NetworkDevice.builder()
                    .name(name)
                    .deviceType(NetworkDeviceType.ACCESS_POINT)
                    .ipAddress(ip)
                    .location(location)
                    .totalPorts(2)
                    .status(NetworkDeviceStatus.ONLINE)
                    .managementUrl(url)
                    .modelVendor(model)
                    .snmpCommunity("skylink-snmp")
                    .snmpPort(161)
                    .snmpVersion("2c")
                    .notes("Connected to Switch 01 (" + switchUplinkPort + " PoE). Dual-Band 2.4GHz & 5GHz Wi-Fi.")
                    .build();
            ap = deviceRepository.save(ap);
        } else {
            ap.setName(name);
            ap.setIpAddress(ip);
            ap.setManagementUrl(url);
            ap.setModelVendor(model);
            ap.setLocation(location);
            ap.setDeviceType(NetworkDeviceType.ACCESS_POINT);
            ap.setSnmpCommunity("skylink-snmp");
            ap.setSnmpPort(161);
            ap.setSnmpVersion("2c");
            ap.setTotalPorts(2);
            deviceRepository.save(ap);
        }

        // Configure AP Ports: NET/PoE & NET2
        List<NetworkPort> apPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(ap.getId());
        if (apPorts.isEmpty()) {
            NetworkPort p1 = NetworkPort.builder()
                    .device(ap)
                    .portNumber("NET/PoE")
                    .portMode(PortMode.TRUNK)
                    .portStatus(PortStatus.ACTIVE_CONNECTED)
                    .speedNegotiation("1 Gbps")
                    .vlan("10, 12, 15")
                    .ipAddress(ip)
                    .targetDeviceName("Switch 01 (Server & Uplink Switch)")
                    .targetPortName(switchUplinkPort)
                    .deviceCategory("PoE Powered Uplink (Switch 01)")
                    .notes("802.3af 48V PoE Power & 1G Data Trunk from Switch 01 " + switchUplinkPort)
                    .build();
            NetworkPort p2 = NetworkPort.builder()
                    .device(ap)
                    .portNumber("NET2 (LAN)")
                    .portMode(PortMode.ACCESS)
                    .portStatus(PortStatus.EMPTY_DISABLED)
                    .speedNegotiation("1 Gbps")
                    .deviceCategory("Secondary LAN Bridge")
                    .build();
            portRepository.saveAll(List.of(p1, p2));
        } else {
            for (NetworkPort p : apPorts) {
                if (p.getPortNumber().contains("NET1") || p.getPortNumber().contains("PoE") || p.getPortNumber().contains("NET/")) {
                    p.setPortNumber("NET/PoE");
                    p.setPortMode(PortMode.TRUNK);
                    p.setPortStatus(PortStatus.ACTIVE_CONNECTED);
                    p.setSpeedNegotiation("1 Gbps");
                    p.setVlan("10, 12, 15");
                    p.setIpAddress(ip);
                    p.setTargetDeviceName("Switch 01 (Server & Uplink Switch)");
                    p.setTargetPortName(switchUplinkPort);
                    p.setDeviceCategory("PoE Powered Uplink (Switch 01)");
                    p.setNotes("802.3af 48V PoE Power & 1G Data Trunk from Switch 01 " + switchUplinkPort);
                } else if (p.getPortNumber().contains("NET2")) {
                    p.setPortNumber("NET2 (LAN)");
                    p.setPortMode(PortMode.ACCESS);
                    p.setSpeedNegotiation("1 Gbps");
                    p.setDeviceCategory("Secondary LAN Bridge");
                }
            }
            portRepository.saveAll(apPorts);
        }

        // Wire Switch 01 uplink port to point to this AP
        NetworkDevice sw01 = deviceRepository.findAll().stream()
                .filter(d -> d.getName() != null && d.getName().contains("Switch 01"))
                .findFirst().orElse(null);
        if (sw01 != null) {
            portRepository.findByDeviceIdOrderByPortNumberAsc(sw01.getId()).stream()
                    .filter(p -> switchUplinkPort.equalsIgnoreCase(p.getPortNumber()))
                    .findFirst().ifPresent(upPort -> {
                        upPort.setTargetDeviceName(name);
                        upPort.setTargetPortName("NET/PoE");
                        upPort.setHostnameOrUser(name);
                        upPort.setDeviceCategory("Wi-Fi Access Point (Dual-Band)");
                        upPort.setIpAddress(ip);
                        upPort.setPortStatus(PortStatus.ACTIVE_CONNECTED);
                        upPort.setNotes("PoE Power (48V) to " + name);
                        portRepository.save(upPort);
                    });
        }
    }

    private void createEmptyPortsForDevice(NetworkDevice device, int totalPorts, String prefix) {
        List<NetworkPort> existingPorts = portRepository.findByDeviceIdOrderByPortNumberAsc(device.getId());
        List<String> existingNames = existingPorts.stream().map(NetworkPort::getPortNumber).toList();
        List<NetworkPort> newPorts = new ArrayList<>();

        for (int i = 1; i <= totalPorts; i++) {
            String pName = prefix.equals("GE") ? String.format("GE%02d", i) : (prefix + i);
            if (!existingNames.contains(pName)) {
                NetworkPort p = NetworkPort.builder()
                        .device(device)
                        .portNumber(pName)
                        .portMode(PortMode.ACCESS)
                        .portStatus(PortStatus.EMPTY_DISABLED)
                        .speedNegotiation("1 Gbps")
                        .build();
                newPorts.add(p);
            }
        }
        if (!newPorts.isEmpty()) {
            portRepository.saveAll(newPorts);
        }
    }

    private String sanitizePortUser(String name) {
        if (name == null || name.isBlank()) return null;
        String result = name;

        // Strip known demo employee personas
        String[] demoNames = {
            "Sophia", "Damian", "Tessa", "Dorian", "George", "Ryan", "Leo", "Noshin", "Thomas", "Mark",
            "Raisa", "Lana", "Tony", "Jake", "Rovert", "Nick", "Frank", "Victor", "Steve", "Ted", "Spencer",
            "Farhin", "Neil", "Alex", "Rashiq", "Logan", "Arthur", "Bruce", "Noah", "Justin", "Rushan", "Tahsin"
        };

        for (String demo : demoNames) {
            result = result.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(demo) + "\\b", "").trim();
            result = result.replaceAll("(?i)\\(" + java.util.regex.Pattern.quote(demo) + "\\)", "").trim();
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(demo) + "\\s*/\\s*", "").trim();
        }

        // Clean leftover empty parentheses or separators
        result = result.replaceAll("\\(\\s*\\)", "").replaceAll("^/+|/+$", "").trim();
        result = result.replaceAll("^\\((.*)\\)$", "$1").trim();

        return result.isBlank() ? null : result;
    }
}
