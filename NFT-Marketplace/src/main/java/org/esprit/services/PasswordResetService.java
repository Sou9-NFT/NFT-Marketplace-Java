package org.esprit.services;

import org.esprit.models.User;
import org.esprit.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for handling password reset functionality.
 */
public class PasswordResetService {
    private UserService userService;
    
    /**
     * Creates a new PasswordResetService instance.
     */
    public PasswordResetService() {
        this.userService = new UserService();
    }
    
    /**
     * Initiates a password reset for the given email address.
     * 
     * @param email The email address of the user
     * @return true if password reset was initiated successfully, false otherwise
     */
    public boolean initiatePasswordReset(String email) {
        try {
            // Check if user exists
            User user = userService.getByEmail(email);
            if (user == null) {
                return false;
            }
            
            // Generate a secure random token
            String resetToken = generateSecureToken();
            
            // Set the token expiration time (1 hour from now)
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
            
            // Update the user with the reset token and expiration time
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiresAt(expiresAt);
            userService.update(user);
            
            // Send the password reset email
            return EmailService.sendPasswordResetEmail(
                    user.getEmail(), 
                    user.getName(), 
                    resetToken);
            
        } catch (Exception e) {
            System.err.println("Error initiating password reset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Validates a password reset token and returns the associated user if valid.
     * 
     * @param token The password reset token to validate
     * @return The user associated with the token, or null if invalid or expired
     */
    public User validatePasswordResetToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            // Find all users
            for (User user : userService.getAll()) {
                // Check if the token matches and is not expired
                if (token.equals(user.getPasswordResetToken()) && 
                    user.getPasswordResetTokenExpiresAt() != null &&
                    user.getPasswordResetTokenExpiresAt().isAfter(LocalDateTime.now())) {
                    return user;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error validating reset token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Completes the password reset process by updating the user's password.
     * 
     * @param token The password reset token
     * @param newPassword The new password to set
     * @return true if the password was reset successfully, false otherwise
     */
    public boolean completePasswordReset(String token, String newPassword) {
        try {
            // Validate the token and get the user
            User user = validatePasswordResetToken(token);
            if (user == null) {
                return false;
            }
            
            // Update the user's password
            user.setPassword(newPassword);
            
            // Clear the reset token
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiresAt(null);
            
            // Save the updated user
            userService.update(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error completing password reset: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generates a secure random token for password reset.
     * 
     * @return A secure random token
     */
    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}