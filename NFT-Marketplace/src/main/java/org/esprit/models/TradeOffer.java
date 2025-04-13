package org.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TradeOffer {
    private int id;
    private User sender;
    private User receiverName;
    private Artwork offeredItem;
    private Artwork receivedItem;
    private String description;
    private LocalDateTime creationDate;
    private String status = "pending";
    //private List<TradeDispute> disputes = new ArrayList<>();

    public TradeOffer() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(User receiverName) {
        this.receiverName = receiverName;
    }

    public Artwork getOfferedItem() {
        return offeredItem;
    }

    public void setOfferedItem(Artwork offeredItem) {
        this.offeredItem = offeredItem;
    }

    public Artwork getReceivedItem() {
        return receivedItem;
    }

    public void setReceivedItem(Artwork receivedItem) {
        this.receivedItem = receivedItem;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /*public List<TradeDispute> getDisputes() {
        return disputes;
    }

    public void addDispute(TradeDispute dispute) {
        this.disputes.add(dispute);
    }

    public boolean hasDisputes() {
        return !disputes.isEmpty();
    }*/

    @Override
    public String toString() {
        return "TradeOffer{id=" + id + ", status='" + status + "'}";
    }
}
