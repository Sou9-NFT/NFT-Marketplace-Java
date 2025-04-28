package org.esprit.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        this.roles.add("ROLE_USER"); // Set default role to ROLE_USER
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

    // Validation class to store validation results
    public static class ValidationResult {
        private boolean valid;
        private Map<String, String> errors;

        public ValidationResult() {
            this.valid = true;
            this.errors = new HashMap<>();
        }

        public void addError(String field, String message) {
            this.valid = false;
            this.errors.put(field, message);
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public String getError(String field) {
            return errors.get(field);
        }

        public boolean hasError(String field) {
            return errors.containsKey(field);
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors +
                    '}';
        }
    }

    // Validation method for the entire user entity
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        
        // Email validation
        if (email == null || email.trim().isEmpty()) {
            result.addError("email", "Email cannot be blank");
        } else if (!isValidEmail(email)) {
            result.addError("email", "The email " + email + " is not a valid email.");
        }
        
        // Name validation
        if (name == null || name.trim().isEmpty()) {
            result.addError("name", "Name cannot be blank");
        } else if (name.length() < 2) {
            result.addError("name", "Your name must be at least 2 characters long");
        } else if (name.length() > 32) {
            result.addError("name", "Your name cannot be longer than 32 characters");
        } else if (!isValidName(name)) {
            result.addError("name", "Name can only contain letters and spaces");
        }
        
        // Password validation for new users (id = 0) or password changes
        if (id == 0 || (password != null && !password.trim().isEmpty())) {
            if (password == null || password.trim().isEmpty()) {
                result.addError("password", "Password cannot be blank");
            } else if (password.length() < 6) {
                result.addError("password", "Your password must be at least 6 characters long");
            } else if (password.length() > 50) {
                result.addError("password", "Your password cannot be longer than 50 characters");
            } else if (!isValidPassword(password)) {
                result.addError("password", "Password must contain at least one uppercase letter and one number");
            }
        }
        
        // Balance validation (if set)
        if (balance != null && balance.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("balance", "Balance cannot be negative");
        }
        
        // Profile picture URL validation (if set)
        if (profilePicture != null && !profilePicture.trim().isEmpty() && !profilePicture.startsWith("/")) {
            if (!isValidProfilePicture(profilePicture)) {
                result.addError("profilePicture", "The profile picture must be a valid URL");
            }
        }
        
        // GitHub username validation (if set)
        if (githubUsername != null && !githubUsername.trim().isEmpty()) {
            if (!isValidGithubUsername(githubUsername)) {
                result.addError("githubUsername", "Invalid GitHub username format");
            }
        }
        
        return result;
    }
    
    // Helper validation methods
    private boolean isValidEmail(String email) {
        // Strict email validation regex (similar to Symfony's strict mode)
        String emailRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        // Must contain at least one uppercase letter and one number
        String passwordRegex = "^(?=.*[A-Z])(?=.*\\d).+$";
        return Pattern.compile(passwordRegex).matcher(password).matches();
    }

    private boolean isValidName(String name) {
        // Must contain only letters and spaces
        String nameRegex = "^[a-zA-Z\\s]+$";
        return Pattern.compile(nameRegex).matcher(name).matches();
    }

    private boolean isValidProfilePicture(String url) {
        // Simple URL validation (could be enhanced)
        String urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        return url.startsWith("/") || Pattern.compile(urlRegex).matcher(url).matches();
    }

    private boolean isValidGithubUsername(String username) {
        // GitHub username validation (alphanumeric with hyphens, no consecutive hyphens)
        String githubRegex = "^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$";
        return Pattern.compile(githubRegex).matcher(username).matches();
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
