package org.esprit.controllers;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
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
import org.esprit.utils.CryptoService;

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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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
    private ArtworkService artworkService;
    private CryptoService cryptoService;

    /**
     * Sets the current user for this controller and refreshes the bet sessions
     * @param user The current user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Reload all data when user is set
        loadAllData();
    }
    
    /**
     * Loads all data for both tabs
     */
    private void loadAllData() {
        loadMarketplaceBetSessions();
        loadMyBetSessions();
    }    private java.util.Timer countdownTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        betSessionService = new BetSessionService();
        artworkService = new ArtworkService();
        cryptoService = new CryptoService();
        
        // Configure TAB 1: Marketplace table columns
        marketplaceArtworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new SimpleStringProperty(artwork.getTitle()) : 
                   new SimpleStringProperty("N/A");
        });
        
        // Use simple text display for marketplace artwork column (no images)
        marketplaceArtworkColumn.setCellFactory(column -> new javafx.scene.control.TableCell<BetSession, String>() {
            @Override
            protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                
                if (empty || title == null) {
                    setText(null);
                } else {
                    setText(title);
                    
                    // Add mystery mode indicator if applicable
                    BetSession session = getTableView().getItems().get(getIndex());
                    if (session.isMysteriousMode()) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad;");
                        setText(title + " 🔮");  // Add mystery emoji
                    } else {
                        setStyle("-fx-font-weight: normal;");
                        setText(title);
                    }
                }
            }
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
            new SimpleStringProperty(String.format("%.2f Dannous", cellData.getValue().getCurrentPrice())));
            
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
                   new SimpleStringProperty(formatCountdown(time)) : 
                   new SimpleStringProperty("N/A");
        });
            activeCurrentPriceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f Dannous", cellData.getValue().getCurrentPrice())));
            
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
            new SimpleStringProperty(String.format("%.2f Dannous", cellData.getValue().getCurrentPrice())));
            
        completedStatusColumn.setCellValueFactory(cellData -> 
            cellData.getValue().statusProperty());
        
        // Set up action columns for all tables
        setupActionColumns();
          // Load bet session data if user is already set
        if (currentUser != null) {
            loadAllData();
        }
        
        // Set up a timer to update the countdown every minute
        countdownTimer = new java.util.Timer(true);
        countdownTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // Use Platform.runLater since we're updating UI from a background thread
                javafx.application.Platform.runLater(() -> {
                    // Only refresh if there are items in the table
                    if (activeBetsTableView.getItems() != null && !activeBetsTableView.getItems().isEmpty()) {
                        // Refresh the active table to update countdowns
                        activeBetsTableView.refresh();
                    }
                });
            }
        }, 0, 60000); // Update every minute (60000 ms)
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
     * Shows bet session details in a dialog with enhanced styling
     * @param session The selected bet session
     */
    private void showBetSessionDetails(BetSession session) {
        // Create a new dialog
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Bet Session Details");
        dialog.setMinWidth(720);
        dialog.setMinHeight(520);

        // Create a two-column layout with image on left, details on right
        javafx.scene.layout.HBox mainLayout = new javafx.scene.layout.HBox(25);
        mainLayout.setPadding(new javafx.geometry.Insets(25));
        mainLayout.setAlignment(javafx.geometry.Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f5f7fa);");

        // Left side - Image panel with enhanced styling
        javafx.scene.layout.VBox imagePanel = new javafx.scene.layout.VBox(15);
        imagePanel.setAlignment(javafx.geometry.Pos.CENTER);
        imagePanel.setPadding(new javafx.geometry.Insets(10));
        imagePanel.setMinWidth(320);
        imagePanel.setStyle("-fx-background-color: white; -fx-border-radius: 8px; " +
                            "-fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // Display artwork image with enhanced container
        javafx.scene.image.ImageView artworkImageView = new javafx.scene.image.ImageView();
        artworkImageView.setFitHeight(280);
        artworkImageView.setFitWidth(280);
        artworkImageView.setPreserveRatio(true);
        
        // Add a more stylish border and drop shadow to the image
        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane();
        imageContainer.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1px; -fx-padding: 8px; " +
                               "-fx-background-color: white; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        imageContainer.setEffect(new javafx.scene.effect.DropShadow(15, javafx.scene.paint.Color.rgb(0, 0, 0, 0.15)));
        imageContainer.getChildren().add(artworkImageView);

        // Create a stylish counter for number of bids
        javafx.scene.control.Label bidCountLabel = new javafx.scene.control.Label(
            "Current bids: " + session.getNumberOfBids() + "/10");
        bidCountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50; " +
                               "-fx-background-color: #ecf0f1; -fx-padding: 5 10; -fx-background-radius: 4px;");

        // Add image and bid counter to the image panel
        imagePanel.getChildren().addAll(imageContainer, bidCountLabel);
        
        // Ensure image panel has a fixed size
        imagePanel.setMinSize(320, 320);
        
        // Make sure image is displayed by adjusting settings
        artworkImageView.setCache(true);
        artworkImageView.setSmooth(true);
        imageContainer.setMinSize(280, 280);
        imageContainer.setPrefSize(280, 280);

        if (session.getArtwork() != null && session.getArtwork().getImageName() != null) {
            // Try multiple possible paths for the image
            String imageName = session.getArtwork().getImageName();
            System.out.println("Trying to load image: " + imageName);
            
            // Path 1: Direct path in src/main/resources
            String imagePath1 = "src/main/resources/uploads/" + imageName;
            // Path 2: Runtime classpath path
            String imagePath2 = "/uploads/" + imageName;
            // Path 3: Absolute path to target directory
            String imagePath3 = "target/classes/uploads/" + imageName;
            
            java.io.File imageFile1 = new java.io.File(imagePath1);
            boolean loaded = false;
            
            // Try first path (direct file access)
            if (imageFile1.exists()) {
                System.out.println("Image found at: " + imagePath1);
                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile1.toURI().toString());
                    artworkImageView.setImage(image);
                    loaded = true;
                    System.out.println("Image loaded successfully from path 1");
                } catch (Exception e) {
                    System.err.println("Error loading image from path 1: " + e.getMessage());
                }
            }
            
            // Try second path (resource stream)
            if (!loaded) {
                try {
                    java.net.URL resourceUrl = getClass().getResource(imagePath2);
                    if (resourceUrl != null) {
                        javafx.scene.image.Image image = new javafx.scene.image.Image(resourceUrl.toString());
                        artworkImageView.setImage(image);
                        loaded = true;
                        System.out.println("Image loaded successfully from path 2");
                    } else {
                        System.out.println("Resource not found at: " + imagePath2);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading image from path 2: " + e.getMessage());
                }
            }
            
            // Try third path (target directory)
            if (!loaded) {
                java.io.File imageFile3 = new java.io.File(imagePath3);
                if (imageFile3.exists()) {
                    System.out.println("Image found at: " + imagePath3);
                    try {
                        javafx.scene.image.Image image = new javafx.scene.image.Image(imageFile3.toURI().toString());
                        artworkImageView.setImage(image);
                        loaded = true;
                        System.out.println("Image loaded successfully from path 3");
                    } catch (Exception e) {
                        System.err.println("Error loading image from path 3: " + e.getMessage());
                    }
                }
            }
            
            // If the image was loaded successfully, apply effects if needed
            if (loaded) {
                // Apply blur effect for mystery mode items
                if (session.isMysteriousMode()) {
                    int numberOfBids = session.getNumberOfBids();
                    if (numberOfBids < 10) {
                        // Calculate blur amount - higher at 0 bids, lower as bids increase
                        double blurAmount = 10.0 - numberOfBids;
                        // Scale to reasonable range
                        blurAmount = Math.max(blurAmount * 2.0, 1.0);
                        // Apply blur effect
                        javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(blurAmount);
                        artworkImageView.setEffect(blur);
                        
                        // Add styled mystery mode indicator label
                        javafx.scene.control.Label mysteryLabel = new javafx.scene.control.Label("Mystery Mode - Bids: " + numberOfBids + "/10");
                        mysteryLabel.setStyle("-fx-background-color: rgba(142, 68, 173, 0.8); -fx-text-fill: white; " +
                                             "-fx-padding: 8px; -fx-font-weight: bold; -fx-background-radius: 4px;");
                        imageContainer.getChildren().add(mysteryLabel);
                    }
                }
            } else {
                // If image wasn't loaded from any path, try to load a default image
                System.out.println("Attempting to load default image");
                try {
                    java.net.URL defaultImageUrl = getClass().getResource("/assets/default/artwork-placeholder.png");
                    if (defaultImageUrl != null) {
                        javafx.scene.image.Image defaultImage = new javafx.scene.image.Image(defaultImageUrl.toString());
                        artworkImageView.setImage(defaultImage);
                        System.out.println("Default image loaded successfully");
                    } else {
                        System.out.println("Default image resource not found");
                    }
                } catch (Exception e) {
                    System.err.println("Error loading default image: " + e.getMessage());
                }
            }
        }

        // Right side - Details panel with enhanced styling
        javafx.scene.layout.VBox detailsPanel = new javafx.scene.layout.VBox(18);
        detailsPanel.setPadding(new javafx.geometry.Insets(15));
        detailsPanel.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        detailsPanel.setMinWidth(360);
        detailsPanel.setStyle("-fx-background-color: white; -fx-border-radius: 8px; " +
                             "-fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        // Style for header labels - more modern and attractive
        String headerStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                             "-fx-border-color: transparent transparent #3498db transparent; -fx-border-width: 0 0 2 0; " +
                             "-fx-padding: 0 0 5 0;";
        
        // Artwork title as header with enhanced styling
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(
            session.getArtwork() != null ? session.getArtwork().getTitle() : "Unknown Artwork");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);
        
        // Basic info section with enhanced styling
        javafx.scene.layout.GridPane basicInfoGrid = createStyledInfoGrid();
        int row = 0;
        
        // Add basic info fields
        addField(basicInfoGrid, "Creator:", session.getAuthor() != null ? session.getAuthor().getName() : "N/A", row++);
        addField(basicInfoGrid, "Status:", capitalizeFirstLetter(session.getStatus()), row++);
        addField(basicInfoGrid, "Mystery Mode:", session.isMysteriousMode() ? "Activated" : "Disabled", row++);
        
        // Price info section with header
        javafx.scene.control.Label priceHeaderLabel = new javafx.scene.control.Label("Price Information");
        priceHeaderLabel.setStyle(headerStyle);
        
        javafx.scene.layout.GridPane priceInfoGrid = createStyledInfoGrid();
        row = 0;
        
        // Fetch ETH price (only once for both price displays)
        double ethPrice = cryptoService.fetchEthereumPrice();
        
        // Add price info fields with both Dannous and ETH equivalent
        double initialPriceInDannous = session.getInitialPrice();
        double currentPriceInDannous = session.getCurrentPrice();
        
        // Use the service to format prices with ETH equivalent
        String initialPriceText = cryptoService.formatPriceWithEth(initialPriceInDannous);
        String currentPriceText = cryptoService.formatPriceWithEth(currentPriceInDannous);
        
        addField(priceInfoGrid, "Initial price:", initialPriceText, row++);
        addField(priceInfoGrid, "Current price:", currentPriceText, row++);
        
        // Description section with header
        javafx.scene.control.Label descHeaderLabel = new javafx.scene.control.Label("Description");
        descHeaderLabel.setStyle(headerStyle);
        
        // Create description content with enhanced styling
        javafx.scene.control.Label descriptionLabel = new javafx.scene.control.Label();
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-line-spacing: 1.2;");
        descriptionLabel.setMaxWidth(350);
        
        // Check if we should display description section
        boolean showDescription = true;
        
        if (session.isMysteriousMode()) {
            // In mystery mode, only show description if generated description exists
            if (session.getGeneratedDescription() != null && !session.getGeneratedDescription().isEmpty()) {
                descriptionLabel.setText(session.getGeneratedDescription());
            } else {
                // Don't show description section at all for mystery mode items with no generated description
                showDescription = false;
            }
        } else if (session.getArtwork() != null && session.getArtwork().getDescription() != null) {
            // Show the original artwork description for non-mystery mode
            descriptionLabel.setText(session.getArtwork().getDescription());
        } else {
            descriptionLabel.setText("No description available.");
        }
        
        // Create a styled container for the description
        javafx.scene.layout.VBox descriptionContainer = new javafx.scene.layout.VBox(5);
        descriptionContainer.getChildren().add(descriptionLabel);
        descriptionContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12px; " +
                                     "-fx-background-radius: 6px; -fx-border-color: #e9ecef; " +
                                     "-fx-border-radius: 6px; -fx-border-width: 1px;");

        // Add all sections to the details panel
        detailsPanel.getChildren().add(titleLabel);
        detailsPanel.getChildren().add(basicInfoGrid);
        detailsPanel.getChildren().add(priceHeaderLabel);
        detailsPanel.getChildren().add(priceInfoGrid);
        
        // Only add description to the panel if we should show it
        if (showDescription) {
            detailsPanel.getChildren().add(descHeaderLabel);
            detailsPanel.getChildren().add(descriptionContainer);
        }

        // Add close button with enhanced styling and hover effect
        javafx.scene.control.Button closeButton = new javafx.scene.control.Button("Close");
        closeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " +
                             "-fx-padding: 10 25; -fx-background-radius: 5px;");
        
        // Add hover effects
        closeButton.setOnMouseEntered(e -> 
            closeButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; " + 
                                "-fx-padding: 10 25; -fx-background-radius: 5px;"));
        closeButton.setOnMouseExited(e -> 
            closeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " + 
                                "-fx-padding: 10 25; -fx-background-radius: 5px;"));
        
        closeButton.setOnAction(e -> dialog.close());
        
        // Create a container for the button
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox();
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.setPadding(new javafx.geometry.Insets(20, 0, 10, 0));
        buttonBox.getChildren().add(closeButton);
        
        // Add image panel and details panel to the main layout
        mainLayout.getChildren().addAll(imagePanel, detailsPanel);
        
        // Create root layout with main content and button at bottom
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.getChildren().addAll(mainLayout, buttonBox);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f5f7fa);");

        // Set the scene
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Helper method to create a styled grid for information display
     */
    private javafx.scene.layout.GridPane createStyledInfoGrid() {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setPadding(new javafx.geometry.Insets(5));
        grid.setHgap(15);
        grid.setVgap(10);
        return grid;
    }
    
    /**
     * Helper method to add a field to a grid with proper styling
     */
    private void addField(javafx.scene.layout.GridPane grid, String labelText, String value, int row) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");
        
        javafx.scene.control.Label valueLabel = new javafx.scene.control.Label(value);
        valueLabel.setStyle("-fx-text-fill: #333333;");
        
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }
    
    /**
     * Helper method to capitalize the first letter of a string
     */
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
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
          // Show current information        grid.add(new javafx.scene.control.Label("Artwork:"), 0, 0);
        grid.add(new javafx.scene.control.Label(session.getArtwork() != null ? session.getArtwork().getTitle() : "N/A"), 1, 0);
          // Fetch ETH price for display
        double ethPrice = cryptoService.fetchEthereumPrice();
        double currentPriceInDannous = session.getCurrentPrice();
        
        // Use the service to format price with ETH equivalent
        String currentPriceText = cryptoService.formatPriceWithEth(currentPriceInDannous);
        
        grid.add(new javafx.scene.control.Label("Current price:"), 0, 1);
        grid.add(new javafx.scene.control.Label(currentPriceText), 1, 1);
          // Add bid input field
        grid.add(new javafx.scene.control.Label("Your bid (Dannous):"), 0, 2);
        javafx.scene.control.TextField bidField = new javafx.scene.control.TextField();
        bidField.setPromptText("Enter amount higher than current price");
        grid.add(bidField, 1, 2);
        
        // Add ETH equivalent display
        javafx.scene.control.Label ethEquivalentLabel = new javafx.scene.control.Label("");
        
        // Add final copy of ethPrice for lambda
        final double displayEthPrice = ethPrice;
        // Update ETH equivalent on bid amount change
        bidField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                double bidAmount = Double.parseDouble(newValue);
                if (displayEthPrice > 0) {
                    double ethEquivalent = cryptoService.convertDannousToEth(bidAmount);
                    ethEquivalentLabel.setText(String.format("≈ %.8f ETH", ethEquivalent));
                } else {
                    ethEquivalentLabel.setText("ETH conversion unavailable");
                }
            } catch (NumberFormatException e) {
                ethEquivalentLabel.setText("");
            }
        });
        
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
        dialog.setMinWidth(500);
        
        // Create form components with improved styling
        Label authorInfoLabel = new Label("Author: " + currentUser.getName());
        authorInfoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        ComboBox<Artwork> artworkComboBox = new ComboBox<>();
        artworkComboBox.setMaxWidth(Double.MAX_VALUE);
        artworkComboBox.setStyle("-fx-background-radius: 4; -fx-font-size: 13px;");
        
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setMaxWidth(Double.MAX_VALUE);
        startDatePicker.setStyle("-fx-background-radius: 4;");
        
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setMaxWidth(Double.MAX_VALUE);
        endDatePicker.setStyle("-fx-background-radius: 4;");
        
        TextField initialPriceField = new TextField();
        initialPriceField.setMaxWidth(Double.MAX_VALUE);
        initialPriceField.setPromptText("Enter initial price in Dannous");
        initialPriceField.setStyle("-fx-background-radius: 4; -fx-prompt-text-fill: #95a5a6;");
        
        Label statusLabel = new Label("Status: pending (automatically set)");
        statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
        
        // Add Mystery Mode checkbox with description
        CheckBox mysteryModeCheckBox = new CheckBox("Enable Mystery Mode");
        mysteryModeCheckBox.setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
        
        Label mysteryDescLabel = new Label("Artwork will be blurred until 10 bids are placed");
        mysteryDescLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #8e44ad; -fx-font-size: 11px;");
        
        // Layout with improved spacing and padding
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(20));
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setStyle("-fx-background-color: white;");
        
        // Add separator for visual organization
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #bdc3c7;");
        
        // Add fields with labels styled consistently
        addFormLabel(formLayout, "Author:", 0, 0);
        formLayout.add(authorInfoLabel, 1, 0);
        
        addFormLabel(formLayout, "Artwork:", 0, 1);
        formLayout.add(artworkComboBox, 1, 1);
        
        addFormLabel(formLayout, "Start Date:", 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        
        addFormLabel(formLayout, "End Date:", 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        
        addFormLabel(formLayout, "Initial Price:", 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        
        addFormLabel(formLayout, "Status:", 0, 5);
        formLayout.add(statusLabel, 1, 5);
        
        formLayout.add(separator, 0, 6, 2, 1);
        
        // Mystery mode section
        VBox mysteryBox = new VBox(5, mysteryModeCheckBox, mysteryDescLabel);
        mysteryBox.setPadding(new Insets(5, 0, 0, 0));
        formLayout.add(mysteryBox, 0, 7, 2, 1);
        
        // Styled buttons
        Button saveButton = new Button("Create Bet Session");
        saveButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " +
                           "-fx-padding: 10 20; -fx-background-radius: 4;");
        saveButton.setOnMouseEntered(e -> saveButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                                                             "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 4;"));
        saveButton.setOnMouseExited(e -> saveButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " + 
                                                            "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 4;"));
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                             "-fx-padding: 10 20; -fx-background-radius: 4;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                                                                "-fx-padding: 10 20; -fx-background-radius: 4;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                                               "-fx-padding: 10 20; -fx-background-radius: 4;"));
        
        HBox buttonLayout = new HBox(15, saveButton, cancelButton);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setPadding(new Insets(20, 10, 10, 10));
        // Add space at the bottom of the layout
        VBox spacer = new VBox();
        spacer.setMinHeight(20); // 20 pixels of bottom space
        buttonLayout.setPadding(new Insets(20, 10, 20, 10)); // Increase bottom padding from 10 to 20
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
        
        // Create scene with improved styling
        VBox root = new VBox(formLayout, buttonLayout);
        root.setStyle("-fx-background-color: #f9f9f9;");
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // Helper method to add consistently styled form labels
    private void addFormLabel(GridPane grid, String text, int col, int row) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
        grid.add(label, col, row);
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
     */    private void setupActionColumns() {
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
            // Setup active bets action column (Tab 2 - My Active Bet Sessions)
        activeActionsColumn.setCellFactory(col -> new javafx.scene.control.TableCell<BetSession, Void>() {
            private final javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View");
            private final javafx.scene.control.Button editButton = new javafx.scene.control.Button("Edit");
            private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button("Delete");
            private final javafx.scene.layout.HBox buttonsBox = new javafx.scene.layout.HBox(5, viewButton, editButton, deleteButton);
            
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
                
                // Configure delete button with red styling
                deleteButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                deleteButton.setOnAction(event -> {
                    BetSession session = getTableView().getItems().get(getIndex());
                    handleDeleteBetSession(session);
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
                    
                    // Only allow editing and deleting pending sessions
                    boolean isPending = "pending".equalsIgnoreCase(session.getStatus());
                    editButton.setDisable(!isPending);
                    deleteButton.setDisable(!isPending);
                    deleteButton.setVisible(isPending); // Only show delete button for pending sessions
                    
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
    }    private boolean canUserPlaceBid(User user, double bidAmount) {
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
      /**
     * Handles deleting a bet session
     * @param betSession The bet session to delete
     */
    private void handleDeleteBetSession(BetSession betSession) {
        if (betSession == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", 
                      "Please select a bet session to delete.");
            return;
        }
        
        // Check if bet session is in 'pending' status
        if (!"pending".equals(betSession.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Cannot Delete",
                      "Only pending bet sessions can be deleted.");
            return;
        }
        
        // Confirm deletion with the user
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Bet Session");
        confirmAlert.setContentText("Are you sure you want to delete this bet session?\nThis action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    // Delete the bet session
                    betSessionService.deleteBetSession(betSession.getId());
                    
                    // Refresh the table
                    loadAllData();
                    
                    // Show success message
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                              "Bet session deleted successfully.");
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Database Error", 
                              "Error deleting bet session: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * Calculates and formats a countdown to a future date
     * @param endDateTime The future date to count down to
     * @return A formatted string representing the countdown (e.g., "2d 5h 30m")
     */
    private String formatCountdown(LocalDateTime endDateTime) {
        if (endDateTime == null) return "N/A";
        
        LocalDateTime now = LocalDateTime.now();
        
        // If the end time is in the past, return "Ended"
        if (endDateTime.isBefore(now)) {
            return "Ended";
        }
        
        // Calculate the time between now and the end time
        long daysBetween = java.time.Duration.between(now, endDateTime).toDays();
        long hoursBetween = java.time.Duration.between(now, endDateTime).toHours() % 24;
        long minutesBetween = java.time.Duration.between(now, endDateTime).toMinutes() % 60;
        
        // Format the countdown differently based on remaining time
        if (daysBetween > 0) {
            // More than a day left, show days and hours
            return String.format("%dd %dh", daysBetween, hoursBetween);
        } else if (hoursBetween > 0) {
            // Less than a day but more than an hour, show hours and minutes
            return String.format("%dh %dm", hoursBetween, minutesBetween);
        } else if (minutesBetween > 0) {
            // Less than an hour, show only minutes
            return String.format("%dm", minutesBetween);
        } else {
            // Less than a minute left
            return "< 1m";
        }
    }
    
    /**
     * Stops the countdown timer when the controller is being destroyed
     * This should be called when closing the application or switching scenes
     */
    public void shutdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }
}
