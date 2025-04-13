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

public class UserDashboardController {

    @FXML
    private Label userNameLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button profileButton;
    
    @FXML
    private Button rafflesButton;
    
    @FXML
    private Button artworksButton;
    
    @FXML
    private Button marketplaceButton;
    
    @FXML
    private Button walletButton;
    
    @FXML
    private Button notificationsButton;
    
    private User currentUser;
    
    public void initialize() {
        // Initialize the controller
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && userNameLabel != null) {
            userNameLabel.setText("Welcome, " + user.getName());
        } else if (userNameLabel != null) {
            // Default text if user is null
            userNameLabel.setText("Welcome");
        }
    }
    
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not log out: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleProfileButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);
            
            navigateToView(event, profileView, "NFT Marketplace - User Profile");
        } catch (IOException e) {
            showAlert("Error", "Could not load profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRafflesButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleList.fxml"));
            Parent raffleView = loader.load();
            
            RaffleListController controller = loader.getController();
            controller.setUser(currentUser);
            
            navigateToView(event, raffleView, "NFT Marketplace - Raffles");
        } catch (IOException e) {
            showAlert("Error", "Could not load raffles: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleArtworksButton(ActionEvent event) {
        // Check if artwork view exists
        if (getClass().getResource("/fxml/ArtworkList.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArtworkList.fxml"));
                Parent artworkView = loader.load();
                
                // If there's a controller with setUser method, call it
                Object controller = loader.getController();
                tryToSetUser(controller);
                
                navigateToView(event, artworkView, "NFT Marketplace - Artworks");
            } catch (IOException e) {
                showAlert("Error", "Could not load artworks: " + e.getMessage());
            }
        } else {
            showComingSoonView(event, "Artworks");
        }
    }
    
    @FXML
    private void handleMarketplaceButton(ActionEvent event) {
        if (getClass().getResource("/fxml/Marketplace.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Marketplace.fxml"));
                Parent marketplaceView = loader.load();
                
                // If there's a controller with setUser method, call it
                Object controller = loader.getController();
                tryToSetUser(controller);
                
                navigateToView(event, marketplaceView, "NFT Marketplace - Marketplace");
            } catch (IOException e) {
                showAlert("Error", "Could not load marketplace: " + e.getMessage());
            }
        } else {
            showComingSoonView(event, "Marketplace");
        }
    }
    
    @FXML
    private void handleWalletButton(ActionEvent event) {
        if (getClass().getResource("/fxml/Wallet.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Wallet.fxml"));
                Parent walletView = loader.load();
                
                // If there's a controller with setUser method, call it
                Object controller = loader.getController();
                tryToSetUser(controller);
                
                navigateToView(event, walletView, "NFT Marketplace - Wallet");
            } catch (IOException e) {
                showAlert("Error", "Could not load wallet: " + e.getMessage());
            }
        } else {
            showComingSoonView(event, "Wallet");
        }
    }
      @FXML
    private void handleNotificationsButton(ActionEvent event) {
        if (getClass().getResource("/fxml/Notifications.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Notifications.fxml"));
                Parent notificationsView = loader.load();
                
                // If there's a controller with setUser method, call it
                Object controller = loader.getController();
                tryToSetUser(controller);
                
                navigateToView(event, notificationsView, "NFT Marketplace - Notifications");
            } catch (IOException e) {
                showAlert("Error", "Could not load notifications: " + e.getMessage());
            }
        } else {
            showComingSoonView(event, "Notifications");
        }
    }
    
    @FXML
    private void handleBetSessionButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BetSession.fxml"));
            Parent betSessionView = loader.load();
            
            // If there's a controller with setUser method, call it
            Object controller = loader.getController();
            tryToSetUser(controller);
            
            navigateToView(event, betSessionView, "NFT Marketplace - Bet Sessions");
        } catch (IOException e) {
            showAlert("Error", "Could not load bet sessions: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to safely try to set user on a controller without
     * requiring specific controller types
     */
    private void tryToSetUser(Object controller) {
        if (controller != null) {
            try {
                // Try to call setUser method if it exists
                controller.getClass().getMethod("setUser", User.class)
                    .invoke(controller, currentUser);
            } catch (Exception e) {
                // Try setCurrentUser method if setUser failed
                try {
                    controller.getClass().getMethod("setCurrentUser", User.class)
                        .invoke(controller, currentUser);
                } catch (Exception ex) {
                    // Silently ignore if neither method exists
                    System.out.println("Warning: Could not set user in controller " + controller.getClass().getName());
                }
            }
        }
    }
    
    private void showComingSoonView(ActionEvent event, String featureName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ComingSoon.fxml"));
            
            // If ComingSoon.fxml doesn't exist, create a simple view
            Parent comingSoonView;
            if (loader.getLocation() == null) {
                // Show an alert instead of navigating
                showAlert("Coming Soon", featureName + " feature is coming soon!");
            } else {
                comingSoonView = loader.load();
                
                // If there's a ComingSoonController, set the feature name
                if (loader.getController() != null) {
                    try {
                        loader.getController().getClass().getMethod("setFeatureName", String.class)
                            .invoke(loader.getController(), featureName);
                    } catch (Exception e) {
                        // Ignore if method doesn't exist
                    }
                }
                
                navigateToView(event, comingSoonView, "NFT Marketplace - Coming Soon");
            }
        } catch (IOException e) {
            showAlert("Coming Soon", featureName + " feature is coming soon!");
        }
    }
    
    private void navigateToView(ActionEvent event, Parent view, String title) {
        Scene currentScene = ((Node) event.getSource()).getScene();
        Stage stage = (Stage) currentScene.getWindow();
        
        stage.setScene(new Scene(view));
        stage.setTitle(title);
        stage.show();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}