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
    private Label userNameSidebarLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button logoutSidebarButton;
    
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
    private Button blogsButton;
    
    @FXML
    private Button tradeOfferButton;
    
    @FXML
    private Button tradeDisputeButton;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private Label pageTitleLabel;
    
    private User currentUser;
    
    private Button lastActiveButton;
    
    private ProfileController profileController;
    
    // This method is called automatically when the FXML is loaded
    public void initialize() {
        // Initialize the controller - set default active button
        setActiveButton(profileButton);
        
        // Load profile view by default
        loadProfileView();
    }
      /**
     * Loads the profile view programmatically
     */
    private void loadProfileView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            // Store the controller reference for later updates
            profileController = loader.getController();
            
            // If we already have a user, set it
            if (currentUser != null) {
                profileController.setUser(currentUser);
            }
            
            // Just add to contentArea during initialization, skip window title update
            contentArea.getChildren().clear();
            contentArea.getChildren().add(profileView);
            
            // Update page title if available
            if (pageTitleLabel != null) {
                pageTitleLabel.setText("User Profile");
            }
        } catch (IOException e) {
            System.err.println("Could not load profile view: " + e.getMessage());
        }
    }    /**
     * Sets the current user for this controller
     * @param user The current user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            String userName = user.getName();
            
            // Update both top bar and sidebar user labels
            if (userNameLabel != null) {
                userNameLabel.setText("Welcome, " + userName);
            }
            
            if (userNameSidebarLabel != null) {
                userNameSidebarLabel.setText(userName);
            }
            
            // Update the profile controller if it exists
            if (profileController != null) {
                profileController.setUser(user);
            }
              // Now that the user is set, we can update the window title if the scene is ready
            if (contentArea.getScene() != null && contentArea.getScene().getWindow() != null) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setTitle("Sou9 NFT - User Profile");
                
                // Set the stage to fullscreen
                stage.setMaximized(true);
            }
        } else {
            // Default text if user is null
            if (userNameLabel != null) {
                userNameLabel.setText("Welcome");
            }
            if (userNameSidebarLabel != null) {
                userNameSidebarLabel.setText("Guest");
            }
        }
    }
    
    /**
     * Handle user logout
     */
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
    
    /**
     * Handle profile button click
     */
    @FXML
    private void handleProfileButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(profileView, "User Profile");
            setActiveButton(profileButton);
        } catch (IOException e) {
            showAlert("Error", "Could not load profile: " + e.getMessage());
        }
    }
    
    /**
     * Handle raffles button click
     */
    @FXML
    private void handleRafflesButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleList.fxml"));
            Parent raffleView = loader.load();
            
            RaffleListController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(raffleView, "Raffles");
            setActiveButton(rafflesButton);
        } catch (IOException e) {
            showAlert("Error", "Could not load raffles: " + e.getMessage());
        }
    }
    
    /**
     * Handle artworks button click
     */
    @FXML
    private void handleArtworksButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArtworkManagement.fxml"));
            Parent artworkView = loader.load();
            
            ArtworkManagementController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            loadContentInPlace(artworkView, "Artwork Management");
            setActiveButton(artworksButton);
        } catch (IOException e) {
            showAlert("Error", "Could not load artwork management: " + e.getMessage());
        }
    }
    
    /**
     * Handle marketplace button click
     */
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
                setActiveButton(marketplaceButton);
            } catch (IOException e) {
                showAlert("Error", "Could not load marketplace: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Marketplace");
            setActiveButton(marketplaceButton);
        }
    }
    
    /**
     * Handle wallet button click
     */
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
                setActiveButton(walletButton);
            } catch (IOException e) {
                showAlert("Error", "Could not load wallet: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Wallet");
            setActiveButton(walletButton);
        }
    }

    /**
     * Handle notifications button click
     */
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
                setActiveButton(notificationsButton);
            } catch (IOException e) {
                showAlert("Error", "Could not load notifications: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Notifications");
            setActiveButton(notificationsButton);
        }
    }
    
    /**
     * Handle bet session button click
     */
    @FXML
    private void handleBetSessionButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MyBetSessions.fxml"));
            Parent betSessionView = loader.load();
            
            // If there's a controller with setUser method, call it
            Object controller = loader.getController();
            tryToSetUser(controller);
            
            loadContentInPlace(betSessionView, "Bet Sessions");
            setActiveButton(betSessionButton);
        } catch (IOException e) {
            showAlert("Error", "Could not load bet sessions: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBlogsButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BlogList.fxml"));
            Parent blogView = loader.load();
            
            // Pass the current user to the blog list controller
            BlogListController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            loadContentInPlace(blogView, "Blogs");
            setActiveButton(blogsButton);
        } catch (IOException e) {
            showAlert("Error", "Could not load blogs: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleTradeOfferButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeOfferList.fxml"));
            Parent tradeOfferView = loader.load();
            
            TradeOfferListController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(tradeOfferView, "Trade Offers");
        } catch (IOException e) {
            showAlert("Error", "Could not load trade offers: " + e.getMessage());
            System.err.println("Error in handleTradeOfferButton: " + e.getMessage());
            e.printStackTrace();
        }
    }
      @FXML
    private void handleTradeDisputeButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeDisputeList.fxml"));
            Parent tradeDisputeView = loader.load();
            
            TradeDisputeListController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(tradeDisputeView, "Trade Disputes");
        } catch (IOException e) {
            showAlert("Error", "Could not load trade disputes: " + e.getMessage());
            System.err.println("Error in handleTradeDisputeButton: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTradeRequestButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TradeRequestList.fxml"));
            Parent tradeRequestView = loader.load();
            
            TradeRequestListController controller = loader.getController();
            controller.setUser(currentUser);
            
            loadContentInPlace(tradeRequestView, "Trade Requests");
        } catch (IOException e) {
            showAlert("Error", "Could not load trade requests: " + e.getMessage());
            System.err.println("Error in handleTradeRequestButton: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets the active navigation button by highlighting it
     * @param button The button to set as active
     */
    private void setActiveButton(Button button) {
        // Remove active style from previous button if exists
        if (lastActiveButton != null) {
            lastActiveButton.getStyleClass().remove("sidebar-button-active");
        }
        
        // Add active style to current button
        if (button != null) {
            button.getStyleClass().add("sidebar-button-active");
            lastActiveButton = button;
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
    }    /**
     * Loads content into the contentArea StackPane with proper transition
     * @param view The view to load
     * @param title The title/section name to set
     */
    private void loadContentInPlace(Parent view, String title) {
        // Clear existing content and set new content
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
        
        // Update the page title in the top bar
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(title);
        }
        
        // Update the window title to reflect the current section
        // Only try to update window title if scene is available
        if (contentArea.getScene() != null && contentArea.getScene().getWindow() != null) {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Sou9 NFT - " + title);
        }
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
    
    /**
     * Show an alert dialog with a message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * This method is called to configure the stage after the scene is fully initialized.
     * It sets the stage to fullscreen and applies other window-specific settings.
     */
    public void setStageFullScreen() {
        if (contentArea.getScene() != null && contentArea.getScene().getWindow() != null) {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            
            // Set to maximized (takes full screen but keeps taskbar visible)
            stage.setMaximized(true);
            
            // Alternative: true fullscreen (hides taskbar)
            // stage.setFullScreen(true);
        }
    }
}