package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.UserCustomMetadataField;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomMetadataFieldRepository extends JpaRepository<UserCustomMetadataField, Long> {
    List<UserCustomMetadataField> findByUsernameOrderByDisplayOrderAscCreatedAtAsc(String username);
    Optional<UserCustomMetadataField> findByUsernameAndFieldKey(String username, String fieldKey);
}
