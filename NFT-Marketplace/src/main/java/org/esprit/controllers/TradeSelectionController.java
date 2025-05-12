package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.esprit.models.User;
import java.io.IOException;

public class TradeSelectionController {
    private User currentUser;
    private UserDashboardController dashboardController;
    
    public void setUser(User user) {
        this.currentUser = user;
    }
    
    public void setDashboardController(UserDashboardController controller) {
        this.dashboardController = controller;
    }
    
    @FXML
    private void handleTradeOffers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeOfferList.fxml"));
            Parent tradeView = loader.load();
            
            TradeOfferListController controller = loader.getController();
            controller.setUser(currentUser);
            
            dashboardController.loadContent(tradeView, "Trade Offers");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleTradeRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeRequestList.fxml"));
            Parent tradeView = loader.load();
            
            TradeRequestListController controller = loader.getController();
            controller.setUser(currentUser);
            
            dashboardController.loadContent(tradeView, "Trade Requests");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleTradeDisputes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeDisputeList.fxml"));
            Parent tradeView = loader.load();
            
            TradeDisputeListController controller = loader.getController();
            controller.setUser(currentUser);
            
            dashboardController.loadContent(tradeView, "Trade Disputes");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
