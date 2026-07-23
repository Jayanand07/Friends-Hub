package com.example.socialmedia.dto;

public class SearchUserResponse {
    private Long id;
    private String name;
    private String profilePicUrl;
    private String location;
    private String bio;
    private int mutualCount;
    private boolean isFollowing;

    public SearchUserResponse(Long id, String name, String profilePicUrl,
                              String location, String bio, int mutualCount, boolean isFollowing) {
        this.id = id;
        this.name = name;
        this.profilePicUrl = profilePicUrl;
        this.location = location;
        this.bio = bio;
        this.mutualCount = mutualCount;
        this.isFollowing = isFollowing;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getLocation() { return location; }
    public String getBio() { return bio; }
    public int getMutualCount() { return mutualCount; }
    public boolean isFollowing() { return isFollowing; }
}
