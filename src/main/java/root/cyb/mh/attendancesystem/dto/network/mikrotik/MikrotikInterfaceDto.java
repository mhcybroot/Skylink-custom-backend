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
public class MikrotikInterfaceDto {
    @JsonProperty(".id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("running")
    private Boolean running;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("mac-address")
    private String macAddress;

    @JsonProperty("link-downs")
    private Long linkDowns;

    @JsonProperty("rx-byte")
    private Long rxByte;

    @JsonProperty("tx-byte")
    private Long txByte;

    @JsonProperty("rx-error")
    private Long rxError;

    @JsonProperty("tx-error")
    private Long txError;

    @JsonProperty("rx-drop")
    private Long rxDrop;

    @JsonProperty("tx-drop")
    private Long txDrop;
}
