package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.models.TradeOffer;
import org.esprit.services.TradeDisputeService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class CreateTradeDisputeController {
    @FXML private Label tradeIdLabel;
    @FXML private Label offeredItemLabel;
    @FXML private Label receivedItemLabel;
    @FXML private TextArea reasonTextArea;
    @FXML private ImageView evidenceImageView;
    @FXML private Button uploadButton;
    @FXML private Button removeButton;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private User currentUser;
    private TradeOffer tradeOffer;
    private final TradeDisputeService disputeService = new TradeDisputeService();
    private String evidenceImagePath;

    public void initialize() {
        // Initialize UI components
        evidenceImageView.setVisible(false);
        removeButton.setVisible(false);
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setTradeOffer(TradeOffer tradeOffer) {
        this.tradeOffer = tradeOffer;
        updateUI();
    }

    private void updateUI() {
        if (tradeOffer != null) {
            tradeIdLabel.setText(String.valueOf(tradeOffer.getId()));
            offeredItemLabel.setText(tradeOffer.getOfferedItem().getTitle());
            receivedItemLabel.setText(tradeOffer.getReceivedItem().getTitle());
        }
    }

    @FXML
    private void handleUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Evidence Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Load and display the image
                Image image = new Image(selectedFile.toURI().toString());
                evidenceImageView.setImage(image);
                evidenceImageView.setVisible(true);
                removeButton.setVisible(true);
                evidenceImagePath = selectedFile.getAbsolutePath();
            } catch (Exception e) {
                showAlert("Error", "Could not load image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemoveImage(ActionEvent event) {
        evidenceImageView.setImage(null);
        evidenceImageView.setVisible(false);
        removeButton.setVisible(false);
        evidenceImagePath = null;
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (reasonTextArea.getText().trim().isEmpty()) {
            showAlert("Error", "Please provide a reason for the dispute");
            return;
        }

        try {
            TradeDispute dispute = new TradeDispute();
            dispute.setReporter(currentUser.getId());
            dispute.setTradeId(tradeOffer.getId());
            dispute.setOfferedItem(String.valueOf(tradeOffer.getOfferedItem().getId()));
            dispute.setReceivedItem(String.valueOf(tradeOffer.getReceivedItem().getId()));
            dispute.setReason(reasonTextArea.getText().trim());
            dispute.setStatus("pending");
            dispute.setTimestamp(LocalDateTime.now());

            // Process evidence image if one was uploaded
            if (evidenceImagePath != null) {
                try {
                    File imageFile = new File(evidenceImagePath);
                    dispute.setEvidence(resizeAndConvertImage(imageFile));
                } catch (Exception e) {
                    showAlert("Error", "Error processing image: " + e.getMessage());
                    return;
                }
            }

            if (disputeService.createDispute(dispute)) {
                showAlert("Success", "Trade dispute submitted successfully");
                closeWindow();
            } else {
                showAlert("Error", "Failed to submit trade dispute");
            }
        } catch (Exception e) {
            showAlert("Error", "Error creating dispute: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private String resizeAndConvertImage(File imageFile) throws Exception {
        // Read the original image
        BufferedImage originalImage = ImageIO.read(imageFile);
        
        // Calculate new dimensions (maintaining aspect ratio)
        int maxDimension = 800; // Maximum width or height
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            if (originalWidth > originalHeight) {
                newWidth = maxDimension;
                newHeight = (int) ((double) originalHeight / originalWidth * maxDimension);
            } else {
                newHeight = maxDimension;
                newWidth = (int) ((double) originalWidth / originalHeight * maxDimension);
            }
        }
        
        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        // Convert to Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
