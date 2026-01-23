
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReproduceDateParse {
    public static void main(String[] args) {
        String dateStr = "05-01-26";
        String pattern = "MM-dd-yy";

        System.out.println("Testing parsing for date: '" + dateStr + "' with pattern: '" + pattern + "'");

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate date = LocalDate.parse(dateStr, formatter);
            System.out.println("Result: " + date);
        } catch (Exception e) {
            System.out.println("Failed to parse:");
            e.printStackTrace();
        }
    }
}
