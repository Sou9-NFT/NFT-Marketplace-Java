package org.esprit.models;
<<<<<<< HEAD
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
=======

import java.util.List;
import java.util.Map;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Category {

    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_AUDIO = "audio";

    public static final Map<String, List<String>> MIME_TYPES = Map.of(
        TYPE_IMAGE, List.of("image/jpeg", "image/png", "image/gif", "image/webp"),
        TYPE_VIDEO, List.of("video/mp4", "video/webm", "video/ogg"),
        TYPE_AUDIO, List.of("audio/mpeg", "audio/ogg", "audio/wav")
    );

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ListProperty<String> allowedMimeTypes = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<User> manager = new SimpleObjectProperty<>();
    private final ListProperty<Artwork> artworks = new SimpleListProperty<>(FXCollections.observableArrayList());

    // Getters and Setters with JavaFX properties

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        if (!MIME_TYPES.containsKey(type)) {
            throw new IllegalArgumentException("Invalid category type");
        }
        this.type.set(type);
        this.allowedMimeTypes.setAll(MIME_TYPES.get(type));
    }

    public StringProperty typeProperty() {
        return type;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObservableList<String> getAllowedMimeTypes() {
        return allowedMimeTypes.get();
    }

    public void setAllowedMimeTypes(List<String> mimeTypes) {
        this.allowedMimeTypes.setAll(mimeTypes);
    }

    public ListProperty<String> allowedMimeTypesProperty() {
        return allowedMimeTypes;
    }

    public User getManager() {
        return manager.get();
    }

    public void setManager(User manager) {
        this.manager.set(manager);
    }

    public ObjectProperty<User> managerProperty() {
        return manager;
    }

    public ObservableList<Artwork> getArtworks() {
        return artworks.get();
    }

    public void addArtwork(Artwork artwork) {
        if (!this.artworks.contains(artwork)) {
            this.artworks.add(artwork);
            artwork.setCategory(this);
        }
    }

    public void removeArtwork(Artwork artwork) {
        if (this.artworks.remove(artwork)) {
            if (artwork.getCategory() == this) {
                artwork.setCategory(null);
            }
        }
    }

    public ListProperty<Artwork> artworksProperty() {
        return artworks;
    }

    @Override
    public String toString() {
        return name.get();
>>>>>>> origin/beta
    }
}
