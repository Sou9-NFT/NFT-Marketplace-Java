package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.esprit.models.Artwork;
import org.esprit.models.TradeOffer;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.TradeOfferService;
import org.esprit.services.UserService;

import java.time.LocalDateTime;

public class CreateTradeController {
    
    @FXML
    private TextField offeredArtworkField;
    
    @FXML
    private TextField requestedArtworkField;
    
    @FXML
    private TextField receiverField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private TradeOfferService tradeService;
    private ArtworkService artworkService;
    private UserService userService;
    private TradeOfferListController parentController;
    
    public void initialize() {
        tradeService = new TradeOfferService();
        artworkService = new ArtworkService();
        userService = new UserService();
    }
    
    public void setUser(User user) {
        this.currentUser = user;
    }
    
    public void setParentController(TradeOfferListController controller) {
        this.parentController = controller;
    }
    
    @FXML
    private void handleCreateTrade(ActionEvent event) {
        String offeredArtworkIdStr = offeredArtworkField.getText().trim();
        String requestedArtworkIdStr = requestedArtworkField.getText().trim();
        String receiverUsername = receiverField.getText().trim();
        String description = descriptionField.getText().trim();
        
        // Validate inputs
        if (offeredArtworkIdStr.isEmpty() || requestedArtworkIdStr.isEmpty() || receiverUsername.isEmpty()) {
            showStatus("Please fill in all required fields", true);
            return;
        }
        
        try {
            // Parse artwork IDs
            int offeredArtworkId = Integer.parseInt(offeredArtworkIdStr);
            int requestedArtworkId = Integer.parseInt(requestedArtworkIdStr);
            
            // Get artwork objects
            Artwork offeredArtwork = artworkService.getOne(offeredArtworkId);
            Artwork requestedArtwork = artworkService.getOne(requestedArtworkId);
            
            if (offeredArtwork == null || requestedArtwork == null) {
                showStatus("One or both artworks not found", true);
                return;
            }
            
            // Verify artwork ownership
            if (offeredArtwork.getOwnerId() != currentUser.getId()) {
                showStatus("You can only offer artworks you own", true);
                return;
            }
            
            // Get receiver user
            User receiver = userService.findByUsername(receiverUsername);
            if (receiver == null) {
                showStatus("Receiver user not found", true);
                return;
            }
            
            // Verify receiver owns the requested artwork
            if (requestedArtwork.getOwnerId() != receiver.getId()) {
                showStatus("The requested artwork is not owned by the receiver", true);
                return;
            }
            
            // Create new trade offer
            TradeOffer tradeOffer = new TradeOffer();
            tradeOffer.setSender(currentUser);
            tradeOffer.setReceiverName(receiver);
            tradeOffer.setOfferedItem(offeredArtwork);
            tradeOffer.setReceivedItem(requestedArtwork);
            tradeOffer.setDescription(description);
            tradeOffer.setCreationDate(LocalDateTime.now());
            tradeOffer.setStatus("pending");
            
            // Save trade offer
            tradeService.add(tradeOffer);
            
            // Show success and close dialog
            showStatus("Trade offer created successfully!", false);
            
            // Refresh parent view if available
            if (parentController != null) {
                parentController.refreshTrades();
            }
            
            // Close the dialog
            ((Stage) offeredArtworkField.getScene().getWindow()).close();
            
        } catch (NumberFormatException e) {
            showStatus("Please enter valid artwork ID numbers", true);
        } catch (Exception e) {
            showStatus("Error creating trade offer: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) offeredArtworkField.getScene().getWindow()).close();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}