package root.cyb.mh.attendancesystem.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.model.Client;
import root.cyb.mh.attendancesystem.model.Contractor;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.EmployeeImportLog;
import root.cyb.mh.attendancesystem.model.EmployeeWorkOrder;
import root.cyb.mh.attendancesystem.repository.ClientRepository;
import root.cyb.mh.attendancesystem.repository.ContractorRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeImportLogRepository;
import root.cyb.mh.attendancesystem.repository.EmployeeWorkOrderRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeWorkOrderService {

    @Autowired
    private EmployeeWorkOrderRepository employeeWorkOrderRepository;

    @Autowired
    private EmployeeImportLogRepository employeeImportLogRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContractorRepository contractorRepository;

    @Transactional
    public void importWorkOrders(InputStream is, Employee employee) throws IOException {
        EmployeeImportLog log = new EmployeeImportLog();
        log.setImportDate(LocalDateTime.now());
        log.setImportType("EMPLOYEE_WORK_ORDER");
        log.setStatus("PROCESSING");
        log.setImportedBy(employee);
        log = employeeImportLogRepository.save(log);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader).getRecords();

        log.setTotalRecords(records.size());
        employeeImportLogRepository.save(log);

        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MM-dd-yy"))
                .appendOptional(DateTimeFormatter.ofPattern("M-d-yy"))
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yy"))
                .appendOptional(DateTimeFormatter.ofPattern("M/d/yy"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("M/d/yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                .toFormatter();

        int count = 0;
        List<EmployeeWorkOrder> batchToSave = new ArrayList<>();

        try {
            for (CSVRecord record : records) {
                String woNum = record.get("WO #");
                if (woNum == null || woNum.trim().isEmpty()) {
                    continue;
                }

                EmployeeWorkOrder wo = employeeWorkOrderRepository.findByWoNumber(woNum.trim())
                        .orElse(new EmployeeWorkOrder());

                wo.setImportBatchId(log.getId());
                wo.setWoNumber(woNum.trim());
                wo.setImportedBy(employee);

                // Basic Fields
                wo.setInvoiceNumber(getRecordValue(record, "Invoice #"));
                wo.setPpwNumber(getRecordValue(record, "PPW#"));
                wo.setLoanNumber(getRecordValue(record, "Loan #"));
                wo.setCustomerBank(getRecordValue(record, "Customer/Bank"));
                wo.setAdmin(getRecordValue(record, "Assigned Admin"));
                wo.setWorkType(getRecordValue(record, "Work Type"));

                // Status deduction
                String sentDate = getRecordValue(record, "Sent to Client");
                String paidDate = getRecordValue(record, "Client Paid Date");
                if (paidDate != null && !paidDate.trim().isEmpty()) {
                    wo.setStatus("Closed");
                } else if (sentDate != null && !sentDate.trim().isEmpty()) {
                    wo.setStatus("Invoiced");
                } else {
                    wo.setStatus("Open");
                }

                // Address
                wo.setAddress(getRecordValue(record, "Address"));
                wo.setCity(getRecordValue(record, "City"));
                wo.setState(getRecordValue(record, "State"));
                wo.setZip(getRecordValue(record, "Zip"));

                // Dates
                wo.setInvoiceDate(parseDate(getRecordValue(record, "Invoice Date"), dateFormatter));
                wo.setDateDue(parseDate(getRecordValue(record, "Date Due"), dateFormatter));
                wo.setDateDueClient(parseDate(getRecordValue(record, "Date Due Client"), dateFormatter));
                wo.setSentToClientDate(parseDate(sentDate, dateFormatter));
                wo.setClientPaidDate(parseDate(paidDate, dateFormatter));

                // Financials
                wo.setContractorInvoiceTotal(parseCurrency(getRecordValue(record, " Contractor Total ")));
                wo.setContractorPaidAmount(parseCurrency(getRecordValue(record, " Contractor Paid Amount ")));
                wo.setContractorDiscountPercent(parsePercentage(getRecordValue(record, "Contractor Discount%")));

                wo.setClientInvoiceTotal(parseCurrency(getRecordValue(record, " Client Total ")));
                wo.setClientPaidAmount(parseCurrency(getRecordValue(record, " Client Paid Amount ")));
                wo.setClientDiscountTotal(parseCurrency(getRecordValue(record, " Client Discount Total ")));
                wo.setClientDiscountPercent(parsePercentage(getRecordValue(record, "Client Discount%")));
                wo.setWriteOffAmount(parseCurrency(getRecordValue(record, " Write Off Amount ")));

                // Derived booleans
                wo.setContractorInvoicePaid(wo.getContractorPaidAmount() != null
                        && wo.getContractorPaidAmount().compareTo(BigDecimal.ZERO) > 0);
                if (wo.getClientPaidAmount() != null && wo.getClientInvoiceTotal() != null) {
                    BigDecimal remaining = wo.getRemainingClientBalance();
                    wo.setClientInvoicePaid(remaining.compareTo(BigDecimal.ZERO) <= 0);
                } else {
                    wo.setClientInvoicePaid(wo.getClientPaidDate() != null);
                }

                // Client Relationship
                String clientName = getRecordValue(record, "Client");
                wo.setOriginalClientString(clientName);
                if (clientName != null && !clientName.trim().isEmpty()) {
                    String cleanClientName = clientName.trim();
                    wo.setClient(clientRepository.findFirstByCode(cleanClientName)
                            .or(() -> clientRepository.findFirstByNameIgnoreCase(cleanClientName))
                            .orElseGet(() -> {
                                Client c = new Client();
                                c.setName(cleanClientName);
                                c.setCode(cleanClientName);
                                c.setActive(true);
                                return clientRepository.save(c);
                            }));
                }

                // Contractor Relationship
                String contName = getRecordValue(record, "Contractor");
                wo.setOriginalContractorString(contName);
                if (contName != null && !contName.trim().isEmpty()) {
                    String cleanName = contName.trim();
                    contractorRepository.findFirstByNameIgnoreCase(cleanName)
                            .ifPresent(wo::setContractor);
                }

                batchToSave.add(wo);
                count++;

                if (batchToSave.size() >= 100) {
                    employeeWorkOrderRepository.saveAll(batchToSave);
                    batchToSave.clear();
                }
            }

            if (!batchToSave.isEmpty()) {
                employeeWorkOrderRepository.saveAll(batchToSave);
                batchToSave.clear();
            }

            log.setStatus("SUCCESS");
            log.setRecordsProcessed(count);
            log.setSuccessCount(count);
            employeeImportLogRepository.save(log);

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            employeeImportLogRepository.save(log);
            throw new RuntimeException("Error processing CSV: " + e.getMessage(), e);
        }
    }

    private String getRecordValue(CSVRecord record, String column) {
        if (record.isMapped(column)) {
            return record.get(column);
        }
        return null;
    }

    private LocalDate parseDate(String val, DateTimeFormatter formatter) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(val.trim(), formatter);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseCurrency(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            String clean = val.replace("$", "").replace(",", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parsePercentage(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            String clean = val.replace("%", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void deleteImportBatch(Long batchId) {
        employeeWorkOrderRepository.deleteByImportBatchId(batchId);
        employeeImportLogRepository.deleteById(batchId);
    }

    @Transactional
    public void cleanupLegacyData() {
        employeeWorkOrderRepository.deleteByImportBatchIdIsNull();
    }
}
