package org.esprit.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.esprit.models.Artwork;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.RaffleService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class CreateRaffleController {
    @FXML
    private TextField titleField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private ComboBox<Artwork> artworkComboBox;
    
    @FXML
    private DatePicker endDatePicker;
    
    @FXML
    private ComboBox<String> endTimeHourCombo;
    
    @FXML
    private ComboBox<String> endTimeMinuteCombo;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private RaffleService raffleService;
    private ArtworkService artworkService;
    private RaffleListController parentController;
    
    public void initialize() {
        raffleService = new RaffleService();
        artworkService = new ArtworkService();
        
        // Initialize time selection combos
        setupTimeComboBoxes();
        
        // Set default end date to tomorrow
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        
        // Setup artwork ComboBox
        setupArtworkComboBox();
    }
    
    private void setupArtworkComboBox() {
        // Configure how artwork items are displayed in the ComboBox
        artworkComboBox.setCellFactory(param -> new ListCell<Artwork>() {
            @Override
            protected void updateItem(Artwork artwork, boolean empty) {
                super.updateItem(artwork, empty);
                
                if (empty || artwork == null) {
                    setText(null);
                } else {
                    setText(artwork.getTitle());
                }
            }
        });
        
        // Configure how the selected artwork is displayed in the ComboBox
        artworkComboBox.setConverter(new StringConverter<Artwork>() {
            @Override
            public String toString(Artwork artwork) {
                if (artwork == null) {
                    return null;
                }
                return artwork.getTitle();
            }
            
            @Override
            public Artwork fromString(String string) {
                // This is needed for the converter but won't be used in our case
                return null;
            }
        });
    }
    
    private void loadUserArtworks() {
        try {
            if (currentUser != null) {
                // Get all artworks owned by the current user
                List<Artwork> userArtworks = artworkService.getByOwner(currentUser.getId());
                
                ObservableList<Artwork> artworkOptions = FXCollections.observableArrayList();
                
                if (userArtworks.isEmpty()) {
                    // Create a special artwork object that indicates "NONE"
                    final String PLACEHOLDER_TITLE = "NONE - Create an artwork first";
                    final String PLACEHOLDER_DESC = "No artwork available";
                    
                    try {
                        // Special case for when user has no artworks
                        // We need a placeholder that won't trigger validation errors
                        Artwork noArtwork = new PlaceholderArtwork(
                            -1,
                            currentUser.getId(),
                            currentUser.getId(),
                            1,
                            PLACEHOLDER_TITLE,
                            PLACEHOLDER_DESC,
                            0.01,
                            "none.png",
                            LocalDateTime.now(),
                            LocalDateTime.now()
                        );
                        artworkOptions.add(noArtwork);
                    } catch (Exception e) {
                        System.out.println("Error creating placeholder artwork: " + e.getMessage());
                    }
                } else {
                    artworkOptions.addAll(userArtworks);
                }
                
                artworkComboBox.setItems(artworkOptions);
                
                // Select the first item by default
                if (!artworkOptions.isEmpty()) {
                    artworkComboBox.setValue(artworkOptions.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error loading your artworks: " + e.getMessage(), true);
        }
    }
    
    private void setupTimeComboBoxes() {
        // Setup hours (00-23)
        endTimeHourCombo.setItems(FXCollections.observableArrayList(
            IntStream.rangeClosed(0, 23)
                .mapToObj(i -> String.format("%02d", i))
                .collect(Collectors.toList())
        ));
        endTimeHourCombo.setValue("12"); // Default to noon
        
        // Setup minutes (00-59)
        endTimeMinuteCombo.setItems(FXCollections.observableArrayList(
            IntStream.rangeClosed(0, 59)
                .mapToObj(i -> String.format("%02d", i))
                .collect(Collectors.toList())
        ));
        endTimeMinuteCombo.setValue("00"); // Default to 00 minutes
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        // Load user's artworks when user is set
        loadUserArtworks();
    }
    
    public void setParentController(RaffleListController controller) {
        this.parentController = controller;
    }
    
    @FXML
    private void handleCreateRaffle(ActionEvent actionEvent) {
        // Clear any previous status messages
        clearStatus();
        
        // Get input values
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        Artwork selectedArtwork = artworkComboBox.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String hour = endTimeHourCombo.getValue();
        String minute = endTimeMinuteCombo.getValue();
        
        // Entity-level validation with specific error checks
        boolean hasErrors = false;
        
        // Check title (entity validation)
        if (title.isEmpty()) {
            titleField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showStatus("Please enter a title", true);
            hasErrors = true;
        } else if (title.length() < 5) {
            titleField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showStatus("Title must be at least 5 characters long", true);
            hasErrors = true;
        } else {
            titleField.setStyle("");
        }
        
        // Check description (entity validation)
        if (description.isEmpty()) {
            descriptionField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Please enter a description", true);
            hasErrors = true;
        } else if (description.length() < 10) {
            descriptionField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Description must be at least 10 characters long", true);
            hasErrors = true;
        } else {
            descriptionField.setStyle("");
        }
        
        // Check artwork selection (entity validation)
        if (selectedArtwork == null) {
            artworkComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Please select an artwork", true);
            hasErrors = true;
        } else if (selectedArtwork.getId() == -1) {
            artworkComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("You don't have any artworks to raffle. Please create an artwork first.", true);
            hasErrors = true;
        } else {
            artworkComboBox.setStyle("");
        }
        
        // Check end date (entity validation)
        if (endDate == null) {
            endDatePicker.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Please select an end date", true);
            hasErrors = true;
        } else if (endDate.isBefore(LocalDate.now())) {
            endDatePicker.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("End date cannot be in the past", true);
            hasErrors = true;
        } else {
            endDatePicker.setStyle("");
        }
        
        // Check time selection (entity validation)
        if (hour == null || minute == null) {
            endTimeHourCombo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            endTimeMinuteCombo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Please select both hour and minute", true);
            hasErrors = true;
        } else {
            endTimeHourCombo.setStyle("");
            endTimeMinuteCombo.setStyle("");
        }
        
        // If there are validation errors, don't proceed
        if (hasErrors) {
            return;
        }
        
        try {
            // Create LocalDateTime with the selected date and time
            LocalDateTime endDateTime = LocalDateTime.of(
                endDate,
                LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute))
            );
            
            // Check if the end date/time is in the future (entire entity validation)
            if (endDateTime.isBefore(LocalDateTime.now())) {
                endDatePicker.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                endTimeHourCombo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                endTimeMinuteCombo.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showStatus("End date and time must be in the future", true);
                return;
            }
            
            // All validation passed, create and save the raffle
            Raffle raffle = new Raffle(
                title,
                description,
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                currentUser,
                selectedArtwork.getId()
            );
            
            raffleService.add(raffle);
            
            // Show success message
            showStatus("Raffle created successfully!", false);
            
            // Refresh parent view
            if (parentController != null) {
                parentController.refreshRaffles();
            }
            
            // Close the dialog after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        ((Stage) titleField.getScene().getWindow()).close();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error creating raffle: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        ((Stage) titleField.getScene().getWindow()).close();
    }
    
    private void clearStatus() {
        statusLabel.setVisible(false);
        statusLabel.setText("");
        statusLabel.setStyle("");
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }
    
    // Inner class to create a placeholder artwork without validation
    private static class PlaceholderArtwork extends Artwork {
        public PlaceholderArtwork(int id, int creatorId, int ownerId, int categoryId, 
                                 String title, String description, double price, 
                                 String imageName, LocalDateTime createdAt, LocalDateTime updatedAt) {
            // Call superclass constructor but handle exceptions
            super();
            // Set the properties directly
            setId(id);
            
            try {
                setCreatorId(creatorId > 0 ? creatorId : 1);
                setOwnerId(ownerId > 0 ? ownerId : 1);
                setCategoryId(categoryId > 0 ? categoryId : 1);
            } catch (IllegalArgumentException e) {
                // Ignore validation errors for IDs
                try {
                    // Call these with reflection if needed
                    java.lang.reflect.Field creatorIdField = Artwork.class.getDeclaredField("creatorId");
                    creatorIdField.setAccessible(true);
                    creatorIdField.set(this, creatorId > 0 ? creatorId : 1);
                    
                    java.lang.reflect.Field ownerIdField = Artwork.class.getDeclaredField("ownerId");
                    ownerIdField.setAccessible(true);
                    ownerIdField.set(this, ownerId > 0 ? ownerId : 1);
                    
                    java.lang.reflect.Field categoryIdField = Artwork.class.getDeclaredField("categoryId");
                    categoryIdField.setAccessible(true);
                    categoryIdField.set(this, categoryId > 0 ? categoryId : 1);
                } catch (Exception ex) {
                    // Last resort
                    System.err.println("Failed to set fields via reflection: " + ex.getMessage());
                }
            }
            
            try {
                // Try to bypass validation for these fields
                java.lang.reflect.Field titleField = Artwork.class.getDeclaredField("title");
                titleField.setAccessible(true);
                titleField.set(this, title);
                
                java.lang.reflect.Field descriptionField = Artwork.class.getDeclaredField("description");
                descriptionField.setAccessible(true);
                descriptionField.set(this, description);
                
                java.lang.reflect.Field priceField = Artwork.class.getDeclaredField("price");
                priceField.setAccessible(true);
                priceField.set(this, price);
                
                java.lang.reflect.Field imageNameField = Artwork.class.getDeclaredField("imageName");
                imageNameField.setAccessible(true);
                imageNameField.set(this, imageName);
                
                java.lang.reflect.Field createdAtField = Artwork.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(this, createdAt);
                
                java.lang.reflect.Field updatedAtField = Artwork.class.getDeclaredField("updatedAt");
                updatedAtField.setAccessible(true);
                updatedAtField.set(this, updatedAt);
            } catch (Exception e) {
                System.err.println("Error in PlaceholderArtwork constructor: " + e.getMessage());
            }
        }
        
        @Override
        public void validate() {
            // Override to disable validation
        }
    }
}