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
    private TableColumn<TradeOffer, Void> editColumn;
    
    @FXML
    private Button createTradeButton;
    
    private User currentUser;
    private TradeOfferService tradeService;
    private ObservableList<TradeOffer> tradeList;
    
    @FXML
    public void initialize() {
        System.out.println("Initializing TradeOfferListController...");
        try {
            tradeService = new TradeOfferService();
            System.out.println("TradeOfferService initialized.");

            tradeList = FXCollections.observableArrayList();
            System.out.println("Trade list initialized.");

            // Initialize table columns
            senderColumn.setCellValueFactory(data -> {
                System.out.println("Setting sender column value factory.");
                return data.getValue().getSender() != null ? 
                    new SimpleStringProperty(data.getValue().getSender().getName()) : 
                    new SimpleStringProperty("");
            });
            
            receiverColumn.setCellValueFactory(data -> {
                System.out.println("Setting receiver column value factory.");
                return data.getValue().getReceiverName() != null ? 
                    new SimpleStringProperty(data.getValue().getReceiverName().getName()) : 
                    new SimpleStringProperty("");
            });
            
            offeredItemColumn.setCellValueFactory(data -> {
                System.out.println("Setting offered item column value factory.");
                return data.getValue().getOfferedItem() != null ? 
                    new SimpleStringProperty(data.getValue().getOfferedItem().getTitle()) : 
                    new SimpleStringProperty("");
            });
            
            requestedItemColumn.setCellValueFactory(data -> {
                System.out.println("Setting requested item column value factory.");
                return data.getValue().getReceivedItem() != null ? 
                    new SimpleStringProperty(data.getValue().getReceivedItem().getTitle()) : 
                    new SimpleStringProperty("");
            });
            
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            System.out.println("Status column value factory set.");

            tradeTable.setItems(tradeList);
            System.out.println("Trade table items set.");

            // Add edit button to each row
            editColumn.setCellFactory(column -> new TableCell<>() {
                private final Button editButton = new Button("Edit");

                {
                    editButton.setOnAction(event -> {
                        TradeOffer tradeOffer = getTableView().getItems().get(getIndex());
                        handleEditTrade(tradeOffer);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(editButton);
                    }
                }
            });
            System.out.println("TradeOfferListController initialized successfully.");
        } catch (Exception e) {
            System.err.println("Error during TradeOfferListController initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setUser(User user) {
        System.out.println("Setting user in TradeOfferListController: " + (user != null ? user.getName() : "null"));
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

    private void handleEditTrade(TradeOffer tradeOffer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditTrade.fxml"));
            Parent editTradeView = loader.load();

            EditTradeController controller = loader.getController();
            controller.setTradeOffer(tradeOffer);

            Stage stage = new Stage();
            stage.setScene(new Scene(editTradeView));
            stage.setTitle("Edit Trade Offer");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}