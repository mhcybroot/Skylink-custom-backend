package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirewallConnectionDto {
    private String id;
    private String protocol; // "tcp", "udp", "icmp"
    private String srcAddress; // e.g. "10.10.15.250:54321"
    private String dstAddress; // e.g. "142.250.190.46:443"
    private String replySrcAddress;
    private String replyDstAddress;
    private String tcpState; // "established", "time-wait", "syn-sent"
    private Long origBytes;
    private Long replBytes;
    private String totalBytesFormatted;
    private String clientUserOrHostname;
}
