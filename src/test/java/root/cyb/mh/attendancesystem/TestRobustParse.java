
package root.cyb.mh.attendancesystem;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class TestRobustParse {

    @Test
    public void testRobustness() {
        String[] inputs = {
                "05-01-26", "5-1-26", "05/01/26", "2026-05-01", " 05-01-26 ", "\u00A005-01-26\u00A0"
        };

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("MM-dd-yy"))
                .appendOptional(DateTimeFormatter.ofPattern("M-d-yy"))
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yy"))
                .appendOptional(DateTimeFormatter.ofPattern("M/d/yy"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .toFormatter();

        for (String input : inputs) {
            String clean = input.replace("\u00A0", "").trim();
            try {
                LocalDate date = LocalDate.parse(clean, formatter);
                System.out.println("Parsed '" + input + "' -> " + date);
            } catch (Exception e) {
                System.out.println("Failed to parse '" + input + "'");
            }
        }
    }
}
