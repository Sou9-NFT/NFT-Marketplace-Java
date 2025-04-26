package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.services.TradeDisputeService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;

public class EditTradeDisputeController {
    @FXML
    private TextArea reasonArea;
    
    @FXML
    private ImageView evidenceImageView;
    
    @FXML
    private Button uploadButton;
    
    @FXML
    private Button removeButton;
    
    @FXML
    private Button submitButton;
    
    @FXML
    private Button cancelButton;
    
    private User currentUser;
    private TradeDispute dispute;
    private final TradeDisputeService disputeService = new TradeDisputeService();
    private String evidenceImagePath;
    
    public void initialize() {
        // Initially hide the image view and remove button
        evidenceImageView.setVisible(false);
        removeButton.setVisible(false);
    }
    
    public void setDispute(TradeDispute dispute) {
        this.dispute = dispute;
        updateUI();
    }
    
    public void setUser(User user) {
        this.currentUser = user;
    }
    
    private void updateUI() {
        if (dispute != null) {
            reasonArea.setText(dispute.getReason());
            
            // Display evidence image if available
            if (dispute.getEvidence() != null && !dispute.getEvidence().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(dispute.getEvidence());
                    Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                    evidenceImageView.setImage(image);
                    evidenceImageView.setVisible(true);
                    removeButton.setVisible(true);
                } catch (Exception e) {
                    showAlert("Error", "Could not load evidence image: " + e.getMessage());
                }
            }
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
    
    private String resizeAndConvertImage(File imageFile) throws IOException {
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
    
    @FXML
    private void handleSubmit(ActionEvent event) {
        // Validate inputs
        if (reasonArea.getText().isEmpty()) {
            showAlert("Error", "Please provide a reason for the dispute");
            return;
        }
        
        String evidence = dispute.getEvidence(); // Keep existing evidence by default
        
        // If a new image was uploaded, process it
        if (evidenceImagePath != null) {
            try {
                File imageFile = new File(evidenceImagePath);
                evidence = resizeAndConvertImage(imageFile);
            } catch (Exception e) {
                showAlert("Error", "Error processing image: " + e.getMessage());
                return;
            }
        }
        
        // Update dispute
        try {
            if (disputeService.updateDispute(dispute.getId(), reasonArea.getText(), evidence)) {
                showAlert("Success", "Trade dispute updated successfully");
                
                // Get the parent controller and refresh the table
                Stage stage = (Stage) submitButton.getScene().getWindow();
                TradeDisputeListController parentController = (TradeDisputeListController) stage.getUserData();
                if (parentController != null) {
                    parentController.loadDisputes();
                }
                
                // Close the popup window
                stage.hide();
            } else {
                showAlert("Error", "Failed to update trade dispute");
            }
        } catch (Exception e) {
            showAlert("Error", "Error updating dispute: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        // Close the popup window
        cancelButton.getScene().getWindow().hide();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 