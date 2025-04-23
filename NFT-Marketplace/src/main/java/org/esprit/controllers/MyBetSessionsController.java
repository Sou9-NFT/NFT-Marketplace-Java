package org.esprit.controllers;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.Bid;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.BetSessionService;
import org.esprit.services.BidService;

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

public class MyBetSessionsController implements Initializable {

    // Tab 1: Browse Bet Sessions - Marketplace table
    @FXML
    private TableView<BetSession> marketplaceBetsTableView;
    
    @FXML
    private TableColumn<BetSession, String> marketplaceArtworkColumn;
    
    @FXML
    private TableColumn<BetSession, String> marketplaceSellerColumn;
    
    @FXML
    private TableColumn<BetSession, String> marketplaceEndTimeColumn;
    
    @FXML
    private TableColumn<BetSession, String> marketplaceCurrentPriceColumn;
    
    @FXML
    private TableColumn<BetSession, Void> marketplaceActionsColumn;
    
    // Tab 1: Browse Bet Sessions - Recent bids table
    @FXML
    private TableView<BidRecord> recentBidsTableView;
    
    @FXML
    private TableColumn<BidRecord, String> bidArtworkColumn;
    
    @FXML
    private TableColumn<BidRecord, String> bidSellerColumn;
    
    @FXML
    private TableColumn<BidRecord, String> bidTimeColumn;
    
    @FXML
    private TableColumn<BidRecord, String> bidAmountColumn;
    
    @FXML
    private TableColumn<BidRecord, String> bidStatusColumn;
    
    @FXML
    private TableColumn<BidRecord, Void> bidActionsColumn;
    
    // Tab 2: My Bet Sessions - Active table
    @FXML
    private TableView<BetSession> activeBetsTableView;
    
    @FXML
    private TableColumn<BetSession, String> activeArtworkColumn;
    
    @FXML
    private TableColumn<BetSession, String> activeStartTimeColumn;
    
    @FXML
    private TableColumn<BetSession, String> activeEndTimeColumn;
    
    @FXML
    private TableColumn<BetSession, String> activeCurrentPriceColumn;
    
    @FXML
    private TableColumn<BetSession, String> activeStatusColumn;
    
    @FXML
    private TableColumn<BetSession, Void> activeActionsColumn;
    
    // Tab 2: My Bet Sessions - Completed table
    @FXML
    private TableView<BetSession> completedBetsTableView;
    
    @FXML
    private TableColumn<BetSession, String> completedArtworkColumn;
    
    @FXML
    private TableColumn<BetSession, String> completedStartTimeColumn;
    
    @FXML
    private TableColumn<BetSession, String> completedEndTimeColumn;
    
    @FXML
    private TableColumn<BetSession, String> completedFinalPriceColumn;
    
    @FXML
    private TableColumn<BetSession, String> completedStatusColumn;
    
    @FXML
    private TableColumn<BetSession, Void> completedActionsColumn;
    
    @FXML
    private Button createBetSessionButton;
    
    @FXML
    private Button refreshButton;
    
    // Helper class to represent a bid record
    public static class BidRecord {
        private final BetSession betSession;
        private final LocalDateTime bidTime;
        private final double bidAmount;
        private final String status;
        
        public BidRecord(BetSession betSession, LocalDateTime bidTime, double bidAmount, String status) {
            this.betSession = betSession;
            this.bidTime = bidTime;
            this.bidAmount = bidAmount;
            this.status = status;
        }
        
        public BetSession getBetSession() { return betSession; }
        public LocalDateTime getBidTime() { return bidTime; }
        public double getBidAmount() { return bidAmount; }
        public String getStatus() { return status; }
    }
    
    private BetSessionService betSessionService;
    private User currentUser;

    /**
     * Sets the current user for this controller and refreshes the bet sessions
     * @param user The current user
     */    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Reload all data when user is set
        loadAllData();
    }
    
    /**
     * Loads all data for both tabs
     */
    private void loadAllData() {
        loadMarketplaceBetSessions();
        loadRecentBids();
        loadMyBetSessions();
    }
      private ArtworkService artworkService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        betSessionService = new BetSessionService();
        artworkService = new ArtworkService();
        
        // Configure TAB 1: Marketplace table columns
        marketplaceArtworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
        
        marketplaceSellerColumn.setCellValueFactory(cellData -> {
            User seller = cellData.getValue().getAuthor();
            return seller != null ? 
                   new SimpleStringProperty(seller.getName()) : 
                   new SimpleStringProperty("Unknown");
        });
        
        marketplaceEndTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getEndTime();
            return time != null ? 
                   new SimpleStringProperty(formatDateTime(time)) : 
                   new SimpleStringProperty("N/A");
        });
        
        marketplaceCurrentPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().getCurrentPrice())));
            
        // Configure TAB 1: Recent bids table columns
        bidArtworkColumn.setCellValueFactory(cellData -> {
            BetSession session = cellData.getValue().getBetSession();
            Artwork artwork = session != null ? session.getArtwork() : null;
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
        
        bidSellerColumn.setCellValueFactory(cellData -> {
            BetSession session = cellData.getValue().getBetSession();
            User seller = session != null ? session.getAuthor() : null;
            return seller != null ? 
                   new SimpleStringProperty(seller.getName()) : 
                   new SimpleStringProperty("Unknown");
        });
        
        bidTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatDateTime(cellData.getValue().getBidTime())));
            
        bidAmountColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().getBidAmount())));
            
        bidStatusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus()));
            
        // Configure TAB 2: My active bet sessions table columns
        activeArtworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
          
        activeStartTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getStartTime();
            return time != null ? 
                   new SimpleStringProperty(formatDateTime(time)) : 
                   new SimpleStringProperty("N/A");
        });
        
        activeEndTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getEndTime();
            return time != null ? 
                   new SimpleStringProperty(formatDateTime(time)) : 
                   new SimpleStringProperty("N/A");
        });
            
        activeCurrentPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().getCurrentPrice())));
            
        activeStatusColumn.setCellValueFactory(cellData -> 
            cellData.getValue().statusProperty());
            
        // Configure TAB 2: My completed bet sessions table columns
        completedArtworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
          
        completedStartTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getStartTime();
            return time != null ? 
                   new SimpleStringProperty(formatDateTime(time)) : 
                   new SimpleStringProperty("N/A");
        });
        
        completedEndTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getEndTime();
            return time != null ? 
                   new SimpleStringProperty(formatDateTime(time)) : 
                   new SimpleStringProperty("N/A");
        });
            
        completedFinalPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().getCurrentPrice())));
            
        completedStatusColumn.setCellValueFactory(cellData -> 
            cellData.getValue().statusProperty());
        
        // Set up action columns for all tables
        setupActionColumns();
        
        // Load bet session data if user is already set
        if (currentUser != null) {
            loadAllData();
        }
    }
      /**
     * Loads all active marketplace bet sessions (not created by current user)
     */
    private void loadMarketplaceBetSessions() {
        if (currentUser != null) {
            try {
                // Get active bet sessions NOT created by the current user
                List<BetSession> marketplaceSessions = betSessionService.getActiveSessionsNotByAuthor(currentUser.getId());
                
                // Populate the marketplace table
                marketplaceBetsTableView.setItems(FXCollections.observableArrayList(marketplaceSessions));
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText(null);
                alert.setContentText("Error loading marketplace bet sessions: " + e.getMessage());
                alert.showAndWait();
                
                marketplaceBetsTableView.setItems(FXCollections.observableArrayList());
            }
        } else {
            marketplaceBetsTableView.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Loads recent bids made by the current user
     * Note: This is a placeholder implementation - in a real system, 
     * you would have a BidService to track user bids
     */
    private void loadRecentBids() {
        // This would be replaced with actual bid history from database
        // For now, we're creating some sample data
        if (currentUser != null) {
            try {
                // In a real implementation, you would:
                // 1. Get bid history for current user from database
                // 2. Convert to BidRecord objects
                // 3. Populate the table
                
                // For now, we'll just show an empty table
                recentBidsTableView.setItems(FXCollections.observableArrayList());
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText(null);
                alert.setContentText("Error loading bid history: " + e.getMessage());
                alert.showAndWait();
                
                recentBidsTableView.setItems(FXCollections.observableArrayList());
            }
        } else {
            recentBidsTableView.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Loads bet sessions created by the current user
     */
    private void loadMyBetSessions() {
        if (currentUser != null) {
            try {
                // Get bet sessions created by the current user
                List<BetSession> mySessions = betSessionService.getSessionsByAuthor(currentUser.getId());
                
                // Split the sessions by status (active vs completed)
                List<BetSession> activeSessions = mySessions.stream()
                        .filter(session -> "active".equalsIgnoreCase(session.getStatus()) || 
                                          "pending".equalsIgnoreCase(session.getStatus()))
                        .collect(Collectors.toList());
                
                List<BetSession> completedSessions = mySessions.stream()
                        .filter(session -> "ended".equalsIgnoreCase(session.getStatus()) || 
                                          "completed".equalsIgnoreCase(session.getStatus()))
                        .collect(Collectors.toList());
                
                // Populate the two tables with the respective session types
                activeBetsTableView.setItems(FXCollections.observableArrayList(activeSessions));
                completedBetsTableView.setItems(FXCollections.observableArrayList(completedSessions));
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText(null);
                alert.setContentText("Error loading your bet sessions: " + e.getMessage());
                alert.showAndWait();
                
                // Clear both tables
                activeBetsTableView.setItems(FXCollections.observableArrayList());
                completedBetsTableView.setItems(FXCollections.observableArrayList());
            }
        } else {
            // No user logged in, show empty tables
            activeBetsTableView.setItems(FXCollections.observableArrayList());
            completedBetsTableView.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Shows bet session details in a dialog
     * @param session The selected bet session
     */
    private void showBetSessionDetails(BetSession session) {
        // Create a new dialog
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Bet Session Details");

        // Create layout with all the details
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;
        // Display artwork image if available
        javafx.scene.image.ImageView artworkImageView = new javafx.scene.image.ImageView();
        artworkImageView.setFitHeight(180);
        artworkImageView.setPreserveRatio(true);
        if (session.getArtwork() != null && session.getArtwork().getImageName() != null) {
            String imagePath = "src/main/resources/uploads/" + session.getArtwork().getImageName();
            java.io.File imageFile = new java.io.File(imagePath);
            if (imageFile.exists()) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile.toURI().toString());
                artworkImageView.setImage(image);
            }
        }
        grid.add(artworkImageView, 0, row, 2, 1);
        row++;

        // Remove any display of session.getId() or BetSession ID from the dialog
        // Only show user-friendly fields (author, artwork, dates, prices, status, etc.)
        grid.add(new javafx.scene.control.Label("Author:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getAuthor() != null ? session.getAuthor().getName() : "N/A"), 1, row++);
        grid.add(new javafx.scene.control.Label("Artwork:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getArtwork() != null ? session.getArtwork().getTitle() : "N/A"), 1, row++);
        grid.add(new javafx.scene.control.Label("Created at:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getCreatedAt() != null ? session.getCreatedAt().toString() : "N/A"), 1, row++);
        grid.add(new javafx.scene.control.Label("Start time:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getStartTime() != null ? session.getStartTime().toString() : "N/A"), 1, row++);
        grid.add(new javafx.scene.control.Label("End time:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getEndTime() != null ? session.getEndTime().toString() : "N/A"), 1, row++);
        grid.add(new javafx.scene.control.Label("Initial price:"), 0, row);
        grid.add(new javafx.scene.control.Label(String.format("%.2f", session.getInitialPrice())), 1, row++);
        grid.add(new javafx.scene.control.Label("Current price:"), 0, row);
        grid.add(new javafx.scene.control.Label(String.format("%.2f", session.getCurrentPrice())), 1, row++);
        grid.add(new javafx.scene.control.Label("Status:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.getStatus()), 1, row++);
        // Show if NFT is in Mystery Mode
        grid.add(new javafx.scene.control.Label("Mystery Mode:"), 0, row);
        grid.add(new javafx.scene.control.Label(session.isMysteriousMode() ? "Yes" : "No"), 1, row++);

        // Add close button
        javafx.scene.control.Button closeButton = new javafx.scene.control.Button("Close");
        closeButton.setOnAction(e -> dialog.close());

        // Create root layout
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20, grid, closeButton);
        root.setPadding(new javafx.geometry.Insets(10));
        root.setAlignment(javafx.geometry.Pos.CENTER);

        // Set the scene
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Shows a dialog to place a bid on a bet session
     * @param session The bet session to bid on
     */
    private void showPlaceBidDialog(BetSession session) {
        // Create a new dialog
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Place Bid");
        
        // Create bid form
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
        grid.setHgap(10);
        grid.setVgap(10);
        
        // Show current information
        grid.add(new javafx.scene.control.Label("Artwork:"), 0, 0);
        grid.add(new javafx.scene.control.Label(session.getArtwork() != null ? session.getArtwork().getTitle() : "N/A"), 1, 0);
        
        grid.add(new javafx.scene.control.Label("Current price:"), 0, 1);
        grid.add(new javafx.scene.control.Label(String.format("%.2f", session.getCurrentPrice())), 1, 1);
        
        // Add bid input field
        grid.add(new javafx.scene.control.Label("Your bid:"), 0, 2);
        javafx.scene.control.TextField bidField = new javafx.scene.control.TextField();
        bidField.setPromptText("Enter amount higher than current price");
        grid.add(bidField, 1, 2);
        
        // Add buttons
        javafx.scene.control.Button submitButton = new javafx.scene.control.Button("Submit Bid");
        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("Cancel");
        
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, submitButton, cancelButton);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Submit button action
        submitButton.setOnAction(e -> {
            try {
                double bidAmount = Double.parseDouble(bidField.getText());
                if (bidAmount <= session.getCurrentPrice()) {
                    showError("Bid must be higher than the current price.");
                    return;
                }
                // Check if user has enough balance
                if (!canUserPlaceBid(currentUser, bidAmount)) {
                    return;
                }
                try {
                    // Create a new Bid entity
                    Bid bid = new Bid(bidAmount, session, currentUser);
                    BidService bidService = new BidService();
                    bidService.addBid(bid);

                    // Deduct bid amount from user balance
                    BigDecimal newBalance = currentUser.getBalance().subtract(BigDecimal.valueOf(bidAmount));
                    currentUser.setBalance(newBalance);
                    new org.esprit.services.UserService().update(currentUser);

                    // Update session with new price
                    session.setCurrentPrice(bidAmount);
                    BetSessionService service = new BetSessionService();
                    service.updateBetSession(session);

                    // Success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Your bid has been placed successfully!");
                    alert.showAndWait();

                    // Refresh the table
                    loadMyBetSessions();

                    // Close dialog
                    dialog.close();
                } catch (Exception ex) {
                    showError("Failed to place bid: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number.");
            }
        });
        
        // Cancel button action
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create root layout
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20, grid, buttonBox);
        root.setPadding(new javafx.geometry.Insets(10));
        root.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Set the scene
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Shows an error message
     * @param message The error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Format a LocalDateTime for display in the UI
     * @param dateTime The LocalDateTime to format
     * @return A formatted string representation
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.toLocalDate() + " " + 
               String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
    }
    
    /**
     * Refreshes all the data in the tabs
     */
    @FXML
    private void refreshBetSessions() {
        loadAllData();
    }
    
    /**
     * Refreshes only the active marketplace bet sessions in the first tab
     */
    @FXML
    private void refreshMarketplaceBets() {
        loadMarketplaceBetSessions();
    }
    
    /**
     * Handles the creation of a new bet session
     */
    @FXML
    private void handleCreateBetSession() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Not Logged In", 
                      "You must be logged in to create bet sessions.");
            return;
        }
        
        // Create a new dialog for creating a bet session
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Create New Bet Session");
        
        // Create form components
        Label authorInfoLabel = new Label("Author: " + currentUser.getName());
        ComboBox<Artwork> artworkComboBox = new ComboBox<>();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField();
        Label statusLabel = new Label("Status: pending (automatically set)");
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        // Add Mystery Mode checkbox
        javafx.scene.control.CheckBox mysteryModeCheckBox = new javafx.scene.control.CheckBox("Mystery Mode");
        formLayout.add(new Label("Mystery Mode:"), 0, 6);
        formLayout.add(mysteryModeCheckBox, 1, 6);
        
        formLayout.add(new Label("Author:"), 0, 0);
        formLayout.add(authorInfoLabel, 1, 0);
        formLayout.add(new Label("Artwork:"), 0, 1);
        formLayout.add(artworkComboBox, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Status:"), 0, 5);
        formLayout.add(statusLabel, 1, 5);
        // Add buttons
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
                
                // Set Artwork from ComboBox
                Artwork selectedArtwork = artworkComboBox.getValue();
                if (selectedArtwork == null) {
                    showAlert(Alert.AlertType.ERROR, "Missing Input", "Please select an artwork.");
                    return;
                }
                newBetSession.setArtwork(selectedArtwork);
                
                // Get time values - set to noon by default
                LocalTime defaultTime = LocalTime.of(12, 0);
                
                if (startDatePicker.getValue() != null) {
                    newBetSession.setStartTime(startDatePicker.getValue().atTime(defaultTime));
                } else {
                    showAlert(Alert.AlertType.ERROR, "Missing Input", 
                              "Please select a start date.");
                    return;
                }
                
                if (endDatePicker.getValue() != null) {
                    newBetSession.setEndTime(endDatePicker.getValue().atTime(defaultTime));
                } else {
                    showAlert(Alert.AlertType.ERROR, "Missing Input", 
                              "Please select an end date.");
                    return;
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    newBetSession.setInitialPrice(initialPrice);
                    
                    // Automatically set current price to initial price
                    newBetSession.setCurrentPrice(initialPrice);
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", 
                              "Please enter a valid price.");
                    return;
                }
                
                // Set status to pending automatically
                newBetSession.setStatus("pending");
                // Set creation date to current date/time
                newBetSession.setCreatedAt(LocalDateTime.now());
                // Set mystery mode from checkbox
                newBetSession.setMysteriousMode(mysteryModeCheckBox.isSelected());
                // Save to database
                betSessionService.addBetSession(newBetSession);
                
                // Refresh tables
                loadAllData();
                
                // Close dialog
                dialog.close();
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                          "Bet session created successfully.");
                
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                          "Error saving bet session: " + ex.getMessage());
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Fill artworkComboBox with artworks owned by the user
        try {
            List<Artwork> userArtworks = artworkService.getByOwner(currentUser.getId());
            artworkComboBox.setItems(FXCollections.observableArrayList(userArtworks));
            artworkComboBox.setPromptText("Select your artwork");
            artworkComboBox.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Artwork item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitle());
                }
            });
            artworkComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Artwork item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitle());
                }
            });
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load your artworks: " + ex.getMessage());
        }
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Handles editing an existing bet session
     * @param betSession The bet session to edit
     */
    private void handleEditBetSession(BetSession betSession) {
        if (betSession == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", 
                      "Please select a bet session to edit.");
            return;
        }
        
        // Check if bet session is in 'pending' status
        if (!"pending".equals(betSession.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Edit",
                      "Only pending bet sessions can be updated.");
            return;
        }
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Update Bet Session");
        
        // Create form components with values from the selected session
        String authorInfo = "Author: " + betSession.getAuthor().getName();
        String artworkInfo = "Artwork: " + betSession.getArtwork().getTitle();
        
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField initialPriceField = new TextField(String.valueOf(betSession.getInitialPrice()));
        TextField currentPriceField = new TextField(String.valueOf(betSession.getCurrentPrice()));
        
        // Set values from selected bet session
        if (betSession.getStartTime() != null) {
            startDatePicker.setValue(betSession.getStartTime().toLocalDate());
        }
        
        if (betSession.getEndTime() != null) {
            endDatePicker.setValue(betSession.getEndTime().toLocalDate());
        }
        
        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        Label authorLabel = new Label(authorInfo);
        Label artworkLabel = new Label(artworkInfo);
        
        formLayout.add(new Label("Author:"), 0, 0);
        formLayout.add(authorLabel, 1, 0);
        formLayout.add(new Label("Artwork:"), 0, 1);
        formLayout.add(artworkLabel, 1, 1);
        formLayout.add(new Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new Label("Current Price:"), 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        
        // Buttons
        Button updateButton = new Button("Update");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonLayout = new HBox(10, updateButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new Insets(10));
        
        updateButton.setOnAction(e -> {
            try {
                // Update bet session with form data
                
                // Get time values - preserve existing time components
                LocalTime defaultTime = LocalTime.of(12, 0);
                LocalTime existingStartTime = betSession.getStartTime() != null ? 
                                            betSession.getStartTime().toLocalTime() : 
                                            defaultTime;
                LocalTime existingEndTime = betSession.getEndTime() != null ? 
                                          betSession.getEndTime().toLocalTime() : 
                                          defaultTime;
                
                if (startDatePicker.getValue() != null) {
                    betSession.setStartTime(startDatePicker.getValue().atTime(existingStartTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    betSession.setEndTime(endDatePicker.getValue().atTime(existingEndTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    betSession.setInitialPrice(initialPrice);
                    
                    double currentPrice = Double.parseDouble(currentPriceField.getText());
                    betSession.setCurrentPrice(currentPrice);
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", 
                              "Please enter valid prices.");
                    return;
                }
                
                // Save to database
                betSessionService.updateBetSession(betSession);
                
                // Refresh tables
                loadAllData();
                
                // Close dialog
                dialog.close();
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                          "Bet session updated successfully.");
                
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                          "Error updating bet session: " + ex.getMessage());
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        VBox root = new VBox(10, formLayout, buttonLayout);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
      /**
     * Shows an alert dialog with the given type, title, and message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
      /**
     * Sets up action columns with buttons for all tables
     */
    private void setupActionColumns() {
        // Setup marketplace bets action column (Tab 1)
        marketplaceActionsColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View");
            private final javafx.scene.control.Button bidButton = new javafx.scene.control.Button("Place Bid");
            private final javafx.scene.layout.HBox buttonsBox = new javafx.scene.layout.HBox(5, viewButton, bidButton);
            
            {
                // Configure view button
                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    showBetSessionDetails(session);
                });
                
                // Configure bid button
                bidButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                bidButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    showPlaceBidDialog(session);
                });
                
                // Center the buttons
                buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    BetSession session = getTableView().getItems().get(getIndex());
                    
                    // Only allow bidding on active sessions
                    bidButton.setDisable(!"active".equalsIgnoreCase(session.getStatus()));
                    
                    setGraphic(buttonsBox);
                }
            }
        });
        
        // Setup recent bids action column (Tab 1)
        bidActionsColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View Details");
            
            {
                // Configure view button
                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BidRecord bid = getTableView().getItems().get(getIndex());
                    showBetSessionDetails(bid.getBetSession());
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
        
        // Setup active bets action column (Tab 2 - My Active Bet Sessions)
        activeActionsColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View");
            private final javafx.scene.control.Button editButton = new javafx.scene.control.Button("Edit");
            private final javafx.scene.layout.HBox buttonsBox = new javafx.scene.layout.HBox(5, viewButton, editButton);
            
            {
                // Configure view button
                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    showBetSessionDetails(session);
                });
                
                // Configure edit button
                editButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                editButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    handleEditBetSession(session);
                });
                
                // Center the buttons
                buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    BetSession session = getTableView().getItems().get(getIndex());
                    
                    // Only allow editing pending sessions
                    editButton.setDisable(!"pending".equalsIgnoreCase(session.getStatus()));
                    
                    setGraphic(buttonsBox);
                }
            }
        });
        
        // Setup completed bets action column (Tab 2 - My Completed Bet Sessions)
        completedActionsColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View");
            
            {
                // Configure view button
                viewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                viewButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    showBetSessionDetails(session);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

        private boolean canUserPlaceBid(User user, double bidAmount) {
        if (user == null || user.getBalance() == null) {
            Alert alert = new Alert(AlertType.ERROR, "User or balance not found.");
            alert.showAndWait();
            return false;
        }
        if (user.getBalance().doubleValue() < bidAmount) {
            Alert alert = new Alert(AlertType.WARNING, "Insufficient balance to place this bid.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
