package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Entity
@Data
@Table(name = "processing_work_orders", indexes = {
        @Index(name = "idx_processing_wo_creator", columnList = "created_by_employee_id"),
        @Index(name = "idx_processing_wo_wo_number", columnList = "wo_number"),
        @Index(name = "idx_processing_wo_analyst", columnList = "analyst"),
        @Index(name = "idx_processing_wo_status", columnList = "status"),
        @Index(name = "idx_processing_wo_work_date", columnList = "work_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_processing_wo_number_creator", columnNames = { "wo_number", "created_by_employee_id" })
})
public class ProcessingWorkOrder {

    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/uuuu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/dd/uuuu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M-d-uuuu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM-dd-uuuu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M/d/uu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/dd/uu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("M-d-uu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM-dd-uu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d, uuuu").toFormatter(Locale.US),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d, uuuu").toFormatter(Locale.US));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wo_number", nullable = false)
    private String woNumber;

    @Column(name = "wo_type")
    private String woType;

    @Column(name = "client_code")
    private String clientCode;

    private String address;

    @Column(name = "photo_count")
    private Integer photoCount;

    @Column(name = "work_date")
    private LocalDate workDate;

    private String category;

    @Column(name = "late_status")
    private String lateStatus;

    private String analyst;

    private String status = "NEW";

    @Column(name = "date_due")
    private LocalDate dateDue;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ba_from_wo")
    private String baFromWo;

    @Column(name = "ba_by_analyst")
    private String baByAnalyst;

    @Column(name = "bid_count")
    private Integer bidCount;

    @Column(name = "bid_amount", precision = 19, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "client_invoice", precision = 19, scale = 2)
    private BigDecimal clientInvoice;

    @Column(name = "crew_invoice", precision = 19, scale = 2)
    private BigDecimal crewInvoice;

    @Column(name = "assigned_analyst_employee_id")
    private String assignedAnalystEmployeeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_employee_id", nullable = false, updatable = false)
    private String createdByEmployeeId;

    @Column(name = "last_updated_by_employee_id")
    private String lastUpdatedByEmployeeId;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setWorkDateFromText(String dateText) {
        this.workDate = parseLocalDate(dateText);
    }

    public static LocalDate parseLocalDate(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        String normalized = rawDate.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        throw new IllegalArgumentException("Unable to parse LocalDate value: '" + rawDate + "'");
    }
}
