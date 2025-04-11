package org.esprit.controllers;

import java.io.IOException;

import org.esprit.models.User;
import org.esprit.services.UserService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService;
    
    public LoginController() {
        userService = new UserService();
    }
    
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required.");
            return;
        }
        
        try {
            // Check if user exists with the provided email
            User user = userService.getByEmail(email);
            
            if (user != null && user.getPassword().equals(password)) {
                // Authentication successful - Navigate to profile page
                navigateToProfile(event, user);
            } else {
                showError("Invalid email or password.");
            }
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToProfile(ActionEvent event, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            // Pass the authenticated user to the profile controller
            ProfileController controller = loader.getController();
            controller.setUser(user);
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(profileView, 800, 600));
            stage.setTitle("NFT Marketplace - Profile");
            stage.show();
        } catch (IOException e) {
            showError("Error loading profile page: " + e.getMessage());
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
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}