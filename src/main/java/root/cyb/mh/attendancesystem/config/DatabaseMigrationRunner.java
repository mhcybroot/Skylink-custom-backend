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
            // Drop the old constraint
            jdbcTemplate.execute(
                    "ALTER TABLE employee_daily_work_status DROP CONSTRAINT IF EXISTS employee_daily_work_status_status_check");
            // Add the new constraint with INCOMPLETE_SHIFT
            jdbcTemplate.execute(
                    "ALTER TABLE employee_daily_work_status ADD CONSTRAINT employee_daily_work_status_status_check CHECK (status::text = ANY (ARRAY['NOT_ENTERED'::character varying, 'ENTERED_OFFICE'::character varying, 'LOGGED_IN'::character varying, 'WORKING'::character varying, 'ON_BREAK'::character varying, 'ENDED_WORK'::character varying, 'LEFT_WITHOUT_PUNCH'::character varying, 'COMPLETED_DAY'::character varying, 'INCOMPLETE_SHIFT'::character varying]::text[]))");
            System.out.println(
                    "Successfully updated employee_daily_work_status_status_check constraint to include INCOMPLETE_SHIFT.");
        } catch (Exception e) {
            System.err.println("Failed to update status constraint: " + e.getMessage());
        }
    }
}
