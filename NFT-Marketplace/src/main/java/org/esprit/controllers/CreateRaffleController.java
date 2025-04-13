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
        
        try {
            // Parse artwork ID
            int artworkId = Integer.parseInt(artworkIdStr);
            
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
            
            // Verify artwork exists and ownership
            Artwork artwork = artworkService.getOne(artworkId);
            if (artwork == null) {
                showStatus("Artwork with ID " + artworkId + " not found", true);
                return;
            }
            
            if (artwork.getOwnerId() != currentUser.getId()) {
                showStatus("You can only create raffles for artworks you own", true);
                return;
            }
            
            // Create and save the raffle
            Raffle raffle = new Raffle(
                title,
                description,
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()),
                currentUser,
                artworkId
            );
            
            raffleService.add(raffle);
            
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
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}