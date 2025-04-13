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
    private ComboBox<User> winnerComboBox;
    
    @FXML
    private ListView<User> participantsListView;
    
    @FXML
    private Label statusLabel;
    
    private Raffle raffle;
    private RaffleService raffleService;
    private RaffleListController parentController;
    
    public void initialize() {
        raffleService = new RaffleService();
        
        // Setup status options
        statusComboBox.setItems(FXCollections.observableArrayList(
            "active",
            "completed",
            "cancelled"
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
        
        // Setup winner selection
        winnerComboBox.setItems(FXCollections.observableArrayList(raffle.getParticipants()));
        winnerComboBox.setCellFactory(cb -> new ListCell<User>() {
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
        
        // Set current winner if exists
        if (raffle.getWinnerId() != null) {
            winnerComboBox.getItems().stream()
                .filter(u -> u.getId() == raffle.getWinnerId())
                .findFirst()
                .ifPresent(winner -> winnerComboBox.setValue(winner));
        }
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate endDate = endDatePicker.getValue();
        String hours = hoursField.getText().trim();
        String minutes = minutesField.getText().trim();
        String status = statusComboBox.getValue();
        User winner = winnerComboBox.getValue();
        
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

            // Validate that end date/time is in the future
            if (endDateTime.isBefore(LocalDateTime.now())) {
                showStatus("End date/time must be in the future", true);
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
            raffle.setWinnerId(winner != null ? winner.getId() : null);
            
            // Save to database
            raffleService.update(raffle);
            
            // Show success and close dialog
            showStatus("Raffle updated successfully!", false);
            
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
            showStatus("Invalid time format", true);
            return;
        } catch (SQLException e) {
            showStatus("Error updating raffle: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleDelete(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Raffle");
        alert.setHeaderText("Delete Raffle");
        alert.setContentText("Are you sure you want to delete this raffle? This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                raffleService.delete(raffle);
                
                // Refresh parent view
                if (parentController != null) {
                    parentController.refreshRaffles();
                }
                
                // Close the dialog
                ((Stage) titleField.getScene().getWindow()).close();
                
            } catch (SQLException e) {
                showStatus("Error deleting raffle: " + e.getMessage(), true);
                e.printStackTrace();
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