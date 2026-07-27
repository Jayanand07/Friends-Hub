package com.example.socialmedia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PublicKeyRequest {
    @NotBlank(message = "Public key is required")
    @Size(max = 4096, message = "Public key cannot exceed 4096 characters")
    private String publicKey;

    public PublicKeyRequest() {
    }

    public PublicKeyRequest(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
