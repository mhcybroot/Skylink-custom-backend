package root.cyb.mh.attendancesystem;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.File;

public class TestCsvParse {

    @Test
    public void testParseInvoiceDate() {
        try {
            File csvFile = new File("invoice_report.csv");
            System.out.println("Reading CSV from: " + csvFile.getAbsolutePath());
            if (!csvFile.exists()) {
                System.out.println("ERROR: File does not exist!");
                // Try looking in parent or specific location if needed, but project root is
                // standard
                // Also check specifically for
                // C:/Users/HP/IdeaProjects/Skylink/invoice_report.csv
                csvFile = new File("C:/Users/HP/IdeaProjects/Skylink/invoice_report.csv");
                if (csvFile.exists()) {
                    System.out.println("Found it at absolute path: " + csvFile.getAbsolutePath());
                } else {
                    System.out.println("ERROR: File really does not exist!");
                    return;
                }
            }

            Reader in = new FileReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy");

            int count = 0;
            for (CSVRecord record : records) {
                if (count >= 5)
                    break;
                String dateStr = record.get("Invoice Date");
                System.out.println("Row " + count);
                System.out.println("  Raw string: '" + dateStr + "'");
                if (dateStr != null) {
                    System.out.println("  Length: " + dateStr.length());
                    System.out.print("  Chars: ");
                    for (char c : dateStr.toCharArray()) {
                        System.out.print((int) c + " ");
                    }
                    System.out.println();

                    try {
                        LocalDate date = LocalDate.parse(dateStr.trim(), formatter);
                        System.out.println("  Parsed: " + date);
                    } catch (Exception e) {
                        System.out.println("  FAILED to parse.");
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("  Date string is null");
                }
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
