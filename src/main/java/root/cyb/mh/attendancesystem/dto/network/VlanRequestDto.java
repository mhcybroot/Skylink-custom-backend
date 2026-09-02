package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VlanRequestDto {
    private Long id;
    private Integer vlanId;
    private String name;
    private String subnet;
    private String description;
    private String badgeColor;
}
