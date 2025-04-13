package org.esprit.models;

import java.time.LocalDateTime;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BetSession {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final ObjectProperty<User> author = new SimpleObjectProperty<>();
    private final ObjectProperty<Artwork> artwork = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<LocalDateTime> endTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> startTime = new SimpleObjectProperty<>();
    private final DoubleProperty initialPrice = new SimpleDoubleProperty();
    private final DoubleProperty currentPrice = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty("pending");

    // Constructor
    public BetSession() {
        this.currentPrice.set(this.initialPrice.get());
        updateStatus();
    }

    // Getters and Setters with JavaFX properties

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public User getAuthor() {
        return author.get();
    }

    public void setAuthor(User author) {
        this.author.set(author);
    }

    public ObjectProperty<User> authorProperty() {
        return author;
    }

    public Artwork getArtwork() {
        return artwork.get();
    }

    public void setArtwork(Artwork artwork) {
        this.artwork.set(artwork);
    }

    public ObjectProperty<Artwork> artworkProperty() {
        return artwork;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }

    public LocalDateTime getEndTime() {
        return endTime.get();
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime.set(endTime);
        updateStatus();
    }

    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime.get();
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime.set(startTime);
    }

    public ObjectProperty<LocalDateTime> startTimeProperty() {
        return startTime;
    }

    public double getInitialPrice() {
        return initialPrice.get();
    }

    public void setInitialPrice(double initialPrice) {
        this.initialPrice.set(initialPrice);
    }

    public DoubleProperty initialPriceProperty() {
        return initialPrice;
    }

    public double getCurrentPrice() {
        return currentPrice.get();
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice.set(currentPrice);
    }

    public DoubleProperty currentPriceProperty() {
        return currentPrice;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public StringProperty statusProperty() {
        return status;
    }

    // Update status based on time
    public void updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (endTime.get() != null && endTime.get().isBefore(now)) {
            status.set("ended");
        } else if (startTime.get() != null && startTime.get().isAfter(now)) {
            status.set("pending");
        } else {
            status.set("active");
        }
    }
}
