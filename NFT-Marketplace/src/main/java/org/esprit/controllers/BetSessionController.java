package org.esprit.controllers;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.services.BetSessionService;
import org.esprit.services.UserService;
import org.esprit.utils.TimeUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BetSessionController implements Initializable {

    @FXML
    private TableView<BetSession> tableView;
    
    @FXML
    private TableColumn<BetSession, Integer> idColumn;
    
    @FXML
    private TableColumn<BetSession, String> authorColumn;
    
    @FXML
    private TableColumn<BetSession, String> artworkColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> createdAtColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> startTimeColumn;
    
    @FXML
    private TableColumn<BetSession, LocalDateTime> endTimeColumn;
    
    @FXML
    private TableColumn<BetSession, Number> initialPriceColumn;
    
    @FXML
    private TableColumn<BetSession, Number> currentPriceColumn;
    
    @FXML
    private TableColumn<BetSession, String> statusColumn;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button updateButton;
      @FXML
    private Button deleteButton;
    
    private BetSessionService betSessionService;
    private UserService userService;
    
    // Add field for current user
    private User currentUser;
    
    // Scheduler for countdown updates
    private ScheduledExecutorService countdownExecutor;
    
    public BetSessionController() {
        betSessionService = new BetSessionService();
        userService = new UserService();
    }
    
    // Add method to set current user
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup table columns
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        
        authorColumn.setCellValueFactory(cellData -> {
            User author = cellData.getValue().getAuthor();
            return author != null ? 
                   new SimpleStringProperty(author.getName()) : 
                   new SimpleStringProperty("N/A");
        });
        
        artworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
          createdAtColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());
        startTimeColumn.setCellValueFactory(cellData -> cellData.getValue().startTimeProperty());
        
        // Custom cell factory to show countdown for active sessions
        endTimeColumn.setCellValueFactory(cellData -> cellData.getValue().endTimeProperty());
        endTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime endTime, boolean empty) {
                super.updateItem(endTime, empty);
                
                if (empty || endTime == null) {
                    setText(null);
                    return;
                }
                
                BetSession betSession = getTableView().getItems().get(getIndex());
                if (betSession != null && "active".equals(betSession.getStatus())) {
                    // For active sessions, show a countdown
                    setText(TimeUtils.formatCountdown(endTime));
                } else {
                    // For other sessions, show the regular date/time
                    setText(endTime.toString());
                }
            }
        });
        
        initialPriceColumn.setCellValueFactory(cellData -> cellData.getValue().initialPriceProperty());
        currentPriceColumn.setCellValueFactory(cellData -> cellData.getValue().currentPriceProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        // Set up countdown timer to refresh the display every second
        startCountdownTimer();
        
        // Load data
        loadBetSessions();
    }
    private void loadBetSessions() {
        try {
            tableView.setItems(FXCollections.observableArrayList(betSessionService.getAllBetSessions()));
        } catch (Exception e) {
            System.err.println("Error loading bet sessions: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load bet sessions from the database.");
            alert.showAndWait();
        }
    }
      @FXML
    private void showAddDialog() {
        // Check if a user is logged in
        if (currentUser == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "You must be logged in to create a bet session.");
            alert.showAndWait();
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add New Bet Session");
        
        // Create form components
        Label authorInfoLabel = new Label("Author: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");
        TextField artworkIdField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField();
        Label statusLabel = new Label("Status: pending (automatically set)");
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new Label("Author:"), 0, 0);
        formLayout.add(authorInfoLabel, 1, 0);
        formLayout.add(new Label("Artwork ID:"), 0, 1);
        formLayout.add(artworkIdField, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Status:"), 0, 5);
        formLayout.add(statusLabel, 1, 5);
          // Buttons
        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonLayout = new HBox(10, saveButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new Insets(10));
        
        saveButton.setOnAction(e -> {
            try {
                // Create new BetSession from form data
                BetSession newBetSession = new BetSession();
                
                // Set Author from current user automatically
                newBetSession.setAuthor(currentUser);
                
                // Set Artwork from ID field
                try {
                    int artworkId = Integer.parseInt(artworkIdField.getText());
                    Artwork artwork = new Artwork();
                    artwork.setId(artworkId);
                    newBetSession.setArtwork(artwork);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter a valid numeric ID for Artwork.");
                    alert.showAndWait();
                    return;
                }
                
                // Get time values - set to noon by default
                LocalTime defaultTime = LocalTime.of(12, 0);
                
                if (startDatePicker.getValue() != null) {
                    newBetSession.setStartTime(startDatePicker.getValue().atTime(defaultTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    newBetSession.setEndTime(endDatePicker.getValue().atTime(defaultTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    newBetSession.setInitialPrice(initialPrice);
                    
                    // Automatically set current price to initial price
                    newBetSession.setCurrentPrice(initialPrice);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter a valid price.");
                    alert.showAndWait();
                    return;
                }
                
                // Set status to pending automatically
                newBetSession.setStatus("pending");
                
                // Set creation date to current date/time
                newBetSession.setCreatedAt(LocalDateTime.now());
                
                // Save to database
                betSessionService.addBetSession(newBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                Alert alert = new Alert(AlertType.ERROR, 
                        "Error saving bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    @FXML
    private void showUpdateDialog() {
        BetSession selectedBetSession = tableView.getSelectionModel().getSelectedItem();
        if (selectedBetSession == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "Please select a bet session to update.");
            alert.showAndWait();
            return;
        }
        
        // Check if bet session is in 'pending' status
        if (!"pending".equals(selectedBetSession.getStatus())) {
            Alert alert = new Alert(AlertType.WARNING,
                    "Only pending bet sessions can be updated.");
            alert.setTitle("Cannot Update");
            alert.setHeaderText("Bet Session Not Pending");
            alert.showAndWait();
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Update Bet Session");
        
        // Create form components - only for editable fields
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField();
        TextField currentPriceField = new TextField();
        ComboBox<String> statusComboBox = new ComboBox<>();
        
        // Display author and artwork information (read-only)
        String authorInfo = selectedBetSession.getAuthor() != null ? 
                           "ID: " + selectedBetSession.getAuthor().getId() + 
                           (selectedBetSession.getAuthor().getName() != null ? 
                           ", Name: " + selectedBetSession.getAuthor().getName() : "") : 
                           "N/A";
                           
        String artworkInfo = selectedBetSession.getArtwork() != null ? 
                            "ID: " + selectedBetSession.getArtwork().getId() + 
                            (selectedBetSession.getArtwork().getTitle() != null ? 
                            ", Title: " + selectedBetSession.getArtwork().getTitle() : "") : 
                            "N/A";
        
        Label authorLabel = new Label(authorInfo);
        Label artworkLabel = new Label(artworkInfo);
        
        // Status options - restrict to relevant statuses
        statusComboBox.getItems().addAll("pending", "active");
        
        // Set values from selected bet session
        if (selectedBetSession.getStartTime() != null) {
            startDatePicker.setValue(selectedBetSession.getStartTime().toLocalDate());
        }
        
        if (selectedBetSession.getEndTime() != null) {
            endDatePicker.setValue(selectedBetSession.getEndTime().toLocalDate());
        }
        
        initialPriceField.setText(String.valueOf(selectedBetSession.getInitialPrice()));
        currentPriceField.setText(String.valueOf(selectedBetSession.getCurrentPrice()));
        statusComboBox.setValue(selectedBetSession.getStatus());
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new Label("Author (read-only):"), 0, 0);
        formLayout.add(authorLabel, 1, 0);
        formLayout.add(new Label("Artwork (read-only):"), 0, 1);
        formLayout.add(artworkLabel, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Current Price:"), 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        formLayout.add(new Label("Status:"), 0, 6);
        formLayout.add(statusComboBox, 1, 6);
        
        // Buttons
        Button updateButton = new Button("Update");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonLayout = new HBox(10, updateButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new Insets(10));
        
        updateButton.setOnAction(e -> {
            try {
                // Author and Artwork remain unchanged
                // Only update time, price and status
                
                // Get time values - preserve existing time if available, otherwise use noon
                LocalTime defaultTime = LocalTime.of(12, 0);
                LocalTime existingStartTime = selectedBetSession.getStartTime() != null ? 
                                                      selectedBetSession.getStartTime().toLocalTime() : 
                                                      defaultTime;
                LocalTime existingEndTime = selectedBetSession.getEndTime() != null ? 
                                                    selectedBetSession.getEndTime().toLocalTime() : 
                                                    defaultTime;
                
                if (startDatePicker.getValue() != null) {
                    selectedBetSession.setStartTime(startDatePicker.getValue().atTime(existingStartTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    selectedBetSession.setEndTime(endDatePicker.getValue().atTime(existingEndTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    selectedBetSession.setInitialPrice(initialPrice);
                    
                    double currentPrice = Double.parseDouble(currentPriceField.getText());
                    selectedBetSession.setCurrentPrice(currentPrice);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR, 
                            "Please enter valid prices.");
                    alert.showAndWait();
                    return;
                }
                
                selectedBetSession.setStatus(statusComboBox.getValue());
                
                // Save to database
                betSessionService.updateBetSession(selectedBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                Alert alert = new Alert(AlertType.ERROR, 
                        "Error updating bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    @FXML
    private void deleteSelectedBetSession() {
        BetSession selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Create confirmation dialog
            Alert confirmDialog = new Alert(AlertType.CONFIRMATION,
                    "Are you sure you want to delete this bet session?",
                    ButtonType.YES,
                    ButtonType.NO
            );
            confirmDialog.setTitle("Confirm Deletion");
            confirmDialog.setHeaderText("Delete Bet Session #" + selected.getId());
            
            // Show dialog and wait for response
            ButtonType result = confirmDialog.showAndWait().orElse(ButtonType.NO);
            
            // If user confirms, delete the bet session
            if (result == ButtonType.YES) {
                try {
                    betSessionService.deleteBetSession(selected.getId());
                    loadBetSessions();
                } catch (SQLException ex) {
                    Alert errorAlert = new Alert(AlertType.ERROR,
                            "Error deleting bet session: " + ex.getMessage());
                    errorAlert.setTitle("Database Error");
                    errorAlert.setHeaderText("Failed to Delete Bet Session");
                    errorAlert.showAndWait();
                }
            }
        } else {
            // No bet session selected, show warning
            Alert warning = new Alert(AlertType.WARNING,
                    "Please select a bet session to delete."
            );
            warning.setTitle("No Selection");
            warning.setHeaderText("No Bet Session Selected");
            warning.showAndWait();
        }
    }

    /**
     * Starts a timer to update the countdown display every second
     */
    private void startCountdownTimer() {
        // Stop any existing timer first
        stopCountdownTimer();
        
        // Create a new scheduler
        countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule a task to update the table every second
        countdownExecutor.scheduleAtFixedRate(() -> {
            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                tableView.refresh();
            });
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Stops the countdown timer and releases resources
     */
    private void stopCountdownTimer() {
        if (countdownExecutor != null && !countdownExecutor.isShutdown()) {
            countdownExecutor.shutdownNow();
            countdownExecutor = null;
        }
    }

    // Clean up resources when controller is no longer needed
    public void cleanup() {
        stopCountdownTimer();
    }

    void setBetSession(BetSession selectedSession) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
