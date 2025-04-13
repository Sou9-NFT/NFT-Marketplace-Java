package org.esprit.controllers;

import java.io.IOException;

import org.esprit.models.User;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class RaffleListController {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilter;
    
    @FXML
    private GridPane rafflesGrid;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    
    public void initialize() {
        // Initialize the controller
        statusFilter.getItems().addAll("All", "Active", "Ended", "Upcoming");
        statusFilter.setValue("All");
        
        // Add listener for search field and status filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        refreshRaffles();
    }
    
    @FXML
    private void handleCreateRaffle(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateRaffle.fxml"));
            Parent createRaffleView = loader.load();
            
            // Set the current user in the create raffle controller if it exists
            Object controller = loader.getController();
            // Try to set the user using reflection to avoid direct dependency
            try {
                controller.getClass().getMethod("setCurrentUser", User.class)
                    .invoke(controller, currentUser);
            } catch (Exception ex) {
                // Silently ignore if the method doesn't exist
                System.out.println("Warning: Could not set user in CreateRaffleController");
            }
            
            Stage stage = new Stage();
            stage.setScene(new Scene(createRaffleView));
            stage.setTitle("Create New Raffle");
            stage.show();
            
            // Refresh the raffles list when the create raffle window is closed
            stage.setOnHidden(e -> refreshRaffles());
            
        } catch (IOException e) {
            showError("Could not open create raffle view: " + e.getMessage());
        }
    }
    
    public void refreshRaffles() {
        // Clear the current grid
        rafflesGrid.getChildren().clear();
        
        // TODO: Load raffles from the database and display them in the grid
        // This would typically involve a service call to fetch raffles
        // For now, let's just show a message
        
        statusLabel.setText("Raffles will be loaded here");
        statusLabel.setVisible(true);
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }
    
    /**
     * Cleanup resources before controller is destroyed
     * Called when application is closing to handle any necessary cleanup
     */
    public void cleanup() {
        // Perform any necessary cleanup here such as:
        // - Stopping background tasks
        // - Closing connections
        // - Releasing resources
        
        System.out.println("RaffleListController cleanup performed");
    }
}