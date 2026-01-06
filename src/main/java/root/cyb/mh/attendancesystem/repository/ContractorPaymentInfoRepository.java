package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import root.cyb.mh.attendancesystem.model.ContractorPaymentInfo;
import java.util.List;

public interface ContractorPaymentInfoRepository extends JpaRepository<ContractorPaymentInfo, Long> {
    List<ContractorPaymentInfo> findByContractorId(Long contractorId);

    List<ContractorPaymentInfo> findByContractorIdAndActiveTrue(Long contractorId);

    List<ContractorPaymentInfo> findByContractorIdAndActiveFalse(Long contractorId);
}
