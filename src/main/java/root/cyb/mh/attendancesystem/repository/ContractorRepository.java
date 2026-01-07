package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.Contractor;
import java.util.List;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, Long> {
        List<Contractor> findByActiveTrue();

        List<Contractor> findByActiveTrue(org.springframework.data.domain.Sort sort);

        @org.springframework.data.jpa.repository.Query("SELECT c FROM Contractor c WHERE " +
                        "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "CAST(c.id as string) LIKE :keyword")
        List<Contractor> searchContractors(@org.springframework.data.repository.query.Param("keyword") String keyword,
                        org.springframework.data.domain.Sort sort);

        long countByCreatedAtAfter(java.time.LocalDateTime date);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Contractor c WHERE c.active = true AND (SELECT COUNT(p) FROM PaymentRequest p WHERE p.contractor = c AND p.requestDate >= :sinceDate) = 0")
        long countStaleContractors(
                        @org.springframework.data.repository.query.Param("sinceDate") java.time.LocalDate sinceDate);
}
