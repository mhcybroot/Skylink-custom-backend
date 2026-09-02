package root.cyb.mh.attendancesystem.dto.network.mikrotik;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MikrotikDhcpLeaseDto {
    @JsonProperty(".id")
    private String id;

    @JsonProperty("address")
    private String address;

    @JsonProperty("mac-address")
    private String macAddress;

    @JsonProperty("host-name")
    private String hostName;

    @JsonProperty("server")
    private String server;

    @JsonProperty("status")
    private String status;

    @JsonProperty("active-address")
    private String activeAddress;

    @JsonProperty("active-mac-address")
    private String activeMacAddress;

    @JsonProperty("active-host-name")
    private String activeHostName;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("expires-after")
    private String expiresAfter;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("dynamic")
    private Boolean dynamic;

    public String getEffectiveHostName() {
        if (activeHostName != null && !activeHostName.isBlank()) return activeHostName;
        if (hostName != null && !hostName.isBlank()) return hostName;
        if (comment != null && !comment.isBlank()) return comment;
        return null;
    }

    public String getEffectiveIp() {
        if (activeAddress != null && !activeAddress.isBlank()) return activeAddress;
        return address;
    }

    public String getEffectiveMac() {
        if (activeMacAddress != null && !activeMacAddress.isBlank()) return activeMacAddress;
        return macAddress;
    }
}
