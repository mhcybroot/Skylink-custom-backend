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
public class MikrotikArpDto {
    @JsonProperty(".id")
    private String id;

    @JsonProperty("address")
    private String address;

    @JsonProperty("mac-address")
    private String macAddress;

    @JsonProperty("interface")
    private String networkInterface;

    @JsonProperty("complete")
    private Boolean complete;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("dynamic")
    private Boolean dynamic;

    @JsonProperty("comment")
    private String comment;
}
