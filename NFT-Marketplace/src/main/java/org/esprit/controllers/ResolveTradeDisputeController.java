package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.services.TradeDisputeService;

import java.io.IOException;

public class ResolveTradeDisputeController {
    @FXML
    private Label reporterLabel;
    @FXML
    private Label tradeIdLabel;
    @FXML
    private Label reasonLabel;
    @FXML
    private Label evidenceLabel;
    @FXML
    private ComboBox<String> resolutionComboBox;
    @FXML
    private TextArea resolutionNotesField;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox root;

    private User currentUser;
    private TradeDispute dispute;
    private final TradeDisputeService disputeService = new TradeDisputeService();

    public void initialize() {
        resolutionComboBox.getItems().addAll("resolved", "rejected");
        if (dispute != null) {
            updateUI();
        }
    }

    private void updateUI() {
        reporterLabel.setText(dispute.getReporterName());
        tradeIdLabel.setText(String.valueOf(dispute.getTradeId()));
        reasonLabel.setText(dispute.getReason());
        evidenceLabel.setText(dispute.getEvidence());
    }

    @FXML
    private void handleResolve() {
        try {
            String resolution = resolutionComboBox.getValue();
            String notes = resolutionNotesField.getText();

            if (resolution == null) {
                showError("Please select a resolution");
                return;
            }

            if (disputeService.updateDisputeStatus(dispute.getId(), resolution)) {
                showSuccess("Dispute resolved successfully");
                // Return to dispute list
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeDisputeList.fxml"));
                root.getScene().setRoot(loader.load());
                TradeDisputeListController controller = loader.getController();
                controller.setUser(currentUser);
            } else {
                showError("Failed to resolve dispute");
            }
        } catch (Exception e) {
            showError("Error resolving dispute: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeDisputeList.fxml"));
            root.getScene().setRoot(loader.load());
            TradeDisputeListController controller = loader.getController();
            controller.setUser(currentUser);
        } catch (IOException e) {
            showError("Error returning to dispute list: " + e.getMessage());
        }
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
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setText(message);
    }
} 