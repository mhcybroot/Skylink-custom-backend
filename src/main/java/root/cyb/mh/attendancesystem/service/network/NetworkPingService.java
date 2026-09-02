package root.cyb.mh.attendancesystem.service.network;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.dto.network.DevicePingStatusDto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class NetworkPingService {

    private final Map<String, List<Double>> pingHistory = new ConcurrentHashMap<>();
    private final ExecutorService pingExecutor = Executors.newFixedThreadPool(10);

    private static final List<TargetNode> TARGETS = List.of(
            new TargetNode("Core Gateway (MikroTik)", "10.10.15.1", "ROUTER", "fas fa-network-wired", 80),
            new TargetNode("Switch 01 (Server & Uplink)", "10.10.16.2", "SWITCH", "fas fa-server", 80),
            new TargetNode("Switch 02 (User Access)", "10.10.16.3", "SWITCH", "fas fa-server", 80),
            new TargetNode("Switch 03 (User Access)", "10.10.16.4", "SWITCH", "fas fa-server", 80),
            new TargetNode("Master AP (GWN7615)", "10.10.12.12", "ACCESS_POINT", "fas fa-wifi", 443),
            new TargetNode("Slave AP (GWN7615)", "10.10.12.13", "ACCESS_POINT", "fas fa-wifi", 443),
            new TargetNode("WAN Primary (Cloudflare)", "1.1.1.1", "WAN_GATEWAY", "fas fa-globe", 53),
            new TargetNode("WAN Secondary (Google)", "8.8.8.8", "WAN_GATEWAY", "fab fa-google", 53)
    );

    public List<DevicePingStatusDto> pingAllTargets() {
        List<CompletableFuture<DevicePingStatusDto>> futures = new ArrayList<>();
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        for (TargetNode target : TARGETS) {
            futures.add(CompletableFuture.supplyAsync(() -> executePing(target, timeStr), pingExecutor));
        }

        List<DevicePingStatusDto> results = new ArrayList<>();
        for (CompletableFuture<DevicePingStatusDto> f : futures) {
            try {
                results.add(f.get(3, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("Ping timeout or error: {}", e.getMessage());
            }
        }

        return results;
    }

    private DevicePingStatusDto executePing(TargetNode target, String timestamp) {
        boolean reachable = false;
        double rttMs = 0.0;

        // 1. Try native Linux ICMP ping (high accuracy sub-ms)
        try {
            Process p = new ProcessBuilder("ping", "-c", "1", "-W", "1", target.ip).start();
            boolean finished = p.waitFor(1200, TimeUnit.MILLISECONDS);
            if (finished && p.exitValue() == 0) {
                reachable = true;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("time=")) {
                            int idx = line.indexOf("time=");
                            int msIdx = line.indexOf(" ms", idx);
                            if (idx != -1 && msIdx != -1) {
                                rttMs = Double.parseDouble(line.substring(idx + 5, msIdx).trim());
                            }
                        }
                    }
                }
            } else {
                p.destroyForcibly();
            }
        } catch (Exception ignored) {}

        // 2. Fallback to TCP Socket if ICMP was blocked/dropped
        if (!reachable) {
            long start = System.nanoTime();
            try {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(target.ip, target.port), 1000);
                    reachable = true;
                    long end = System.nanoTime();
                    rttMs = Math.round(((end - start) / 1_000_000.0) * 10.0) / 10.0;
                }
            } catch (Exception e) {
                try {
                    reachable = InetAddress.getByName(target.ip).isReachable(1000);
                    if (reachable) {
                        long end = System.nanoTime();
                        rttMs = Math.round(((end - start) / 1_000_000.0) * 10.0) / 10.0;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (rttMs <= 0.0 && reachable) {
            rttMs = 0.5;
        }

        // Maintain history (last 15 samples)
        List<Double> history = pingHistory.computeIfAbsent(target.ip, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (history) {
            history.add(reachable ? rttMs : 0.0);
            while (history.size() > 15) {
                history.remove(0);
            }
        }

        String statusBadge = reachable ? (rttMs > 150 ? "DEGRADED" : "ONLINE") : "OFFLINE";
        String statusColor = reachable ? (rttMs > 150 ? "warning" : "success") : "danger";
        int packetLoss = reachable ? 0 : 100;

        return DevicePingStatusDto.builder()
                .deviceName(target.name)
                .ipAddress(target.ip)
                .deviceCategory(target.category)
                .icon(target.icon)
                .isOnline(reachable)
                .responseTimeMs(reachable ? rttMs : 0.0)
                .packetLossPercent(packetLoss)
                .statusBadge(statusBadge)
                .statusColor(statusColor)
                .latencyHistory(new ArrayList<>(history))
                .lastChecked(timestamp)
                .build();
    }

    private record TargetNode(String name, String ip, String category, String icon, int port) {}
}
