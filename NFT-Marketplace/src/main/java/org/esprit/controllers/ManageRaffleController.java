package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.RaffleService;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class ManageRaffleController {
    @FXML
    private TextField titleField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private DatePicker endDatePicker;
    
    @FXML
    private TextField hoursField;
    
    @FXML
    private TextField minutesField;
    
    @FXML
    private ComboBox<String> statusComboBox;
    
    @FXML
    private ListView<User> participantsListView;
    
    @FXML
    private Label statusLabel;
    
    private Raffle raffle;
    private RaffleService raffleService;
    private RaffleListController parentController;
    private User currentUser;
    
    public void initialize() {
        raffleService = new RaffleService();
        
        // Setup status options
        statusComboBox.setItems(FXCollections.observableArrayList(
            "active",
            "ended"
        ));

        // Add listeners to enforce numeric input for time fields
        hoursField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                hoursField.setText(newText.replaceAll("[^\\d]", ""));
            }
            if (newText.length() > 2) {
                hoursField.setText(oldText);
            }
            if (!newText.isEmpty()) {
                int hours = Integer.parseInt(newText);
                if (hours > 23) {
                    hoursField.setText("23");
                }
            }
        });

        minutesField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                minutesField.setText(newText.replaceAll("[^\\d]", ""));
            }
            if (newText.length() > 2) {
                minutesField.setText(oldText);
            }
            if (!newText.isEmpty()) {
                int minutes = Integer.parseInt(newText);
                if (minutes > 59) {
                    minutesField.setText("59");
                }
            }
        });
    }
    
    public void setRaffle(Raffle raffle) {
        this.raffle = raffle;
        populateFields();
    }
    
    public void setParentController(RaffleListController controller) {
        this.parentController = controller;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    private void populateFields() {
        titleField.setText(raffle.getTitle());
        descriptionField.setText(raffle.getRaffleDescription());
        
        // Set end date and time
        LocalDateTime endDateTime = raffle.getEndTime().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
            
        endDatePicker.setValue(endDateTime.toLocalDate());
        hoursField.setText(String.format("%02d", endDateTime.getHour()));
        minutesField.setText(String.format("%02d", endDateTime.getMinute()));
        
        // Set status
        statusComboBox.setValue(raffle.getStatus());
        
        // Setup participants list
        participantsListView.setItems(FXCollections.observableArrayList(raffle.getParticipants()));
        participantsListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getName());
                }
            }
        });
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        // Verify current user is the creator
        if (currentUser == null || raffle.getCreator().getId() != currentUser.getId()) {
            showStatus("Only the creator of this raffle can modify it", true);
            return;
        }
        
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate endDate = endDatePicker.getValue();
        String hours = hoursField.getText().trim();
        String minutes = minutesField.getText().trim();
        String status = statusComboBox.getValue();
        
        // Validate inputs
        if (title.isEmpty()) {
            showStatus("Please enter a title", true);
            return;
        }
        
        if (description.isEmpty()) {
            showStatus("Please enter a description", true);
            return;
        }
        
        if (endDate == null) {
            showStatus("Please select an end date", true);
            return;
        }

        // Validate time inputs
        if (hours.isEmpty() || minutes.isEmpty()) {
            showStatus("Please enter both hours and minutes", true);
            return;
        }

        try {
            int hoursVal = Integer.parseInt(hours);
            int minutesVal = Integer.parseInt(minutes);
            
            if (hoursVal < 0 || hoursVal > 23) {
                showStatus("Hours must be between 0 and 23", true);
                return;
            }
            
            if (minutesVal < 0 || minutesVal > 59) {
                showStatus("Minutes must be between 0 and 59", true);
                return;
            }

            // Create LocalDateTime from date and time components
            LocalDateTime endDateTime = LocalDateTime.of(
                endDate, 
                LocalTime.of(hoursVal, minutesVal)
            );

            // Validate that end date/time is in the future for active raffles
            if (status.equals("active") && endDateTime.isBefore(LocalDateTime.now())) {
                showStatus("End date/time must be in the future for active raffles", true);
                return;
            }
            
            if (status == null) {
                showStatus("Please select a status", true);
                return;
            }
        
            // Update raffle
            raffle.setTitle(title);
            raffle.setRaffleDescription(description);
            raffle.setEndTime(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            raffle.setStatus(status);
            
            // Save changes
            raffleService.update(raffle);
            
            // Show success message
            showStatus("Raffle updated successfully!", false);
            
            // Refresh parent view
            if (parentController != null) {
                parentController.refreshRaffles();
            }
            
            // Close window after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        ((Stage) titleField.getScene().getWindow()).close();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (NumberFormatException e) {
            showStatus("Invalid time format", true);
        } catch (SQLException e) {
            showStatus("Error saving raffle: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleDelete(ActionEvent event) {
        // Verify current user is the creator
        if (currentUser == null || raffle.getCreator().getId() != currentUser.getId()) {
            showStatus("Only the creator of this raffle can delete it", true);
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("Are you sure you want to delete this raffle? This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                raffleService.delete(raffle);
                
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Raffle has been successfully deleted.");
                successAlert.showAndWait();
                
                // Refresh parent controller
                if (parentController != null) {
                    parentController.refreshRaffles();
                }
                
                // Close window
                ((Stage) titleField.getScene().getWindow()).close();
                
            } catch (SQLException e) {
                showStatus("Error deleting raffle: " + e.getMessage(), true);
            }
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