package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processing_work_order_history")
public class ProcessingWorkOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processing_work_order_id")
    private Long processingWorkOrderId;

    @Column(name = "wo_number")
    private String woNumber;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by_name")
    private String changedByName;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    public ProcessingWorkOrderHistory() {}

    public ProcessingWorkOrderHistory(Long processingWorkOrderId, String woNumber, String actionType, String oldValue, String newValue, String changedByName) {
        this.processingWorkOrderId = processingWorkOrderId;
        this.woNumber = woNumber;
        this.actionType = actionType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedByName = changedByName;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProcessingWorkOrderId() {
        return processingWorkOrderId;
    }

    public void setProcessingWorkOrderId(Long processingWorkOrderId) {
        this.processingWorkOrderId = processingWorkOrderId;
    }

    public String getWoNumber() {
        return woNumber;
    }

    public void setWoNumber(String woNumber) {
        this.woNumber = woNumber;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
