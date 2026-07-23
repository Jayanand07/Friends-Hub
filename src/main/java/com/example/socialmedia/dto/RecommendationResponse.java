package com.example.socialmedia.dto;

import java.util.List;

public class RecommendationResponse {
    private Long id;
    private String name;
    private String profilePicUrl;
    private String location;
    private String bio;
    private int matchScore;
    private List<String> matchReasons;

    public RecommendationResponse(Long id, String name, String profilePicUrl,
                                  String location, String bio, int matchScore, List<String> matchReasons) {
        this.id = id;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.location = location;
        this.bio = bio;
        this.matchScore = matchScore;
        this.matchReasons = matchReasons;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getLocation() { return location; }
    public String getBio() { return bio; }
    public int getMatchScore() { return matchScore; }
    public List<String> getMatchReasons() { return matchReasons; }
}
