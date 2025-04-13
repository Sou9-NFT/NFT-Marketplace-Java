package org.esprit.controllers;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.io.IOException;

import org.esprit.models.Artwork;
import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.ParticipantService;
import org.esprit.services.RaffleService;
import org.esprit.services.UserService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.util.Optional;

public class RaffleDetailsController {
    @FXML
    private ImageView artworkImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label creatorLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label artworkIdLabel;
    @FXML
    private Label winnerLabel;
    @FXML
    private ListView<String> participantsListView;
    @FXML
    private Button participateButton;
    @FXML
    private Button manageButton;
    @FXML
    private Button deleteButton;

    private Raffle raffle;
    private User currentUser;
    private RaffleService raffleService;
    private ArtworkService artworkService;
    private ParticipantService participantService;
    private UserService userService;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private RaffleListController parentController;
    private javafx.animation.Timeline autoRefreshTimeline;

    public void initialize() {
        raffleService = new RaffleService();
        artworkService = new ArtworkService();
        participantService = new ParticipantService();
        userService = new UserService();
        
        // Setup auto-refresh timeline with shorter interval for more responsive updates
        autoRefreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(2), // Refresh every 2 seconds
                event -> refreshRaffle()
            )
        );
        autoRefreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
    }

    public void setRaffle(Raffle raffle) {
        this.raffle = raffle;
        loadRaffleDetails();
        autoRefreshTimeline.play(); // Start auto-refresh
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Update the UI if needed based on the current user
        if (currentUser != null && raffle != null) {
            refreshRaffle();
        }
    }

    public void setParentController(RaffleListController controller) {
        this.parentController = controller;
    }

    private void refreshRaffle() {
        try {
            // Get the latest raffle state
            Raffle updatedRaffle = raffleService.getOne(raffle.getId());
            
            if (updatedRaffle == null) {
                // Raffle was deleted
                autoRefreshTimeline.stop();
                handleClose();
                return;
            }

            // Check if any state changed
            boolean stateChanged = !updatedRaffle.getStatus().equals(raffle.getStatus()) ||
                (updatedRaffle.getWinnerId() != null && !updatedRaffle.getWinnerId().equals(raffle.getWinnerId())) ||
                (updatedRaffle.getWinnerId() == null && raffle.getWinnerId() != null) ||
                updatedRaffle.getParticipants().size() != raffle.getParticipants().size();
            
            if (stateChanged) {
                // Show notification if raffle just ended
                if (updatedRaffle.getStatus().equals("ended") && !raffle.getStatus().equals("ended")) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Raffle Ended");
                        alert.setHeaderText(null);
                        if (updatedRaffle.getWinnerId() != null) {
                            try {
                                User winner = userService.getOne(updatedRaffle.getWinnerId());
                                alert.setContentText("The raffle has ended! Winner: " + winner.getName());
                            } catch (Exception e) {
                                alert.setContentText("The raffle has ended! A winner has been selected.");
                            }
                        } else {
                            alert.setContentText("The raffle has ended without participants.");
                        }
                        alert.show();
                    });
                }
                
                // Update the UI with new raffle state
                this.raffle = updatedRaffle;
                javafx.application.Platform.runLater(this::loadRaffleDetails);
                
                // Refresh parent view if needed
                if (parentController != null) {
                    javafx.application.Platform.runLater(() -> parentController.refreshRaffles());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Don't stop refresh timeline on transient errors
        } catch (Exception e) {
            e.printStackTrace();
            autoRefreshTimeline.stop(); // Stop on critical errors
        }
    }

    private void loadRaffleDetails() {
        titleLabel.setText(raffle.getTitle());
        descriptionArea.setText(raffle.getRaffleDescription());
        creatorLabel.setText(raffle.getCreatorName());
        startDateLabel.setText(dateFormat.format(raffle.getStartTime()));
        endDateLabel.setText(dateFormat.format(raffle.getEndTime()));
        statusLabel.setText(raffle.getStatus());
        artworkIdLabel.setText(String.valueOf(raffle.getArtworkId()));

        // Load artwork image
        try {
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            loadArtworkImage(artwork);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load participants and winner
        participantsListView.getItems().clear();
        
        // Show winner prominently if raffle is ended
        if (raffle.getStatus().equals("ended")) {
            if (raffle.getWinnerId() != null) {
                try {
                    User winner = userService.getOne(raffle.getWinnerId());
                    if (winner != null) {
                        // Add winner at the top with trophy emoji
                        participantsListView.getItems().add("🏆 WINNER: " + winner.getName() + " 🏆");
                        winnerLabel.setText("Winner: " + winner.getName());
                        winnerLabel.getStyleClass().add("winner-label");
                        winnerLabel.setVisible(true);
                        
                        // Add a separator
                        participantsListView.getItems().add("------------------------");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                winnerLabel.setText("No winner selected yet");
                winnerLabel.setVisible(true);
            }
        } else {
            winnerLabel.setVisible(false);
        }
        
        // Add all participants
        for (User participant : raffle.getParticipants()) {
            if (raffle.getWinnerId() != null && participant.getId() == raffle.getWinnerId()) {
                continue; // Skip winner as they're already shown at the top
            }
            participantsListView.getItems().add(participant.getName());
        }

        // Update buttons if currentUser is set
        if (currentUser != null) {
            updateButtons();
        }
    }

    private void loadArtworkImage(Artwork artwork) {
        if (artwork != null && artwork.getImageName() != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/uploads/" + artwork.getImageName()));
                artworkImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading artwork image " + artwork.getImageName() + ": " + e.getMessage());
            }
        }
    }

    private void updateButtons() {
        if (currentUser != null) {
            boolean isCreator = raffle.getCreator().getId() == currentUser.getId();
            boolean isParticipant = raffle.getParticipants().stream()
                .anyMatch(p -> p.getId() == currentUser.getId());
            boolean isActive = raffle.getStatus().equals("active");
            
            // Update participate button
            participateButton.setDisable(isCreator || isParticipant || !isActive);
            String buttonText = isParticipant ? "Already Participating" : 
                              !isActive ? "Raffle " + raffle.getStatus() :
                              "Participate";
            participateButton.setText(buttonText);
            
            // Show/hide manage and delete buttons based on creator status
            manageButton.setVisible(isCreator);
            deleteButton.setVisible(isCreator);
        }
    }

    @FXML
    private void handleParticipate() {
        try {
            // Verify raffle is still active
            Raffle updatedRaffle = raffleService.getOne(raffle.getId());
            if (!updatedRaffle.getStatus().equals("active")) {
                showError("This raffle is no longer active");
                this.raffle = updatedRaffle;
                loadRaffleDetails();
                return;
            }

            // Create new participant
            Participant participant = new Participant(raffle, currentUser);
            participantService.add(participant);

            // Refresh raffle details
            this.raffle = raffleService.getOne(raffle.getId());
            loadRaffleDetails();
            
            // Refresh parent view if needed
            if (parentController != null) {
                parentController.refreshRaffles();
            }
        } catch (Exception e) {
            showError("Error joining raffle: " + e.getMessage());
        }
    }

    @FXML
    private void handleManage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageRaffle.fxml"));
            Parent manageView = loader.load();
            
            ManageRaffleController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setParentController(parentController);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(manageView));
            stage.setTitle("Manage Raffle - " + raffle.getTitle());
            stage.show();
            
            // Close the details window
            ((Stage) titleLabel.getScene().getWindow()).close();
            
        } catch (IOException e) {
            showError("Could not open manage raffle view: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
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
                
                // Close the details window
                ((Stage) titleLabel.getScene().getWindow()).close();
                
            } catch (SQLException e) {
                showError("Error deleting raffle: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClose() {
        autoRefreshTimeline.stop(); // Stop auto-refresh when closing
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}