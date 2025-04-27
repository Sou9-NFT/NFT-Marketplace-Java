package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
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
    private ComboBox<Artwork> offeredArtworkCombo;
    
    @FXML
    private ComboBox<Artwork> requestedArtworkCombo;
    
    @FXML
    private ComboBox<User> receiverCombo;
    
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
        loadData();
    }
    
    private void loadData() {
        try {
            // Load user's artworks for offered artwork combo
            ObservableList<Artwork> userArtworks = FXCollections.observableArrayList(
                artworkService.getByOwner(currentUser.getId())
            );
            offeredArtworkCombo.setItems(userArtworks);
            
            // Load all users except current user for receiver combo
            ObservableList<User> allUsers = FXCollections.observableArrayList(
                userService.getAll().stream()
                    .filter(user -> user.getId() != currentUser.getId())
                    .toList()
            );
            receiverCombo.setItems(allUsers);
            
            // Set cell factories to display meaningful text
            offeredArtworkCombo.setCellFactory(param -> new ListCell<Artwork>() {
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
            
            requestedArtworkCombo.setCellFactory(param -> new ListCell<Artwork>() {
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
            
            receiverCombo.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            
            // Set button cell factories to display meaningful text
            offeredArtworkCombo.setButtonCell(new ListCell<Artwork>() {
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
            
            requestedArtworkCombo.setButtonCell(new ListCell<Artwork>() {
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
            
            receiverCombo.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            
            // Update requested artwork combo when receiver is selected
            receiverCombo.setOnAction(event -> {
                User selectedUser = receiverCombo.getValue();
                if (selectedUser != null) {
                    try {
                        ObservableList<Artwork> receiverArtworks = FXCollections.observableArrayList(
                            artworkService.getByOwner(selectedUser.getId())
                        );
                        requestedArtworkCombo.setItems(receiverArtworks);
                    } catch (Exception e) {
                        showStatus("Error loading receiver's artworks: " + e.getMessage(), true);
                    }
                }
            });
            
        } catch (Exception e) {
            showStatus("Error loading data: " + e.getMessage(), true);
        }
    }
    
    public void setParentController(TradeOfferListController controller) {
        this.parentController = controller;
    }
    
    @FXML
    private void handleCreateTrade(ActionEvent event) {
        Artwork offeredArtwork = offeredArtworkCombo.getValue();
        Artwork requestedArtwork = requestedArtworkCombo.getValue();
        User receiver = receiverCombo.getValue();
        String description = descriptionField.getText().trim();
        
        // Validate inputs
        if (offeredArtwork == null || requestedArtwork == null || receiver == null) {
            showStatus("Please select all required fields", true);
            return;
        }
        
        try {
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
            ((Stage) offeredArtworkCombo.getScene().getWindow()).close();
            
        } catch (Exception e) {
            showStatus("Error creating trade offer: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) offeredArtworkCombo.getScene().getWindow()).close();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}