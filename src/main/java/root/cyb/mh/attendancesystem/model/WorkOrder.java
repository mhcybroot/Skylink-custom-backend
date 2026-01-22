package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String woNumber; // WO #

    private String status;
    private String workType;
    private LocalDate dateDue;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String originalClientString; // Raw CSV Value

    private String address;
    private String city;
    private String state;
    private String zip;

    @ManyToOne
    @JoinColumn(name = "contractor_id")
    private Contractor contractor;

    private Long importBatchId;

    public Long getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(Long importBatchId) {
        this.importBatchId = importBatchId;
    }

    private String originalContractorString; // Raw CSV Value

    private Integer photosCount; // Photos

    private String admin;
    private String category;
    private LocalDate dateReceived;

    private boolean contractorInvoicePaid; // Cont. Invoice Paid (Yes/No)
    private boolean clientInvoicePaid; // Client Invoice Paid (Yes/No)

    private BigDecimal clientInvoiceTotal;
    private BigDecimal contractorInvoiceTotal; // Cont. Invoice Total

    private LocalDate invoiceDate;

    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    // New Fields for Redesign (invoice_report.csv)
    private String invoiceNumber;
    private String ppwNumber;
    private String loanNumber;
    private String customerBank;

    private LocalDate dateDueClient;
    private LocalDate sentToClientDate;
    private LocalDate clientPaidDate;

    // Financials
    private BigDecimal contractorDiscountPercent; // e.g. 0.00
    private BigDecimal contractorPaidAmount;

    private BigDecimal clientDiscountPercent;
    private BigDecimal clientDiscountTotal;
    private BigDecimal clientPaidAmount;
    private BigDecimal writeOffAmount;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
