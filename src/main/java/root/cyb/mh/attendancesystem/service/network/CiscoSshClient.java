package root.cyb.mh.attendancesystem.service.network;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import root.cyb.mh.attendancesystem.dto.network.cisco.CiscoMacTableEntryDto;
import root.cyb.mh.attendancesystem.dto.network.cisco.CiscoPortStatusDto;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CiscoSshClient {

    public Map<String, Object> testConnection(String host, int port, String username, String password) throws Exception {
        Map<String, Object> info = new LinkedHashMap<>();
        String output = executeCommand(host, port, username, password, "show version");
        if (output != null && !output.isBlank()) {
            info.put("connected", true);
            info.put("rawVersion", output.substring(0, Math.min(output.length(), 400)));
            info.put("host", host);
            info.put("sshPort", port);
        } else {
            info.put("connected", false);
            info.put("error", "No response from switch via SSH.");
        }
        return info;
    }

    public List<CiscoPortStatusDto> fetchPortStatuses(String host, int port, String username, String password) {
        List<CiscoPortStatusDto> list = new ArrayList<>();
        try {
            String output = executeCommand(host, port, username, password, "show interfaces status");
            if (output == null || output.isBlank()) return list;

            // Pattern e.g. "gi1        connected    1         a-full  a-1000   1000BASE-T" or "gi2  notconnect  1  auto  auto"
            Pattern pattern = Pattern.compile("^(gi\\d+|ge\\d+|fa\\d+|eth\\d+|\\d+)\\s+(.+?)\\s+(connected|notconnect|disabled|err-disabled|down|up)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(output);

            int idx = 1;
            while (matcher.find()) {
                String rawPort = matcher.group(1);
                String status = matcher.group(3).toLowerCase();
                String duplex = matcher.group(5);
                String speed = matcher.group(6);

                boolean operUp = status.contains("connected") || status.contains("up");
                boolean adminUp = !status.contains("disabled");

                CiscoPortStatusDto dto = CiscoPortStatusDto.builder()
                        .ifIndex(idx++)
                        .portName(CiscoSnmpClient.normalizePortName(rawPort))
                        .operUp(operUp)
                        .adminUp(adminUp)
                        .duplex(duplex.contains("full") ? "Full" : "Half")
                        .speed(speed.contains("1000") || speed.contains("1G") ? "1 Gbps" : (speed.contains("100") ? "100 Mbps" : "Auto"))
                        .build();
                list.add(dto);
            }
        } catch (Exception e) {
            log.error("SSH fetchPortStatuses failed for {}: {}", host, e.getMessage());
        }
        return list;
    }

    public List<CiscoMacTableEntryDto> fetchMacTable(String host, int port, String username, String password) {
        List<CiscoMacTableEntryDto> list = new ArrayList<>();
        try {
            String output = executeCommand(host, port, username, password, "show mac address-table");
            if (output == null || output.isBlank()) return list;

            // Pattern e.g. "15    345a.6017.d126    DYNAMIC       gi8" or "15  34:5a:60:17:d1:26  DYNAMIC  gi8"
            Pattern pattern = Pattern.compile("(\\d+)\\s+([0-9a-fA-F.:-]{12,17})\\s+(\\S+)\\s+(gi\\d+|ge\\d+|fa\\d+|eth\\d+|\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(output);

            while (matcher.find()) {
                int vlan = Integer.parseInt(matcher.group(1));
                String rawMac = matcher.group(2);
                String type = matcher.group(3);
                String rawPort = matcher.group(4);

                String normMac = formatCiscoMac(rawMac);

                list.add(CiscoMacTableEntryDto.builder()
                        .vlan(vlan)
                        .macAddress(normMac)
                        .entryType(type)
                        .portName(CiscoSnmpClient.normalizePortName(rawPort))
                        .build());
            }
        } catch (Exception e) {
            log.error("SSH fetchMacTable failed for {}: {}", host, e.getMessage());
        }
        return list;
    }

    private String executeCommand(String host, int port, String username, String password, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = jsch.getSession(username != null ? username.trim() : "admin", host.trim(), port > 0 ? port : 22);
            if (password != null) session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            session.setTimeout(6000);
            session.connect();

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            channel.connect(5000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        } finally {
            if (channel != null && channel.isConnected()) channel.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private String formatCiscoMac(String rawMac) {
        String clean = rawMac.replaceAll("[^a-fA-F0-9]", "").toLowerCase();
        if (clean.length() != 12) return rawMac;
        return String.format("%s:%s:%s:%s:%s:%s",
                clean.substring(0, 2), clean.substring(2, 4), clean.substring(4, 6),
                clean.substring(6, 8), clean.substring(8, 10), clean.substring(10, 12));
    }
}
