package org.esprit.models;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Category {
    // Constants for validation
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final String PROFANITY_API_URL = "https://vector.profanity.dev";
    
    private int id;
    private int managerId;
    private String name;
    private String type;
    private String description;
    private List<String> allowedMimeTypes;

    public Category() {
        this.allowedMimeTypes = new ArrayList<>();
    }

    public Category(int id, int managerId, String name, String type, String description, List<String> allowedMimeTypes) {
        this.id = id;
        setManagerId(managerId);
        setName(name);
        setType(type);
        setDescription(description);
        setAllowedMimeTypes(allowedMimeTypes);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { 
        if (managerId <= 0) {
            throw new IllegalArgumentException("Manager ID must be a positive number");
        }
        this.managerId = managerId; 
    }

    public String getName() { return name; }
    public void setName(String name) { 
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        if (name.trim().length() < MIN_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name must be at least " + MIN_NAME_LENGTH + " characters");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        this.name = name.trim(); 
    }

    public String getType() { return type; }
    public void setType(String type) { 
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Category type cannot be null or empty");
        }
        this.type = type.trim(); 
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { 
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        this.description = description.trim(); 
    }

    public List<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(List<String> allowedMimeTypes) { 
        if (allowedMimeTypes == null) {
            this.allowedMimeTypes = new ArrayList<>();
        } else if (allowedMimeTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one MIME type must be specified");
        } else {
            this.allowedMimeTypes = new ArrayList<>(allowedMimeTypes); 
        }
    }
    
    /**
     * Add a single MIME type to the list of allowed MIME types
     * @param mimeType The MIME type to add
     */
    public void addMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new IllegalArgumentException("MIME type cannot be null or empty");
        }
        
        if (this.allowedMimeTypes == null) {
            this.allowedMimeTypes = new ArrayList<>();
        }
        
        String trimmedMimeType = mimeType.trim();
        if (!this.allowedMimeTypes.contains(trimmedMimeType)) {
            this.allowedMimeTypes.add(trimmedMimeType);
        }
    }
    
    /**
     * Remove a MIME type from the list of allowed MIME types
     * @param mimeType The MIME type to remove
     * @return true if removed, false if not found
     */
    public boolean removeMimeType(String mimeType) {
        if (this.allowedMimeTypes == null) {
            return false;
        }
        return this.allowedMimeTypes.remove(mimeType);
    }

    /**
     * Check if the provided text contains profanity using the profanity API
     * 
     * @param text The text to check for profanity
     * @return true if profanity is detected, false otherwise
     * @throws IOException if there's an issue with the API call
     * @throws InterruptedException if the API call is interrupted
     */
    private boolean containsProfanity(String text) throws IOException, InterruptedException {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // Create the JSON request body
        String requestBody = "{\"message\":\"" + text.replace("\"", "\\\"") + "\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PROFANITY_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Check if the API detected profanity (you may need to adjust this based on the actual API response)
        return response.statusCode() == 200 && response.body().contains("true");
    }
    
    /**
     * Check both name and description for profanity
     * 
     * @throws IllegalArgumentException if profanity is detected in either field
     * @throws IOException if there's an issue with the API call
     * @throws InterruptedException if the API call is interrupted
     */
    public void checkForProfanity() throws IOException, InterruptedException {
        if (containsProfanity(name)) {
            throw new IllegalArgumentException("Category name contains inappropriate content");
        }
        
        if (containsProfanity(description)) {
            throw new IllegalArgumentException("Category description contains inappropriate content");
        }
    }

    /**
     * Validates all properties of the category object including profanity check
     * @throws IllegalArgumentException if any validation fails
     */
    public void validate() {
        if (name == null || name.trim().isEmpty() || name.trim().length() < MIN_NAME_LENGTH || name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Category type cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty() || description.trim().length() < MIN_DESCRIPTION_LENGTH || description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be between " + MIN_DESCRIPTION_LENGTH + " and " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one MIME type must be specified");
        }
        if (managerId <= 0) {
            throw new IllegalArgumentException("Manager ID must be a positive number");
        }
        
        try {
            // Check for profanity in name and description
            checkForProfanity();
        } catch (IOException | InterruptedException e) {
            // Log the error but don't prevent validation
            System.err.println("Warning: Could not check for profanity: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
