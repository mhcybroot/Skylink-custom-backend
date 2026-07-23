package root.cyb.mh.attendancesystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.dto.RtvRecordRequest;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.RtvAuditLog;
import root.cyb.mh.attendancesystem.model.RtvRecord;
import root.cyb.mh.attendancesystem.model.enums.RtvActionTypeEnum;

import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.RtvAuditLogRepository;
import root.cyb.mh.attendancesystem.repository.RtvRecordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RtvSheetService {

    private final RtvRecordRepository rtvRecordRepository;
    private final RtvAuditLogRepository rtvAuditLogRepository;
    private final EmployeeRepository employeeRepository;

    public List<RtvRecord> getAllRecords() {
        return rtvRecordRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    public RtvRecord getRecordById(Long id) {
        return rtvRecordRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("RTV Record not found with ID: " + id));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional
    public RtvRecord createRecord(RtvRecordRequest req, String username) {
        validateMandatoryFields(req);
        Employee processor = resolveEmployee(req.getOriginalProcessorId());
        Employee solvedBy = resolveEmployee(req.getRtvSolvedById());

        RtvRecord record = RtvRecord.builder()
                .woNumber(req.getWoNumber())
                .clientCode(req.getClientCode())
                .originalProcessor(processor)
                .rtvSolvedBy(solvedBy)
                .issueFrom(req.getIssueFrom())
                .details(req.getDetails())
                .rtvStatus(req.getRtvStatus() != null ? req.getRtvStatus() : root.cyb.mh.attendancesystem.model.enums.RtvStatusEnum.ON_PROCESS)
                .notes(req.getNotes())
                .createdBy(username)
                .isDeleted(false)
                .build();

        RtvRecord saved = rtvRecordRepository.save(record);

        // Audit Log for Creation
        String actorName = resolveActorName(username);
        createAuditLog(saved.getId(), saved.getWoNumber(), RtvActionTypeEnum.CREATED, username, actorName,
                "RECORD", null, saved.getWoNumber(), "RTV Record created for WO: " + saved.getWoNumber());

        return saved;
    }

    @Transactional
    public RtvRecord updateRecord(Long id, RtvRecordRequest req, String username) {
        validateMandatoryFields(req);
        RtvRecord existing = getRecordById(id);
        String actorName = resolveActorName(username);

        Employee newProcessor = resolveEmployee(req.getOriginalProcessorId());
        Employee newSolvedBy = resolveEmployee(req.getRtvSolvedById());

        // Audit Field Modifications
        compareAndLog(id, existing.getWoNumber(), username, actorName, "WO Number", existing.getWoNumber(), req.getWoNumber());
        compareAndLog(id, existing.getWoNumber(), username, actorName, "Client Code", existing.getClientCode(), req.getClientCode());
        
        String oldProcName = existing.getOriginalProcessor() != null ? existing.getOriginalProcessor().getName() : "Unassigned";
        String newProcName = newProcessor != null ? newProcessor.getName() : "Unassigned";
        compareAndLog(id, existing.getWoNumber(), username, actorName, "Original Processor", oldProcName, newProcName);

        String oldSolvedName = existing.getRtvSolvedBy() != null ? existing.getRtvSolvedBy().getName() : "Unassigned";
        String newSolvedName = newSolvedBy != null ? newSolvedBy.getName() : "Unassigned";
        compareAndLog(id, existing.getWoNumber(), username, actorName, "RTV Solved By", oldSolvedName, newSolvedName);

        String oldIssue = existing.getIssueFrom() != null ? existing.getIssueFrom().name() : "";
        String newIssue = req.getIssueFrom() != null ? req.getIssueFrom().name() : "";
        compareAndLog(id, existing.getWoNumber(), username, actorName, "Issue From", oldIssue, newIssue);

        String oldStatus = existing.getRtvStatus() != null ? existing.getRtvStatus().name() : "";
        String newStatus = req.getRtvStatus() != null ? req.getRtvStatus().name() : "";
        if (!Objects.equals(oldStatus, newStatus)) {
            createAuditLog(id, existing.getWoNumber(), RtvActionTypeEnum.STATUS_CHANGED, username, actorName,
                    "RTV Status", oldStatus, newStatus, "Status updated from " + oldStatus + " to " + newStatus);
        }

        compareAndLog(id, existing.getWoNumber(), username, actorName, "Details", existing.getDetails(), req.getDetails());
        compareAndLog(id, existing.getWoNumber(), username, actorName, "Notes", existing.getNotes(), req.getNotes());

        // Update Entity
        existing.setWoNumber(req.getWoNumber());
        existing.setClientCode(req.getClientCode());
        existing.setOriginalProcessor(newProcessor);
        existing.setRtvSolvedBy(newSolvedBy);
        existing.setIssueFrom(req.getIssueFrom());
        existing.setRtvStatus(req.getRtvStatus());
        existing.setDetails(req.getDetails());
        existing.setNotes(req.getNotes());

        return rtvRecordRepository.save(existing);
    }

    @Transactional
    public void deleteRecord(Long id, String username) {
        RtvRecord record = getRecordById(id);
        record.setIsDeleted(true);
        rtvRecordRepository.save(record);

        String actorName = resolveActorName(username);
        createAuditLog(id, record.getWoNumber(), RtvActionTypeEnum.DELETED, username, actorName,
                "RECORD", record.getWoNumber(), "DELETED", "RTV Record for WO " + record.getWoNumber() + " was deleted");
    }

    public List<RtvAuditLog> getLifecycleHistory(Long rtvId) {
        return rtvAuditLogRepository.findByRtvIdOrderByTimestampDesc(rtvId);
    }

    private void compareAndLog(Long rtvId, String woNumber, String username, String actorName,
                               String fieldName, String oldVal, String newVal) {
        String cleanOld = oldVal == null ? "" : oldVal.trim();
        String cleanNew = newVal == null ? "" : newVal.trim();

        if (!cleanOld.equalsIgnoreCase(cleanNew)) {
            createAuditLog(rtvId, woNumber, RtvActionTypeEnum.UPDATED, username, actorName,
                    fieldName, cleanOld, cleanNew, fieldName + " changed from '" + cleanOld + "' to '" + cleanNew + "'");
        }
    }

    private void createAuditLog(Long rtvId, String woNumber, RtvActionTypeEnum actionType,
                                String username, String actorName, String fieldName,
                                String oldVal, String newVal, String description) {
        RtvAuditLog log = RtvAuditLog.builder()
                .rtvId(rtvId)
                .woNumber(woNumber)
                .actionType(actionType)
                .actorUsername(username)
                .actorFullName(actorName)
                .timestamp(LocalDateTime.now())
                .fieldName(fieldName)
                .oldValue(oldVal)
                .newValue(newVal)
                .changeDescription(description)
                .build();
        rtvAuditLogRepository.save(log);
    }

    private Employee resolveEmployee(String empId) {
        if (empId == null || empId.isBlank()) return null;
        return employeeRepository.findById(empId).orElse(null);
    }

    private String resolveActorName(String username) {
        if (username == null) return "Unknown";
        return employeeRepository.findAll().stream()
                .filter(e -> username.equalsIgnoreCase(e.getUsername()) || username.equalsIgnoreCase(e.getId()))
                .findFirst()
                .map(Employee::getName)
                .orElse(username);
    }

    private void validateMandatoryFields(RtvRecordRequest req) {
        if (req.getWoNumber() == null || req.getWoNumber().isBlank()) {
            throw new IllegalArgumentException("WO Number is a mandatory field");
        }
        if (req.getClientCode() == null || req.getClientCode().isBlank()) {
            throw new IllegalArgumentException("Client Code is a mandatory field");
        }
        if (req.getOriginalProcessorId() == null || req.getOriginalProcessorId().isBlank()) {
            throw new IllegalArgumentException("Original Processor is a mandatory field");
        }
        if (req.getIssueFrom() == null) {
            throw new IllegalArgumentException("Issue From is a mandatory field");
        }
        if (req.getDetails() == null || req.getDetails().isBlank()) {
            throw new IllegalArgumentException("Details is a mandatory field");
        }
    }
}
