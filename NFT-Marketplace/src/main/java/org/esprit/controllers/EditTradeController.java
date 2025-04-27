package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.esprit.models.TradeOffer;
import org.esprit.models.Artwork;
import org.esprit.services.ArtworkService;
import org.esprit.services.TradeOfferService;

import java.net.URL;
import java.util.ResourceBundle;

public class EditTradeController implements Initializable {
    
    @FXML
    private ComboBox<Artwork> offeredItemComboBox;
    
    @FXML
    private ComboBox<Artwork> requestedItemComboBox;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private Label statusLabel;
    
    private TradeOffer tradeOffer;
    private TradeOfferService tradeService;
    private ArtworkService artworkService;
    private TradeOfferListController parentController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tradeService = new TradeOfferService();
        artworkService = new ArtworkService();
        
        // Set up cell factories for ComboBoxes to display artwork titles
        offeredItemComboBox.setCellFactory(param -> new ListCell<Artwork>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });
        
        requestedItemComboBox.setCellFactory(param -> new ListCell<Artwork>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });
        
        // Set up button cell factories for ComboBoxes
        offeredItemComboBox.setButtonCell(new ListCell<Artwork>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });
        
        requestedItemComboBox.setButtonCell(new ListCell<Artwork>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });
    }
    
    public void setTradeOffer(TradeOffer tradeOffer) {
        this.tradeOffer = tradeOffer;
        populateFields();
    }
    
    public void setParentController(TradeOfferListController controller) {
        this.parentController = controller;
    }
    
    private void populateFields() {
        if (tradeOffer != null) {
            try {
                // Load user's artworks for offered artwork combo
                ObservableList<Artwork> userArtworks = FXCollections.observableArrayList(
                    artworkService.getByOwner(tradeOffer.getSender().getId())
                );
                offeredItemComboBox.setItems(userArtworks);
                offeredItemComboBox.setValue(tradeOffer.getOfferedItem());
                
                // Load receiver's artworks for requested artwork combo
                ObservableList<Artwork> receiverArtworks = FXCollections.observableArrayList(
                    artworkService.getByOwner(tradeOffer.getReceiverName().getId())
                );
                requestedItemComboBox.setItems(receiverArtworks);
                requestedItemComboBox.setValue(tradeOffer.getReceivedItem());
                
                // Set description
                descriptionField.setText(tradeOffer.getDescription());
                
            } catch (Exception e) {
                showError("Error loading trade offer data: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        if (tradeOffer == null) {
            showError("No trade offer selected");
            return;
        }
        
        try {
            // Update trade offer with new values
            tradeOffer.setOfferedItem(offeredItemComboBox.getValue());
            tradeOffer.setReceivedItem(requestedItemComboBox.getValue());
            tradeOffer.setDescription(descriptionField.getText());
            
            // Save changes
            tradeService.update(tradeOffer);
            
            // Refresh parent view
            if (parentController != null) {
                parentController.refreshTrades();
            }
            
            // Show success message
            showSuccess("Trade offer updated successfully!");
            
            // Close the window after a short delay to show the success message
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            ((Stage) descriptionField.getScene().getWindow()).close();
                        });
                    }
                },
                1000 // 1 second delay
            );
            
        } catch (Exception e) {
            showError("Error updating trade offer: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) descriptionField.getScene().getWindow()).close();
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setVisible(true);
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setVisible(true);
    }
}
