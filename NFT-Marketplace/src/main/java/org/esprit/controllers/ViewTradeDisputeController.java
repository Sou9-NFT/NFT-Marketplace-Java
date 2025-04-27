package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.services.TradeDisputeService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ViewTradeDisputeController {
    @FXML
    private Label reporterLabel;
    @FXML
    private Label tradeIdLabel;
    @FXML
    private Label offeredItemLabel;
    @FXML
    private Label receivedItemLabel;
    @FXML
    private TextArea reasonTextArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private ImageView evidenceImageView;
    @FXML
    private VBox root;

    private User currentUser;
    private TradeDispute dispute;
    private final TradeDisputeService disputeService = new TradeDisputeService();

    public void initialize() {
        if (dispute != null) {
            updateUI();
        }
    }

    private void updateUI() {
        reporterLabel.setText(dispute.getReporterName());
        tradeIdLabel.setText(String.valueOf(dispute.getTradeId()));
        offeredItemLabel.setText(dispute.getOfferedItemTitle());
        receivedItemLabel.setText(dispute.getReceivedItemTitle() != null ? dispute.getReceivedItemTitle() : "N/A");
        reasonTextArea.setText(dispute.getReason());
        statusLabel.setText(dispute.getStatus());
        dateLabel.setText(dispute.getTimestamp().toString());
        
        // Load evidence image if available
        if (dispute.getEvidence() != null && !dispute.getEvidence().isEmpty()) {
            try {
                byte[] imageData = Base64.getDecoder().decode(dispute.getEvidence());
                Image image = new Image(new ByteArrayInputStream(imageData));
                evidenceImageView.setImage(image);
            } catch (Exception e) {
                evidenceImageView.setImage(null);
            }
        } else {
            evidenceImageView.setImage(null);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeDisputeList.fxml"));
            root.getScene().setRoot(loader.load());
            TradeDisputeListController controller = loader.getController();
            controller.setUser(currentUser);
        } catch (IOException e) {
            showError("Error returning to dispute list: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) evidenceImageView.getScene().getWindow();
        stage.close();
    }

    public void setDispute(TradeDispute dispute) {
        this.dispute = dispute;
        if (reporterLabel != null) {
            updateUI();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 