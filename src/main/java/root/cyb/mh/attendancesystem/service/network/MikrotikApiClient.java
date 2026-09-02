package root.cyb.mh.attendancesystem.service.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import root.cyb.mh.attendancesystem.dto.network.PingResultDto;
import root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikArpDto;
import root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikDhcpLeaseDto;
import root.cyb.mh.attendancesystem.dto.network.mikrotik.MikrotikInterfaceDto;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class MikrotikApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    private HttpClient createTolerantHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            javax.net.ssl.SSLParameters sslParams = new javax.net.ssl.SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm(null);

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParams)
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to create custom SSLContext, falling back to default HttpClient: {}", e.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();
        }
    }

    private String getBaseUrl(String host, int port, boolean useSsl) {
        String scheme = useSsl ? "https" : "http";
        return String.format("%s://%s:%d/rest", scheme, host.trim(), port);
    }

    private String getAuthHeader(String username, String password) {
        String auth = (username != null ? username : "") + ":" + (password != null ? password : "");
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> testConnection(String host, int port, String username, String password, boolean useSsl) throws Exception {
        String url = getBaseUrl(host, port, useSsl) + "/system/resource";
        HttpClient client = createTolerantHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", getAuthHeader(username, password))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new IllegalArgumentException("Authentication failed (HTTP " + response.statusCode() + "). Check MikroTik username and password.");
        } else {
            throw new IllegalStateException("MikroTik returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    public List<MikrotikDhcpLeaseDto> fetchDhcpLeases(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ip/dhcp-server/lease";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<MikrotikDhcpLeaseDto>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch DHCP leases from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<MikrotikArpDto> fetchArpTable(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ip/arp";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<MikrotikArpDto>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch ARP table from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<MikrotikInterfaceDto> fetchInterfaces(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/interface";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();

            List<Map<String, Object>> rawInterfaces = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            log.info("Fetched {} interfaces from MikroTik. Sample: {}", rawInterfaces.size(), !rawInterfaces.isEmpty() ? rawInterfaces.get(0) : "empty");

            // Also fetch ethernet details to merge MAC addresses & stats
            Map<String, Map<String, Object>> ethMap = new HashMap<>();
            try {
                String ethUrl = getBaseUrl(host, port, useSsl) + "/interface/ethernet";
                String ethJson = executeGet(ethUrl, username, password);
                if (ethJson != null && !ethJson.isBlank()) {
                    List<Map<String, Object>> ethList = objectMapper.readValue(ethJson, new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> eth : ethList) {
                        Object name = eth.get("name");
                        if (name != null) {
                            ethMap.put(name.toString().trim(), eth);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not fetch /interface/ethernet: {}", e.getMessage());
            }

            // Also fetch bridge details
            Map<String, String> bridgeMacMap = new HashMap<>();
            try {
                String brUrl = getBaseUrl(host, port, useSsl) + "/interface/bridge";
                String brJson = executeGet(brUrl, username, password);
                if (brJson != null && !brJson.isBlank()) {
                    List<Map<String, Object>> brList = objectMapper.readValue(brJson, new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> br : brList) {
                        Object name = br.get("name");
                        Object mac = br.get("mac-address");
                        if (name != null && mac != null) {
                            bridgeMacMap.put(name.toString().trim(), mac.toString().trim());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not fetch /interface/bridge: {}", e.getMessage());
            }

            List<MikrotikInterfaceDto> dtos = new ArrayList<>();
            for (Map<String, Object> raw : rawInterfaces) {
                String name = parseStringSafely(raw.get("name"));
                if (name == null) continue;

                Map<String, Object> eth = ethMap.get(name);

                String mac = parseStringSafely(raw.get("mac-address"));
                if (mac == null && eth != null) {
                    mac = parseStringSafely(eth.get("mac-address"));
                    if (mac == null) mac = parseStringSafely(eth.get("orig-mac-address"));
                }
                if (mac == null && bridgeMacMap.containsKey(name)) {
                    mac = bridgeMacMap.get(name);
                }

                Long rxByte = getFirstLong(raw, "rx-byte", "rx-bytes", "byte");
                if (rxByte == 0 && eth != null) rxByte = getFirstLong(eth, "rx-bytes", "rx-byte");

                Long txByte = getFirstLong(raw, "tx-byte", "tx-bytes");
                if (txByte == 0 && eth != null) txByte = getFirstLong(eth, "tx-bytes", "tx-byte");

                Long rxError = getFirstLong(raw, "rx-error", "rx-errors");
                Long txError = getFirstLong(raw, "tx-error", "tx-errors");
                Long rxDrop = getFirstLong(raw, "rx-drop", "rx-drops");
                Long txDrop = getFirstLong(raw, "tx-drop", "tx-drops");
                Long linkDowns = getFirstLong(raw, "link-downs", "link_downs");

                boolean running = "true".equalsIgnoreCase(String.valueOf(raw.get("running"))) || Boolean.TRUE.equals(raw.get("running"));
                boolean disabled = "true".equalsIgnoreCase(String.valueOf(raw.get("disabled"))) || Boolean.TRUE.equals(raw.get("disabled"));

                MikrotikInterfaceDto dto = MikrotikInterfaceDto.builder()
                        .id(parseStringSafely(raw.get(".id")))
                        .name(name)
                        .type(parseStringSafely(raw.get("type")))
                        .running(running)
                        .disabled(disabled)
                        .comment(parseStringSafely(raw.get("comment")))
                        .macAddress(mac)
                        .linkDowns(linkDowns)
                        .rxByte(rxByte)
                        .txByte(txByte)
                        .rxError(rxError)
                        .txError(txError)
                        .rxDrop(rxDrop)
                        .txDrop(txDrop)
                        .build();

                dtos.add(dto);
            }

            return dtos;
        } catch (Exception e) {
            log.error("Failed to fetch interfaces from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long getFirstLong(Map<String, Object> map, String... keys) {
        if (map == null) return 0L;
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null) {
                Long val = parseLongSafely(map.get(k));
                if (val != null && val > 0) return val;
            }
        }
        return 0L;
    }

    private Long parseLongSafely(Object val) {
        if (val == null) return 0L;
        try {
            if (val instanceof Number) return ((Number) val).longValue();
            String s = val.toString().replaceAll("[^0-9]", "");
            return s.isEmpty() ? 0L : Long.parseLong(s);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String parseStringSafely(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public PingResultDto pingViaRouter(String host, int port, String username, String password, boolean useSsl, String targetIp) {
        long start = System.currentTimeMillis();
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ping";
            HttpClient client = createTolerantHttpClient();
            Map<String, Object> body = Map.of("address", targetIp.trim(), "count", 1);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Authorization", getAuthHeader(username, password))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long totalTime = System.currentTimeMillis() - start;

            if (response.statusCode() == 200) {
                List<Map<String, Object>> pingReplies = objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
                if (!pingReplies.isEmpty()) {
                    Map<String, Object> reply = pingReplies.get(0);
                    Object received = reply.get("received");
                    Object status = reply.get("status");
                    Object time = reply.get("time");

                    boolean ok = (received != null && Integer.parseInt(received.toString()) > 0) ||
                                 "echo reply".equalsIgnoreCase(String.valueOf(status));

                    long rtt = totalTime;
                    if (time != null) {
                        try {
                            String timeStr = time.toString().replaceAll("[^0-9]", "");
                            if (!timeStr.isBlank()) {
                                rtt = Long.parseLong(timeStr);
                            }
                        } catch (Exception ignored) {}
                    }

                    return PingResultDto.builder()
                            .targetIp(targetIp)
                            .reachable(ok)
                            .responseTimeMs(rtt)
                            .message(ok ? "Online (via MikroTik Gateway, RTT: " + (time != null ? time : rtt + "ms") + ")" : "Host Unreachable on LAN")
                            .build();
                }
            } else if (response.statusCode() == 404) {
                String url2 = getBaseUrl(host, port, useSsl) + "/tool/ping";
                HttpRequest request2 = HttpRequest.newBuilder()
                        .uri(URI.create(url2))
                        .timeout(Duration.ofSeconds(4))
                        .header("Authorization", getAuthHeader(username, password))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> resp2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
                if (resp2.statusCode() == 200) {
                    List<Map<String, Object>> pingReplies = objectMapper.readValue(resp2.body(), new TypeReference<List<Map<String, Object>>>() {});
                    if (!pingReplies.isEmpty()) {
                        Map<String, Object> reply = pingReplies.get(0);
                        Object received = reply.get("received");
                        Object status = reply.get("status");
                        Object time = reply.get("time");
                        boolean ok = (received != null && Integer.parseInt(received.toString()) > 0) || "echo reply".equalsIgnoreCase(String.valueOf(status));
                        return PingResultDto.builder()
                                .targetIp(targetIp)
                                .reachable(ok)
                                .responseTimeMs(totalTime)
                                .message(ok ? "Online (via MikroTik Gateway: " + time + ")" : "Host Unreachable on LAN")
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MikroTik remote ping failed for {}: {}", targetIp, e.getMessage());
        }
        return null;
    }

    public Map<String, Object> snmpGet(String mkHost, int mkPort, String mkUser, String mkPass, boolean mkSsl,
                                       String targetSwitchIp, String community, String oid) {
        try {
            HttpClient client = createTolerantHttpClient();
            String url = getBaseUrl(mkHost, mkPort, mkSsl) + "/tool/snmp-get";
            Map<String, Object> payload = Map.of(
                    "address", targetSwitchIp.trim(),
                    "community", (community != null && !community.isBlank()) ? community.trim() : "public",
                    "oid", oid.trim()
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader(mkUser, mkPass))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Map<String, Object>> list = objectMapper.readValue(response.body(), new TypeReference<>() {});
                if (!list.isEmpty()) return list.get(0);
            }
        } catch (Exception e) {
            log.warn("MikroTik snmp-get proxy failed for switch {} OID {}: {}", targetSwitchIp, oid, e.getMessage());
        }
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> snmpWalk(String mkHost, int mkPort, String mkUser, String mkPass, boolean mkSsl,
                                             String targetSwitchIp, String community, String rootOid) {
        try {
            HttpClient client = createTolerantHttpClient();
            String url = getBaseUrl(mkHost, mkPort, mkSsl) + "/tool/snmp-walk";
            Map<String, Object> payload = Map.of(
                    "address", targetSwitchIp.trim(),
                    "community", (community != null && !community.isBlank()) ? community.trim() : "public",
                    "oid", rootOid.trim()
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", getAuthHeader(mkUser, mkPass))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("MikroTik snmp-walk proxy failed for switch {} OID {}: {}", targetSwitchIp, rootOid, e.getMessage());
        }
        return Collections.emptyList();
    }

    public Map<String, Object> fetchSystemResource(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/system/resource";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyMap();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch system resource from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> fetchSystemHealth(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/system/health";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.debug("Failed to fetch system health from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchFirewallConnections(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ip/firewall/connection";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch firewall connections from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchNeighbors(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ip/neighbor";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch neighbors from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchDnsCache(String host, int port, String username, String password, boolean useSsl) {
        try {
            String url = getBaseUrl(host, port, useSsl) + "/ip/dns/cache";
            String json = executeGet(url, username, password);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch DNS cache from MikroTik ({}): {}", host, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String executeGet(String url, String username, String password) throws Exception {
        HttpClient client = createTolerantHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", getAuthHeader(username, password))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            log.warn("MikroTik GET {} returned status {}", url, response.statusCode());
            return null;
        }
    }
}
