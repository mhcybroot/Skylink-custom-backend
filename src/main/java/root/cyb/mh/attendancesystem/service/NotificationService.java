package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.Notification;
import root.cyb.mh.attendancesystem.repository.NotificationRepository;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendNotification(String username, String title, String message, String type, String link) {
        // 1. Save to DB
        Notification notification = new Notification();
        notification.setRecipientUsername(username);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLinkAction(link);
        notification.setRead(false);
        notificationRepository.save(notification);

        // 2. Push via WebSocket
        // Destination: /user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);

        // 3. Push via Service Worker (Web Push)
        try {
            pushNotificationService.sendPushNotification(username, message);
        } catch (Exception e) {
            // Log but don't fail the WebSocket/DB save
            System.err.println("Web Push Failed: " + e.getMessage());
        }
    }

    public List<Notification> getUnreadNotifications(String username) {
        return notificationRepository.findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(username);
    }

    public List<Notification> getUnreadNotifications(String username, int limit) {
        return notificationRepository.findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(
                username,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAllAsRead(String username) {
        List<Notification> unread = notificationRepository
                .findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(username);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public List<Notification> getAllNotifications(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }
}
