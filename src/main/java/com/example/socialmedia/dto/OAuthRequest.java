package com.example.socialmedia.dto;

import lombok.Data;

@Data
public class OAuthRequest {
    private String email;
    private String name;
    private String idToken;

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getIdToken() { return idToken; }
}

