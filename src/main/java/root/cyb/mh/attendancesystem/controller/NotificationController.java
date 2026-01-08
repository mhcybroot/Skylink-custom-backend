package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.model.Notification;
import root.cyb.mh.attendancesystem.service.NotificationService;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/unread")
    @ResponseBody
    public List<Notification> getUnreadNotifications(Principal principal,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        if (principal == null)
            return java.util.Collections.emptyList();
        return notificationService.getUnreadNotifications(principal.getName(), limit);
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public void markAllAsRead(Principal principal) {
        if (principal != null) {
            notificationService.markAllAsRead(principal.getName());
        }
    }

    @GetMapping("/history")
    public String history(Principal principal, org.springframework.ui.Model model) {
        if (principal != null) {
            model.addAttribute("notifications", notificationService.getAllNotifications(principal.getName()));
        }
        return "notifications/history";
    }
}
