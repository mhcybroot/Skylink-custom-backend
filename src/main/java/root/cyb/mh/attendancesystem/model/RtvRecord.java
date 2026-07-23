package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import root.cyb.mh.attendancesystem.model.enums.IssueFromEnum;
import root.cyb.mh.attendancesystem.model.enums.RtvStatusEnum;

import java.time.LocalDateTime;

@Entity
@Table(name = "rtv_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RtvRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String woNumber;

    @Column(nullable = false)
    private String clientCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "original_processor_id")
    private Employee originalProcessor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rtv_solved_by_id")
    private Employee rtvSolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueFromEnum issueFrom;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RtvStatusEnum rtvStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private Boolean isDeleted = false;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.rtvStatus == null) this.rtvStatus = RtvStatusEnum.ON_PROCESS;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
