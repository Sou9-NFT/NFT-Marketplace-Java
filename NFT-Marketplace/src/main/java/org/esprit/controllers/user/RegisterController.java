package org.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField walletAddressField;
    
    @FXML
    private TextField githubUsernameField;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        clearFields();
    }
    
    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        walletAddressField.clear();
        githubUsernameField.clear();
        errorLabel.setText("");
    }
    
    @FXML
    private void handleRegister() {
        // Get form data
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String walletAddress = walletAddressField.getText().trim();
        String githubUsername = githubUsernameField.getText().trim();
        
        // Validate input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Name, email and password are required");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }
        
        try {
            // Check if email is already in use
            User existingUser = userService.getByEmail(email);
            if (existingUser != null) {
                errorLabel.setText("Email already in use");
                return;
            }
            
            // Create new user
            User newUser = new User(email, password, name);
            if (!walletAddress.isEmpty()) {
                newUser.setWalletAddress(walletAddress);
            }
            if (!githubUsername.isEmpty()) {
                newUser.setGithubUsername(githubUsername);
            }
            
            // Save user to database
            userService.add(newUser);
            
            // Auto-login the new user
            MainViewController.setCurrentUser(newUser);
            
            // Get the main controller and update UI
            MainViewController mainController = (MainViewController) 
                nameField.getScene().getWindow().getUserData();
            if (mainController != null) {
                mainController.updateUIForUser();
            }
            
            // Clear the form and go back to main view
            clearFields();
            handleCancel();
        } catch (Exception e) {
            errorLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel() {
        // Clear the form fields and return to main view
        clearFields();
        
        // Get the main controller to clear the content area
        MainViewController mainController = (MainViewController) 
            nameField.getScene().getWindow().getUserData();
        if (mainController != null) {
            try {
                mainController.updateUIForUser();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleLoginLink() {
        // Navigate to login form
        MainViewController mainController = (MainViewController) 
            nameField.getScene().getWindow().getUserData();
        if (mainController != null) {
            try {
                // Use reflection to call the handleLogin method
                java.lang.reflect.Method method = MainViewController.class.getDeclaredMethod("handleLogin");
                method.setAccessible(true);
                method.invoke(mainController);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}