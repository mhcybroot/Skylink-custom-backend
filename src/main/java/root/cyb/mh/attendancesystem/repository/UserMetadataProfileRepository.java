package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.UserMetadataProfile;

import java.util.List;

@Repository
public interface UserMetadataProfileRepository extends JpaRepository<UserMetadataProfile, Long> {
    List<UserMetadataProfile> findByUsernameOrderByCreatedAtDesc(String username);
}
