package org.esprit.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.esprit.models.User;
import org.esprit.services.UserService;
import org.esprit.utils.GitHubOAuthService;
import org.esprit.utils.PasswordHasher;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class GitHubOAuthController {

    @FXML
    private WebView webView;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button cancelButton;
    
    private final UserService userService;
    
    public GitHubOAuthController() {
        userService = new UserService();
    }
    
    public void initialize() {
        // Show the progress indicator while loading
        progressIndicator.setVisible(true);
        statusLabel.setText("Redirecting to GitHub...");
        
        // Initialize the WebView with GitHub OAuth page
        Platform.runLater(() -> {
            WebEngine engine = webView.getEngine();
            
            // Set up a change listener to detect the callback URL
            engine.locationProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.startsWith(org.esprit.utils.GitHubOAuthConfig.getRedirectUri())) {
                    handleCallback(newValue);
                }
            });
            
            // Load the GitHub authorization URL
            engine.load(GitHubOAuthService.getAuthorizationUrl());
        });
    }
    
    private void handleCallback(String url) {
        try {
            statusLabel.setText("Processing GitHub authorization...");
            
            // Parse the callback URL
            URI uri = new URI(url);
            String query = uri.getQuery();
            
            // Extract the authorization code and state
            String code = null;
            String state = null;
            
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        
                        if ("code".equals(key)) {
                            code = value;
                        } else if ("state".equals(key)) {
                            state = value;
                        }
                    }
                }
            }
            
            if (code != null && state != null) {
                // Exchange the code for an access token
                String accessToken = GitHubOAuthService.getAccessToken(code, state);
                
                // Get user information from GitHub
                Map<String, String> userInfo = GitHubOAuthService.getUserInfo(accessToken);
                
                // Process the user information
                processGitHubUser(userInfo);
            } else {
                statusLabel.setText("Error: Missing authorization code or state parameter");
            }
            
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            // Use logger instead of printStackTrace in production
            System.err.println("Error in GitHub OAuth callback: " + e.getMessage());
        }
    }
    
    private void processGitHubUser(Map<String, String> userInfo) throws Exception {
        // Check if the user already exists in our database
        String githubUsername = userInfo.get("login");
        User existingUser = findUserByGitHubUsername(githubUsername);
        
        if (existingUser != null) {
            // User already exists, log them in
            loginExistingUser(existingUser);
        } else {
            // User doesn't exist, create a new account
            createNewUserFromGitHub(userInfo);
        }
    }
    
    private User findUserByGitHubUsername(String githubUsername) throws Exception {
        // Search for existing users with the same GitHub username
        for (User user : userService.getAll()) {
            if (githubUsername.equals(user.getGithubUsername())) {
                return user;
            }
        }
        return null;
    }
    
    private void loginExistingUser(User user) {
        try {
            statusLabel.setText("Logging in with existing account...");
            
            // Navigate to the appropriate dashboard based on user role
            if (user.getRoles().contains("ROLE_ADMIN")) {
                navigateToAdminDashboard(user);
            } else {
                navigateToUserDashboard(user);
            }
        } catch (Exception e) {
            statusLabel.setText("Error logging in: " + e.getMessage());
            // Use logger instead of printStackTrace in production
            System.err.println("Error in login: " + e.getMessage());
        }
    }
    
    private void createNewUserFromGitHub(Map<String, String> userInfo) throws Exception {
        statusLabel.setText("Creating new account from GitHub profile...");
        
        // Extract user information
        String githubUsername = userInfo.get("login");
        String name = userInfo.get("name");
        String email = userInfo.get("email");
        String avatarUrl = userInfo.get("avatar_url");
        
        // If GitHub doesn't provide an email, generate one
        if (email == null || email.isEmpty()) {
            email = githubUsername + "@github.example.com";
        }
        
        // If GitHub doesn't provide a name, use the username
        if (name == null || name.isEmpty()) {
            name = githubUsername;
        }
        
        // Generate a random password (the user won't need to know this as they'll use GitHub to login)
        String randomPassword = UUID.randomUUID().toString();
        String hashedPassword = PasswordHasher.hashPassword(randomPassword);
        
        // Create new user
        User newUser = new User(email, hashedPassword, name);
        newUser.setGithubUsername(githubUsername);
        
        // Set default profile picture or GitHub avatar if available
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            newUser.setProfilePicture(avatarUrl);
        } else {
            newUser.setProfilePicture("/assets/default/default_profile.jpg");
        }
        
        // Set default balance
        newUser.setBalance(new BigDecimal("0.00"));
        
        // Add user role
        List<String> roles = new ArrayList<>();
        roles.add("ROLE_USER");
        newUser.setRoles(roles);
        
        // Add user to database
        userService.add(newUser);
        
        // Navigate to user dashboard
        navigateToUserDashboard(newUser);
    }
    
    private void navigateToAdminDashboard(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
        Parent adminView = loader.load();
        
        AdminDashboardController controller = loader.getController();
        controller.setCurrentUser(user);
        
        Scene currentScene = webView.getScene();
        Stage stage = (Stage) currentScene.getWindow();
        
        stage.setScene(new Scene(adminView, 900, 600));
        stage.setTitle("NFT Marketplace - Admin Dashboard");
        stage.show();
    }
    
    private void navigateToUserDashboard(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDashboard.fxml"));
        Parent userDashboardView = loader.load();
        
        UserDashboardController controller = loader.getController();
        controller.setCurrentUser(user);
        
        Scene currentScene = webView.getScene();
        Stage stage = (Stage) currentScene.getWindow();
        
        stage.setScene(new Scene(userDashboardView, 800, 600));
        stage.setTitle("NFT Marketplace - User Dashboard");
        stage.show();
    }
    
    @FXML
    private void handleCancel() {
        try {
            // Navigate back to login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            Scene currentScene = webView.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView, 600, 400));
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Error returning to login: " + e.getMessage());
            // Use logger instead of printStackTrace in production
            System.err.println("Error navigating to login: " + e.getMessage());
        }
    }
}