package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import root.cyb.mh.attendancesystem.dto.CrewDistanceDto;
import root.cyb.mh.attendancesystem.model.Contractor;
import root.cyb.mh.attendancesystem.repository.ContractorRepository;
import root.cyb.mh.attendancesystem.repository.WorkOrderRepository;
import root.cyb.mh.attendancesystem.service.ZipCodeGeoService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CrewMapController {

    @Autowired
    private ContractorRepository contractorRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ZipCodeGeoService zipCodeGeoService;

    @GetMapping("/crew-map")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String renderCrewMapPage(Model model) {
        model.addAttribute("activeLink", "crew-map");
        model.addAttribute("contractors", contractorRepository.findByActiveTrue());
        return "admin-crew-map";
    }

    // API: Get all active crews with geocoding
    @GetMapping("/api/crew-map/crews")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<CrewDistanceDto>> getAllCrews() {
        List<Contractor> contractors = contractorRepository.findByActiveTrue();
        List<CrewDistanceDto> dtos = new ArrayList<>();

        for (Contractor c : contractors) {
            Double lat = c.getLatitude();
            Double lng = c.getLongitude();

            // Auto geocode zip code if lat/lng is null
            if ((lat == null || lng == null) && c.getZipCode() != null && !c.getZipCode().isBlank()) {
                ZipCodeGeoService.GeoPoint pt = zipCodeGeoService.getCoordinatesForZip(c.getZipCode());
                if (pt != null) {
                    lat = pt.getLatitude();
                    lng = pt.getLongitude();
                }
            }

            // Fallback default coordinates (Dallas area) if no zip or coords available
            if (lat == null || lng == null) {
                lat = 32.7767;
                lng = -96.7970;
            }

            long activeWos = workOrderRepository.countByContractorId(c.getId());

            CrewDistanceDto dto = CrewDistanceDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .email(c.getEmail())
                    .phone(c.getPhone())
                    .zipCode(c.getZipCode())
                    .area(c.getArea())
                    .latitude(lat)
                    .longitude(lng)
                    .serviceRadiusMiles(c.getServiceRadiusMiles() != null ? c.getServiceRadiusMiles() : 30)
                    .coverageZipCodes(c.getCoverageZipCodes())
                    .active(c.isActive())
                    .activeWorkOrdersCount(activeWos)
                    .build();

            dtos.add(dto);
        }

        return ResponseEntity.ok(dtos);
    }

    // API: Search nearest crews for a target Zip Code or query
    @GetMapping("/api/crew-map/nearest")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Map<String, Object>> getNearestCrews(
            @RequestParam("query") String query,
            @RequestParam(value = "maxDistance", required = false, defaultValue = "100") Double maxDistance) {

        Map<String, Object> response = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            response.put("error", "Search query or Zip Code is required");
            return ResponseEntity.badRequest().body(response);
        }

        ZipCodeGeoService.GeoPoint targetPoint = zipCodeGeoService.getCoordinatesForZip(query.trim());
        if (targetPoint == null) {
            response.put("error", "Could not locate Zip Code or address");
            return ResponseEntity.badRequest().body(response);
        }

        double targetLat = targetPoint.getLatitude();
        double targetLng = targetPoint.getLongitude();

        response.put("searchQuery", query);
        response.put("targetLatitude", targetLat);
        response.put("targetLongitude", targetLng);
        response.put("targetCity", targetPoint.getCity());
        response.put("targetState", targetPoint.getState());

        List<Contractor> contractors = contractorRepository.findByActiveTrue();
        List<CrewDistanceDto> rankedCrews = new ArrayList<>();

        for (Contractor c : contractors) {
            Double lat = c.getLatitude();
            Double lng = c.getLongitude();

            if ((lat == null || lng == null) && c.getZipCode() != null && !c.getZipCode().isBlank()) {
                ZipCodeGeoService.GeoPoint pt = zipCodeGeoService.getCoordinatesForZip(c.getZipCode());
                if (pt != null) {
                    lat = pt.getLatitude();
                    lng = pt.getLongitude();
                }
            }

            if (lat == null || lng == null) {
                lat = 32.7767;
                lng = -96.7970;
            }

            double distanceMiles = zipCodeGeoService.calculateHaversineDistanceMiles(targetLat, targetLng, lat, lng);
            int radius = c.getServiceRadiusMiles() != null ? c.getServiceRadiusMiles() : 30;
            boolean isInRange = distanceMiles <= radius;

            // Direct Zipcode match override
            if (c.getCoverageZipCodes() != null && c.getCoverageZipCodes().contains(query.trim())) {
                isInRange = true;
            }

            long activeWos = workOrderRepository.countByContractorId(c.getId());

            CrewDistanceDto dto = CrewDistanceDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .email(c.getEmail())
                    .phone(c.getPhone())
                    .zipCode(c.getZipCode())
                    .area(c.getArea())
                    .latitude(lat)
                    .longitude(lng)
                    .serviceRadiusMiles(radius)
                    .coverageZipCodes(c.getCoverageZipCodes())
                    .active(c.isActive())
                    .distanceMiles(distanceMiles)
                    .isInRange(isInRange)
                    .activeWorkOrdersCount(activeWos)
                    .build();

            rankedCrews.add(dto);
        }

        // Sort by distance ascending
        rankedCrews.sort(Comparator.comparingDouble(CrewDistanceDto::getDistanceMiles));

        response.put("crews", rankedCrews);
        return ResponseEntity.ok(response);
    }

    // API: Update Contractor Base Location & Coverage Settings
    @PostMapping("/api/crew-map/update-crew")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Map<String, Object>> updateCrewLocation(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        if (!payload.containsKey("id")) {
            response.put("error", "Contractor ID is required");
            return ResponseEntity.badRequest().body(response);
        }

        Long contractorId = Long.valueOf(payload.get("id").toString());
        Optional<Contractor> opt = contractorRepository.findById(contractorId);

        if (opt.isEmpty()) {
            response.put("error", "Contractor not found");
            return ResponseEntity.status(404).body(response);
        }

        Contractor contractor = opt.get();

        if (payload.containsKey("zipCode")) {
            contractor.setZipCode((String) payload.get("zipCode"));
            ZipCodeGeoService.GeoPoint pt = zipCodeGeoService.getCoordinatesForZip(contractor.getZipCode());
            if (pt != null) {
                contractor.setLatitude(pt.getLatitude());
                contractor.setLongitude(pt.getLongitude());
            }
        }

        if (payload.containsKey("area")) {
            contractor.setArea((String) payload.get("area"));
        }

        if (payload.containsKey("phone")) {
            contractor.setPhone((String) payload.get("phone"));
        }

        if (payload.containsKey("serviceRadiusMiles") && payload.get("serviceRadiusMiles") != null) {
            contractor.setServiceRadiusMiles(Integer.parseInt(payload.get("serviceRadiusMiles").toString()));
        }

        if (payload.containsKey("coverageZipCodes")) {
            contractor.setCoverageZipCodes((String) payload.get("coverageZipCodes"));
        }

        if (payload.containsKey("latitude") && payload.get("latitude") != null) {
            contractor.setLatitude(Double.parseDouble(payload.get("latitude").toString()));
        }

        if (payload.containsKey("longitude") && payload.get("longitude") != null) {
            contractor.setLongitude(Double.parseDouble(payload.get("longitude").toString()));
        }

        contractorRepository.save(contractor);

        response.put("success", true);
        response.put("message", "Crew location and coverage updated successfully");
        return ResponseEntity.ok(response);
    }
}
