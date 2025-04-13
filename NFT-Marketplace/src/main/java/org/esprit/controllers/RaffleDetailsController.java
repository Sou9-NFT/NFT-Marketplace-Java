package org.esprit.controllers;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.File;

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

        // Load artwork details
        try {
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            if (artwork != null) {
                artworkIdLabel.setText("Artwork: " + artwork.getTitle());                
                loadArtworkImage(artwork);
                
                // Show ownership information when raffle is ended
                if (raffle.getStatus().equals("ended") && raffle.getWinnerId() != null) {
                    try {
                        User owner = userService.getOne(artwork.getOwnerId());
                        if (owner != null) {
                            String ownerInfo = "Current Owner: " + owner.getName();
                            
                            // Check if the current user is the owner
                            if (currentUser != null && currentUser.getId() == owner.getId()) {
                                ownerInfo += " (You own this artwork!)";
                            }
                            
                            // Append owner info to the artwork label
                            artworkIdLabel.setText(artworkIdLabel.getText() + " - " + ownerInfo);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading artwork owner details: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            artworkIdLabel.setText("Failed to load artwork details");
        }

        // Load participants and winner
        participantsListView.getItems().clear();
        
        // Show winner prominently if raffle is ended
        if (raffle.getStatus().equals("ended")) {
            if (raffle.getWinnerId() != null) {
                try {
                    User winner = userService.getOne(raffle.getWinnerId());
                    String winnerText = "Winner: " + winner.getName();
                    
                    // Highlight if current user is the winner
                    if (currentUser != null && currentUser.getId() == winner.getId()) {
                        winnerText += " (You won this raffle!)";
                    }
                    
                    winnerLabel.setText(winnerText);
                    winnerLabel.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    winnerLabel.setText("Winner: Unknown");
                }
            } else {
                winnerLabel.setText("No winner (no participants)");
            }
            winnerLabel.setVisible(true);
        } else {
            winnerLabel.setVisible(false);
        }

        // Add participants to list
        for (User participant : raffle.getParticipants()) {
            participantsListView.getItems().add(participant.getName());
        }

        updateButtons();
    }

    private void loadArtworkImage(Artwork artwork) {
        boolean imageLoaded = false;

        if (artwork != null && artwork.getImageName() != null) {
            try {
                // Try multiple approaches to load the image
                
                // 1. Try absolute path with src/main/resources
                File imageFile = new File("src/main/resources/uploads/" + artwork.getImageName());
                if (imageFile.exists()) {
                    try {
                        Image image = new Image(imageFile.toURI().toString());
                        if (!image.isError()) {
                            artworkImageView.setImage(image);
                            artworkImageView.setFitWidth(250);
                            artworkImageView.setFitHeight(150);
                            artworkImageView.setPreserveRatio(true);
                            imageLoaded = true;
                            System.out.println("Details: Loaded image from src/main/resources/uploads: " + artwork.getImageName());
                        }
                    } catch (Exception e) {
                        System.err.println("Details: Failed to load image from src/main/resources path: " + e.getMessage());
                    }
                }
                
                // 2. Try using the class resource loader
                if (!imageLoaded) {
                    try {
                        String imagePath = "/uploads/" + artwork.getImageName();
                        java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                        
                        if (is != null) {
                            try {
                                Image image = new Image(is);
                                if (!image.isError()) {
                                    artworkImageView.setImage(image);
                                    artworkImageView.setFitWidth(250);
                                    artworkImageView.setFitHeight(150);
                                    artworkImageView.setPreserveRatio(true);
                                    imageLoaded = true;
                                    System.out.println("Details: Loaded image from resource stream: " + artwork.getImageName());
                                }
                            } catch (Exception e) {
                                System.err.println("Details: Failed to load image from resource stream: " + e.getMessage());
                            }
                        } else {
                            System.out.println("Details: Resource stream is null for: " + imagePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Details: Failed to create resource stream: " + e.getMessage());
                    }
                }
                
                // 3. Try absolute path with direct project path
                if (!imageLoaded) {
                    try {
                        File projectRoot = new File("").getAbsoluteFile();
                        File uploadsDir = new File(projectRoot, "uploads");
                        File directImageFile = new File(uploadsDir, artwork.getImageName());
                        
                        if (directImageFile.exists()) {
                            Image image = new Image(directImageFile.toURI().toString());
                            if (!image.isError()) {
                                artworkImageView.setImage(image);
                                artworkImageView.setFitWidth(250);
                                artworkImageView.setFitHeight(150);
                                artworkImageView.setPreserveRatio(true);
                                imageLoaded = true;
                                System.out.println("Details: Loaded image from direct project path: " + directImageFile.getPath());
                            }
                        } else {
                            System.out.println("Details: Image file not found at path: " + directImageFile.getPath());
                        }
                    } catch (Exception e) {
                        System.err.println("Details: Failed to load image from direct project path: " + e.getMessage());
                    }
                }
                
                // 4. Try using the file class loader
                if (!imageLoaded) {
                    try {
                        ClassLoader classLoader = getClass().getClassLoader();
                        java.net.URL imageUrl = classLoader.getResource("uploads/" + artwork.getImageName());
                        if (imageUrl != null) {
                            Image image = new Image(imageUrl.toString());
                            if (!image.isError()) {
                                artworkImageView.setImage(image);
                                artworkImageView.setFitWidth(250);
                                artworkImageView.setFitHeight(150);
                                artworkImageView.setPreserveRatio(true);
                                imageLoaded = true;
                                System.out.println("Details: Loaded image using class loader: " + imageUrl);
                            }
                        } else {
                            System.out.println("Details: Image URL not found using class loader");
                        }
                    } catch (Exception e) {
                        System.err.println("Details: Failed to load image using class loader: " + e.getMessage());
                    }
                }
                
                // 5. Fallback: Try to find any image in uploads directory to use as a fallback
                if (!imageLoaded) {
                    try {
                        File uploadsDir = new File("src/main/resources/uploads");
                        if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                            // List available files for debugging
                            System.out.println("Details: Available files in uploads directory:");
                            File[] allFiles = uploadsDir.listFiles();
                            if (allFiles != null) {
                                for (File file : allFiles) {
                                    System.out.println("  - " + file.getName());
                                }
                            }
                            
                            // Find image files
                            File[] imageFiles = uploadsDir.listFiles((dir, name) -> 
                                name.toLowerCase().endsWith(".png") || 
                                name.toLowerCase().endsWith(".jpg") || 
                                name.toLowerCase().endsWith(".jpeg")
                            );
                            
                            if (imageFiles != null && imageFiles.length > 0) {
                                // Use the first image found as a fallback
                                File fallbackFile = imageFiles[0];
                                System.out.println("Details: Using fallback image: " + fallbackFile.getName());
                                
                                try {
                                    Image image = new Image(fallbackFile.toURI().toString());
                                    if (!image.isError()) {
                                        artworkImageView.setImage(image);
                                        artworkImageView.setFitWidth(250);
                                        artworkImageView.setFitHeight(150);
                                        artworkImageView.setPreserveRatio(true);
                                        imageLoaded = true;
                                        System.out.println("Details: Loaded fallback image: " + fallbackFile.getName());
                                    }
                                } catch (Exception e) {
                                    System.err.println("Details: Failed to load fallback image: " + e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Details: Error finding fallback image: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Details: Error loading artwork image " + artwork.getImageName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // If image still not loaded, create a visual placeholder
        if (!imageLoaded) {
            // Create a placeholder with a visual indication and text
            javafx.scene.layout.StackPane placeholderPane = new javafx.scene.layout.StackPane();
            placeholderPane.setPrefWidth(250);
            placeholderPane.setPrefHeight(150);
            placeholderPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #BBDEFB, #64B5F6);");
            
            Label placeholderLabel = new Label("Artwork\nImage Not Available");
            placeholderLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #3F51B5; -fx-font-weight: bold;");
            placeholderLabel.setWrapText(true);
            placeholderLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            placeholderPane.getChildren().add(placeholderLabel);
            
            // Get parent container of artworkImageView and replace it with the placeholder
            javafx.scene.layout.VBox parentContainer = (javafx.scene.layout.VBox) artworkImageView.getParent();
            int imageViewIndex = parentContainer.getChildren().indexOf(artworkImageView);
            
            if (imageViewIndex >= 0) {
                // We need to hide the original ImageView but keep it in the scene graph
                // (since other code might reference it)
                artworkImageView.setVisible(false);
                
                // Insert the placeholder at the same index
                if (!parentContainer.getChildren().contains(placeholderPane)) {
                    parentContainer.getChildren().add(imageViewIndex, placeholderPane);
                }
            }
            
            System.out.println("Using placeholder for artwork details view: " + 
                (artwork != null ? artwork.getTitle() : "Unknown"));
        } else {
            // Make sure the imageView is visible in case it was hidden previously
            artworkImageView.setVisible(true);
            
            // Remove any placeholder if it exists
            javafx.scene.layout.VBox parentContainer = (javafx.scene.layout.VBox) artworkImageView.getParent();
            parentContainer.getChildren().removeIf(node -> node instanceof javafx.scene.layout.StackPane);
        }
    }

    private void updateButtons() {
        if (currentUser != null) {
            // Ensure raffle has a creator
            if (raffle.getCreator() == null) {
                // Create a temporary creator for this raffle
                User tempCreator = new User();
                tempCreator.setId(currentUser.getId()); // Assuming current user might be the creator
                tempCreator.setName("Unknown Creator");
                raffle.setCreator(tempCreator);
            }
            
            // Check if the raffle is active for participate button
            boolean isActive = raffle.getStatus().equals("active");
            boolean isParticipant = raffle.getParticipants().stream()
                .anyMatch(p -> p.getId() == currentUser.getId());
            
            // Update participate button
            participateButton.setDisable(isParticipant || !isActive);
            String buttonText = isParticipant ? "Already Participating" : 
                              !isActive ? "Raffle " + raffle.getStatus() :
                              "Participate";
            participateButton.setText(buttonText);
            
            // Always show all buttons - permission checks are done in handler methods
            participateButton.setVisible(true);
            manageButton.setVisible(true);
            deleteButton.setVisible(true);
            
            participateButton.setManaged(true);
            manageButton.setManaged(true);
            deleteButton.setManaged(true);
        } else {
            // If not logged in, hide all action buttons
            participateButton.setVisible(false);
            manageButton.setVisible(false);
            deleteButton.setVisible(false);
            
            participateButton.setManaged(false);
            manageButton.setManaged(false);
            deleteButton.setManaged(false);
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
        // Check if raffle has a creator - still create one if needed
        if (raffle.getCreator() == null) {
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageRaffle.fxml"));
            Parent manageView = loader.load();
            
            ManageRaffleController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setParentController(parentController);
            controller.setCurrentUser(currentUser); // Pass the current user to the controller
            
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
        // Check if raffle has a creator - still create one if needed
        if (raffle.getCreator() == null) {
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Raffle");
        alert.setHeaderText("Delete Raffle");
        alert.setContentText("Are you sure you want to delete this raffle? This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                raffleService.delete(raffle);
                
                // Show success alert
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Raffle Deleted");
                successAlert.setHeaderText(null);
                successAlert.setContentText("The raffle has been successfully deleted.");
                successAlert.showAndWait();
                
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