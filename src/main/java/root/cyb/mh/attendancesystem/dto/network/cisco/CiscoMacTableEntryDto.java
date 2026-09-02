package root.cyb.mh.attendancesystem.dto.network.cisco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiscoMacTableEntryDto {
    private Integer vlan;
    private String macAddress;
    private String portName; // e.g. "gi1", "GE01"
    private String entryType; // "Dynamic", "Static"
}
