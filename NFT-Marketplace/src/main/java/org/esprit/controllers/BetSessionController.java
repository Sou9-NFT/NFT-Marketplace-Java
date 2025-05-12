package org.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.services.BetSessionService;
import org.esprit.services.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BetSessionController implements Initializable {

    @FXML
    private TableView<BetSession> tableView;
    
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
    private TableColumn<BetSession, Void> actionsColumn;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button statsButton;
    
    private BetSessionService betSessionService;
    private UserService userService;
    
    // Add field for current user
    private User currentUser;
    
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
                   new SimpleStringProperty("N/A");        });
          createdAtColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());
        createdAtColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime createdAt, boolean empty) {
                super.updateItem(createdAt, empty);
                
                if (empty || createdAt == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way
                setText(createdAt.format(formatter));
            }
        });
        
        startTimeColumn.setCellValueFactory(cellData -> cellData.getValue().startTimeProperty());
        startTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime startTime, boolean empty) {
                super.updateItem(startTime, empty);
                
                if (empty || startTime == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way
                setText(startTime.format(formatter));
            }
        });
          // Custom cell factory to format date display for end time
        endTimeColumn.setCellValueFactory(cellData -> cellData.getValue().endTimeProperty());
        endTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            @Override
            protected void updateItem(LocalDateTime endTime, boolean empty) {
                super.updateItem(endTime, empty);
                
                if (empty || endTime == null) {
                    setText(null);
                    return;
                }
                
                // Format date in a readable way for all sessions
                setText(endTime.format(formatter));
            }
        });
          initialPriceColumn.setCellValueFactory(cellData -> cellData.getValue().initialPriceProperty());
        currentPriceColumn.setCellValueFactory(cellData -> cellData.getValue().currentPriceProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Set up actions column with icon buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("\uD83D\uDC41"); // Eye icon
            private final Button editButton = new Button("\uD83D\uDD8C"); // Unicode for paintbrush/edit
            private final Button deleteButton = new Button("\uD83D\uDDD1"); // Trash icon
            private final HBox hbox = new HBox(5, viewButton, editButton, deleteButton);
            {
                viewButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                editButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                deleteButton.setStyle("-fx-font-size: 12px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    viewBetSessionDetails();
                });
                editButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    showUpdateDialog();
                });
                deleteButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    tableView.getSelectionModel().select(session);
                    deleteSelectedBetSession();
                });
            }            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        
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
    }    @FXML
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
        
        // Create form components with styling
        Label titleLabel = new Label("Create New Bet Session");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label authorInfoLabel = new Label("Author: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");
        authorInfoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");
        
        TextField artworkIdField = new TextField();
        artworkIdField.setPromptText("Enter artwork ID");
        artworkIdField.setStyle("-fx-background-radius: 4;");
        
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setStyle("-fx-background-radius: 4;");
        
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setStyle("-fx-background-radius: 4;");
        
        TextField initialPriceField = new TextField();
        initialPriceField.setPromptText("Enter initial price");
        initialPriceField.setStyle("-fx-background-radius: 4;");
        
        Label statusLabel = new Label("Status: pending (automatically set)");
        statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
        
        // Styled field labels
        Label authorLabel = new Label("Author:");
        Label artworkIdLabel = new Label("Artwork ID:");
        Label startDateLabel = new Label("Start Date:");
        Label endDateLabel = new Label("End Date:");
        Label initialPriceLabel = new Label("Initial Price:");
        Label statusTitleLabel = new Label("Status:");
        Label mysteryModeLabel = new Label("Mystery Mode:");
        
        // Apply common style to all labels
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #2c3e50;";
        authorLabel.setStyle(labelStyle);
        artworkIdLabel.setStyle(labelStyle);
        startDateLabel.setStyle(labelStyle);
        endDateLabel.setStyle(labelStyle);
        initialPriceLabel.setStyle(labelStyle);
        statusTitleLabel.setStyle(labelStyle);
        mysteryModeLabel.setStyle(labelStyle);
        
        // Layout with styled background
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(20));
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e0e6ed); -fx-background-radius: 8;");
        
        formLayout.add(authorLabel, 0, 0);
        formLayout.add(authorInfoLabel, 1, 0);
        formLayout.add(artworkIdLabel, 0, 1);
        formLayout.add(artworkIdField, 1, 1);
        formLayout.add(startDateLabel, 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(endDateLabel, 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(initialPriceLabel, 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(statusTitleLabel, 0, 5);
        formLayout.add(statusLabel, 1, 5);
        
        // Add Mystery Mode checkbox with styling
        CheckBox mysteryModeCheckBox = new CheckBox("Enable Mystery Mode");
        mysteryModeCheckBox.setStyle("-fx-text-fill: #e74c3c;");
        
        // Container for checkbox with special styling
        HBox mysteryContainer = new HBox(10, mysteryModeCheckBox);
        mysteryContainer.setStyle("-fx-padding: 5; -fx-background-color: rgba(231, 76, 60, 0.1); -fx-background-radius: 4;");
        
        formLayout.add(mysteryModeLabel, 0, 6);
        formLayout.add(mysteryContainer, 1, 6);
        
        // Styled buttons
        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;");
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;");
        
        // Button hover effects
        saveButton.setOnMouseEntered(e -> saveButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        
        HBox buttonLayout = new HBox(15, saveButton, cancelButton);
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
                
                // Set mysterious mode from checkbox
                newBetSession.setMysteriousMode(mysteryModeCheckBox.isSelected());
                
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
        
        // Create scene with styled background
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ecf0f1;");
        root.getChildren().addAll(titleLabel, formLayout, buttonLayout);
        
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
        
        // Create title label with styling
        Label titleLabel = new Label("Update Bet Session");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Create form components with styling
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setStyle("-fx-background-radius: 4;");
        
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setStyle("-fx-background-radius: 4;");
        
        TextField initialPriceField = new TextField();
        initialPriceField.setStyle("-fx-background-radius: 4;");
        
        TextField currentPriceField = new TextField();
        currentPriceField.setStyle("-fx-background-radius: 4;");
        
        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.setStyle("-fx-background-radius: 4;");
        
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
        authorLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        
        Label artworkLabel = new Label(artworkInfo);
        artworkLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
        
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
        
        // Create styled labels
        Label authorTitleLabel = new Label("Author (read-only):");
        Label artworkTitleLabel = new Label("Artwork (read-only):");
        Label startDateLabel = new Label("Start Date:");
        Label endDateLabel = new Label("End Date:");
        Label initialPriceLabel = new Label("Initial Price:");
        Label currentPriceLabel = new Label("Current Price:");
        Label statusLabel = new Label("Status:");
        
        // Apply common style to all labels
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #2c3e50;";
        authorTitleLabel.setStyle(labelStyle);
        artworkTitleLabel.setStyle(labelStyle);
        startDateLabel.setStyle(labelStyle);
        endDateLabel.setStyle(labelStyle);
        initialPriceLabel.setStyle(labelStyle);
        currentPriceLabel.setStyle(labelStyle);
        statusLabel.setStyle(labelStyle);
        
        // Layout with styled background
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(20));
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e0e6ed); -fx-background-radius: 8;");
        
        formLayout.add(authorTitleLabel, 0, 0);
        formLayout.add(authorLabel, 1, 0);
        formLayout.add(artworkTitleLabel, 0, 1);
        formLayout.add(artworkLabel, 1, 1);
        formLayout.add(startDateLabel, 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(endDateLabel, 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(initialPriceLabel, 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(currentPriceLabel, 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        formLayout.add(statusLabel, 0, 6);
        formLayout.add(statusComboBox, 1, 6);
        
        // Styled buttons
        Button updateButton = new Button("Update");
        updateButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;");
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;");
        
        // Button hover effects
        updateButton.setOnMouseEntered(e -> updateButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        updateButton.setOnMouseExited(e -> updateButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4;"));
        
        HBox buttonLayout = new HBox(15, updateButton, cancelButton);
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
        VBox root = new VBox(10, titleLabel, formLayout, buttonLayout);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ecf0f1;");
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
      @FXML
    private void viewBetSessionDetails() {
        BetSession selectedBetSession = tableView.getSelectionModel().getSelectedItem();
        if (selectedBetSession == null) {
            Alert alert = new Alert(AlertType.WARNING, 
                    "Please select a bet session to view.");
            alert.showAndWait();
            return;
        }
        
        try {
            // Create new stage for the detail view
            Stage detailStage = new Stage();
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.setTitle("Bet Session Details");
            
            // Create layout for details
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            
            // Header and info sections
            Label titleLabel = new Label("Bet Session Details");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            
            // Session info
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(15);
            infoGrid.setVgap(10);
            infoGrid.setPadding(new Insets(10));
            
            // Artwork image
            ImageView artworkImageView = new ImageView();
            artworkImageView.setFitWidth(300);
            artworkImageView.setFitHeight(300);
            artworkImageView.setPreserveRatio(true);
            
            // Try to load artwork image
            if (selectedBetSession.getArtwork() != null && selectedBetSession.getArtwork().getImageName() != null) {
                String imageName = selectedBetSession.getArtwork().getImageName();
                
                try {
                    Image image = null;
                    
                    // Check if the image name is a URL (starts with http:// or https://)
                    if (imageName.startsWith("http://") || imageName.startsWith("https://")) {
                        // Directly load from URL
                        System.out.println("Debug - Loading image from URL: " + imageName);
                        image = new Image(imageName);
                        artworkImageView.setImage(image);
                    } else {
                        // Legacy approach - load from local file
                        String imagePath = "src/main/resources/uploads/" + imageName;
                        java.io.File imageFile = new java.io.File(imagePath);
                        
                        // Load image if file exists
                        if (imageFile.exists()) {
                            image = new Image(imageFile.toURI().toString());
                            artworkImageView.setImage(image);
                            System.out.println("Debug - Loading image from local path: " + imagePath);
                        } else {
                            // If file doesn't exist, try using resource stream as fallback
                            System.out.println("Debug - Image file not found at: " + imagePath + ", trying fallback methods");
                            
                            // Try with different extensions and using resource stream
                            int artworkId = selectedBetSession.getArtwork().getId();
                            String resourcePath = "/uploads/artwork_" + artworkId + ".jpg";
                            image = new Image(getClass().getResourceAsStream(resourcePath));
                            
                            if (image.getWidth() == 0) {
                                resourcePath = "/uploads/artwork_" + artworkId + ".png";
                                image = new Image(getClass().getResourceAsStream(resourcePath));
                            }
                            
                            if (image.getWidth() == 0) {
                                image = new Image(getClass().getResourceAsStream("/assets/default/artwork-placeholder.png"));
                            }
                            
                            artworkImageView.setImage(image);
                        }
                    }
                    
                    System.out.println("Debug - Mystery Mode: " + selectedBetSession.isMysteriousMode());
                    System.out.println("Debug - Number of Bids: " + selectedBetSession.getNumberOfBids());
                    
                    // Apply blur effect only if the session is in mystery mode
                    if (selectedBetSession.isMysteriousMode()) {
                        // Apply blur effect based on number of bids
                        // 0 bids = full blur (100%), 10+ bids = no blur (0%)
                        int numberOfBids = selectedBetSession.getNumberOfBids();
                        if (numberOfBids < 10) {
                            // Calculate blur amount (from 10 to 0)
                            double blurAmount = 10.0 - numberOfBids;
                            // Scale it to a reasonable range (10 = very blurry, 0 = clear)
                            blurAmount = Math.max(blurAmount * 2.5, 1.0); // Ensure at least some blur
                            
                            // Apply Gaussian blur effect
                            javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(blurAmount);
                            artworkImageView.setEffect(blur);
                            System.out.println("Debug - Applied blur with strength: " + blurAmount);
                            
                            // Add a label showing the number of bids
                            Label blurLabel = new Label("Mystery Mode - Current bids: " + numberOfBids + "/10");
                            blurLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");
                            root.getChildren().add(4, blurLabel); // Insert after the image
                        }
                    }
                } catch (Exception e) {
                    // Fall back to default image
                    System.err.println("Error loading image: " + e.getMessage());
                    try {
                        Image defaultImage = new Image(getClass().getResourceAsStream("/assets/default/artwork-placeholder.png"));
                        artworkImageView.setImage(defaultImage);
                    } catch (Exception ex) {
                        System.err.println("Could not load default artwork image: " + ex.getMessage());
                    }
                }
            }
            
            // Add info to grid
            // infoGrid.add(new Label("ID:"), 0, 0);
            // infoGrid.add(new Label(String.valueOf(selectedBetSession.getId())), 1, 0);
            // Do not display BetSession ID
            if (selectedBetSession.getAuthor() != null) {
                infoGrid.add(new Label("Author:"), 0, 0);
                infoGrid.add(new Label(selectedBetSession.getAuthor().getName()), 1, 0);
            }
            if (selectedBetSession.getArtwork() != null) {
                infoGrid.add(new Label("Artwork:"), 0, 1);
                infoGrid.add(new Label(selectedBetSession.getArtwork().getTitle()), 1, 1);
            }
            infoGrid.add(new Label("Initial Price:"), 0, 2);
            infoGrid.add(new Label(String.valueOf(selectedBetSession.getInitialPrice())), 1, 2);
            infoGrid.add(new Label("Current Price:"), 0, 3);
            infoGrid.add(new Label(String.valueOf(selectedBetSession.getCurrentPrice())), 1, 3);
            infoGrid.add(new Label("Status:"), 0, 4);            infoGrid.add(new Label(selectedBetSession.getStatus()), 1, 4);
            if (selectedBetSession.getStartTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                infoGrid.add(new Label("Start Time:"), 0, 5);
                infoGrid.add(new Label(selectedBetSession.getStartTime().format(formatter)), 1, 5);
            }            
            if (selectedBetSession.getEndTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                infoGrid.add(new Label("End Time:"), 0, 6);
                infoGrid.add(new Label(selectedBetSession.getEndTime().format(formatter)), 1, 6);
            }
            
            // Add mysterious mode indicator
            infoGrid.add(new Label("Mysterious Mode:"), 0, 7);
            infoGrid.add(new Label(selectedBetSession.isMysteriousMode() ? "Enabled" : "Disabled"), 1, 7);
            
            // Add artwork description section - show either original or mysterious description
            VBox descriptionBox = new VBox(10);
            Label descriptionTitle = new Label();
            Label descriptionContent = new Label();
            
            // Style the description content for better readability
            descriptionContent.setWrapText(true);
            descriptionContent.setMaxWidth(450);
            descriptionContent.setStyle("-fx-font-size: 14px;");
              // Debug message to see what values we're working with
            System.out.println("Debug - Mysterious Mode: " + selectedBetSession.isMysteriousMode());
            System.out.println("Debug - Generated Description: " + (selectedBetSession.getGeneratedDescription() != null ? selectedBetSession.getGeneratedDescription() : "null"));
            
            if (selectedBetSession.isMysteriousMode() && selectedBetSession.getGeneratedDescription() != null 
                    && !selectedBetSession.getGeneratedDescription().isEmpty()) {
                // Show mysterious description if mysterious mode is enabled and description exists
                descriptionTitle.setText("Mysterious Description:");
                descriptionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                descriptionContent.setText(selectedBetSession.getGeneratedDescription());
                System.out.println("Debug - Using generated description");
            } else {
                // Show original description
                descriptionTitle.setText("Artwork Description:");
                descriptionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                if (selectedBetSession.getArtwork() != null && selectedBetSession.getArtwork().getDescription() != null) {
                    descriptionContent.setText(selectedBetSession.getArtwork().getDescription());
                    System.out.println("Debug - Using artwork description");
                } else {
                    descriptionContent.setText("No description available.");
                    System.out.println("Debug - No description available");
                }
            }
            
            descriptionBox.getChildren().addAll(descriptionTitle, descriptionContent);
            
            // Close button
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> detailStage.close());
              // Add all components to root layout
            root.getChildren().addAll(
                titleLabel, 
                new Separator(), 
                new Label("Artwork Image:"),
                artworkImageView, 
                new Separator(),
                infoGrid,
                new Separator(),
                descriptionBox,
                closeButton
            );
            
            // Create scene and show the stage
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/bet-session.css").toExternalForm());
              detailStage.setScene(scene);
            
            // Set the stage to fullscreen
            detailStage.setMaximized(true);
            
            detailStage.show();
        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR, 
                    "Error showing bet session details: " + e.getMessage());
            alert.showAndWait();            e.printStackTrace();
        }
    }

    void setBetSession(BetSession selectedSession) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    // Example method for placing a bid (add this where you handle bid placement)
    @FXML
    public void showBetStatistics() {
        try {
            // Load the BetStatistics.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BetStatistics.fxml"));
            Parent root = loader.load();
            
            // Get the controller
            BetStatisticsController statisticsController = loader.getController();
            
            // Create a new stage for the statistics window
            Stage statisticsStage = new Stage();
            statisticsStage.initModality(Modality.APPLICATION_MODAL);
            statisticsStage.setTitle("Bet Statistics Dashboard");
            
            // Set the scene
            Scene scene = new Scene(root);
            statisticsStage.setScene(scene);
            
            // Show the stage
            statisticsStage.show();
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR, 
                    "Error opening statistics window: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
