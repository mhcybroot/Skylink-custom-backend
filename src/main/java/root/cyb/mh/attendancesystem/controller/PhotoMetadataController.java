package root.cyb.mh.attendancesystem.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import root.cyb.mh.attendancesystem.model.UserMetadataProfile;
import root.cyb.mh.attendancesystem.repository.UserMetadataProfileRepository;
import root.cyb.mh.attendancesystem.service.PhotoMetadataService;
import root.cyb.mh.attendancesystem.service.PhotoMetadataService.PhotoUpdateRequest;

import java.util.*;

@Controller
public class PhotoMetadataController {

    @Autowired
    private PhotoMetadataService photoMetadataService;

    @Autowired
    private UserMetadataProfileRepository profileRepository;

    @Autowired
    private root.cyb.mh.attendancesystem.repository.UserCustomMetadataFieldRepository customFieldRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/tools/photo-metadata")
    public String showToolPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("activeLink", "photo-metadata-tool");
        model.addAttribute("username", userDetails != null ? userDetails.getUsername() : "User");
        return "photo-metadata-tool";
    }

    // --- Profile API endpoints ---

    @GetMapping("/api/photo-metadata/profiles")
    @ResponseBody
    public ResponseEntity<List<UserMetadataProfile>> getUserProfiles(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UserMetadataProfile> profiles = profileRepository.findByUsernameOrderByCreatedAtDesc(userDetails.getUsername());
        return ResponseEntity.ok(profiles);
    }

    @PostMapping("/api/photo-metadata/profiles")
    @ResponseBody
    public ResponseEntity<?> saveUserProfile(
            @RequestBody UserMetadataProfile profile,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        profile.setUsername(userDetails.getUsername());
        UserMetadataProfile saved = profileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/api/photo-metadata/profiles/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUserProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<UserMetadataProfile> profileOpt = profileRepository.findById(id);
        if (profileOpt.isPresent() && profileOpt.get().getUsername().equals(userDetails.getUsername())) {
            profileRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- Custom Metadata Fields API ---

    @GetMapping("/api/photo-metadata/custom-fields")
    @ResponseBody
    public ResponseEntity<List<root.cyb.mh.attendancesystem.model.UserCustomMetadataField>> getCustomFields(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<root.cyb.mh.attendancesystem.model.UserCustomMetadataField> fields = customFieldRepository.findByUsernameOrderByDisplayOrderAscCreatedAtAsc(userDetails.getUsername());
        return ResponseEntity.ok(fields);
    }

    @PostMapping("/api/photo-metadata/custom-fields")
    @ResponseBody
    public ResponseEntity<?> saveCustomField(
            @RequestBody root.cyb.mh.attendancesystem.model.UserCustomMetadataField field,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        field.setUsername(userDetails.getUsername());
        if (field.getFieldKey() == null || field.getFieldKey().isBlank()) {
            field.setFieldKey(field.getFieldLabel().toLowerCase().replaceAll("[^a-z0-9]", "_"));
        }
        root.cyb.mh.attendancesystem.model.UserCustomMetadataField saved = customFieldRepository.save(field);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/api/photo-metadata/custom-fields/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCustomField(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<root.cyb.mh.attendancesystem.model.UserCustomMetadataField> fieldOpt = customFieldRepository.findById(id);
        if (fieldOpt.isPresent() && fieldOpt.get().getUsername().equals(userDetails.getUsername())) {
            customFieldRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- Transient Inspection API ---

    @PostMapping("/api/photo-metadata/inspect")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> inspectPhotos(@RequestParam("photos") List<MultipartFile> files) {
        List<Map<String, Object>> metadataList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                byte[] bytes = file.getBytes();
                Map<String, Object> meta = photoMetadataService.extractMetadata(bytes, file.getOriginalFilename());
                metadataList.add(meta);
            } catch (Exception e) {
                Map<String, Object> errMeta = new HashMap<>();
                errMeta.put("filename", file.getOriginalFilename());
                errMeta.put("error", e.getMessage());
                metadataList.add(errMeta);
            }
        }
        return ResponseEntity.ok(metadataList);
    }

    // --- Transient Processing & ZIP Download Endpoint ---

    @PostMapping("/api/photo-metadata/download-zip")
    public ResponseEntity<byte[]> downloadZip(
            @RequestParam("photos") List<MultipartFile> files,
            @RequestParam(value = "metadataJson", required = false) String metadataJson) {
        try {
            Map<String, byte[]> rawFiles = new LinkedHashMap<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    rawFiles.put(file.getOriginalFilename(), file.getBytes());
                }
            }

            List<PhotoUpdateRequest> updateRequests = new ArrayList<>();
            if (metadataJson != null && !metadataJson.isBlank()) {
                updateRequests = objectMapper.readValue(metadataJson, new TypeReference<List<PhotoUpdateRequest>>() {});
            }

            byte[] zipBytes = photoMetadataService.createZipWithUpdatedPhotos(rawFiles, updateRequests);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "modified_photos.zip");
            headers.setContentLength(zipBytes.length);

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error creating ZIP: " + e.getMessage()).getBytes());
        }
    }
}
