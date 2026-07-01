package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import root.cyb.mh.attendancesystem.model.PhoneNotification;
import root.cyb.mh.attendancesystem.repository.PhoneNotificationRepository;

import java.util.List;

@Service
public class PhoneNotificationService {

    @Autowired
    private PhoneNotificationRepository phoneNotificationRepository;

    public void saveNotification(PhoneNotification notification) {
        phoneNotificationRepository.save(notification);
    }

    public List<PhoneNotification> getAllNotifications() {
        return phoneNotificationRepository.findAllByOrderByInterceptedAtDesc();
    }

    public List<PhoneNotification> getNotificationsForEmployee(String username) {
        return phoneNotificationRepository.findByEmployeeUsernameOrderByInterceptedAtDesc(username);
    }

    public List<String> getDistinctEmployeeUsernames() {
        return phoneNotificationRepository.findDistinctEmployeeUsernames();
    }

    public List<String> getDistinctPackageNames() {
        return phoneNotificationRepository.findDistinctPackageNames();
    }

    public List<PhoneNotification> searchNotifications(String employeeUsername, String packageName, String searchTerm) {
        // If all parameters are empty or null, we could just return all, 
        // but the query handles NULLs gracefully. We should convert empty strings to null.
        String emp = (employeeUsername != null && !employeeUsername.trim().isEmpty()) ? employeeUsername : null;
        String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName : null;
        String searchStr = (searchTerm != null && !searchTerm.trim().isEmpty()) ? "%" + searchTerm.toLowerCase() + "%" : null;
        
        return phoneNotificationRepository.searchNotifications(emp, pkg, searchStr);
    }
}
