package org.esprit.models;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class Raffle {
    private Integer id;
    private Integer artworkId;  // Added artwork_id field
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
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
    }

    public String getRaffleDescription() {
        return raffleDescription;
    }

    public void setRaffleDescription(String raffleDescription) {
        this.raffleDescription = raffleDescription;
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
