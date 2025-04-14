package org.esprit.models;
import java.time.LocalDateTime;


public class Artwork {
    // Constants for validation
    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final double MIN_PRICE = 0.01;
    
    private int id;
    private int creatorId;
    private int ownerId;
    private int categoryId;
    private String title;
    private String description;
    private double price;
    private String imageName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Artwork() {}

    public Artwork(int id, int creatorId, int ownerId, int categoryId, String title, String description, double price, String imageName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        setTitle(title);
        setDescription(description);
        setPrice(price);
        this.imageName = imageName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { 
        if (creatorId <= 0) {
            throw new IllegalArgumentException("Creator ID must be a positive number");
        }
        this.creatorId = creatorId; 
    }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { 
        if (ownerId <= 0) {
            throw new IllegalArgumentException("Owner ID must be a positive number");
        }
        this.ownerId = ownerId; 
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { 
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be a positive number");
        }
        this.categoryId = categoryId; 
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { 
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (title.trim().length() < MIN_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be at least " + MIN_TITLE_LENGTH + " characters");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
        this.title = title.trim(); 
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { 
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        this.description = description.trim(); 
    }

    public double getPrice() { return price; }
    public void setPrice(double price) { 
        if (price < MIN_PRICE) {
            throw new IllegalArgumentException("Price must be at least " + MIN_PRICE);
        }
        this.price = price; 
    }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { 
        if (imageName == null || imageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Image name cannot be null or empty");
        }
        this.imageName = imageName; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { 
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation date cannot be null");
        }
        this.createdAt = createdAt; 
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { 
        if (updatedAt == null) {
            throw new IllegalArgumentException("Update date cannot be null");
        }
        this.updatedAt = updatedAt; 
    }

    /**
     * Validates all properties of the artwork object
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (title == null || title.trim().isEmpty() || title.trim().length() < MIN_TITLE_LENGTH || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be between " + MIN_TITLE_LENGTH + " and " + MAX_TITLE_LENGTH + " characters");
        }
        if (description == null || description.trim().isEmpty() || description.trim().length() < MIN_DESCRIPTION_LENGTH || description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be between " + MIN_DESCRIPTION_LENGTH + " and " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (price < MIN_PRICE) {
            throw new IllegalArgumentException("Price must be at least " + MIN_PRICE);
        }
        if (imageName == null || imageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Image name cannot be null or empty");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be a positive number");
        }
        if (creatorId <= 0) {
            throw new IllegalArgumentException("Creator ID must be a positive number");
        }
        if (ownerId <= 0) {
            throw new IllegalArgumentException("Owner ID must be a positive number");
        }
    }

    @Override
    public String toString() {
        return title + " by Creator #" + creatorId;
    }
}
