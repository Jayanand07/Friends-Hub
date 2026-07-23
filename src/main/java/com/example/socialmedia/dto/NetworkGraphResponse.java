package com.example.socialmedia.dto;

import java.util.List;

public class NetworkGraphResponse {

    private NodeDTO center;
    private List<NodeDTO> friends;
    private List<MutualNodeDTO> mutuals;

    public NetworkGraphResponse(NodeDTO center, List<NodeDTO> friends, List<MutualNodeDTO> mutuals) {
        this.center = center;
        this.friends = friends;
        this.mutuals = mutuals;
    }

    public NodeDTO getCenter() { return center; }
    public List<NodeDTO> getFriends() { return friends; }
    public List<MutualNodeDTO> getMutuals() { return mutuals; }

    public static class NodeDTO {
        private Long id;
        private String firstName;
        private String profilePicUrl;

        public NodeDTO(Long id, String firstName, String profilePicUrl) {
            this.id = id;
            this.firstName = firstName;
            this.profilePicUrl = profilePicUrl;
        }

        public Long getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getProfilePicUrl() { return profilePicUrl; }
    }

    public static class MutualNodeDTO extends NodeDTO {
        private List<Long> mutualFriendIds;

        public MutualNodeDTO(Long id, String firstName, String profilePicUrl, List<Long> mutualFriendIds) {
            super(id, firstName, profilePicUrl);
            this.mutualFriendIds = mutualFriendIds;
        }

        public List<Long> getMutualFriendIds() { return mutualFriendIds; }
    }
}
