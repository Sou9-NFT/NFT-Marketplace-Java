package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import org.esprit.models.Artwork;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.RaffleService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private void handleCreateRaffle(ActionEvent event) {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String artworkIdStr = artworkIdField.getText().trim();
        LocalDate endDate = endDatePicker.getValue();
        String hour = endTimeHourCombo.getValue();
        String minute = endTimeMinuteCombo.getValue();
        
        // Validate inputs
        if (title.isEmpty()) {
            showStatus("Please enter a title", true);
            return;
        }
        
        if (description.isEmpty()) {
            showStatus("Please enter a description", true);
            return;
        }
        
        if (artworkIdStr.isEmpty()) {
            showStatus("Please enter an artwork ID", true);
            return;
        }
        
        if (endDate == null) {
            showStatus("Please select an end date", true);
            return;
        }
        
        if (hour == null || minute == null) {
            showStatus("Please select both hour and minute", true);
            return;
        }
        
        // Create LocalDateTime with the selected date and time
        LocalDateTime endDateTime = LocalDateTime.of(
            endDate,
            LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute))
        );
        
        // Check if the end date/time is in the future
        if (endDateTime.isBefore(LocalDateTime.now())) {
            showStatus("End date and time must be in the future", true);
            return;
        }
        
        try {
            // Parse and validate artwork ID
            int artworkId = Integer.parseInt(artworkIdStr);
            Artwork artwork = artworkService.getOne(artworkId);
            
            if (artwork == null) {
                showStatus("Artwork with ID " + artworkId + " not found", true);
                return;
            }
            
            // Verify artwork ownership
            if (artwork.getOwnerId() != currentUser.getId()) {
                showStatus("You can only create raffles for artworks you own", true);
                return;
            }
            
            // Create new raffle with precise end time
            Raffle raffle = new Raffle(
                title,
                description,
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                currentUser,
                artworkId
            );
            
            // Save raffle to database
            raffleService.add(raffle);
            
            // Show success and close dialog
            showStatus("Raffle created successfully!", false);
            
            // Refresh parent view
            if (parentController != null) {
                parentController.refreshRaffles();
            }
            
            // Close the dialog
            ((Stage) titleField.getScene().getWindow()).close();
            
        } catch (NumberFormatException e) {
            showStatus("Please enter a valid artwork ID number", true);
        } catch (SQLException e) {
            showStatus("Error creating raffle: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) titleField.getScene().getWindow()).close();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}