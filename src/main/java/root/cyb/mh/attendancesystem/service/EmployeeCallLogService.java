package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.EmployeeCallLog;
import root.cyb.mh.attendancesystem.model.dto.EmployeeCallLogDto;
import root.cyb.mh.attendancesystem.repository.EmployeeCallLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeCallLogService {

    @Autowired
    private EmployeeCallLogRepository callLogRepository;

    public void saveCallLogs(String employeeUsername, List<EmployeeCallLogDto> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) return;
        
        LocalDateTime now = LocalDateTime.now();
        for (EmployeeCallLogDto dto : dtoList) {
            EmployeeCallLog log = new EmployeeCallLog();
            log.setEmployeeUsername(employeeUsername);
            log.setCallerName(dto.getCallerName());
            log.setCallNumber(dto.getCallNumber());
            log.setCallType(dto.getCallType());
            log.setDurationSeconds(dto.getDurationSeconds());
            log.setCallTimestamp(dto.getCallTimestamp());
            log.setSyncedAt(now);
            callLogRepository.save(log);
        }
    }

    public List<EmployeeCallLog> searchCallLogs(String employeeUsername) {
        String emp = (employeeUsername != null && !employeeUsername.trim().isEmpty()) ? employeeUsername : null;
        return callLogRepository.searchCallLogs(emp);
    }

    public List<String> getDistinctEmployeeUsernames() {
        return callLogRepository.findDistinctEmployeeUsernames();
    }
}
