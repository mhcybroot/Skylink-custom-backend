package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.LeaveRequest;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

        // For Employee: View their own history
        List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

        // For HR/Admin: View all (could filter by Pending later)
        List<LeaveRequest> findAllByOrderByCreatedAtDesc();

        // Find pending requests
        List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequest.Status status);

        // Find requests where the applicant reports to the given approver ID (Primary
        // OR Assistant)
        List<LeaveRequest> findByEmployee_ReportsTo_IdOrEmployee_ReportsToAssistant_Id(String approverId,
                        String assistantId);

        // Count approved leaves for a specific date
        long countByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(java.time.LocalDate date1,
                        java.time.LocalDate date2, LeaveRequest.Status status);

        // Find approved leaves for a specific date (for Dashboard filtering)
        List<LeaveRequest> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(java.time.LocalDate date1,
                        java.time.LocalDate date2, LeaveRequest.Status status);

        // Fetch all approved leaves that fall within a specific year
        @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :endOfYear AND l.endDate >= :startOfYear")
        List<LeaveRequest> findApprovedLeavesInYear(@org.springframework.data.repository.query.Param("startOfYear") java.time.LocalDate startOfYear, @org.springframework.data.repository.query.Param("endOfYear") java.time.LocalDate endOfYear);
}
