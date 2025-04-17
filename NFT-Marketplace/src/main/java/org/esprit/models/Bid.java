package org.esprit.models;

import java.time.LocalDateTime;

/**
 * Represents a bid made by a user on a bet session.
 */
public class Bid {
    private int id;
    private double bidValue;
    private LocalDateTime bidTime;
    private BetSession betSession;
    private User author;
    
    /**
     * Default constructor that initializes bidTime to current time
     */
    public Bid() {
        this.bidTime = LocalDateTime.now();
    }
    
    /**
     * Parameterized constructor
     * 
     * @param bidValue The bid value/amount
     * @param betSession The bet session this bid belongs to
     * @param author The user who made the bid
     */
    public Bid(double bidValue, BetSession betSession, User author) {
        this();
        this.bidValue = bidValue;
        this.betSession = betSession;
        this.author = author;
    }
    
    /**
     * Full constructor
     * 
     * @param id The bid ID
     * @param bidValue The bid value/amount
     * @param bidTime The time the bid was made
     * @param betSession The bet session this bid belongs to
     * @param author The user who made the bid
     */
    public Bid(int id, double bidValue, LocalDateTime bidTime, BetSession betSession, User author) {
        this.id = id;
        this.bidValue = bidValue;
        this.bidTime = bidTime;
        this.betSession = betSession;
        this.author = author;
    }

    // Getters and Setters
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getBidValue() {
        return bidValue;
    }

    public void setBidValue(double bidValue) {
        this.bidValue = bidValue;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public BetSession getBetSession() {
        return betSession;
    }

    public void setBetSession(BetSession betSession) {
        this.betSession = betSession;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
    
    @Override
    public String toString() {
        return "Bid{" +
                "id=" + id +
                ", bidValue=" + bidValue +
                ", bidTime=" + bidTime +
                ", betSession=" + (betSession != null ? betSession.getId() : "null") +
                ", author=" + (author != null ? author.getName() : "null") +
                '}';
    }
}
