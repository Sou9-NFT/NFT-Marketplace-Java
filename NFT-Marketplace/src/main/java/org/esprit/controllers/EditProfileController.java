package org.esprit.controllers;

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
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.io.IOException;

public class EditProfileController {

    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField currentPasswordField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField walletField;
    
    @FXML
    private TextField githubField;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private UserService userService;
    
    public EditProfileController() {
        userService = new UserService();
    }
    
    public void initialize() {
        // This method is automatically called after the FXML file has been loaded
        // We'll populate fields in the setUser method
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        populateFields();
    }
    
    private void populateFields() {
        if (currentUser != null) {
            nameField.setText(currentUser.getName());
            emailField.setText(currentUser.getEmail());
            
            // Leave password fields empty for security
            
            // Set optional fields if they exist
            if (currentUser.getWalletAddress() != null) {
                walletField.setText(currentUser.getWalletAddress());
            }
            
            if (currentUser.getGithubUsername() != null) {
                githubField.setText(currentUser.getGithubUsername());
            }
        }
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String walletAddress = walletField.getText().trim();
        String githubUsername = githubField.getText().trim();
        
        // Basic validation
        if (name.isEmpty() || email.isEmpty()) {
            showStatus("Name and email are required.");
            return;
        }
        
        // Email validation (simple check)
        if (!email.contains("@") || !email.contains(".")) {
            showStatus("Please enter a valid email address.");
            return;
        }
        
        try {
            // Check if user wants to change password
            if (!newPassword.isEmpty()) {
                // Verify current password
                if (!currentUser.getPassword().equals(currentPassword)) {
                    showStatus("Current password is incorrect.");
                    return;
                }
                
                // Check if new passwords match
                if (!newPassword.equals(confirmPassword)) {
                    showStatus("New passwords do not match.");
                    return;
                }
                
                // Password validation (simple check)
                if (newPassword.length() < 6) {
                    showStatus("New password must be at least 6 characters.");
                    return;
                }
                
                // Update password
                currentUser.setPassword(newPassword);
            }
            
            // Check if the email is being changed and if it's already in use
            if (!email.equals(currentUser.getEmail())) {
                User existingUser = userService.getByEmail(email);
                if (existingUser != null && existingUser.getId() != currentUser.getId()) {
                    showStatus("This email is already in use by another account.");
                    return;
                }
            }
            
            // Update other fields
            currentUser.setName(name);
            currentUser.setEmail(email);
            currentUser.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
            currentUser.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
            
            // Save changes to database
            userService.update(currentUser);
            
            // Return to profile page with updated user
            navigateToProfile(event);
        } catch (Exception e) {
            showStatus("Error updating profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        navigateToProfile(event);
    }
    
    private void navigateToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            // Pass the user to the profile controller
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);
            
            // If we just updated the profile, show a success message
            if (statusLabel.isVisible() && !statusLabel.getText().startsWith("Error")) {
                controller.refreshUserData();
            }
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(profileView, 800, 600));
            stage.show();
        } catch (IOException e) {
            showStatus("Error navigating back to profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }
}