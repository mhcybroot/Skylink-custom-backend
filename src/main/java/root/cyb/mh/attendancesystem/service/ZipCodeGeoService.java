package root.cyb.mh.attendancesystem.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ZipCodeGeoService {

    public static class GeoPoint {
        private final double latitude;
        private final double longitude;
        private final String city;
        private final String state;

        public GeoPoint(double latitude, double longitude, String city, String state) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.city = city;
            this.state = state;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getCity() {
            return city;
        }

        public String getState() {
            return state;
        }
    }

    // Built-in offline database for key US Zip codes / Cities
    private static final Map<String, GeoPoint> ZIP_DB = new HashMap<>();

    static {
        // Texas (Dallas / Fort Worth / Houston / Austin / San Antonio)
        ZIP_DB.put("75001", new GeoPoint(32.9612, -96.8292, "Addison", "TX"));
        ZIP_DB.put("75002", new GeoPoint(33.0906, -96.6111, "Allen", "TX"));
        ZIP_DB.put("75006", new GeoPoint(32.9556, -96.8906, "Carrollton", "TX"));
        ZIP_DB.put("75007", new GeoPoint(33.0031, -96.8972, "Carrollton", "TX"));
        ZIP_DB.put("75010", new GeoPoint(33.0489, -96.8778, "Carrollton", "TX"));
        ZIP_DB.put("75019", new GeoPoint(32.9692, -96.9858, "Coppell", "TX"));
        ZIP_DB.put("75024", new GeoPoint(33.0767, -96.8250, "Plano", "TX"));
        ZIP_DB.put("75025", new GeoPoint(33.0850, -96.7417, "Plano", "TX"));
        ZIP_DB.put("75034", new GeoPoint(33.1558, -96.8286, "Frisco", "TX"));
        ZIP_DB.put("75035", new GeoPoint(33.1517, -96.7644, "Frisco", "TX"));
        ZIP_DB.put("75040", new GeoPoint(32.9126, -96.6389, "Garland", "TX"));
        ZIP_DB.put("75041", new GeoPoint(32.8800, -96.6400, "Garland", "TX"));
        ZIP_DB.put("75042", new GeoPoint(32.9000, -96.6700, "Garland", "TX"));
        ZIP_DB.put("75043", new GeoPoint(32.8500, -96.6100, "Garland", "TX"));
        ZIP_DB.put("75044", new GeoPoint(32.9700, -96.6300, "Garland", "TX"));
        ZIP_DB.put("75063", new GeoPoint(32.9197, -96.9531, "Irving", "TX"));
        ZIP_DB.put("75080", new GeoPoint(32.9669, -96.7444, "Richardson", "TX"));
        ZIP_DB.put("75093", new GeoPoint(33.0306, -96.8042, "Plano", "TX"));
        ZIP_DB.put("75201", new GeoPoint(32.7864, -96.7970, "Dallas", "TX"));
        ZIP_DB.put("75202", new GeoPoint(32.7792, -96.8058, "Dallas", "TX"));
        ZIP_DB.put("75204", new GeoPoint(32.7981, -96.7903, "Dallas", "TX"));
        ZIP_DB.put("75205", new GeoPoint(32.8336, -96.7936, "Dallas", "TX"));
        ZIP_DB.put("75219", new GeoPoint(32.8139, -96.8089, "Dallas", "TX"));
        ZIP_DB.put("75220", new GeoPoint(32.8683, -96.8669, "Dallas", "TX"));
        ZIP_DB.put("75230", new GeoPoint(32.9039, -96.7903, "Dallas", "TX"));
        ZIP_DB.put("75240", new GeoPoint(32.9367, -96.7869, "Dallas", "TX"));
        ZIP_DB.put("75252", new GeoPoint(33.0039, -96.7861, "Dallas", "TX"));
        ZIP_DB.put("76102", new GeoPoint(32.7555, -97.3308, "Fort Worth", "TX"));
        ZIP_DB.put("77002", new GeoPoint(29.7569, -95.3656, "Houston", "TX"));
        ZIP_DB.put("78701", new GeoPoint(30.2711, -97.7437, "Austin", "TX"));
        ZIP_DB.put("78205", new GeoPoint(29.4241, -98.4936, "San Antonio", "TX"));

        // California
        ZIP_DB.put("90001", new GeoPoint(33.9731, -118.2479, "Los Angeles", "CA"));
        ZIP_DB.put("90012", new GeoPoint(34.0617, -118.2386, "Los Angeles", "CA"));
        ZIP_DB.put("90210", new GeoPoint(34.0901, -118.4065, "Beverly Hills", "CA"));
        ZIP_DB.put("94102", new GeoPoint(37.7793, -122.4193, "San Francisco", "CA"));

        // New York / Florida / Illinois
        ZIP_DB.put("10001", new GeoPoint(40.7501, -73.9996, "New York", "NY"));
        ZIP_DB.put("33101", new GeoPoint(25.7751, -80.1947, "Miami", "FL"));
        ZIP_DB.put("60601", new GeoPoint(41.8858, -87.6229, "Chicago", "IL"));
    }

    /**
     * Looks up coordinates by 5-digit zip code.
     */
    public GeoPoint getCoordinatesForZip(String rawZip) {
        if (rawZip == null) return null;
        String zip = rawZip.trim();
        if (zip.contains("-")) {
            zip = zip.split("-")[0].trim();
        }
        if (ZIP_DB.containsKey(zip)) {
            return ZIP_DB.get(zip);
        }

        // Algorithmic fallback estimate based on numeric range if exact zip is not in pre-seeded list
        try {
            int zipNum = Integer.parseInt(zip);
            // Rough regional center centroids fallback
            if (zipNum >= 75000 && zipNum <= 76999) {
                return new GeoPoint(32.7767 + ((zipNum % 100) * 0.005), -96.7970 + ((zipNum % 50) * 0.005), "Dallas Area (" + zip + ")", "TX");
            } else if (zipNum >= 77000 && zipNum <= 77999) {
                return new GeoPoint(29.7604, -95.3698, "Houston Area (" + zip + ")", "TX");
            } else if (zipNum >= 78000 && zipNum <= 78999) {
                return new GeoPoint(30.2672, -97.7431, "Austin/SA Area (" + zip + ")", "TX");
            } else if (zipNum >= 90000 && zipNum <= 96199) {
                return new GeoPoint(34.0522, -118.2437, "California Region (" + zip + ")", "CA");
            } else if (zipNum >= 100000 && zipNum <= 14999) {
                return new GeoPoint(40.7128, -74.0060, "New York Region (" + zip + ")", "NY");
            }
        } catch (Exception ignored) {
        }

        // Standard US centroid default if unknown string
        return new GeoPoint(32.7767, -96.7970, "Dallas Base (" + zip + ")", "TX");
    }

    /**
     * Reverse geocodes coordinates (lat, lng) to the nearest zip code and city in local DB.
     */
    public Map<String, String> findNearestZipForCoords(double lat, double lng) {
        Map<String, String> result = new HashMap<>();
        String closestZip = null;
        GeoPoint closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, GeoPoint> entry : ZIP_DB.entrySet()) {
            double dist = calculateHaversineDistanceMiles(lat, lng, entry.getValue().getLatitude(), entry.getValue().getLongitude());
            if (dist < minDistance) {
                minDistance = dist;
                closestZip = entry.getKey();
                closestPoint = entry.getValue();
            }
        }

        if (closestZip != null && closestPoint != null) {
            result.put("zipCode", closestZip);
            result.put("city", closestPoint.getCity());
            result.put("state", closestPoint.getState());
            result.put("area", closestPoint.getCity() + ", " + closestPoint.getState());
        }

        return result;
    }

    /**
     * Calculates distance in miles between two coordinates using the Haversine formula.
     */
    public double calculateHaversineDistanceMiles(double lat1, double lon1, double lat2, double lon2) {
        final double R = 3958.8; // Earth's radius in miles

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = R * c;
        return Math.round(distance * 10.0) / 10.0; // Round to 1 decimal place
    }
}
