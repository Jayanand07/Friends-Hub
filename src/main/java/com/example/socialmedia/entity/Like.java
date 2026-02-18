package com.example.socialmedia.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "post_id" })
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Like() {
    }

    public Like(Long id, Post post, User user) {
        this.id = id;
        this.post = post;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static LikeBuilder builder() {
        return new LikeBuilder();
    }

    public static class LikeBuilder {
        private Long id;
        private Post post;
        private User user;

        LikeBuilder() {
        }

        public LikeBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LikeBuilder post(Post post) {
            this.post = post;
            return this;
        }

        public LikeBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Like build() {
            return new Like(id, post, user);
        }
    }
}
