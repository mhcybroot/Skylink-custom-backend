package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_logs")
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private LocalDateTime importDate;

    private Integer recordsProcessed;

    private String status; // SUCCESS, FAILED, PARTIAL

    private String importType; // WORK_ORDER, EMPLOYEE, etc.

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getImportDate() {
        return importDate;
    }

    public void setImportDate(LocalDateTime importDate) {
        this.importDate = importDate;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImportType() {
        return importType;
    }

    public void setImportType(String importType) {
        this.importType = importType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
