package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.ClientDueConfig;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientDueConfigRepository extends JpaRepository<ClientDueConfig, Long> {

    Optional<ClientDueConfig> findByClientIdentifierIgnoreCase(String clientIdentifier);

    List<ClientDueConfig> findAllByOrderByClientNameAsc();
}
