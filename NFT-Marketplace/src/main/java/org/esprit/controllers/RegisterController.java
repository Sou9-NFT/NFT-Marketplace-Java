package org.esprit.controllers;

import java.io.IOException;
import java.util.Map;

import org.esprit.models.User;
import org.esprit.models.User.ValidationResult;
import org.esprit.services.UserService;
import org.esprit.utils.IdenticonGenerator;
import org.esprit.utils.PasswordHasher;

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
    private Label nameErrorLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    @FXML
    private Label errorLabel; // Changed from statusMessageLabel to match the FXML

    private final UserService userService;
    
    public RegisterController() {
        userService = new UserService();
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(new Scene(loginView, 800, 600));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showError("Error loading login page: " + e.getMessage());
        }
    }
    
    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void hideFieldErrors() {
        nameErrorLabel.setVisible(false);
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        confirmPasswordErrorLabel.setVisible(false);
    }
    
    private void showError(String message) {
        errorLabel.setText(message); // Changed from statusMessageLabel to errorLabel
        errorLabel.setVisible(true); // Changed from statusMessageLabel to errorLabel
        errorLabel.getStyleClass().remove("status-success"); // Changed from statusMessageLabel to errorLabel
        errorLabel.getStyleClass().add("status-error"); // Changed from statusMessageLabel to errorLabel
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        hideFieldErrors();
        errorLabel.setVisible(false); // Changed from statusMessageLabel to errorLabel
        
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            showFieldError(confirmPasswordErrorLabel, "Passwords do not match");
            return;
        }
        
        // Validate input using User's validation
        User tempUser = new User(email, password, name);
        ValidationResult validationResult = tempUser.validate();
        
        if (!validationResult.isValid()) {
            boolean hasErrors = false;
            
            Map<String, String> errors = validationResult.getErrors();
            if (errors.containsKey("name")) {
                showFieldError(nameErrorLabel, errors.get("name"));
                hasErrors = true;
            }
            
            if (errors.containsKey("email")) {
                showFieldError(emailErrorLabel, errors.get("email"));
                hasErrors = true;
            }
            
            if (errors.containsKey("password")) {
                showFieldError(passwordErrorLabel, errors.get("password"));
                hasErrors = true;
            }
            
            // If any validation errors were found, stop here
            if (hasErrors) {
                return;
            }
        }
        
        try {
            // Check if user with this email already exists
            User existingUser = userService.getByEmail(email);
            if (existingUser != null) {
                showFieldError(emailErrorLabel, "A user with this email already exists");
                return;
            }
            
            // Create new user with form data and hash the password
            User newUser = new User(email, PasswordHasher.hashPassword(password), name);
            
            try {
                // Generate unique identicon based on email
                String uploadsDir = System.getProperty("user.dir") + "/src/main/resources/uploads";
                String identiconFilename = IdenticonGenerator.generateIdenticon(email, uploadsDir);
                
                // Set the path to be used by the application
                newUser.setProfilePicture("/uploads/" + identiconFilename);
                
            } catch (Exception e) {
                // If identicon generation fails, fall back to default profile image
                newUser.setProfilePicture("/assets/default/default_profile.jpg");
                System.err.println("Failed to generate identicon: " + e.getMessage());
            }
            
            // Add the user to the database
            userService.add(newUser);
            
            // Navigate to login screen with success message
            navigateToLogin(event, "Registration successful! Please log in.");
            
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToLogin(ActionEvent event, String successMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            LoginController controller = loader.getController();
            if (successMessage != null) {
                controller.showSuccess(successMessage);
            }
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView, 800, 600));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showError("Error loading login page: " + e.getMessage());
        }
    }
}