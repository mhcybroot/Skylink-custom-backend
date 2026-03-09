package root.cyb.mh.attendancesystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("--- DEBUG DB START ---");
            jdbcTemplate.query(
                    "SELECT * FROM attendance_log WHERE DATE(timestamp) = CURRENT_DATE ORDER BY timestamp DESC LIMIT 10",
                    (rs, rowNum) -> {
                        System.out.println("LOG -> EMP: " + rs.getString("employee_id") + " TIMESTAMP: "
                                + rs.getString("timestamp"));
                        return null;
                    });

            jdbcTemplate.query("SELECT * FROM employee_daily_work_status WHERE date = CURRENT_DATE", (rs, rowNum) -> {
                System.out.println(
                        "STATUS -> EMP: " + rs.getString("employee_id") + " STATUS: " + rs.getString("status"));
                return null;
            });
            System.out.println("--- DEBUG DB END ---");

            // Re-apply constraint fix just in case it was missed
            jdbcTemplate.execute(
                    "ALTER TABLE employee_daily_work_status DROP CONSTRAINT IF EXISTS employee_daily_work_status_status_check");
            jdbcTemplate.execute(
                    "ALTER TABLE employee_daily_work_status ADD CONSTRAINT employee_daily_work_status_status_check CHECK (status::text = ANY (ARRAY['NOT_ENTERED'::character varying, 'ENTERED_OFFICE'::character varying, 'LOGGED_IN'::character varying, 'WORKING'::character varying, 'ON_BREAK'::character varying, 'ENDED_WORK'::character varying, 'LEFT_WITHOUT_PUNCH'::character varying, 'COMPLETED_DAY'::character varying, 'INCOMPLETE_SHIFT'::character varying]::text[]))");
        } catch (Exception e) {
            System.err.println("DB Debug Failed: " + e.getMessage());
        }
    }
}
