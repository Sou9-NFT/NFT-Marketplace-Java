package org.esprit.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String email;
    private List<String> roles;
    private BigDecimal balance;
    private String password;
    private LocalDateTime createdAt;
    private String name;
    private String profilePicture;
    private String walletAddress;
    private String githubUsername;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiresAt;

    // Default constructor
    public User() {
        this.roles = new ArrayList<>();
        this.balance = new BigDecimal("0.000");
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public User(String email, String password, String name) {
        this();
        this.email = email;
        this.password = password;
        this.name = name;
    }

    // Full constructor
    public User(int id, String email, List<String> roles, BigDecimal balance, String password, 
                LocalDateTime createdAt, String name, String profilePicture, String walletAddress, 
                String githubUsername, String passwordResetToken, LocalDateTime passwordResetTokenExpiresAt) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.balance = balance;
        this.password = password;
        this.createdAt = createdAt;
        this.name = name;
        this.profilePicture = profilePicture;
        this.walletAddress = walletAddress;
        this.githubUsername = githubUsername;
        this.passwordResetToken = passwordResetToken;
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) {
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
