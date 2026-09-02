package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUsageSummaryDto {
    private String dateRangeLabel;
    private String totalOfficeDataFormatted;
    private long totalOfficeDataBytes;
    private double averageProductivityScore;

    private String topWorkstation;
    private String topWorkService;
    private String topEntertainmentService;

    private double overallWorkDevPercent;
    private double overallCommunicationPercent;
    private double overallMediaPercent;
    private double overallGeneralWebPercent;

    @Builder.Default
    private List<EmployeeUsageItemDto> employeeUsageList = new ArrayList<>();
}
