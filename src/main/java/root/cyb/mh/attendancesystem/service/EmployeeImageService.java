package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import root.cyb.mh.attendancesystem.model.EmployeeImage;
import root.cyb.mh.attendancesystem.repository.EmployeeImageRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeImageService {

    @Autowired
    private EmployeeImageRepository employeeImageRepository;

    private final String UPLOAD_DIR = "uploads/images/";

    public void saveImage(String employeeUsername, MultipartFile file) throws IOException {
        if (file.isEmpty()) return;

        // Ensure directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown.jpg";
        }
        
        // Generate unique filename to avoid collisions
        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(uniqueFilename);
        
        // Save file locally
        file.transferTo(filePath.toAbsolutePath().toFile());

        // Save to DB
        EmployeeImage image = new EmployeeImage();
        image.setEmployeeUsername(employeeUsername);
        image.setOriginalFileName(originalFilename);
        image.setLocalFilePath(filePath.toString());
        image.setWebPath("/uploads/images/" + uniqueFilename);
        image.setUploadedAt(LocalDateTime.now());
        
        employeeImageRepository.save(image);
    }

    public List<EmployeeImage> searchImages(String employeeUsername) {
        String emp = (employeeUsername != null && !employeeUsername.trim().isEmpty()) ? employeeUsername : null;
        return employeeImageRepository.searchImages(emp);
    }

    public List<String> getDistinctEmployeeUsernames() {
        return employeeImageRepository.findDistinctEmployeeUsernames();
    }
}
