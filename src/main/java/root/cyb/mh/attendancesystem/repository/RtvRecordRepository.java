package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.RtvRecord;
import root.cyb.mh.attendancesystem.model.enums.RtvStatusEnum;

import java.util.List;

@Repository
public interface RtvRecordRepository extends JpaRepository<RtvRecord, Long> {

    List<RtvRecord> findByIsDeletedFalseOrderByCreatedAtDesc();

    List<RtvRecord> findByIsDeletedFalseAndRtvStatusOrderByCreatedAtDesc(RtvStatusEnum rtvStatus);

    List<RtvRecord> findByIsDeletedTrueOrderByUpdatedAtDesc();
}
