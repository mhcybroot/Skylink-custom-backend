package root.cyb.mh.attendancesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewDistanceDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String zipCode;
    private String area;
    private Double latitude;
    private Double longitude;
    private Integer serviceRadiusMiles;
    private String coverageZipCodes;
    private boolean active;

    // Distance calculation fields
    private Double distanceMiles;
    private Boolean isInRange;
    private Long activeWorkOrdersCount;
}
