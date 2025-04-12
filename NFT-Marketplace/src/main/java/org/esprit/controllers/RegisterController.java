package org.esprit.controllers;

import java.io.IOException;

import org.esprit.models.User;
import org.esprit.models.User.ValidationResult;
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

public class RegisterController {

    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService;
    
    public RegisterController() {
        userService = new UserService();
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Basic confirmation password check (this is still in controller as it involves 2 fields)
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }
        
        try {
            // Check if user with this email already exists
            User existingUser = userService.getByEmail(email);
            if (existingUser != null) {
                showError("A user with this email already exists.");
                return;
            }
            
            // Create new user with form data
            User newUser = new User(email, password, name);
            
            // Set default profile picture
            newUser.setProfilePicture("/assets/default/default_profile.jpg");
            
            // Validate user at entity level
            ValidationResult validationResult = newUser.validate();
            
            if (!validationResult.isValid()) {
                // Show the first validation error found
                for (String errorMessage : validationResult.getErrors().values()) {
                    showError(errorMessage);
                    return;
                }
            }
            
            // If validation passed, save the user
            userService.add(newUser);
            
            // Show success and navigate to login
            showError("Registration successful! Please login.");
            switchToLogin(event);
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            Scene scene = new Scene(loginView, 600, 400);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Error loading login page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}