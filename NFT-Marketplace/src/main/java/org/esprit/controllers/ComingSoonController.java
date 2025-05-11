package org.esprit.controllers;

import java.io.IOException;

import org.esprit.models.User;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ComingSoonController {

    @FXML
    private Label featureNameLabel;
    
    @FXML
    private Button backButton;
    
    private User currentUser;
    private String featureName = "Feature";
    private boolean isAdmin = false;
    
    public void initialize() {
        // Initialize controller
    }
    
    public void setFeatureName(String name) {
        this.featureName = name;
        if (featureNameLabel != null) {
            featureNameLabel.setText(name);
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        // Check if user has admin role
        if (user != null && user.getRoles() != null) {
            isAdmin = user.getRoles().contains("ROLE_ADMIN");
        }
    }
      @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Check if currentUser is null - if it is, default to the user dashboard
            if (currentUser == null) {
                showAlert("Session Error", "Your session information is missing. Redirecting to login page.");
                navigateToLogin(event);
                return;
            }
            
            // Determine which dashboard to return to based on user role
            String dashboardPath = isAdmin ? "/fxml/AdminDashboard.fxml" : "/fxml/UserDashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(dashboardPath));
            Parent dashboardView = loader.load();
            
            // Set the current user in the appropriate controller
            if (isAdmin) {
                AdminDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else {
                UserDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }
            
            // Navigate back to the dashboard
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(dashboardView));
            stage.setTitle("NFT Marketplace - " + (isAdmin ? "Admin" : "User") + " Dashboard");
            
            // Set the stage to fullscreen before showing it
            stage.setMaximized(true);
            stage.show();
            
            // Call the controller's method to ensure fullscreen is set
            if (isAdmin) {
                AdminDashboardController controller = loader.getController();
                controller.setStageFullScreen();
            } else {
                UserDashboardController controller = loader.getController();
                controller.setStageFullScreen();
            }
        } catch (IOException e) {
            System.err.println("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
            // If we can't load the dashboard, go to login
            try {
                navigateToLogin(event);
            } catch (Exception ex) {
                System.err.println("Failed to navigate to login: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error navigating to login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}