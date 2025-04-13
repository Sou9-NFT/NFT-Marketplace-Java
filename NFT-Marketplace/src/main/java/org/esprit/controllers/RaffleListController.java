package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.RaffleService;
import org.esprit.services.ArtworkService;
import org.esprit.models.Artwork;
import org.esprit.models.Participant;
import org.esprit.services.ParticipantService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RaffleListController {
    @FXML
    private GridPane rafflesGrid;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilter;
    
    @FXML
    private Label statusLabel;
    
    private RaffleService raffleService;
    private ParticipantService participantService;
    private User currentUser;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private ObservableList<Raffle> raffles = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;
    private Timer statusCheckTimer;
    
    public void initialize() {
        raffleService = new RaffleService();
        participantService = new ParticipantService();
        
        // Setup status filter
        statusFilter.setItems(FXCollections.observableArrayList(
            "All",
            "Active",
            "Completed",
            "Cancelled",
            "Ended"
        ));
        statusFilter.setValue("All");
        
        // Add listeners
        searchField.textProperty().addListener((obs, old, newValue) -> filterRaffles());
        statusFilter.valueProperty().addListener((obs, old, newValue) -> filterRaffles());
        
        // Setup auto-refresh timer (check every 30 seconds)
        setupAutoRefresh();
        setupStatusCheckTimer();
    }
    
    private void setupAutoRefresh() {
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> {
                try {
                    raffleService.updateExpiredRaffles();
                    loadRaffles();
                } catch (SQLException ex) {
                    System.err.println("Error refreshing raffles: " + ex.getMessage());
                }
            })
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void setupStatusCheckTimer() {
        // Cancel any existing timer
        if (statusCheckTimer != null) {
            statusCheckTimer.cancel();
        }

        // Create new timer
        statusCheckTimer = new Timer(true); // Run as daemon thread
        
        // Schedule status check every minute
        statusCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Update expired raffles
                    raffleService.updateExpiredRaffles();
                    
                    // Refresh the list on the JavaFX Application Thread
                    Platform.runLater(() -> refreshRaffles());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 60000); // Check every minute
    }
    
    // Cleanup method to stop the timer when the window is closed
    public void cleanup() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        if (statusCheckTimer != null) {
            statusCheckTimer.cancel();
            statusCheckTimer = null;
        }
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        loadRaffles();
    }
    
    private void loadRaffles() {
        try {
            raffles.clear();
            List<Raffle> allRaffles = raffleService.getAll();
            raffles.addAll(allRaffles);
            displayRaffles(raffles);
        } catch (SQLException e) {
            showStatus("Error loading raffles: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void filterRaffles() {
        String searchText = searchField.getText().toLowerCase();
        String statusText = statusFilter.getValue();
        
        ObservableList<Raffle> filtered = raffles.filtered(raffle -> {
            boolean matchesSearch = searchText.isEmpty() 
                || raffle.getTitle().toLowerCase().contains(searchText)
                || raffle.getRaffleDescription().toLowerCase().contains(searchText);
                
            boolean matchesStatus = statusText.equals("All") 
                || raffle.getStatus().equalsIgnoreCase(statusText);
                
            return matchesSearch && matchesStatus;
        });
        
        displayRaffles(filtered);
    }
    
    private void displayRaffles(List<Raffle> rafflesToDisplay) {
        rafflesGrid.getChildren().clear();
        
        int column = 0;
        int row = 0;
        int maxColumns = 2; // Adjust based on window size
        
        for (Raffle raffle : rafflesToDisplay) {
            VBox card = createRaffleCard(raffle);
            rafflesGrid.add(card, column, row);
            
            column++;
            if (column >= maxColumns) {
                column = 0;
                row++;
            }
        }
    }
    
    private VBox createRaffleCard(Raffle raffle) {
        VBox card = new VBox(10);
        card.getStyleClass().add("raffle-card");
        card.setPrefWidth(300);

        // Add artwork image
        try {
            ArtworkService artworkService = new ArtworkService();
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            if (artwork != null && artwork.getImageName() != null) {
                ImageView imageView = new ImageView();
                try {
                    Image image = new Image(getClass().getResourceAsStream("/uploads/" + artwork.getImageName()));
                    imageView.setImage(image);
                    imageView.setFitWidth(280);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                } catch (Exception e) {
                    System.err.println("Error loading image " + artwork.getImageName() + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading artwork: " + e.getMessage());
        }
        
        Label titleLabel = new Label(raffle.getTitle());
        titleLabel.getStyleClass().add("raffle-title");
        
        Label descLabel = new Label(raffle.getRaffleDescription());
        descLabel.setWrapText(true);
        
        Label creatorLabel = new Label("Created by: " + raffle.getCreatorName());
        Label statusLabel = new Label("Status: " + raffle.getStatus());
        Label dateLabel = new Label("Ends: " + dateFormat.format(raffle.getEndTime()));
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button viewButton = new Button("View");
        viewButton.setOnAction(e -> handleViewRaffle(raffle));
        viewButton.getStyleClass().add("secondary-button");
        
        Button actionButton = new Button();
        actionButton.setMaxWidth(Double.MAX_VALUE);
        
        if (raffle.getCreator().getId() == currentUser.getId()) {
            actionButton.setText("Manage Raffle");
            actionButton.setOnAction(e -> handleManageRaffle(raffle));
            
            // Add delete button for creator only
            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().addAll("danger-button");
            deleteButton.setOnAction(e -> handleDeleteRaffle(raffle));
            buttonBox.getChildren().addAll(viewButton, actionButton, deleteButton);
        } else {
            actionButton.setText("Participate");
            if (raffle.getParticipants().contains(currentUser)) {
                actionButton.setDisable(true);
                actionButton.setText("Already Participating");
            } else {
                actionButton.setOnAction(e -> handleParticipate(raffle));
            }
            buttonBox.getChildren().addAll(viewButton, actionButton);
        }
        
        card.getChildren().addAll(
            titleLabel,
            descLabel,
            new Separator(),
            creatorLabel,
            statusLabel,
            dateLabel,
            buttonBox
        );
        
        return card;
    }

    @FXML
    private void handleCreateRaffle(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateRaffle.fxml"));
            Parent createRaffleView = loader.load();
            
            CreateRaffleController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create New Raffle");
            stage.setScene(new Scene(createRaffleView));
            stage.showAndWait();
            
        } catch (IOException e) {
            showStatus("Error opening create raffle form: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void handleParticipate(Raffle raffle) {
        try {
            // Create new participant
            Participant participant = new Participant(raffle, currentUser);
            participant.setName(currentUser.getName()); // Set participant name from user's name
            
            // Save to database using ParticipantService
            participantService.add(participant);
            
            // Add to raffle's in-memory list
            raffle.addParticipant(currentUser);
            
            showStatus("Successfully joined the raffle!", false);
            loadRaffles(); // Refresh the display
        } catch (Exception e) {
            showStatus("Error joining raffle: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void handleManageRaffle(Raffle raffle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageRaffle.fxml"));
            Parent manageRaffleView = loader.load();
            
            ManageRaffleController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Manage Raffle - " + raffle.getTitle());
            stage.setScene(new Scene(manageRaffleView));
            stage.showAndWait();
            
        } catch (IOException e) {
            showStatus("Error opening raffle management: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void handleViewRaffle(Raffle raffle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleDetails.fxml"));
            Parent raffleDetailsView = loader.load();
            
            RaffleDetailsController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Raffle Details - " + raffle.getTitle());
            stage.setScene(new Scene(raffleDetailsView));
            stage.showAndWait();
            
        } catch (IOException e) {
            showStatus("Error opening raffle details: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    public void refreshRaffles() {
        loadRaffles();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
    
    private void handleDeleteRaffle(Raffle raffle) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Raffle");
        confirmation.setHeaderText("Delete Raffle");
        confirmation.setContentText("Are you sure you want to delete this raffle? This action cannot be undone.");
        
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                raffleService.delete(raffle);
                showStatus("Raffle deleted successfully!", false);
                loadRaffles(); // Refresh the view
            } catch (SQLException e) {
                showStatus("Error deleting raffle: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }
}