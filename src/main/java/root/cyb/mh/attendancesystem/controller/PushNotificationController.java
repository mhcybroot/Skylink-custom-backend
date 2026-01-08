package root.cyb.mh.attendancesystem.controller;

import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.service.PushNotificationService;
import java.security.Principal;

@RestController
@RequestMapping("/push")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/public-key")
    public String getPublicKey() {
        return pushNotificationService.getPublicKey();
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody SubscriptionRequest request, Principal principal) {
        if (principal == null) {
            // Or allow anonymous if needed? For now, assume user must login once to
            // subscribe.
            return;
        }
        pushNotificationService.subscribe(principal.getName(), request.getEndpoint(), request.getKeys().getP256dh(),
                request.getKeys().getAuth());
    }

    // DTOs
    public static class SubscriptionRequest {
        private String endpoint;
        private Keys keys;

        // Getters/Setters
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Keys getKeys() {
            return keys;
        }

        public void setKeys(Keys keys) {
            this.keys = keys;
        }
    }

    public static class Keys {
        private String p256dh;
        private String auth;

        // Getters/Setters
        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
