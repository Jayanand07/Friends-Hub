package com.example.socialmedia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName
                .substring(originalName.lastIndexOf("."))
                .toLowerCase()
                .replaceAll("[^.a-z0-9]", "");
        }
        String filename = "posts/" + java.util.UUID.randomUUID().toString() + extension;

        // Upload to Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filename;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

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
            // Extract filename from URL
            // Public URL: {supabaseUrl}/storage/v1/object/public/{bucketName}/{filename}
            String prefix = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/";
            String filename = imageUrl.replace(prefix, "");

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
