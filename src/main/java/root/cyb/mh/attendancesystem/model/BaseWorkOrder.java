package root.cyb.mh.attendancesystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseWorkOrder {

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
    private LocalDateTime createdAt;

    // Redesign Fields (invoice_report.csv)
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
        createdAt = LocalDateTime.now();
    }

    // Helper method to get series name from client code
    public String getSeries() {
        if (client != null && client.getCode() != null) {
            try {
                String digits = client.getCode().replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    int clientNum = Integer.parseInt(digits);
                    int seriesBase = (clientNum / 100) * 100;
                    return "Series " + seriesBase;
                }
            } catch (NumberFormatException e) {
                // Fall through to Unknown
            }
        }
        return "Unknown";
    }

    public long getDaysElapsed() {
        if (invoiceDate == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, java.time.LocalDate.now());
    }

    public BigDecimal getRemainingClientBalance() {
        if (Boolean.TRUE.equals(clientInvoicePaid)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = getEffectiveClientTotal();
        BigDecimal paid = clientPaidAmount != null ? clientPaidAmount : BigDecimal.ZERO;
        BigDecimal writeOff = writeOffAmount != null ? writeOffAmount : BigDecimal.ZERO;
        BigDecimal remaining = total.subtract(paid).subtract(writeOff);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    public boolean isPartiallyPaid() {
        if (Boolean.TRUE.equals(clientInvoicePaid) || (status != null && status.equalsIgnoreCase("Cancelled"))) {
            return false;
        }
        BigDecimal remaining = getRemainingClientBalance();
        return clientPaidAmount != null
                && clientPaidAmount.compareTo(BigDecimal.ZERO) > 0
                && remaining.compareTo(BigDecimal.ZERO) > 0;
    }

    public double getPaidPercentage() {
        BigDecimal total = getEffectiveClientTotal();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return (clientPaidAmount != null && clientPaidAmount.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
        }
        if (clientPaidAmount == null || clientPaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        double pct = clientPaidAmount.doubleValue() / total.doubleValue() * 100.0;
        return Math.min(100.0, Math.max(0.0, pct));
    }

    public boolean isUnpaid() {
        if (status != null && status.equalsIgnoreCase("Cancelled")) {
            return false;
        }
        if (Boolean.TRUE.equals(clientInvoicePaid)) {
            return false;
        }
        if (clientPaidAmount != null && clientPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (getRemainingClientBalance().compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
        }
        if (clientPaidDate != null && (clientPaidAmount == null || clientPaidAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            return false;
        }
        return true;
    }

    public BigDecimal getEffectiveClientTotal() {
        if (clientDiscountTotal != null && clientDiscountTotal.compareTo(BigDecimal.ZERO) > 0) {
            return clientDiscountTotal;
        }
        return clientInvoiceTotal != null ? clientInvoiceTotal : BigDecimal.ZERO;
    }

    public boolean isDiscountApplied() {
        return clientDiscountTotal != null
                && clientDiscountTotal.compareTo(BigDecimal.ZERO) > 0
                && clientInvoiceTotal != null
                && clientDiscountTotal.compareTo(clientInvoiceTotal) < 0;
    }
}
