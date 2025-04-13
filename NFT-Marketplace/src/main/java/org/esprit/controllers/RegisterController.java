package org.esprit.controllers;

import java.io.IOException;
import java.util.Map;

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
    
    // Field-specific error labels
    @FXML
    private Label nameErrorLabel;
    
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    private UserService userService;
    
    public RegisterController() {
        userService = new UserService();
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        // Clear all error messages first
        clearAllErrors();
        
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        boolean hasErrors = false;
        
        // Validation for name field
        if (name.isEmpty()) {
            showFieldError(nameErrorLabel, "Name cannot be empty");
            hasErrors = true;
        }
        
        // Validation for email field
        if (email.isEmpty()) {
            showFieldError(emailErrorLabel, "Email cannot be empty");
            hasErrors = true;
        } else if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showFieldError(emailErrorLabel, "Invalid email format");
            hasErrors = true;
        }
        
        // Validation for password
        if (password.isEmpty()) {
            showFieldError(passwordErrorLabel, "Password cannot be empty");
            hasErrors = true;
        } else if (password.length() < 6) {
            showFieldError(passwordErrorLabel, "Password must be at least 6 characters");
            hasErrors = true;
        }
        
        // Basic confirmation password check
        if (!password.equals(confirmPassword)) {
            showFieldError(confirmPasswordErrorLabel, "Passwords do not match");
            hasErrors = true;
        }
        
        // If basic validations failed, stop here
        if (hasErrors) {
            return;
        }
        
        try {
            // Check if user with this email already exists
            User existingUser = userService.getByEmail(email);
            if (existingUser != null) {
                showFieldError(emailErrorLabel, "A user with this email already exists");
                return;
            }
            
            // Create new user with form data
            User newUser = new User(email, password, name);
            
            // Set default profile picture
            newUser.setProfilePicture("/assets/default/default_profile.jpg");
            
            // Validate user at entity level
            ValidationResult validationResult = newUser.validate();
            
            if (!validationResult.isValid()) {
                // Show validation errors on specific fields
                Map<String, String> errors = validationResult.getErrors();
                
                if (errors.containsKey("name")) {
                    showFieldError(nameErrorLabel, errors.get("name"));
                }
                if (errors.containsKey("email")) {
                    showFieldError(emailErrorLabel, errors.get("email"));
                }
                if (errors.containsKey("password")) {
                    showFieldError(passwordErrorLabel, errors.get("password"));
                }
                
                // If other errors exist, show them in the general error label
                for (Map.Entry<String, String> error : errors.entrySet()) {
                    if (!error.getKey().equals("name") && !error.getKey().equals("email") && !error.getKey().equals("password")) {
                        showError("Error: " + error.getValue());
                    }
                }
                
                return;
            }
            
            // If validation passed, save the user
            userService.add(newUser);
            
            // Show success and navigate to login
            showSuccess("Registration successful! Please login.");
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
    
    // Show error in the specific field error label
    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    // Show general error in the main error label
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.getStyleClass().removeAll("status-success");
        errorLabel.getStyleClass().add("status-error");
    }
    
    // Show success message in the main error label
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.getStyleClass().removeAll("status-error");
        errorLabel.getStyleClass().add("status-success");
    }
    
    // Clear all error messages
    private void clearAllErrors() {
        nameErrorLabel.setVisible(false);
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        confirmPasswordErrorLabel.setVisible(false);
        errorLabel.setVisible(false);
    }
}