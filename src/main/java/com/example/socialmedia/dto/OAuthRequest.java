package com.example.socialmedia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    private String name;
    private String googleId;
    @NotBlank(message = "idToken is required")
    private String idToken;

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getIdToken() { return idToken; }
}
