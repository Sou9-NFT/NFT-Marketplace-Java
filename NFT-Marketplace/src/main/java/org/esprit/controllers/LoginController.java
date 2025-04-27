package org.esprit.controllers;

import java.io.IOException;

import org.esprit.components.CryptoTickerComponent;
import org.esprit.models.User;
import org.esprit.services.UserService;
import org.esprit.utils.PasswordHasher;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    // Field-specific error labels
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Button githubLoginButton;
    
    @FXML
    private HBox cryptoTickerContainer;
    
    @FXML
    private ScrollPane cryptoTickerScroll;
    
    private UserService userService;
    private CryptoTickerComponent cryptoTicker;
    
    public LoginController() {
        userService = new UserService();
    }
    
    public void initialize() {
        // Initialize cryptocurrency ticker
        cryptoTicker = new CryptoTickerComponent();
        
        if (cryptoTickerContainer != null) {
            // Clear any existing content and add the crypto ticker directly to the container
            cryptoTickerContainer.getChildren().clear();
            cryptoTickerContainer.getChildren().add(cryptoTicker.getView());
            
            // Make sure the ticker takes full width
            HBox.setHgrow(cryptoTicker.getView(), Priority.ALWAYS);
            cryptoTicker.getView().setMaxWidth(Double.MAX_VALUE);
            cryptoTicker.getView().setFitToWidth(true);
        }
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        // Clear all error messages first
        clearAllErrors();
        
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        boolean hasErrors = false;
        
        // Validate email field
        if (email.isEmpty()) {
            showFieldError(emailErrorLabel, "Email cannot be empty");
            hasErrors = true;
        } else if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showFieldError(emailErrorLabel, "Invalid email format");
            hasErrors = true;
        }
        
        // Validate password field
        if (password.isEmpty()) {
            showFieldError(passwordErrorLabel, "Password cannot be empty");
            hasErrors = true;
        }
        
        // If basic validations failed, stop here
        if (hasErrors) {
            return;
        }
        
        try {
            // Check if user exists with the provided email
            User user = userService.getByEmail(email);
            
            if (user != null && PasswordHasher.verifyPassword(password, user.getPassword())) {
                // Authentication successful
                
                // Check if user has admin role
                if (user.getRoles().contains("ROLE_ADMIN")) {
                    // Route to admin dashboard
                    navigateToAdminDashboard(event, user);
                } else {
                    // Route to normal user profile
                    navigateToProfile(event, user);
                }
            } else {
                // Authentication failed
                if (user == null) {
                    showFieldError(emailErrorLabel, "No account found with this email");
                } else {
                    showFieldError(passwordErrorLabel, "Incorrect password");
                }
            }
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle GitHub OAuth login button click
     * This method will redirect the user to the GitHub OAuth authorization page
     */
    @FXML
    private void handleGitHubLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GitHubOAuth.fxml"));
            Parent oauthView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(oauthView, 600, 400));
            stage.setTitle("NFT Marketplace - GitHub OAuth");
            stage.show();
        } catch (IOException e) {
            showError("Error loading GitHub OAuth page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToAdminDashboard(ActionEvent event, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            Parent adminView = loader.load();
              // Pass the authenticated admin user to the admin dashboard controller
            AdminDashboardController controller = loader.getController();
            controller.setCurrentUser(user);  // This sets the admin user for both user management and blog creation
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(adminView, 900, 600));
            stage.setTitle("NFT Marketplace - Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            showError("Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToProfile(ActionEvent event, User user) {
        try {
            // Load UserDashboard view for regular users
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDashboard.fxml"));
            Parent userDashboardView = loader.load();
            
            // Pass the authenticated user to the user dashboard controller
            UserDashboardController controller = loader.getController();
            controller.setCurrentUser(user);
            
            Scene currentScene = ((Node) event.getSource()).getScene();            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(userDashboardView, 800, 600));
            stage.setTitle("NFT Marketplace - User Dashboard");
            
            // Make the stage maximized by default
            stage.setMaximized(true);
            
            stage.show();
            
            // Call the controller's method to ensure fullscreen is set
            controller.setStageFullScreen();
        } catch (IOException e) {
            showError("Error loading user dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void switchToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            Parent registerView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            Scene scene = new Scene(registerView, 600, 450);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Error loading register page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void switchToForgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ForgotPassword.fxml"));
            Parent forgotPasswordView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(forgotPasswordView, 600, 400));
            stage.setTitle("NFT Marketplace - Forgot Password");
            stage.show();
        } catch (IOException e) {
            showError("Error loading forgot password page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Show error in the specific field error label
    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    // Show general error in the main error label
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.getStyleClass().remove("status-success");
        errorLabel.getStyleClass().add("status-error");
    }
    
    // Show success message in the main error label
    public void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.getStyleClass().remove("status-error");
        errorLabel.getStyleClass().add("status-success");
    }
    
    // Clear all error messages
    private void clearAllErrors() {
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        errorLabel.setVisible(false);
    }
}