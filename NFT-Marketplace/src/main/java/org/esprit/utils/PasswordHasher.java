package org.esprit.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for handling password hashing and verification
 * Uses BCrypt algorithm similar to Symfony's security component
 */
public class PasswordHasher {
    
    // Default work factor for BCrypt (cost parameter)
    private static final int DEFAULT_COST = 10;
    
    /**
     * Hash a password using BCrypt algorithm
     * 
     * @param plainPassword the plain text password to hash
     * @return the hashed password
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(DEFAULT_COST));
    }
      /**
     * Verify if the plain password matches the hashed password
     * 
     * @param plainPassword the plain text password to check
     * @param hashedPassword the hashed password to check against
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            // Convert $2y to $2a if present (for compatibility with Symfony)
            // Note: This only affects the verification process, not storage
            String compatibleHash = hashedPassword;
            if (hashedPassword != null && hashedPassword.startsWith("$2y$")) {
                compatibleHash = "$2a$" + hashedPassword.substring(4);
            }
            
            return BCrypt.checkpw(plainPassword, compatibleHash);
        } catch (IllegalArgumentException e) {
            // This can happen if the stored hash is not in BCrypt format
            // (e.g., when migrating from plain text passwords)
            return false;
        }
    }
      /**
     * Check if a password needs rehashing (for future password upgrades)
     * 
     * @param hashedPassword the current hashed password
     * @return true if the password needs to be rehashed, false otherwise
     */
    public static boolean needsRehash(String hashedPassword) {
        // Check if it's a valid BCrypt hash (accepts both $2a$ and $2y$ formats)
        return hashedPassword == null || 
               !(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2y$") || hashedPassword.startsWith("$2b$"));
    }
}