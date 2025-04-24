package org.esprit.controllers;

import java.io.IOException;

import org.esprit.models.User;
import org.esprit.services.PasswordResetService;
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
import javafx.stage.Stage;

public class ResetPasswordController {

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button resetButton;
    
    private PasswordResetService passwordResetService;
    private String resetToken;
    private User user;
    
    public ResetPasswordController() {
        passwordResetService = new PasswordResetService();
    }
    
    public void initialize() {
        // The initialize method is called automatically by JavaFX
    }
    
    public void setResetToken(String token) {
        this.resetToken = token;
        
        // Validate the token and load the user
        user = passwordResetService.validatePasswordResetToken(token);
        if (user == null) {
            // Invalid or expired token
            showStatus("This password reset link is invalid or has expired. Please request a new one.", true);
            resetButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleResetPassword(ActionEvent event) {
        // Clear previous error messages
        passwordErrorLabel.setVisible(false);
        confirmPasswordErrorLabel.setVisible(false);
        statusLabel.setVisible(false);
        
        // Get and validate password fields
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        boolean hasErrors = false;
        
        // Validate password
        if (password.isEmpty()) {
            showError(passwordErrorLabel, "Password cannot be empty");
            hasErrors = true;
        } else if (password.length() < 6) {
            showError(passwordErrorLabel, "Password must be at least 6 characters");
            hasErrors = true;
        } else if (!isValidPassword(password)) {
            showError(passwordErrorLabel, "Password must contain at least one uppercase letter and one number");
            hasErrors = true;
        }
        
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordErrorLabel, "Passwords do not match");
            hasErrors = true;
        }
        
        if (hasErrors) {
            return;
        }
        
        // If token wasn't validated earlier, validate it now
        if (user == null) {
            user = passwordResetService.validatePasswordResetToken(resetToken);
            if (user == null) {
                showStatus("This password reset link is invalid or has expired. Please request a new one.", true);
                resetButton.setDisable(true);
                return;
            }
        }
        
        resetButton.setDisable(true);
        resetButton.setText("Processing...");
        
        // Hash the password and complete the reset
        String hashedPassword = PasswordHasher.hashPassword(password);
        boolean success = passwordResetService.completePasswordReset(resetToken, hashedPassword);
        
        if (success) {
            showStatus("Your password has been reset successfully!", false);
            
            // Redirect to login after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    
                    // Use JavaFX application thread to update UI
                    javafx.application.Platform.runLater(() -> {
                        try {
                            navigateToLogin(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showStatus("Failed to reset your password. Please try again.", true);
            resetButton.setDisable(false);
            resetButton.setText("Reset Password");
        }
    }
    
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView, 600, 400));
            stage.show();
        } catch (IOException e) {
            showStatus("Error loading login page: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        
        if (isError) {
            statusLabel.getStyleClass().remove("status-success");
            statusLabel.getStyleClass().add("status-error");
        } else {
            statusLabel.getStyleClass().remove("status-error");
            statusLabel.getStyleClass().add("status-success");
        }
    }
    
    private boolean isValidPassword(String password) {
        // Password must contain at least one uppercase letter and one number
        return password.matches(".*[A-Z].*") && password.matches(".*\\d.*");
    }
}