package org.esprit.models;

import java.time.LocalDateTime;

public class TradeState {
    private int id;
    private TradeOffer tradeOffer;
    private Artwork receivedItem;
    private Artwork offeredItem;
    private User sender;
    private User receiver;    private String description;

    

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TradeOffer getTradeOffer() {
        return tradeOffer;
    }

    public void setTradeOffer(TradeOffer tradeOffer) {
        this.tradeOffer = tradeOffer;
    }

    public Artwork getReceivedItem() {
        return receivedItem;
    }

    public void setReceivedItem(Artwork receivedItem) {
        this.receivedItem = receivedItem;
    }

    public Artwork getOfferedItem() {
        return offeredItem;
    }

    public void setOfferedItem(Artwork offeredItem) {
        this.offeredItem = offeredItem;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
