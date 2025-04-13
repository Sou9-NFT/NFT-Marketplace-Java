package org.esprit.models;
import java.util.List;
import java.util.ArrayList;


public class Category {
    // Constants for validation
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    
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
     * Validates all properties of the category object
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
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
