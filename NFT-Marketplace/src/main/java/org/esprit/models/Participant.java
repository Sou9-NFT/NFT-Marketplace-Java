package org.esprit.models;

import java.time.LocalDateTime;

public class Participant {
    private Integer id;
    private Raffle raffle;
    private User user;
    private String name;
    private LocalDateTime joinedAt;

    // Default constructor
    public Participant() {
        this.joinedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public Participant(Raffle raffle, User user) {
        this();
        this.raffle = raffle;
        this.user = user;
        this.name = user != null ? (user.getName() != null ? user.getName() : user.getEmail()) : null;
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Raffle getRaffle() {
        return raffle;
    }

    public void setRaffle(Raffle raffle) {
        this.raffle = raffle;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.name = user.getName() != null ? user.getName() : user.getEmail();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return name != null ? name : (user != null ? user.getEmail() : "Unknown Participant");
    }
}