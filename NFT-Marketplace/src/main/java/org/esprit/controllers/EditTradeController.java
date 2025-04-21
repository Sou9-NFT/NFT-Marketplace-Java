package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import org.esprit.models.TradeOffer;
import org.esprit.models.Artwork;

import java.time.LocalDateTime;
import java.util.List;

public class EditTradeController {

    @FXML
    private ComboBox<Artwork> offeredItemComboBox;

    @FXML
    private ComboBox<Artwork> requestedItemComboBox;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Label statusLabel;

    private TradeOffer tradeOffer;

    public void setTradeOffer(TradeOffer tradeOffer) {
        this.tradeOffer = tradeOffer;
        populateFields();
    }

    private void populateFields() {
        if (tradeOffer != null) {
            offeredItemComboBox.setValue(tradeOffer.getOfferedItem());
            requestedItemComboBox.setValue(tradeOffer.getReceivedItem());
            descriptionField.setText(tradeOffer.getDescription());
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (tradeOffer != null) {
            tradeOffer.setOfferedItem(offeredItemComboBox.getValue());
            tradeOffer.setReceivedItem(requestedItemComboBox.getValue());
            tradeOffer.setDescription(descriptionField.getText());

            // Logic to update the trade offer in the database
            statusLabel.setText("Trade offer updated successfully.");
        } else {
            statusLabel.setText("Error: No trade offer to update.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // Logic to close the edit window
        statusLabel.getScene().getWindow().hide();
    }

    private void loadUserArtworks() {
        try {
            // Mock logic to retrieve user artworks
            List<Artwork> userArtworks = List.of(
                new Artwork(1, 0, 1, 0, "User Artwork 1", "Description 1", 0.0, "Image1.jpg", LocalDateTime.now(), LocalDateTime.now()),
                new Artwork(2, 0, 1, 0, "User Artwork 2", "Description 2", 0.0, "Image2.jpg", LocalDateTime.now(), LocalDateTime.now())
            );
            offeredItemComboBox.getItems().clear();
            offeredItemComboBox.getItems().addAll(userArtworks);

            // Mock logic to retrieve all artworks
            List<Artwork> allArtworks = List.of(
                new Artwork(1, 0, 1, 0, "Artwork 1", "Description 1", 0.0, "Image1.jpg", LocalDateTime.now(), LocalDateTime.now()),
                new Artwork(2, 0, 2, 0, "Artwork 2", "Description 2", 0.0, "Image2.jpg", LocalDateTime.now(), LocalDateTime.now()),
                new Artwork(3, 0, 3, 0, "Artwork 3", "Description 3", 0.0, "Image3.jpg", LocalDateTime.now(), LocalDateTime.now())
            );
            requestedItemComboBox.getItems().clear();
            requestedItemComboBox.getItems().addAll(allArtworks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
