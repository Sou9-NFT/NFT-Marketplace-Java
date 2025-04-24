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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;
    
    @FXML
    private Label emailErrorLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button actionButton;
    
    @FXML
    private Label instructionsLabel;
    
    @FXML
    private VBox emailSection;
    
    @FXML
    private VBox resetCodeSection;
    
    @FXML
    private TextField resetCodeField;
    
    @FXML
    private Label resetCodeErrorLabel;
    
    @FXML
    private VBox newPasswordSection;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label passwordErrorLabel;
    
    @FXML
    private Label confirmPasswordErrorLabel;
    
    private PasswordResetService passwordResetService;
    private String currentEmail; // To store the email entered in the first step
    private enum ResetStage { EMAIL, RESET_CODE, NEW_PASSWORD }
    private ResetStage currentStage = ResetStage.EMAIL;
    
    public ForgotPasswordController() {
        passwordResetService = new PasswordResetService();
    }
    
    /**
     * Handle the main action button click based on the current reset stage
     */
    @FXML
    private void handleAction(ActionEvent event) {
        // Clear previous error messages
        clearErrors();
        
        switch (currentStage) {
            case EMAIL:
                handleEmailSubmission();
                break;
            case RESET_CODE:
                handleResetCodeVerification();
                break;
            case NEW_PASSWORD:
                handlePasswordReset(event);
                break;
        }
    }
    
    /**
     * Handle the first stage: email submission to request a reset code
     */
    private void handleEmailSubmission() {
        String email = emailField.getText().trim();
        
        // Validate email
        if (email.isEmpty()) {
            showError(emailErrorLabel, "Email cannot be empty");
            return;
        } else if (!isValidEmail(email)) {
            showError(emailErrorLabel, "Invalid email format");
            return;
        }
        
        // Store the email for later stages
        currentEmail = email;
        
        actionButton.setDisable(true);
        actionButton.setText("Processing...");
        
        // Initiate password reset
        boolean success = passwordResetService.initiatePasswordReset(email);
        
        if (success) {
            // Update UI for the next stage (code entry)
            advanceToResetCode();
            showStatus("Reset code sent to your email. Please check your inbox and paste the code below.", false);
        } else {
            showStatus("Failed to send reset code. Please check your email and try again.", true);
            actionButton.setDisable(false);
            actionButton.setText("Send Reset Code");
        }
    }
    
    /**
     * Handle the second stage: reset code verification
     */
    private void handleResetCodeVerification() {
        String resetCode = resetCodeField.getText().trim();
        
        if (resetCode.isEmpty()) {
            showError(resetCodeErrorLabel, "Reset code cannot be empty");
            return;
        }
        
        actionButton.setDisable(true);
        actionButton.setText("Verifying...");
        
        // Validate the reset code
        User user = passwordResetService.validatePasswordResetToken(resetCode);
        
        if (user != null && user.getEmail().equals(currentEmail)) {
            // Code is valid, move to password reset stage
            advanceToNewPassword();
            showStatus("Code verified! Please enter your new password.", false);
        } else {
            showError(resetCodeErrorLabel, "Invalid or expired reset code");
            actionButton.setDisable(false);
            actionButton.setText("Verify Code");
        }
    }
    
    /**
     * Handle the final stage: setting the new password
     */
    private void handlePasswordReset(ActionEvent event) {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String resetCode = resetCodeField.getText().trim();
        
        // Basic validation
        if (newPassword.isEmpty()) {
            showError(passwordErrorLabel, "Password cannot be empty");
            return;
        }
        
        if (newPassword.length() < 6) {
            showError(passwordErrorLabel, "Password must be at least 6 characters long");
            return;
        }
        
        if (newPassword.length() > 50) {
            showError(passwordErrorLabel, "Password cannot be longer than 50 characters");
            return;
        }
        
        // Password complexity validation
        if (!isValidPassword(newPassword)) {
            showError(passwordErrorLabel, "Password must contain at least one uppercase letter and one number");
            return;
        }
        
        // Password confirmation validation
        if (!newPassword.equals(confirmPassword)) {
            showError(confirmPasswordErrorLabel, "Passwords do not match");
            return;
        }
        
        actionButton.setDisable(true);
        actionButton.setText("Resetting...");
        
        // Hash the new password
        String hashedPassword = PasswordHasher.hashPassword(newPassword);
        
        // Complete the password reset
        boolean success = passwordResetService.completePasswordReset(resetCode, hashedPassword);
        
        if (success) {
            showStatus("Password reset successful! You can now log in with your new password.", false);
            actionButton.setText("Return to Login");
            actionButton.setOnAction(e -> switchToLogin(e));
            actionButton.setDisable(false);
        } else {
            showStatus("Failed to reset password. The reset code may have expired.", true);
            actionButton.setDisable(false);
            actionButton.setText("Reset Password");
        }
    }
    
    /**
     * Validates the password according to the rules in User.java
     * Password must contain at least one uppercase letter and one number
     */
    private boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[A-Z])(?=.*\\d).+$";
        return password.matches(passwordRegex);
    }
    
    /**
     * Update UI to show the reset code input
     */
    private void advanceToResetCode() {
        currentStage = ResetStage.RESET_CODE;
        
        // Hide email section, show code section
        emailSection.setVisible(false);
        emailSection.setManaged(false);
        resetCodeSection.setVisible(true);
        resetCodeSection.setManaged(true);
        
        // Update button and instructions
        actionButton.setText("Verify Code");
        actionButton.setDisable(false);
        instructionsLabel.setText("Enter the reset code from your email");
    }
    
    /**
     * Update UI to show the new password input
     */
    private void advanceToNewPassword() {
        currentStage = ResetStage.NEW_PASSWORD;
        
        // Show password section
        newPasswordSection.setVisible(true);
        newPasswordSection.setManaged(true);
        
        // Update button and instructions
        actionButton.setText("Reset Password");
        actionButton.setDisable(false);
        instructionsLabel.setText("Create a new password for your account");
    }
    
    /**
     * Clear all error messages
     */
    private void clearErrors() {
        emailErrorLabel.setVisible(false);
        resetCodeErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        confirmPasswordErrorLabel.setVisible(false);
        statusLabel.setVisible(false);
    }
    
    @FXML
    private void switchToLogin(ActionEvent event) {
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
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        return email.matches(emailRegex);
    }
}