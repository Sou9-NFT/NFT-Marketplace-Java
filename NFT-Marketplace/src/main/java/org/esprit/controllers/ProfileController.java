package org.esprit.controllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.esprit.models.User;
import org.esprit.services.UserService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class ProfileController {

    @FXML
    private Label nameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label walletLabel;

    @FXML
    private Label githubLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label createdAtLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ImageView profileImageView;

    private User currentUser;
    private UserService userService;

    public ProfileController() {
        userService = new UserService();
    }

    public void initialize() {
        // This method is automatically called after the FXML file has been loaded
        // We'll populate user data in the setUser method instead
    }

    public void setUser(User user) {
        this.currentUser = user;
        displayUserInfo();
    }

    private void displayUserInfo() {
        if (currentUser != null) {
            nameLabel.setText(currentUser.getName());
            emailLabel.setText(currentUser.getEmail());

            // Handle optional fields
            walletLabel.setText(currentUser.getWalletAddress() != null && !currentUser.getWalletAddress().isEmpty()
                    ? currentUser.getWalletAddress()
                    : "Not set");

            githubLabel.setText(currentUser.getGithubUsername() != null && !currentUser.getGithubUsername().isEmpty()
                    ? currentUser.getGithubUsername()
                    : "Not set");

            balanceLabel.setText(currentUser.getBalance().toString());

            // Format the date
            if (currentUser.getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                createdAtLabel.setText(currentUser.getCreatedAt().format(formatter));
            } else {
                createdAtLabel.setText("Not available");
            }

            // Load profile picture
            loadProfilePicture();
        }
    }

    private void loadProfilePicture() {
        try {
            String profilePicPath = currentUser.getProfilePicture();
            if (profilePicPath == null || profilePicPath.isEmpty()) {
                // If null or empty, use default image
                profilePicPath = "/assets/default/default_profile.jpg";
            }

            Image profileImage;
            
            // Check if the profile picture is a URL (GitHub avatar) or a local resource
            if (profilePicPath.startsWith("http://") || profilePicPath.startsWith("https://")) {
                // For remote URLs (like GitHub avatars)
                profileImage = new Image(profilePicPath);
                System.out.println("Loading remote profile image from: " + profilePicPath);
            } else {
                // For local resource paths
                profileImage = new Image(getClass().getResourceAsStream(profilePicPath));
                System.out.println("Loading local profile image from: " + profilePicPath);
            }
            
            profileImageView.setImage(profileImage);

            // Apply styling to make the image circular
            profileImageView.setStyle(
                    "-fx-background-radius: 50%; " +
                            "-fx-background-color: white; " +
                            "-fx-border-radius: 50%; " +
                            "-fx-border-color: #dddddd; " +
                            "-fx-border-width: 2px;");

        } catch (Exception e) {
            System.err.println("Error loading profile picture: " + e.getMessage());
            e.printStackTrace();
            // Load default image on error
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/assets/default/default_profile.jpg"));
                profileImageView.setImage(defaultImage);
            } catch (Exception ex) {
                System.err.println("Error loading default profile picture: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleEditProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditProfile.fxml"));
            Parent editProfileView = loader.load();

            // Pass the current user to the edit profile controller
            EditProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();

            stage.setScene(new Scene(editProfileView, 800, 600));
            stage.show();
        } catch (IOException e) {
            showStatus("Error loading edit profile page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Navigate back to login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();

            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();

            stage.setScene(new Scene(loginView, 600, 400));
            stage.show();
        } catch (IOException e) {
            showStatus("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshUserData() {
        try {
            // Refresh user data from database
            User refreshedUser = userService.getById(currentUser.getId());
            if (refreshedUser != null) {
                this.currentUser = refreshedUser;
                displayUserInfo();
                showStatus("Profile updated successfully.");
            }
        } catch (Exception e) {
            showStatus("Error refreshing profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }
}