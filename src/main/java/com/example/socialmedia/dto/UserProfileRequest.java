package com.example.socialmedia.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserProfileRequest {
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
    @Size(max = 200, message = "Website URL cannot exceed 200 characters")
    @Pattern(regexp = "^(https?://).*$", message = "Website must start with http:// or https://")
    private String website;
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;
    private String gender;
    private String dob;
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be exactly 10 digits")
    private String phone;
    private String profileVisibility;


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }
}
