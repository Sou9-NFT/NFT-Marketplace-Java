package org.esprit.models;
import java.time.LocalDateTime;


public class Artwork {
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
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageName = imageName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return title + " by Creator #" + creatorId;
    }
}
