package org.esprit.models;

import java.time.LocalDateTime;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BetSession {
    // Constants for validation
    public static final double MIN_PRICE = 0.01;
    public static final String[] VALID_STATUSES = {"pending", "active", "ended", "cancelled"};
    
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final ObjectProperty<User> author = new SimpleObjectProperty<>();
    private final ObjectProperty<Artwork> artwork = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<LocalDateTime> endTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> startTime = new SimpleObjectProperty<>();
    private final DoubleProperty initialPrice = new SimpleDoubleProperty();
    private final DoubleProperty currentPrice = new SimpleDoubleProperty();    private final StringProperty status = new SimpleStringProperty("pending");
    private final BooleanProperty mysteriousMode = new SimpleBooleanProperty(false);
    private final StringProperty generatedDescription = new SimpleStringProperty("");

    // Add numberOfBids property
    private final IntegerProperty numberOfBids = new SimpleIntegerProperty(0);

    public int getNumberOfBids() {
        return numberOfBids.get();
    }

    public void setNumberOfBids(int numberOfBids) {
        this.numberOfBids.set(numberOfBids);
    }

    public IntegerProperty numberOfBidsProperty() {
        return numberOfBids;
    }

    // Constructor
    public BetSession() {
        this.currentPrice.set(this.initialPrice.get());
        updateStatus();
    }

    // Getters and Setters with JavaFX properties and validation

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be a positive number");
        }
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public User getAuthor() {
        return author.get();
    }

    public void setAuthor(User author) {
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        this.author.set(author);
    }

    public ObjectProperty<User> authorProperty() {
        return author;
    }

    public Artwork getArtwork() {
        return artwork.get();
    }

    public void setArtwork(Artwork artwork) {
        if (artwork == null) {
            throw new IllegalArgumentException("Artwork cannot be null");
        }
        this.artwork.set(artwork);
    }

    public ObjectProperty<Artwork> artworkProperty() {
        return artwork;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation date cannot be null");
        }
        this.createdAt.set(createdAt);
    }

    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }   

    public LocalDateTime getEndTime() {
        return endTime.get();
    }

    public void setEndTime(LocalDateTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (startTime.get() != null && endTime.isBefore(startTime.get())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.endTime.set(endTime);
        updateStatus();
    }

    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }    public LocalDateTime getStartTime() {
        return startTime.get();
    }
    
    public void setStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        // Only check if start time is in future for new bet sessions (ones without IDs)
        if (getId() == 0 && startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }
        if (endTime.get() != null && startTime.isAfter(endTime.get())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime.set(startTime);
        updateStatus();
    }

    public ObjectProperty<LocalDateTime> startTimeProperty() {
        return startTime;
    }

    public double getInitialPrice() {
        return initialPrice.get();
    }

    public void setInitialPrice(double initialPrice) {
        if (initialPrice < MIN_PRICE) {
            throw new IllegalArgumentException("Initial price must be at least " + MIN_PRICE);
        }
        this.initialPrice.set(initialPrice);
        // If current price is not set yet or less than initial, set it to initial
        if (currentPrice.get() < initialPrice) {
            setCurrentPrice(initialPrice);
        }
    }

    public DoubleProperty initialPriceProperty() {
        return initialPrice;
    }

    public double getCurrentPrice() {
        return currentPrice.get();
    }

    public void setCurrentPrice(double currentPrice) {
        if (currentPrice < initialPrice.get()) {
            throw new IllegalArgumentException("Current price cannot be less than initial price");
        }
        this.currentPrice.set(currentPrice);
    }

    public DoubleProperty currentPriceProperty() {
        return currentPrice;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        boolean validStatus = false;
        for (String validValue : VALID_STATUSES) {
            if (validValue.equals(status)) {
                validStatus = true;
                break;
            }
        }
        
        if (!validStatus) {
            throw new IllegalArgumentException("Invalid status value. Must be one of: " + String.join(", ", VALID_STATUSES));
        }
        
        this.status.set(status);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public boolean isMysteriousMode() {
        return mysteriousMode.get();
    }

    public void setMysteriousMode(boolean mysteriousMode) {
        this.mysteriousMode.set(mysteriousMode);
    }    public BooleanProperty mysteriousModeProperty() {
        return mysteriousMode;
    }
    
    public String getGeneratedDescription() {
        return generatedDescription.get();
    }
    
    public void setGeneratedDescription(String generatedDescription) {
        this.generatedDescription.set(generatedDescription);
    }
    
    public StringProperty generatedDescriptionProperty() {
        return generatedDescription;
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
    
    /**
     * Validates all properties of the bet session object
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (author.get() == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        
        if (artwork.get() == null) {
            throw new IllegalArgumentException("Artwork cannot be null");
        }
        
        if (startTime.get() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        
        if (endTime.get() == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        
        if (endTime.get().isBefore(startTime.get())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (initialPrice.get() < MIN_PRICE) {
            throw new IllegalArgumentException("Initial price must be at least " + MIN_PRICE);
        }
        
        if (currentPrice.get() < initialPrice.get()) {
            throw new IllegalArgumentException("Current price cannot be less than initial price");
        }
    }
}
