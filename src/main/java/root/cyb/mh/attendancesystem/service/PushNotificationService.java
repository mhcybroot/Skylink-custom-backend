package root.cyb.mh.attendancesystem.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.PushSubscription;
import root.cyb.mh.attendancesystem.repository.PushSubscriptionRepository;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationService {

    @Value("${vapid.public.key}")
    private String publicKey;

    @Value("${vapid.private.key}")
    private String privateKey;

    @Value("${vapid.subject}")
    private String subject;

    private PushService pushService;

    private final PushSubscriptionRepository subscriptionRepository;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    private void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            // Initialize PushService with VAPID keys
            pushService = new PushService(publicKey, privateKey, subject);
        } catch (Exception e) {
            System.err.println("Failed to initialize PushService: " + e.getMessage());
            e.printStackTrace();
            // Do not rethrow, allow app to start without Web Push
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void subscribe(String username, String endpoint, String p256dh, String auth) {
        // Check if exists
        // Simple logic: delete old if endpoint same? Or strictly unique endpoint
        // A user can have multiple devices.
        // We really want to find by endpoint.

        // MVP: Just save. If endpoint exists, update?
        // Endpoint is unique per subscription.

        // Find by endpoint is hard without custom query in repo implemented earlier,
        // wait, I added deleteByEndpoint.
        // I should add findByEndpoint or just Try Catch save.
        // But let's assume endpoint is unique.

        // Clean up any existing for this endpoint to handle re-subscribe or auth change
        subscriptionRepository.deleteByEndpoint(endpoint);

        PushSubscription sub = new PushSubscription();
        sub.setUsername(username);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    public void sendPushNotification(String username, String message) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUsername(username);

        for (PushSubscription sub : subscriptions) {
            try {
                // Payload: JSON string
                // We should format it as JSON: { "title": "...", "body": "...", "url": "..." }
                // The message passed here might be just text or JSON.
                // Let's wrap it if it's simple text.
                String payload = message.startsWith("{") ? message
                        : String.format(
                                "{\"title\": \"Skylink Notification\", \"body\": \"%s\", \"url\": \"/notifications/history\"}",
                                message);

                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes());

                pushService.send(notification);
            } catch (Exception e) {
                // If 410 Gone, remove subscription
                // Simple logging for now
                System.err.println("Failed to send push: " + e.getMessage());
                if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("404"))) {
                    subscriptionRepository.delete(sub);
                }
            }
        }
    }
}
