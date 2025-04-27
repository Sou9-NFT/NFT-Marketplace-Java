package org.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.esprit.models.TradeState;
import org.esprit.models.User;
import org.esprit.models.TradeOffer;
import org.esprit.models.Artwork;
import org.esprit.services.TradeStateService;
import org.esprit.services.ArtworkService;

import java.sql.SQLException;

public class TradeRequestListController {
    @FXML private TableView<TradeState> tradeTable;
    @FXML private TableColumn<TradeState, String> idColumn;
    @FXML private TableColumn<TradeState, String> senderColumn;
    @FXML private TableColumn<TradeState, String> receiverColumn;
    @FXML private TableColumn<TradeState, String> offeredItemColumn;
    @FXML private TableColumn<TradeState, String> requestedItemColumn;
    @FXML private TableColumn<TradeState, String> descriptionColumn;
    @FXML private TableColumn<TradeState, Void> actionColumn;
    
    private User currentUser;
    private TradeStateService tradeStateService;
    private ObservableList<TradeState> tradeList;
    private ArtworkService artworkService;
    
    @FXML
    public void initialize() {
        tradeStateService = new TradeStateService();
        artworkService = new ArtworkService();
        tradeList = FXCollections.observableArrayList();

        setupColumns();
        setupActionColumn();
    }

    private void setupColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        senderColumn.setCellValueFactory(data -> {
            User sender = data.getValue().getSender();
            return new SimpleStringProperty(sender != null ? sender.getName() : "");
        });
        receiverColumn.setCellValueFactory(data -> {
            User receiver = data.getValue().getReceiver();
            return new SimpleStringProperty(receiver != null ? receiver.getName() : "");
        });
        offeredItemColumn.setCellValueFactory(data -> {
            Artwork offered = data.getValue().getOfferedItem();
            return new SimpleStringProperty(offered != null ? offered.getTitle() : "");
        });
        requestedItemColumn.setCellValueFactory(data -> {
            Artwork requested = data.getValue().getReceivedItem();
            return new SimpleStringProperty(requested != null ? requested.getTitle() : "");
        });
        descriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
    }    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<TradeState, Void>() {
            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");
            private final HBox buttonsBox = new HBox(5, acceptButton, rejectButton);            {
                // Style the buttons
                acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                
                acceptButton.setOnAction(event -> {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    handleAcceptTrade(tradeState);
                });
                
                rejectButton.setOnAction(event -> {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    handleRejectTrade(tradeState);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                }
            }
        });
    }    // Accept and Reject handlers will be implemented later

    public void setUser(User user) {
        this.currentUser = user;
        refreshTrades();
    }    public void refreshTrades() {
        try {
            tradeList.clear();
            var trades = tradeStateService.getAllPendingTrades();
            // Filter trades to only show ones where current user is the receiver
            trades.removeIf(trade -> 
                currentUser == null || 
                trade.getReceiver() == null ||
                trade.getReceiver().getId() != currentUser.getId()
            );
            tradeList.addAll(trades);
            tradeTable.setItems(tradeList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load trades from database: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load trades: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleAcceptTrade(TradeState tradeState) {
        try {
            // Validate the trade state
            if (tradeState == null || tradeState.getTradeOffer() == null) {
                showAlert("Error", "Invalid trade state", Alert.AlertType.ERROR);
                return;
            }

            // Security check - ensure current user is the receiver
            if (currentUser == null || tradeState.getReceiver() == null || 
                currentUser.getId() != tradeState.getReceiver().getId()) {
                showAlert("Error", "You are not authorized to accept this trade", Alert.AlertType.ERROR);
                return;
            }

            // Get the items involved in the trade
            Artwork receivedItem = tradeState.getReceivedItem();
            Artwork offeredItem = tradeState.getOfferedItem();

            // Security checks for items
            if (receivedItem == null || offeredItem == null) {
                showAlert("Error", "Invalid trade items", Alert.AlertType.ERROR);
                return;
            }

            // Get the users involved
            User sender = tradeState.getSender();
            User receiver = tradeState.getReceiver();

            // Swap ownership of the items
            int tempOwnerId = offeredItem.getOwnerId();
            offeredItem.setOwnerId(receiver.getId());
            receivedItem.setOwnerId(tempOwnerId);

            // Update items in database
            artworkService.update(receivedItem);
            artworkService.update(offeredItem);

            // Update trade offer status
            TradeOffer tradeOffer = tradeState.getTradeOffer();
            tradeOffer.setStatus("accepted");
            tradeStateService.updateTradeOfferStatus(tradeOffer.getId(), "accepted");

            // Show success message
            showAlert("Success", "Trade accepted and items swapped successfully!", Alert.AlertType.INFORMATION);

            // Refresh the trades list
            refreshTrades();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to accept trade: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleRejectTrade(TradeState tradeState) {
        try {
            // Validate the trade state
            if (tradeState == null || tradeState.getTradeOffer() == null) {
                showAlert("Error", "Invalid trade state", Alert.AlertType.ERROR);
                return;
            }

            // Security check - ensure current user is the receiver
            if (currentUser == null || tradeState.getReceiver() == null || 
                currentUser.getId() != tradeState.getReceiver().getId()) {
                showAlert("Error", "You are not authorized to reject this trade", Alert.AlertType.ERROR);
                return;
            }

            // Update trade offer status
            TradeOffer tradeOffer = tradeState.getTradeOffer();
            tradeOffer.setStatus("rejected");
            tradeStateService.updateTradeOfferStatus(tradeOffer.getId(), "rejected");

            // Show success message
            showAlert("Success", "Trade offer rejected successfully", Alert.AlertType.INFORMATION);

            // Refresh the trades list
            refreshTrades();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to reject trade: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
