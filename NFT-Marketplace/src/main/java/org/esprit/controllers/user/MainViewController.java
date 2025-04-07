package org.esprit.controllers.user;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.esprit.main.MainApp;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML
    private StackPane contentArea;
    
    @FXML
    private Label userInfoLabel;
    
    @FXML
    private Menu userMenu;
    
    @FXML
    private Menu adminMenu;
    
    @FXML
    private MenuItem loginMenuItem;
    
    @FXML
    private MenuItem registerMenuItem;
    
    @FXML
    private MenuItem profileMenuItem;
    
    @FXML
    private MenuItem logoutMenuItem;
    
    @FXML
    private Button myNFTsButton;
    
    @FXML
    private Button myAuctionsButton;
    
    @FXML
    private Button topupButton;
    
    private UserService userService;
    private static User currentUser = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        updateUIForUser();
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    public void updateUIForUser() {
        boolean isLoggedIn = currentUser != null;
        boolean isAdmin = isLoggedIn && currentUser.getRoles() != null && 
                         currentUser.getRoles().contains("ROLE_ADMIN");
        
        // Update menu items
        loginMenuItem.setVisible(!isLoggedIn);
        registerMenuItem.setVisible(!isLoggedIn);
        profileMenuItem.setVisible(isLoggedIn);
        logoutMenuItem.setVisible(isLoggedIn);
        
        // Update sidebar buttons
        myNFTsButton.setVisible(isLoggedIn);
        myAuctionsButton.setVisible(isLoggedIn);
        topupButton.setVisible(isLoggedIn);
        
        // Update admin menu
        adminMenu.setVisible(isAdmin);
        
        // Update user info label
        if (isLoggedIn) {
            userInfoLabel.setText("Logged in as: " + currentUser.getName() + 
                                  " | Balance: " + currentUser.getBalance() + " ETH");
        } else {
            userInfoLabel.setText("Not logged in");
        }
    }
    
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load the requested page.", e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Event Handlers
    
    @FXML
    private void handleExit() {
        Platform.exit();
    }
    
    @FXML
    private void handleLogin() {
        loadContent("/fxml/user/Login.fxml");
    }
    
    @FXML
    private void handleRegister() {
        loadContent("/fxml/user/Register.fxml");
    }
    
    @FXML
    private void handleProfile() {
        loadContent("/fxml/user/Profile.fxml");
    }
    
    @FXML
    private void handleLogout() {
        currentUser = null;
        updateUIForUser();
        
        // Return to home view
        try {
            MainApp.loadMainView();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load main view.", e.getMessage());
        }
    }
    
    @FXML
    private void handleHome() {
        try {
            MainApp.loadMainView();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load main view.", e.getMessage());
        }
    }
    
    @FXML
    private void handleBrowseNFTs() {
        // This will be implemented in the NFT module
        showAlert(Alert.AlertType.INFORMATION, "Info", "Coming Soon", 
                  "The NFT browsing feature will be implemented in the future.");
    }
    
    @FXML
    private void handleMyNFTs() {
        // This will be implemented in the NFT module
        showAlert(Alert.AlertType.INFORMATION, "Info", "Coming Soon", 
                  "The My NFTs feature will be implemented in the future.");
    }
    
    @FXML
    private void handleMyAuctions() {
        // This will be implemented in the Auction module
        showAlert(Alert.AlertType.INFORMATION, "Info", "Coming Soon", 
                  "The My Auctions feature will be implemented in the future.");
    }
    
    @FXML
    private void handleTopup() {
        loadContent("/fxml/user/Topup.fxml");
    }
    
    @FXML
    private void handleManageUsers() {
        loadContent("/fxml/admin/UserManagement.fxml");
    }
    
    @FXML
    private void handleTopupRequests() {
        loadContent("/fxml/admin/TopupRequests.fxml");
    }
}