package root.cyb.mh.attendancesystem.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PhotoMetadataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class PhotoUpdateRequest {
        public String filename;
        public Double latitude;
        public Double longitude;
        public Double altitude;
        public String cameraMake;
        public String cameraModel;
        public String software;
        public String artist;
        public String copyright;
        public String description;
        public String uploadBy;
        public String uploadTimestamp;
        public String dateTimeOriginal;
        public String uploadFrom;
        public String duplicate;
        public Map<String, String> customFields = new HashMap<>();
    }

    /**
     * Reads comprehensive metadata summary from image byte stream using Drew Noakes metadata-extractor.
     */
    public Map<String, Object> extractMetadata(byte[] imageBytes, String filename) {
        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("size", imageBytes.length);

        Map<String, String> allTags = new LinkedHashMap<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            Metadata metadata = ImageMetadataReader.readMetadata(bais);

            for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
                for (com.drew.metadata.Tag tag : directory.getTags()) {
                    allTags.put(tag.getTagName(), tag.getDescription());
                }
            }
            result.put("allTags", allTags);

            // Read GPS
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                GeoLocation geoLocation = gpsDir.getGeoLocation();
                if (geoLocation != null) {
                    result.put("latitude", geoLocation.getLatitude());
                    result.put("longitude", geoLocation.getLongitude());
                }
            }

            // Read Camera & IFD0 info
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                result.put("cameraMake", ifd0.getString(ExifIFD0Directory.TAG_MAKE));
                result.put("cameraModel", ifd0.getString(ExifIFD0Directory.TAG_MODEL));
                result.put("software", ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE));
                result.put("artist", ifd0.getString(ExifIFD0Directory.TAG_ARTIST));
                result.put("copyright", ifd0.getString(ExifIFD0Directory.TAG_COPYRIGHT));
                result.put("description", ifd0.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION));
            }

            // Read SubIFD Info (Exposure, Aperture, ISO, Focal Length, Date Original, Lens, User Comment)
            com.drew.metadata.exif.ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory.class);
            if (subIfd != null) {
                String dateOrig = subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateOrig != null) {
                    result.put("dateOriginal", dateOrig);
                    result.put("dateTimeOriginal", dateOrig);
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED) != null) {
                    result.put("uploadTimestamp", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_EXPOSURE_TIME) != null) {
                    result.put("exposureTime", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FNUMBER) != null) {
                    result.put("aperture", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FNUMBER));
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) != null) {
                    result.put("iso", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FOCAL_LENGTH) != null) {
                    result.put("focalLength", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                }
                if (subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_LENS_MODEL) != null) {
                    result.put("lensModel", subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_LENS_MODEL));
                }

                // Parse clean key-value text from UserComment tag (e.g. UploadBy=Hasan; UploadFrom=PPW 3; Duplicate=True;)
                String userComment = subIfd.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_USER_COMMENT);
                if (userComment != null && userComment.contains("=")) {
                    Map<String, String> customMap = new HashMap<>();
                    String[] parts = userComment.split(";");
                    for (String part : parts) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2) {
                            String k = kv[0].trim();
                            String v = kv[1].trim();
                            if ("UploadBy".equalsIgnoreCase(k)) result.put("uploadBy", v);
                            else if ("UploadFrom".equalsIgnoreCase(k)) result.put("uploadFrom", v);
                            else if ("Duplicate".equalsIgnoreCase(k)) result.put("duplicate", v);
                            else if ("UploadTimestamp".equalsIgnoreCase(k)) result.put("uploadTimestamp", v);
                            else if (!k.isEmpty()) customMap.put(k, v);
                        }
                    }
                    if (!customMap.isEmpty()) {
                        result.put("customFields", customMap);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore metadata read errors for non-JPEG or unsupported formats
        }
        return result;
    }

    /**
     * Updates EXIF metadata in a JPEG byte array using Apache Commons Imaging.
     */
    public byte[] updateImageMetadata(byte[] imageBytes, PhotoUpdateRequest req) {
        if (imageBytes == null || imageBytes.length == 0) {
            return imageBytes;
        }

        try {
            TiffOutputSet outputSet = null;
            ImageMetadata metadata = Imaging.getMetadata(imageBytes);
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                TiffImageMetadata tiffMetadata = jpegMetadata.getExif();
                if (tiffMetadata != null) {
                    outputSet = tiffMetadata.getOutputSet();
                }
            }

            if (outputSet == null) {
                outputSet = new TiffOutputSet();
            }

            // Update GPS Coordinates with Longitude Normalization [-180, 180]
            if (req.latitude != null && req.longitude != null) {
                double lat = req.latitude;
                double lng = req.longitude;
                if (lat > 90.0) lat = 90.0;
                if (lat < -90.0) lat = -90.0;
                while (lng > 180.0) lng -= 360.0;
                while (lng < -180.0) lng += 360.0;
                outputSet.setGpsInDegrees(lng, lat);
            }

            TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();

            // Update Root tags (Make, Model, Software, Artist, Copyright, ImageDescription)
            if (req.cameraMake != null && !req.cameraMake.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_MAKE);
                rootDir.add(TiffTagConstants.TIFF_TAG_MAKE, req.cameraMake);
            }
            if (req.cameraModel != null && !req.cameraModel.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_MODEL);
                rootDir.add(TiffTagConstants.TIFF_TAG_MODEL, req.cameraModel);
            }
            if (req.software != null && !req.software.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_SOFTWARE);
                rootDir.add(TiffTagConstants.TIFF_TAG_SOFTWARE, req.software);
            }
            
            String artistVal = (req.artist != null && !req.artist.isBlank()) ? req.artist : req.uploadBy;
            if (artistVal != null && !artistVal.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_ARTIST);
                rootDir.add(TiffTagConstants.TIFF_TAG_ARTIST, artistVal);
            }
            if (req.copyright != null && !req.copyright.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_COPYRIGHT);
                rootDir.add(TiffTagConstants.TIFF_TAG_COPYRIGHT, req.copyright);
            }
            if (req.description != null && !req.description.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION);
                rootDir.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, req.description);
            }

            // Update EXIF SubIFD Directory (DateTimeOriginal, DateTimeDigitized & Clean Key-Value UserComment)
            TiffOutputDirectory exifDir = outputSet.getOrCreateExifDirectory();
            if (req.dateTimeOriginal != null && !req.dateTimeOriginal.isBlank()) {
                rootDir.removeField(TiffTagConstants.TIFF_TAG_DATE_TIME);
                rootDir.add(TiffTagConstants.TIFF_TAG_DATE_TIME, req.dateTimeOriginal);

                exifDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                exifDir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, req.dateTimeOriginal);

                try {
                    String[] parts = req.dateTimeOriginal.trim().split(" ");
                    if (parts.length > 0) {
                        TiffOutputDirectory gpsDir = outputSet.getOrCreateGpsDirectory();
                        gpsDir.removeField(org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_DATE_STAMP);
                        gpsDir.add(org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants.GPS_TAG_GPS_DATE_STAMP, parts[0]);
                    }
                } catch (Exception ignored) {}
            }
            if (req.uploadTimestamp != null && !req.uploadTimestamp.isBlank()) {
                exifDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
                exifDir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, req.uploadTimestamp);
            } else if (req.dateTimeOriginal != null && !req.dateTimeOriginal.isBlank()) {
                exifDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
                exifDir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, req.dateTimeOriginal);
            }

            StringBuilder commentBuilder = new StringBuilder();
            if (req.uploadBy != null && !req.uploadBy.isBlank()) commentBuilder.append("UploadBy=").append(req.uploadBy).append("; ");
            if (req.uploadTimestamp != null && !req.uploadTimestamp.isBlank()) commentBuilder.append("UploadTimestamp=").append(req.uploadTimestamp).append("; ");
            if (req.uploadFrom != null && !req.uploadFrom.isBlank()) commentBuilder.append("UploadFrom=").append(req.uploadFrom).append("; ");
            if (req.duplicate != null && !req.duplicate.isBlank()) commentBuilder.append("Duplicate=").append(req.duplicate).append("; ");
            
            if (req.customFields != null) {
                for (Map.Entry<String, String> entry : req.customFields.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isBlank()) {
                        commentBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
                    }
                }
            }

            if (commentBuilder.length() > 0) {
                exifDir.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
                exifDir.add(ExifTagConstants.EXIF_TAG_USER_COMMENT, commentBuilder.toString().trim());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ExifRewriter().updateExifMetadataLossless(imageBytes, baos, outputSet);
            return baos.toByteArray();

        } catch (Exception e) {
            // Fallback: return original raw image bytes if EXIF rewrite fails (e.g. PNG / non-JPEG)
            return imageBytes;
        }
    }

    /**
     * Processes list of images with metadata updates and streams directly into a ZIP file.
     */
    public byte[] createZipWithUpdatedPhotos(Map<String, byte[]> rawFiles, List<PhotoUpdateRequest> updateRequests) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Map<String, PhotoUpdateRequest> reqMap = new HashMap<>();
            if (updateRequests != null) {
                for (PhotoUpdateRequest req : updateRequests) {
                    reqMap.put(req.filename, req);
                }
            }

            int counter = 1;
            for (Map.Entry<String, byte[]> entry : rawFiles.entrySet()) {
                String originalFilename = entry.getKey();
                byte[] rawBytes = entry.getValue();

                PhotoUpdateRequest req = reqMap.get(originalFilename);
                byte[] finalBytes = (req != null) ? updateImageMetadata(rawBytes, req) : rawBytes;

                String entryName = (originalFilename != null && !originalFilename.isBlank()) ? originalFilename : ("photo_" + counter + ".jpg");
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                zos.write(finalBytes);
                zos.closeEntry();
                counter++;
            }
            zos.finish();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ZIP archive", e);
        }
        return baos.toByteArray();
    }
}
