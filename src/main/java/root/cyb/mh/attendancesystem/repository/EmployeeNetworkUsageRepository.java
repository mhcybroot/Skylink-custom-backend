package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.EmployeeNetworkUsageRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeNetworkUsageRepository extends JpaRepository<EmployeeNetworkUsageRecord, Long> {
    List<EmployeeNetworkUsageRecord> findByRecordDateOrderByTotalBytesTransferredDesc(LocalDate date);
    List<EmployeeNetworkUsageRecord> findByRecordDateBetweenOrderByTotalBytesTransferredDesc(LocalDate start, LocalDate end);
    Optional<EmployeeNetworkUsageRecord> findByIpAddressAndRecordDate(String ipAddress, LocalDate recordDate);
}
