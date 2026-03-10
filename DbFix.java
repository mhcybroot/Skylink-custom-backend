import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbFix {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/skylink_database",
                    "mhcybroot", "MhR@2025");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(
                    "ALTER TABLE employee_daily_work_status ADD COLUMN total_break_seconds INTEGER NOT NULL DEFAULT 0;");
            System.out.println("Column added successfully!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
