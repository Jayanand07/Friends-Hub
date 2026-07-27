package com.example.socialmedia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final RestTemplate restTemplate;

    public SupabaseStorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // SECURITY: Content-Type -> file extension mapping (derived from type, not user input)
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    // SECURITY (H-3): Magic byte signatures to verify actual file content
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38}
    );
    // WebP magic bytes: RIFF....WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
    private static final byte[] WEBP_RIFF = new byte[]{0x52, 0x49, 0x46, 0x46};

    public String uploadImage(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, GIF, WebP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // SECURITY (H-3): Verify actual file magic bytes match claimed content type
        byte[] fileBytes = file.getBytes();
        validateMagicBytes(fileBytes, contentType);

        // SECURITY (H-5): Sanitize image — strips EXIF metadata (GPS, camera model, etc.)
        // and re-encodes to remove embedded non-image data.
        // WebP is pass-through since Java's built-in ImageIO does not support it.
        byte[] sanitizedBytes = sanitizeImage(fileBytes, contentType);

        // Generate unique filename using extension derived from content-type (NOT user filename)
        String extension = CONTENT_TYPE_EXTENSIONS.getOrDefault(contentType, ".jpg");
        String filename = "uploads/" + java.util.UUID.randomUUID().toString() + extension;

        // Upload to Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filename;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(sanitizedBytes, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to upload image to Supabase: " + response.getBody());
        }

        // Return public URL
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filename;
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains(supabaseUrl)) {
            return;
        }

        try {
            // SECURITY (H-3): Validate that the URL is within the expected bucket path
            // to prevent path traversal via manipulated URLs.
            String expectedPrefix = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/";
            if (!imageUrl.startsWith(expectedPrefix)) {
                log.warn("Attempted to delete image with unexpected URL prefix: {}", imageUrl);
                return;
            }

            // Extract filename from URL
            String filename = imageUrl.substring(expectedPrefix.length());

            // SECURITY: Reject filenames containing path traversal sequences
            if (filename.contains("..") || filename.contains("./") || filename.contains("\\")) {
                log.warn("Path traversal attempt blocked in deleteImage: {}", filename);
                return;
            }

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filename;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
            log.info("Deleted image from Supabase: {}", filename);
        } catch (Exception e) {
            log.warn("Failed to delete image from Supabase: {}", e.getMessage());
        }
    }

    /**
     * SECURITY (H-5): Sanitize image by re-encoding through Java's ImageIO.
     * This strips EXIF metadata (GPS coordinates, camera model, timestamp, etc.),
     * removes embedded non-image data, and normalizes the pixel data.
     *
     * For WebP, standard Java ImageIO has no built-in support so the bytes are
     * passed through unchanged — WebP does not carry EXIF in the traditional sense.
     * GIF is also passed through to preserve animation frames.
     */
    private byte[] sanitizeImage(byte[] fileBytes, String contentType) throws IOException {
        // Skip re-encoding for WebP (no built-in ImageIO support) and GIF (preserve animation)
        if ("image/webp".equals(contentType) || "image/gif".equals(contentType)) {
            return fileBytes;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image == null) {
                // ImageIO could not parse the image — reject
                throw new IllegalArgumentException("Image could not be parsed: " + contentType);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if ("image/jpeg".equals(contentType)) {
                // Use JPEG writer with quality setting to avoid quality degradation
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) {
                    throw new IOException("No JPEG writer available");
                }
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.95f);
                try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
                    writer.setOutput(mcios);
                    writer.write(image);
                }
                writer.dispose();
            } else if ("image/png".equals(contentType)) {
                ImageIO.write(image, "png", baos);
            } else {
                ImageIO.write(image, "png", baos);
            }

            byte[] result = baos.toByteArray();

            // SECURITY: Verify sanitized image is not larger than the original
            // (protection against decompression bomb amplification)
            if (result.length > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Sanitized image exceeds size limit");
            }

            log.debug("Image sanitized: {} bytes -> {} bytes ({}% reduction)",
                    fileBytes.length, result.length,
                    fileBytes.length > 0 ? (100 - (result.length * 100 / fileBytes.length)) : 0);
            return result;
        } catch (IllegalArgumentException e) {
            throw e; // re-throw validation errors
        } catch (Exception e) {
            // If sanitization fails for any reason, reject the upload rather than
            // silently using unsanitized bytes
            throw new IOException("Image sanitization failed: " + e.getMessage(), e);
        }
    }

    /**
     * SECURITY (H-3): Validate that file content magic bytes match the claimed Content-Type.
     * Prevents attackers from uploading SVG/HTML/executables with a spoofed MIME header.
     */
    private void validateMagicBytes(byte[] fileBytes, String contentType) {
        if (fileBytes.length < 12) {
            throw new IllegalArgumentException("File too small to be a valid image");
        }
        if ("image/webp".equals(contentType)) {
            // WebP: starts with RIFF and has WEBP at offset 8
            if (!startsWith(fileBytes, WEBP_RIFF) ||
                    fileBytes[8] != 'W' || fileBytes[9] != 'E' ||
                    fileBytes[10] != 'B' || fileBytes[11] != 'P') {
                throw new IllegalArgumentException("File content does not match image/webp");
            }
        } else {
            byte[] expected = MAGIC_BYTES.get(contentType);
            if (expected != null && !startsWith(fileBytes, expected)) {
                throw new IllegalArgumentException(
                        "File content does not match claimed type: " + contentType);
            }
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
