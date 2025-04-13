package org.esprit.models;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Raffle {
    private Integer id;
    private Integer artworkId;
    private Date startTime;
    private Date endTime;
    private String status = "active";
    private User creator;
    private Date createdAt;
    private Integer winnerId;
    private List<User> participants;
    private String creatorName;
    private String title;
    private String raffleDescription;
    private String artworkTitle;  

    // Validation constants
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_DESCRIPTION_LENGTH = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    // Add a private flag to track when we're loading from database
    private boolean loadingFromDatabase = false;
    
    // Method to set the loading flag (called by RaffleService when loading)
    public void setLoadingFromDatabase(boolean loading) {
        this.loadingFromDatabase = loading;
    }
    
    // Method to check if currently loading from database
    private boolean isLoadingFromDatabase() {
        return loadingFromDatabase;
    }

    public Raffle() {
        this.participants = new ArrayList<>();
        this.createdAt = new Date();
        this.startTime = new Date();
    }

    public Raffle(String title, String raffleDescription, Date endTime, User creator, Integer artworkId) {
        this();
        this.title = title;
        this.raffleDescription = raffleDescription;
        this.endTime = endTime;
        this.creator = creator;
        this.creatorName = creator.getName();
        this.artworkId = artworkId;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getArtworkId() {
        return artworkId;
    }

    public void setArtworkId(Integer artworkId) {
        this.artworkId = artworkId;
        if (id != null) {
            validateArtworkId();
        }
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        // Only validate if this is an existing raffle (has ID) AND we're not loading from database
        // Skip validation when loading from database
        if (id != null && !isLoadingFromDatabase()) {
            validateDates();
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        // Validate status change first
        validateStatusChange(status);
        this.status = status;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
        if (creator != null) {
            this.creatorName = creator.getName();
        }
        if (id != null) {
            validateCreator();
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public void addParticipant(User participant) {
        if (!this.participants.contains(participant)) {
            this.participants.add(participant);
        }
    }

    public void removeParticipant(User participant) {
        this.participants.remove(participant);
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        if (id != null) { // Only validate if not a new object
            validateTitle();
        }
    }

    public String getRaffleDescription() {
        return raffleDescription;
    }

    public void setRaffleDescription(String raffleDescription) {
        this.raffleDescription = raffleDescription;
        // Only validate if this is an existing raffle (has ID) AND we're not loading from database
        if (id != null && !isLoadingFromDatabase()) {
            validateDescription();
        }
    }

    public String getArtworkTitle() {
        return artworkTitle;
    }

    public void setArtworkTitle(String artworkTitle) {
        this.artworkTitle = artworkTitle;
    }

    public boolean shouldContinueIterating() {
        return "active".equals(status) && endTime.after(new Date());
    }

    public void validateForCreation() throws IllegalArgumentException {
        validateTitle();
        validateDescription();
        validateDates();
        validateCreator();
        validateArtworkId();
    }

    public void validateForUpdate() throws IllegalArgumentException {
        validateTitle();
        validateDescription();
        validateDates();
    }

    private void validateTitle() throws IllegalArgumentException {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (title.length() < MIN_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be at least " + MIN_TITLE_LENGTH + " characters long");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
    }

    private void validateDescription() throws IllegalArgumentException {
        if (raffleDescription == null || raffleDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (raffleDescription.length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters long");
        }
        if (raffleDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    private void validateDates() throws IllegalArgumentException {
        Date now = new Date();
        
        if (startTime == null) {
            throw new IllegalArgumentException("Start time must be set");
        }
        
        if (endTime == null) {
            throw new IllegalArgumentException("End time must be set");
        }
        
        if (endTime.before(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        
        // Only check if end time is in future for active raffles
        // Don't validate for existing raffles being loaded from database
        if ("active".equals(status) && endTime.before(now) && !loadingFromDatabase) {
            throw new IllegalArgumentException("End time must be in the future for active raffles");
        }
    }

    private void validateCreator() throws IllegalArgumentException {
        if (creator == null) {
            throw new IllegalArgumentException("Creator must be set");
        }
        if (creator.getId() <= 0) {
            throw new IllegalArgumentException("Creator must have a valid ID");
        }
    }

    private void validateArtworkId() throws IllegalArgumentException {
        if (artworkId == null) {
            throw new IllegalArgumentException("Artwork ID must be set");
        }
        if (artworkId <= 0) {
            throw new IllegalArgumentException("Artwork ID must be a positive number");
        }
    }

    public void validateStatusChange(String newStatus) throws IllegalArgumentException {
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        
        // Define allowed status values
        List<String> allowedStatuses = Arrays.asList("active", "ended", "cancelled");
        if (!allowedStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status. Allowed values are: " + String.join(", ", allowedStatuses));
        }
        
        // Validate status transitions
        if ("active".equals(newStatus) && "ended".equals(this.status)) {
            throw new IllegalArgumentException("Cannot reactivate an ended raffle");
        }
        
        if ("active".equals(newStatus) && endTime.before(new Date())) {
            throw new IllegalArgumentException("Cannot set status to active when end time is in the past");
        }
    }

    @Override
    public String toString() {
        return "Raffle{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", creator=" + creatorName +
                ", status='" + status + '\'' +
                '}';
    }
}
