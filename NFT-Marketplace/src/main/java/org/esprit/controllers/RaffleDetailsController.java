package org.esprit.controllers;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Optional;

import org.esprit.models.Artwork;
import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.*;
import org.esprit.utils.PdfGenerator;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import java.awt.Desktop;

public class RaffleDetailsController {
    @FXML private ImageView artworkImageView;
    @FXML private Label titleLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label creatorLabel;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private Label statusLabel;
    @FXML private Label artworkIdLabel;
    @FXML private Label winnerLabel;
    @FXML private ListView<String> participantsListView;
    @FXML private Button participateButton;
    @FXML private Button manageButton;
    @FXML private Button deleteButton;
    @FXML private Button downloadPdfButton;

    private Raffle raffle;
    private User currentUser;
    private RaffleService raffleService;
    private ArtworkService artworkService;
    private ParticipantService participantService;
    private UserService userService;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private RaffleListController parentController;
    private javafx.animation.Timeline autoRefreshTimeline;
    private String currentCaptchaText;

    public void initialize() {
        raffleService = new RaffleService();
        artworkService = new ArtworkService();
        participantService = new ParticipantService();
        userService = new UserService();
        
        // Setup auto-refresh timeline
        autoRefreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(2),
                event -> refreshRaffle()
            )
        );
        autoRefreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
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
                    Platform.runLater(() -> {
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
                Platform.runLater(this::loadRaffleDetails);
                
                // Refresh parent view if needed
                if (parentController != null) {
                    Platform.runLater(() -> parentController.refreshRaffles());
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
        creatorLabel.setText(raffle.getCreator() != null ? raffle.getCreator().getName() : "Unknown");
        
        // Format dates with null checks
        Date startDate = raffle.getStartTime();
        startDateLabel.setText(startDate != null ? dateFormat.format(startDate) : "Not set");
        
        Date endDate = raffle.getEndTime();
        endDateLabel.setText(endDate != null ? dateFormat.format(endDate) : "Not set");
        
        statusLabel.setText(raffle.getStatus());

        // Clear and populate participants list
        participantsListView.getItems().clear();
        for (User participant : raffle.getParticipants()) {
            participantsListView.getItems().add(participant.getName());
        }

        // Handle artwork display
        try {
            artworkIdLabel.setText("Artwork ID: " + raffle.getArtworkId());
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            if (artwork != null) {
                loadArtworkImage(artwork);
            }
        } catch (Exception e) {
            System.err.println("Error loading artwork details: " + e.getMessage());
        }

        // Handle winner display
        if (raffle.getStatus().equals("ended")) {
            if (raffle.getWinnerId() != null) {
                try {
                    User winner = userService.getOne(raffle.getWinnerId());
                    winnerLabel.setText("Winner: " + winner.getName());
                } catch (Exception e) {
                    winnerLabel.setText("Winner ID: " + raffle.getWinnerId());
                }
            } else if (raffle.getParticipants().isEmpty()) {
                winnerLabel.setText("No winner (no participants)");
            }
            winnerLabel.setVisible(true);
        } else {
            winnerLabel.setVisible(false);
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
        // Show/hide buttons based on permissions
        boolean isCreator = currentUser != null && raffle.getCreator() != null && 
                           currentUser.getId() == raffle.getCreator().getId();
        boolean isActive = raffle.getStatus().equals("active");
        
        // Manage & Delete only for creator
        manageButton.setVisible(isCreator);
        deleteButton.setVisible(isCreator);
        
        // Participate only for non-creators and active raffles
        if (isActive && currentUser != null && !isCreator) {
            // Check if user has already participated
            boolean alreadyParticipated = raffle.getParticipants().stream()
                .anyMatch(p -> p.getId() == currentUser.getId());
            
            participateButton.setVisible(!alreadyParticipated);
            participateButton.setText(alreadyParticipated ? "Already Joined" : "Participate");
            participateButton.setDisable(alreadyParticipated);
        } else {
            participateButton.setVisible(false);
        }
        
        // Download PDF button is always visible for all users
        downloadPdfButton.setVisible(true);
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

            // Create and show the CAPTCHA dialog
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Verify Participation");
            dialog.setHeaderText("Please enter the text shown below");
            
            // Generate CAPTCHA
            currentCaptchaText = CaptchaService.generateCaptchaText();
            Canvas captchaCanvas = CaptchaService.generateCaptchaImage(currentCaptchaText);
            
            // Create dialog content
            VBox content = new VBox(10);
            content.setStyle("-fx-padding: 10; -fx-background-color: #1a1a1a;");
            
            // Add canvas with some styling
            captchaCanvas.setStyle("-fx-effect: dropshadow(gaussian, #00ffff, 10, 0, 0, 0);");
            
            // Add text field for input
            TextField captchaInput = new TextField();
            captchaInput.setPromptText("Enter the text shown above");
            captchaInput.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-prompt-text-fill: #888888;");
            
            content.getChildren().addAll(captchaCanvas, captchaInput);
            
            // Set the content
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(content);
            dialogPane.setStyle("-fx-background-color: #1a1a1a;");
            
            // Add buttons
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            okButton.setStyle("-fx-background-color: #00bfff; -fx-text-fill: white;");
            
            // Handle the dialog result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    try {
                        String userInput = captchaInput.getText().trim().toUpperCase();
                        if (userInput.isEmpty()) {
                            showError("Please enter the CAPTCHA text");
                            return false;
                        }

                        if (!userInput.equals(currentCaptchaText)) {
                            showError("Incorrect CAPTCHA text. Please try again.");
                            return false;
                        }

                        // Create new participant
                        Participant participant = new Participant(raffle, currentUser);
                        participantService.add(participant);

                        // Refresh raffle details
                        this.raffle = raffleService.getOne(raffle.getId());
                        loadRaffleDetails();
                        
                        // Show success message
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText(null);
                        alert.setContentText("You have successfully joined the raffle!");
                        alert.showAndWait();
                        
                        // Refresh parent view if needed
                        if (parentController != null) {
                            parentController.refreshRaffles();
                        }
                        return true;
                    } catch (Exception e) {
                        showError("Error joining raffle: " + e.getMessage());
                        return false;
                    }
                }
                return false;
            });

            // Show the dialog
            dialog.showAndWait();

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
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

    public void setParentController(Object controller) {
        if (controller instanceof RaffleListController) {
            this.parentController = (RaffleListController) controller;
        } else if (controller instanceof RaffleManagementController) {
            // When parent is RaffleManagementController, we'll refresh that instead
            RaffleManagementController managementController = (RaffleManagementController) controller;
            this.parentController = new RaffleListController() {
                @Override
                public void refreshRaffles() {
                    managementController.loadRaffles();
                }
            };
        }
    }

    public void setRaffle(Raffle raffle) {
        this.raffle = raffle;
        if (raffle != null) {
            loadRaffleDetails();
            autoRefreshTimeline.play();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (raffle != null) {
            updateButtons();
        }
    }

    /**
     * Handles the download PDF button action.
     * Generates a PDF report for the current raffle and opens it.
     */
    @FXML
    private void handleDownloadPdf() {
        try {
            Artwork artwork = null;
            try {
                artwork = artworkService.getOne(raffle.getArtworkId());
            } catch (Exception e) {
                System.err.println("Error loading artwork for PDF: " + e.getMessage());
            }
            
            // Generate the PDF
            String pdfPath = PdfGenerator.generateRaffleReport(raffle, artwork, currentUser);
            
            // Try to open the PDF with the default system viewer
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(pdfFile);
                    
                    // Show success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("PDF Generated");
                    alert.setHeaderText(null);
                    alert.setContentText("PDF report has been generated and opened. You can save it to your preferred location.");
                    alert.showAndWait();
                } catch (Exception e) {
                    System.err.println("Error opening PDF: " + e.getMessage());
                    
                    // If we can't open it automatically, tell the user where it is
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("PDF Generated");
                    alert.setHeaderText(null);
                    alert.setContentText("PDF report has been generated at: " + pdfPath + "\n\nPlease open it manually to view and save it.");
                    alert.showAndWait();
                }
            } else {
                // If Desktop is not supported, just show the file location
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("PDF Generated");
                alert.setHeaderText(null);
                alert.setContentText("PDF report has been generated at: " + pdfPath);
                alert.showAndWait();
            }
        } catch (Exception e) {
            showError("Error generating PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}