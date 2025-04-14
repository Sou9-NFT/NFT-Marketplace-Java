package org.esprit.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Comment {
    private Integer id;
    private String content;
    private User user;
    private Blog blog;
    private LocalDateTime createdAt;

    // Default constructor
    public Comment() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public Comment(String content, User user, Blog blog) {
        this();
        this.content = content;
        this.user = user;
        this.blog = blog;
    }

    // Validation class
    public static class ValidationResult {
        private boolean valid;
        private Map<String, String> errors;

        public ValidationResult() {
            this.valid = true;
            this.errors = new HashMap<>();
        }

        public void addError(String field, String message) {
            this.valid = false;
            this.errors.put(field, message);
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    // Validation method
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // Content validation
        if (content == null || content.trim().isEmpty()) {
            result.addError("content", "Comment content cannot be empty");
        } else if (content.length() < 2) {
            result.addError("content", "Comment must be at least 2 characters long");
        } else if (content.length() > 1000) {
            result.addError("content", "Comment cannot be longer than 1000 characters");
        }

        // User validation
        if (user == null) {
            result.addError("user", "Comment must have an author");
        }

        // Blog validation
        if (blog == null) {
            result.addError("blog", "Comment must be associated with a blog post");
        }

        return result;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", user=" + (user != null ? user.getName() : "null") +
                ", blog=" + (blog != null ? blog.getTitle() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}
