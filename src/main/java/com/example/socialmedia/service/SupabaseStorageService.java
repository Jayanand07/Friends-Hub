package com.example.socialmedia.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

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

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = "posts/" + UUID.randomUUID() + extension;

        // Upload to Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filename;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

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
            System.out.println("Deleted image from Supabase: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to delete image from Supabase: " + e.getMessage());
        }
    }
}
