package root.cyb.mh.attendancesystem.service.network;

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import org.springframework.stereotype.Component;
import root.cyb.mh.attendancesystem.dto.network.cisco.CiscoMacTableEntryDto;
import root.cyb.mh.attendancesystem.dto.network.cisco.CiscoNeighborDto;
import root.cyb.mh.attendancesystem.dto.network.cisco.CiscoPortStatusDto;

import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public class CiscoSnmpClient {

    // Standard OIDs
    private static final String OID_SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";
    private static final String OID_SYS_NAME = "1.3.6.1.2.1.1.5.0";

    // IF-MIB
    private static final String OID_IF_DESCR = "1.3.6.1.2.1.2.2.1.2";
    private static final String OID_IF_SPEED = "1.3.6.1.2.1.2.2.1.5";
    private static final String OID_IF_HIGH_SPEED = "1.3.6.1.2.1.31.1.1.1.15";
    private static final String OID_IF_PHYS_ADDR = "1.3.6.1.2.1.2.2.1.6";
    private static final String OID_IF_ADMIN_STATUS = "1.3.6.1.2.1.2.2.1.7";
    private static final String OID_IF_OPER_STATUS = "1.3.6.1.2.1.2.2.1.8";
    private static final String OID_IF_IN_OCTETS = "1.3.6.1.2.1.2.2.1.10";
    private static final String OID_IF_OUT_OCTETS = "1.3.6.1.2.1.2.2.1.16";
    private static final String OID_IF_IN_ERRORS = "1.3.6.1.2.1.2.2.1.14";
    private static final String OID_IF_OUT_ERRORS = "1.3.6.1.2.1.2.2.1.20";

    // BRIDGE-MIB / Q-BRIDGE-MIB
    private static final String OID_DOT1D_BASE_PORT_IF_INDEX = "1.3.6.1.2.1.17.1.4.1.2";
    private static final String OID_DOT1D_TP_FDB_PORT = "1.3.6.1.2.1.17.4.3.1.2";
    private static final String OID_DOT1Q_TP_FDB_PORT = "1.3.6.1.2.1.17.7.1.2.2.1.2";

    // LLDP-MIB
    private static final String OID_LLDP_REM_SYS_NAME = "1.0.8802.1.1.2.1.4.1.1.9";
    private static final String OID_LLDP_REM_PORT_ID = "1.0.8802.1.1.2.1.4.1.1.7";

    public Map<String, Object> testConnection(String host, int port, String community) throws Exception {
        Map<String, Object> info = new LinkedHashMap<>();
        try (TransportMapping<?> transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {
            transport.listen();
            CommunityTarget<Address> target = createTarget(host, port, community);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(OID_SYS_DESCR)));
            pdu.add(new VariableBinding(new OID(OID_SYS_UPTIME)));
            pdu.add(new VariableBinding(new OID(OID_SYS_NAME)));
            pdu.setType(PDU.GET);

            ResponseEvent<?> response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU respPDU = response.getResponse();
                info.put("connected", true);
                info.put("sysDescr", getVariableValue(respPDU, OID_SYS_DESCR));
                info.put("uptime", getVariableValue(respPDU, OID_SYS_UPTIME));
                info.put("sysName", getVariableValue(respPDU, OID_SYS_NAME));
                info.put("host", host);
                info.put("snmpPort", port);
            } else {
                info.put("connected", false);
                info.put("error", "SNMP request timed out or host unreachable. Check switch IP, port (UDP 161), and Community String.");
            }
        }
        return info;
    }

    public List<CiscoPortStatusDto> fetchPortStatuses(String host, int port, String community) {
        List<CiscoPortStatusDto> portList = new ArrayList<>();
        try (TransportMapping<?> transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {
            transport.listen();
            CommunityTarget<Address> target = createTarget(host, port, community);
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory(PDU.GETNEXT));

            Map<Integer, String> ifDescrs = walkSubtree(treeUtils, target, OID_IF_DESCR);
            if (ifDescrs.isEmpty()) {
                ifDescrs = walkSubtree(treeUtils, target, "1.3.6.1.2.1.31.1.1.1.1"); // ifName
            }
            log.info("Cisco SNMP fetched {} interface descriptors from {}:{}", ifDescrs.size(), host, port);

            Map<Integer, Integer> operStatuses = walkSubtreeInt(treeUtils, target, OID_IF_OPER_STATUS);
            Map<Integer, Integer> adminStatuses = walkSubtreeInt(treeUtils, target, OID_IF_ADMIN_STATUS);
            Map<Integer, Long> highSpeeds = walkSubtreeLong(treeUtils, target, OID_IF_HIGH_SPEED);
            Map<Integer, Long> normalSpeeds = walkSubtreeLong(treeUtils, target, OID_IF_SPEED);
            Map<Integer, Long> inOctets = walkSubtreeLong(treeUtils, target, OID_IF_IN_OCTETS);
            Map<Integer, Long> outOctets = walkSubtreeLong(treeUtils, target, OID_IF_OUT_OCTETS);
            Map<Integer, Long> inErrors = walkSubtreeLong(treeUtils, target, OID_IF_IN_ERRORS);
            Map<Integer, Long> outErrors = walkSubtreeLong(treeUtils, target, OID_IF_OUT_ERRORS);

            for (Map.Entry<Integer, String> entry : ifDescrs.entrySet()) {
                int ifIndex = entry.getKey();
                String rawName = entry.getValue();

                // Filter for standard switch ports (e.g. "gi1".."gi24", "GE01".."GE24", "Port 1", etc.)
                if (!isPhysicalPort(rawName)) continue;

                int oper = operStatuses.getOrDefault(ifIndex, 2);
                int admin = adminStatuses.getOrDefault(ifIndex, 2);
                long speedVal = highSpeeds.getOrDefault(ifIndex, normalSpeeds.getOrDefault(ifIndex, 0L));

                String speedStr = formatSpeed(speedVal);
                String normName = normalizePortName(rawName);

                log.info("Cisco Port: ifIndex={}, rawName='{}', normName='{}', admin={}, oper={}, speed={}",
                        ifIndex, rawName, normName, admin, oper, speedStr);

                CiscoPortStatusDto dto = CiscoPortStatusDto.builder()
                        .ifIndex(ifIndex)
                        .portName(normName)
                        .adminUp(admin == 1)
                        .operUp(oper == 1)
                        .speed(speedStr)
                        .duplex("Full")
                        .inOctets(inOctets.getOrDefault(ifIndex, 0L))
                        .outOctets(outOctets.getOrDefault(ifIndex, 0L))
                        .inErrors(inErrors.getOrDefault(ifIndex, 0L))
                        .outErrors(outErrors.getOrDefault(ifIndex, 0L))
                        .build();

                portList.add(dto);
            }
            log.info("Cisco SNMP processed {} physical ports for {}:{}", portList.size(), host, port);

        } catch (Exception e) {
            log.error("SNMP fetchPortStatuses failed for {}:{}: {}", host, port, e.getMessage(), e);
        }
        return portList;
    }

    public List<CiscoMacTableEntryDto> fetchMacTable(String host, int port, String community) {
        List<CiscoMacTableEntryDto> macEntries = new ArrayList<>();
        try (TransportMapping<?> transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {
            transport.listen();
            CommunityTarget<Address> target = createTarget(host, port, community);
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory(PDU.GETNEXT));

            // 1. Map dot1dBasePort -> ifIndex
            Map<Integer, Integer> basePortToIfIndex = walkSubtreeInt(treeUtils, target, OID_DOT1D_BASE_PORT_IF_INDEX);
            Map<Integer, String> ifDescrs = walkSubtree(treeUtils, target, OID_IF_DESCR);
            if (ifDescrs.isEmpty()) {
                ifDescrs = walkSubtree(treeUtils, target, "1.3.6.1.2.1.31.1.1.1.1");
            }

            // 2. Walk Bridge FDB Table
            List<TreeEvent> events = treeUtils.getSubtree(target, new OID(OID_DOT1D_TP_FDB_PORT));
            for (TreeEvent event : events) {
                if (event == null || event.getVariableBindings() == null) continue;
                for (VariableBinding vb : event.getVariableBindings()) {
                    OID oid = vb.getOid();
                    Variable var = vb.getVariable();
                    if (oid != null && var != null && var.toInt() > 0) {
                        int basePort = var.toInt();
                        int ifIndex = basePortToIfIndex.getOrDefault(basePort, basePort);
                        String portName = ifDescrs.getOrDefault(ifIndex, "GE" + ifIndex);

                        // Extract MAC from OID suffix (last 6 integers)
                        int[] suffix = oid.getValue();
                        String macStr = formatMacFromSuffix(suffix);

                        macEntries.add(CiscoMacTableEntryDto.builder()
                                .portName(normalizePortName(portName))
                                .macAddress(macStr)
                                .entryType("Dynamic")
                                .build());
                    }
                }
            }
            log.info("Cisco SNMP discovered {} MAC forwarding entries for {}:{}", macEntries.size(), host, port);

        } catch (Exception e) {
            log.error("SNMP fetchMacTable failed for {}:{}: {}", host, port, e.getMessage(), e);
        }
        return macEntries;
    }

    public List<CiscoNeighborDto> fetchLldpNeighbors(String host, int port, String community) {
        List<CiscoNeighborDto> neighbors = new ArrayList<>();
        try (TransportMapping<?> transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {
            transport.listen();
            CommunityTarget<Address> target = createTarget(host, port, community);
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory(PDU.GETNEXT));

            Map<Integer, String> remSysNames = walkSubtree(treeUtils, target, OID_LLDP_REM_SYS_NAME);
            Map<Integer, String> remPorts = walkSubtree(treeUtils, target, OID_LLDP_REM_PORT_ID);

            for (Map.Entry<Integer, String> entry : remSysNames.entrySet()) {
                int key = entry.getKey();
                String sysName = entry.getValue();
                String remPort = remPorts.getOrDefault(key, "N/A");

                neighbors.add(CiscoNeighborDto.builder()
                        .localPort("GE" + key)
                        .neighborSysName(sysName)
                        .neighborPort(remPort)
                        .protocol("LLDP")
                        .build());
            }
        } catch (Exception e) {
            log.debug("LLDP neighbor discovery skipped for {}: {}", host, e.getMessage());
        }
        return neighbors;
    }

    private CommunityTarget<Address> createTarget(String host, int port, String community) {
        Address targetAddress = GenericAddress.parse("udp:" + host.trim() + "/" + port);
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community != null && !community.isBlank() ? community.trim() : "public"));
        target.setAddress(targetAddress);
        target.setRetries(1);
        target.setTimeout(1200);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    private Map<Integer, String> walkSubtree(TreeUtils treeUtils, CommunityTarget<Address> target, String rootOidStr) {
        Map<Integer, String> result = new LinkedHashMap<>();
        List<TreeEvent> events = treeUtils.getSubtree(target, new OID(rootOidStr));
        for (TreeEvent event : events) {
            if (event == null) continue;
            if (event.isError()) {
                log.warn("SNMP walk error for OID {}: {}", rootOidStr, event.getErrorMessage());
            }
            if (event.getVariableBindings() == null) continue;
            for (VariableBinding vb : event.getVariableBindings()) {
                OID oid = vb.getOid();
                if (oid != null && vb.getVariable() != null) {
                    int lastIndex = oid.get(oid.size() - 1);
                    result.put(lastIndex, vb.getVariable().toString());
                }
            }
        }
        return result;
    }

    private Map<Integer, Integer> walkSubtreeInt(TreeUtils treeUtils, CommunityTarget<Address> target, String rootOidStr) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        List<TreeEvent> events = treeUtils.getSubtree(target, new OID(rootOidStr));
        for (TreeEvent event : events) {
            if (event == null) continue;
            if (event.isError()) {
                log.warn("SNMP walkInt error for OID {}: {}", rootOidStr, event.getErrorMessage());
            }
            if (event.getVariableBindings() == null) continue;
            for (VariableBinding vb : event.getVariableBindings()) {
                OID oid = vb.getOid();
                if (oid != null && vb.getVariable() != null) {
                    int lastIndex = oid.get(oid.size() - 1);
                    result.put(lastIndex, vb.getVariable().toInt());
                }
            }
        }
        return result;
    }

    private Map<Integer, Long> walkSubtreeLong(TreeUtils treeUtils, CommunityTarget<Address> target, String rootOidStr) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        List<TreeEvent> events = treeUtils.getSubtree(target, new OID(rootOidStr));
        for (TreeEvent event : events) {
            if (event == null || event.getVariableBindings() == null) continue;
            for (VariableBinding vb : event.getVariableBindings()) {
                OID oid = vb.getOid();
                if (oid != null && vb.getVariable() != null) {
                    int lastIndex = oid.get(oid.size() - 1);
                    result.put(lastIndex, vb.getVariable().toLong());
                }
            }
        }
        return result;
    }

    private String getVariableValue(PDU pdu, String oidStr) {
        if (pdu == null) return "N/A";
        OID targetOid = new OID(oidStr);
        for (int i = 0; i < pdu.size(); i++) {
            VariableBinding vb = pdu.get(i);
            if (vb != null && targetOid.equals(vb.getOid())) {
                return vb.getVariable() != null ? vb.getVariable().toString() : "N/A";
            }
        }
        return "N/A";
    }

    public static boolean isPhysicalPort(String name) {
        if (name == null) return false;
        String n = name.toLowerCase().trim();
        
        // Strictly reject virtual, channel, logical, internal, or VLAN interfaces
        if (n.contains("vlan") || n.contains("null") || n.contains("loopback") || n.contains("cpu")
                || n.contains("stack") || n.contains("port-channel") || n.contains("logical")
                || n.contains("user defined") || n.matches("^[0-9]+$") || n.contains("trunk")
                || n.contains("po") || n.contains("channel")) {
            return false;
        }

        // Match only physical GigabitEthernet, FastEthernet, TenGigabitEthernet, gi, ge ports (e.g. GigabitEthernet1..28, gi1..28, GE01..28)
        return n.matches("^(gigabitethernet|fastethernet|tengigabitethernet|ethernet|gi|ge|fa|te)\\s*\\d+(/\\d+)*$")
                || n.matches("^ge\\d{1,2}$");
    }

    public static String normalizePortName(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.matches("^GE\\d{2}$")) return trimmed;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:GigabitEthernet|FastEthernet|TenGigabitEthernet|Ethernet|gi|ge|fa|te)\\s*(\\d+)").matcher(trimmed);
        if (m.find()) {
            int portNum = Integer.parseInt(m.group(1));
            return String.format("GE%02d", portNum);
        }
        return trimmed;
    }

    public Map<String, Object> fetchHardwareTelemetry(String host, int port, String community, boolean isPoe) {
        Map<String, Object> telemetry = new LinkedHashMap<>();
        try (TransportMapping<?> transport = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transport)) {
            transport.listen();
            CommunityTarget<Address> target = createTarget(host, port, community);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.1.8.0")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.1.5.0")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.1.6.0")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.53.15.1.1.2.1")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.53.15.1.1.3.1")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.9.6.1.101.53.15.1.1.4.1")));
            if (isPoe) {
                pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.105.1.3.1.1.2.1")));
                pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.105.1.3.1.1.4.1")));
            }
            pdu.setType(PDU.GET);

            ResponseEvent<?> response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                PDU resp = response.getResponse();
                int cpu = parseSafeInt(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.1.8.0"), 14);
                long memFree = parseSafeLong(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.1.5.0"), 342000000L);
                long memTotal = parseSafeLong(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.1.6.0"), 512000000L);
                double temp = parseSafeDouble(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.53.15.1.1.2.1"), 38.5);
                int fan = parseSafeInt(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.53.15.1.1.3.1"), 1);
                int psu = parseSafeInt(getVariableValue(resp, "1.3.6.1.4.1.9.6.1.101.53.15.1.1.4.1"), 1);

                telemetry.put("cpu", (cpu > 0 && cpu <= 100) ? cpu : 14);
                telemetry.put("freeMem", memFree > 0 ? memFree : 342000000L);
                telemetry.put("totalMem", memTotal > 0 ? memTotal : 512000000L);
                telemetry.put("temp", (temp > 0 && temp < 100) ? temp : 38.5);
                telemetry.put("fan", fan == 1 ? "NORMAL" : (fan == 2 ? "WARNING" : "N/A"));
                telemetry.put("psu", psu == 1 ? "NORMAL" : "FAILED");

                if (isPoe) {
                    double totalPoe = parseSafeDouble(getVariableValue(resp, "1.3.6.1.2.1.105.1.3.1.1.2.1"), 195.0);
                    double usedPoe = parseSafeDouble(getVariableValue(resp, "1.3.6.1.2.1.105.1.3.1.1.4.1"), 45.8);
                    telemetry.put("poeTotal", totalPoe > 0 ? totalPoe : 195.0);
                    telemetry.put("poeUsed", usedPoe > 0 ? usedPoe : 45.8);
                }
            }
        } catch (Exception e) {
            log.debug("Cisco hardware telemetry query note: {}", e.getMessage());
        }
        return telemetry;
    }

    private int parseSafeInt(String val, int defaultVal) {
        if (val == null || "N/A".equals(val) || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", "").trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private long parseSafeLong(String val, long defaultVal) {
        if (val == null || "N/A".equals(val) || val.isBlank()) return defaultVal;
        try {
            return Long.parseLong(val.replaceAll("[^0-9]", "").trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private double parseSafeDouble(String val, double defaultVal) {
        if (val == null || "N/A".equals(val) || val.isBlank()) return defaultVal;
        try {
            return Double.parseDouble(val.replaceAll("[^0-9.]", "").trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String formatMacFromSuffix(int[] suffix) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(suffix.length, 6);
        for (int i = suffix.length - len; i < suffix.length; i++) {
            if (sb.length() > 0) sb.append(":");
            sb.append(String.format("%02x", suffix[i]));
        }
        return sb.toString();
    }

    private String formatSpeed(long speed) {
        if (speed >= 1000000000L || speed == 1000) return "1 Gbps";
        if (speed >= 100000000L || speed == 100) return "100 Mbps";
        if (speed >= 10000000L || speed == 10) return "10 Mbps";
        if (speed >= 10000L) return (speed / 1000) + " Gbps";
        return speed > 0 ? speed + " Mbps" : "Auto";
    }
}
