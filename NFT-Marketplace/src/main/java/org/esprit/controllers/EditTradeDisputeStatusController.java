package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.esprit.models.TradeDispute;
import org.esprit.services.TradeDisputeService;

public class EditTradeDisputeStatusController {
    @FXML
    private Label currentStatusLabel;
    
    @FXML
    private ComboBox<String> statusComboBox;
    
    private TradeDispute dispute;
    private final TradeDisputeService disputeService = new TradeDisputeService();
    
    public void initialize() {
        // Initialize status options
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "pending",
            "resolved",
            "rejected"
        );
        statusComboBox.setItems(statusOptions);
    }
    
    public void setDispute(TradeDispute dispute) {
        this.dispute = dispute;
        updateUI();
    }
    
    private void updateUI() {
        currentStatusLabel.setText(dispute.getStatus());
        statusComboBox.setValue(dispute.getStatus());
    }
    
    @FXML
    private void handleUpdate() {
        String newStatus = statusComboBox.getValue();
        if (newStatus != null && !newStatus.equals(dispute.getStatus())) {
            dispute.setStatus(newStatus);
            try {
                disputeService.updateDisputeStatus(dispute.getId(), dispute.getStatus());
                closeWindow();
            } catch (Exception e) {
                showError("Failed to update status: " + e.getMessage());
            }
        } else {
            closeWindow();
        }
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) currentStatusLabel.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}