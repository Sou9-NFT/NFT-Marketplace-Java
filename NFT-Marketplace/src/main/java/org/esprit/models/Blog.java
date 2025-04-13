package org.esprit.models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Blog {
    private Integer id;
    private String title;
    private String translatedTitle;
    private String content;
    private String translatedContent;
    private LocalDate date;
    private User user;
    private String imageFilename;
    private String translationLanguage;

    // Default constructor
    public Blog() {
        this.date = LocalDate.now();
    }

    // Constructor with required fields
    public Blog(String title, String content, User user) {
        this();
        this.title = title;
        this.content = content;
        this.user = user;
    }

    // Validation class to store validation results
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
        
        // Title validation
        if (title == null || title.trim().isEmpty()) {
            result.addError("title", "Title cannot be empty");
        } else if (title.length() < 3) {
            result.addError("title", "Title must be at least 3 characters long");
        } else if (title.length() > 255) {
            result.addError("title", "Title cannot be longer than 255 characters");
        } else if (containsProfanity(title)) {
            result.addError("title", "Title contains inappropriate language");
        }

        // Content validation
        if (content == null || content.trim().isEmpty()) {
            result.addError("content", "Content cannot be empty");
        } else if (content.length() < 10) {
            result.addError("content", "Content must be at least 10 characters long");
        } else if (containsProfanity(content)) {
            result.addError("content", "Content contains inappropriate language");
        }

        // Date validation
        if (date == null) {
            result.addError("date", "Date cannot be empty");
        }

        // User validation
        if (user == null) {
            result.addError("user", "Blog must have an author");
        }

        return result;
    }

    // Simple profanity check (you should implement a more comprehensive solution)
    private boolean containsProfanity(String text) {
        // Add your profanity detection logic here
        return false;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public void setTranslatedTitle(String translatedTitle) {
        this.translatedTitle = translatedTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
    }

    public void setTranslationLanguage(String translationLanguage) {
        this.translationLanguage = translationLanguage;
    }

    @Override
    public String toString() {
        return title + (translatedTitle != null ? " / " + translatedTitle : "");
    }
}
