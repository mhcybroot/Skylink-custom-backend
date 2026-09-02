package root.cyb.mh.attendancesystem.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CableDiagnosticResultDto {
    private Long portId;
    private String portNumber;
    private String deviceName;
    private String overallStatus; // "NORMAL_OK", "CABLE_FAULT", "OPEN_CIRCUIT", "SHORT_CIRCUIT", "CROSS_TALK", "NO_CABLE"
    private Integer estimatedLengthMeters;
    private Integer faultDistanceMeters;
    
    // 4-Pair Status Details (Pair 1-2, 3-6, 4-5, 7-8)
    private String pair12Status; // "OK", "OPEN", "SHORT", "IMPEDANCE_MISMATCH", "UNKNOWN"
    private String pair36Status;
    private String pair45Status;
    private String pair78Status;
    
    private String diagnosticSummary;
    private LocalDateTime testedAt;
}
