package org.esprit.models;
import java.util.List;


public class Category {
    private int id;
    private int managerId;
    private String name;
    private String type;
    private String description;
    private List<String> allowedMimeTypes;

    public Category() {}

    public Category(int id, int managerId, String name, String type, String description, List<String> allowedMimeTypes) {
        this.id = id;
        this.managerId = managerId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.allowedMimeTypes = allowedMimeTypes;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getAllowedMimeTypes() { return allowedMimeTypes; }
    public void setAllowedMimeTypes(List<String> allowedMimeTypes) { this.allowedMimeTypes = allowedMimeTypes; }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
