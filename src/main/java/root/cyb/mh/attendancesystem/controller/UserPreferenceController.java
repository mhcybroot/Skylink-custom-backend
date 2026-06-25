package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.UserPreference;
import root.cyb.mh.attendancesystem.repository.UserPreferenceRepository;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/preferences")
public class UserPreferenceController {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @PostMapping("/nav-items")
    public ResponseEntity<?> savePinnedNavItems(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String principalName = authentication.getName();
        String pinnedItems = payload.get("pinnedItems");

        Optional<UserPreference> existingPref = userPreferenceRepository.findByPrincipalName(principalName);
        UserPreference pref;
        if (existingPref.isPresent()) {
            pref = existingPref.get();
        } else {
            pref = new UserPreference();
            pref.setPrincipalName(principalName);
        }

        pref.setPinnedNavItems(pinnedItems);
        userPreferenceRepository.save(pref);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Preferences saved successfully"));
    }
}
