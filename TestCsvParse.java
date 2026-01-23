
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TestCsvParse {
    public static void main(String[] args) {
        try {
            Reader in = new FileReader("invoice_report.csv");
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
