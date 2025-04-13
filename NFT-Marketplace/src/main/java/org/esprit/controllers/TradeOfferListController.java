package org.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.esprit.models.TradeOffer;
import org.esprit.models.User;
import org.esprit.services.TradeOfferService;

import java.io.IOException;

public class TradeOfferListController {
    
    @FXML
    private TableView<TradeOffer> tradeTable;
    
    @FXML
    private TableColumn<TradeOffer, String> senderColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> receiverColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> offeredItemColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> requestedItemColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> statusColumn;
    
    @FXML
    private Button createTradeButton;
    
    private User currentUser;
    private TradeOfferService tradeService;
    private ObservableList<TradeOffer> tradeList;
    
    public void initialize() {
        tradeService = new TradeOfferService();
        tradeList = FXCollections.observableArrayList();
        
        // Initialize table columns
        senderColumn.setCellValueFactory(data -> 
            data.getValue().getSender() != null ? 
            new SimpleStringProperty(data.getValue().getSender().getName()) : 
            new SimpleStringProperty(""));
            
        receiverColumn.setCellValueFactory(data -> 
            data.getValue().getReceiverName() != null ? 
            new SimpleStringProperty(data.getValue().getReceiverName().getName()) : 
            new SimpleStringProperty(""));
            
        offeredItemColumn.setCellValueFactory(data -> 
            data.getValue().getOfferedItem() != null ? 
            new SimpleStringProperty(data.getValue().getOfferedItem().getTitle()) : 
            new SimpleStringProperty(""));
            
        requestedItemColumn.setCellValueFactory(data -> 
            data.getValue().getReceivedItem() != null ? 
            new SimpleStringProperty(data.getValue().getReceivedItem().getTitle()) : 
            new SimpleStringProperty(""));
            
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        tradeTable.setItems(tradeList);
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        refreshTrades();
    }
    
    @FXML
    private void handleCreateTrade(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateTrade.fxml"));
            Parent createTradeView = loader.load();
            
            CreateTradeController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(createTradeView));
            stage.setTitle("Create New Trade Offer");
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open create trade view: " + e.getMessage());
        }
    }
    
    public void refreshTrades() {
        try {
            tradeList.clear();
            tradeList.addAll(tradeService.getAllTradeOffers());
        } catch (Exception e) {
            showError("Error loading trades: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}