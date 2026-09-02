package root.cyb.mh.attendancesystem.dto.network.mikrotik;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MikrotikConnectionTestRequest {
    private String host;
    @Builder.Default
    private int port = 443;
    private String username;
    private String password;
    @Builder.Default
    private boolean useSsl = true;
}
