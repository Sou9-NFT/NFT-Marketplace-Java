package org.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML
    private ImageView profileImageView;
    
    @FXML
    private Label nameLabel;
    
    @FXML
    private Label emailLabel;
    
    @FXML
    private Label balanceLabel;
    
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField walletAddressField;
    
    @FXML
    private TextField githubUsernameField;
    
    @FXML
    private PasswordField currentPasswordField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService;
    private User currentUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        currentUser = MainViewController.getCurrentUser();
        
        if (currentUser != null) {
            loadUserData();
        } else {
            // Handle error - no user logged in
            errorLabel.setText("No user logged in");
        }
        
        clearStatusMessages();
    }
    
    private void loadUserData() {
        // Set header information
        nameLabel.setText(currentUser.getName());
        emailLabel.setText(currentUser.getEmail());
        balanceLabel.setText("Balance: " + currentUser.getBalance() + " ETH");
        
        // Load profile picture if exists
        if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                Image profileImage = new Image(currentUser.getProfilePicture());
                profileImageView.setImage(profileImage);
            } catch (Exception e) {
                System.err.println("Could not load profile image: " + e.getMessage());
            }
        }
        
        // Set form fields
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        walletAddressField.setText(currentUser.getWalletAddress() != null ? currentUser.getWalletAddress() : "");
        githubUsernameField.setText(currentUser.getGithubUsername() != null ? currentUser.getGithubUsername() : "");
        
        // Clear password fields
        clearPasswordFields();
    }
    
    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
    
    private void clearStatusMessages() {
        statusLabel.setText("");
        errorLabel.setText("");
    }
    
    @FXML
    private void handleSaveProfile() {
        clearStatusMessages();
        
        // Get form data
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String walletAddress = walletAddressField.getText().trim();
        String githubUsername = githubUsernameField.getText().trim();
        
        // Validate input
        if (name.isEmpty() || email.isEmpty()) {
            errorLabel.setText("Name and email are required");
            return;
        }
        
        try {
            // Check if email is already in use by another user
            if (!email.equals(currentUser.getEmail())) {
                User userWithEmail = userService.getByEmail(email);
                if (userWithEmail != null && userWithEmail.getId() != currentUser.getId()) {
                    errorLabel.setText("Email already in use by another user");
                    return;
                }
            }
            
            // Update user data
            currentUser.setName(name);
            currentUser.setEmail(email);
            currentUser.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
            currentUser.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
            
            // Save user to database
            userService.update(currentUser);
            
            // Update UI
            loadUserData();
            
            // Update main view
            MainViewController mainController = (MainViewController) 
                nameField.getScene().getWindow().getUserData();
            if (mainController != null) {
                mainController.updateUIForUser();
            }
            
            statusLabel.setText("Profile updated successfully");
        } catch (Exception e) {
            errorLabel.setText("Failed to update profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleChangePassword() {
        clearStatusMessages();
        
        // Get form data
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate input
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("All password fields are required");
            return;
        }
        
        if (!currentPassword.equals(currentUser.getPassword())) {
            errorLabel.setText("Current password is incorrect");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            errorLabel.setText("New passwords do not match");
            return;
        }
        
        try {
            // Update user password
            currentUser.setPassword(newPassword);
            
            // Save user to database
            userService.update(currentUser);
            
            // Clear password fields
            clearPasswordFields();
            
            statusLabel.setText("Password changed successfully");
        } catch (Exception e) {
            errorLabel.setText("Failed to change password: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleUploadProfilePicture() {
        clearStatusMessages();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        
        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                // In a real application, you would:
                // 1. Upload the image to a server or save it locally
                // 2. Get the URL or path to the saved image
                // 3. Store this URL in the user's profile_picture field
                
                // For this demo, we'll just use the local file path
                String imagePath = selectedFile.toURI().toString();
                
                // Update UI
                profileImageView.setImage(new Image(imagePath));
                
                // Update user data (in a real app, this would be the URL from the server)
                currentUser.setProfilePicture(imagePath);
                
                // Save user to database
                userService.update(currentUser);
                
                statusLabel.setText("Profile picture updated successfully");
            } catch (Exception e) {
                errorLabel.setText("Failed to update profile picture: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}