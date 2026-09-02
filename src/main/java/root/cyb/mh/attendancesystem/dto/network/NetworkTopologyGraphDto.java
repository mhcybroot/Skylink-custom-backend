package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkTopologyGraphDto {
    @Builder.Default
    private List<NetworkTopologyNodeDto> nodes = new ArrayList<>();
    
    @Builder.Default
    private List<NetworkTopologyLinkDto> links = new ArrayList<>();
}
