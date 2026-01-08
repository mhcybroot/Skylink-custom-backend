package root.cyb.mh.attendancesystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import root.cyb.mh.attendancesystem.model.Notification;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(String recipientUsername);

    // With Limit
    List<Notification> findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(String recipientUsername,
            org.springframework.data.domain.Pageable pageable);

    // For full history
    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);
}
