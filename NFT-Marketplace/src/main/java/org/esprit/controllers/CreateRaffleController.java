package org.esprit.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.esprit.models.Artwork;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.RaffleService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateRaffleController {
    @FXML
    private TextField titleField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private TextField artworkIdField;
    
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
        String artworkIdStr = artworkIdField.getText().trim();
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
        
        // Check artwork ID (entity validation)
        if (artworkIdStr.isEmpty()) {
            artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            if (!hasErrors) showStatus("Please enter an artwork ID", true);
            hasErrors = true;
        } else {
            try {
                int artworkId = Integer.parseInt(artworkIdStr);
                if (artworkId <= 0) {
                    artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    if (!hasErrors) showStatus("Artwork ID must be a positive number", true);
                    hasErrors = true;
                } else {
                    artworkIdField.setStyle("");
                }
            } catch (NumberFormatException e) {
                artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                if (!hasErrors) showStatus("Artwork ID must be a valid number", true);
                hasErrors = true;
            }
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
            // Parse artwork ID (already validated above)
            int artworkId = Integer.parseInt(artworkIdStr);
            
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
            
            // Verify artwork exists and check ownership (entity relationship validation)
            Artwork artwork = artworkService.getOne(artworkId);
            if (artwork == null) {
                artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showStatus("Artwork with ID " + artworkId + " not found", true);
                return;
            }
            
            if (artwork.getOwnerId() != currentUser.getId()) {
                artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showStatus("You can only create raffles for artworks you own", true);
                return;
            }
            
            // All validation passed, create and save the raffle
            Raffle raffle = new Raffle(
                title,
                description,
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                currentUser,
                artworkId
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
            
        } catch (NumberFormatException e) {
            artworkIdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showStatus("Please enter a valid artwork ID number", true);
        } catch (Exception e) {
            showStatus("Error creating raffle: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        ((Stage) titleField.getScene().getWindow()).close();
    }
    
    private void clearStatus() {
        statusLabel.setVisible(false);
        titleField.setStyle("");
        descriptionField.setStyle("");
        artworkIdField.setStyle("");
        endDatePicker.setStyle("");
        endTimeHourCombo.setStyle("");
        endTimeMinuteCombo.setStyle("");
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        
        // Apply direct styling for red error messages
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #009900; -fx-font-weight: normal;");
        }
    }
}