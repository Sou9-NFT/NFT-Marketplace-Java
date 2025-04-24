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
import javafx.scene.layout.StackPane;
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
    
    @FXML
    private Button betSessionButton;
    
    @FXML
    private StackPane contentArea;
    
    private User currentUser;
    
    public void initialize() {
        // Initialize the controller
        // We might want to set a default view here
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
            
            loadContentInPlace(profileView, "User Profile");
        } catch (IOException e) {
            showAlert("Error", "Could not load profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRafflesButton(ActionEvent event) {
        try {
            // Try using ClassLoader directly instead of getResource
            ClassLoader classLoader = getClass().getClassLoader();
            java.net.URL resourceUrl = classLoader.getResource("fxml/RaffleList.fxml");
            
            if (resourceUrl == null) {
                System.err.println("Failed to find RaffleList.fxml resource using ClassLoader");
                
                // Fall back to getResource with various paths
                resourceUrl = getClass().getResource("/fxml/RaffleList.fxml");
                
                if (resourceUrl == null) {
                    System.err.println("Failed to find RaffleList.fxml with any method");
                    showAlert("Error", "Could not load raffles: Resource not found");
                    return;
                }
            }
            
            System.out.println("Found RaffleList.fxml at: " + resourceUrl);
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent raffleView = loader.load();
            
            RaffleListController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(raffleView, "Raffles");
        } catch (IOException e) {
            System.err.println("Error loading RaffleList.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load raffles: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleArtworksButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArtworkManagement.fxml"));
            Parent artworkView = loader.load();
            
            ArtworkManagementController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            loadContentInPlace(artworkView, "Artwork Management");
        } catch (IOException e) {
            showAlert("Error", "Could not load artwork management: " + e.getMessage());
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
                
                loadContentInPlace(marketplaceView, "Marketplace");
            } catch (IOException e) {
                showAlert("Error", "Could not load marketplace: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Marketplace");
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
                
                loadContentInPlace(walletView, "Wallet");
            } catch (IOException e) {
                showAlert("Error", "Could not load wallet: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Wallet");
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
                
                loadContentInPlace(notificationsView, "Notifications");
            } catch (IOException e) {
                showAlert("Error", "Could not load notifications: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Notifications");
        }
    }
    
    @FXML
    private void handleBetSessionButton(ActionEvent event) {
        try {
            // Changed to load MyBetSessions.fxml instead of BetSession.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MyBetSessions.fxml"));
            Parent betSessionView = loader.load();
            
            // If there's a controller with setUser method, call it
            Object controller = loader.getController();
            tryToSetUser(controller);
            
            loadContentInPlace(betSessionView, "Bet Sessions");
        } catch (IOException e) {
            showAlert("Error", "Could not load trade offers: " + e.getMessage());
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
    
    /**
     * Loads content into the contentArea StackPane with proper transition
     * @param view The view to load
     * @param title The title/section name to set
     */
    private void loadContentInPlace(Parent view, String title) {
        // Clear existing content and set new content
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
        
        // Update the window title to reflect the current section
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setTitle("Sou9 NFT - " + title);
    }
    
    /**
     * Shows the "Coming Soon" view inside the contentArea
     */
    private void showComingSoonInPlace(String featureName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ComingSoon.fxml"));
            
            if (loader.getLocation() == null) {
                // Show an alert if the FXML doesn't exist
                showAlert("Coming Soon", featureName + " feature is coming soon!");
            } else {
                Parent comingSoonView = loader.load();
                
                // If there's a ComingSoonController, set the feature name
                if (loader.getController() != null) {
                    try {
                        loader.getController().getClass().getMethod("setFeatureName", String.class)
                            .invoke(loader.getController(), featureName);
                    } catch (Exception e) {
                        // Ignore if method doesn't exist
                    }
                }
                
                loadContentInPlace(comingSoonView, "Coming Soon: " + featureName);
            }
        } catch (IOException e) {
            showAlert("Coming Soon", featureName + " feature is coming soon!");
        }
    }
    
    // This method is now only used for complete page transitions like logout
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