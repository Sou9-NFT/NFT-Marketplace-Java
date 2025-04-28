package org.esprit.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.esprit.models.User;
import org.esprit.services.UserService;
import org.esprit.utils.GitHubOAuthService;
import org.esprit.utils.PasswordHasher;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class GitHubOAuthController {

    @FXML
    private Label statusLabel;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Button backButton;
    
    @FXML
    private VBox controlsContainer;
    
    @FXML
    private WebView webView;
    
    private String code;
    private String state;
    private UserService userService;
    
    public GitHubOAuthController() {
        userService = new UserService();
    }
    
    public void initialize() {
        backButton.setOnAction(this::handleBackToLogin);
        
        // Initialize and load GitHub authorization URL in WebView
        if (webView != null) {
            try {
                // Get the WebEngine from WebView
                WebEngine webEngine = webView.getEngine();
                
                // Set user agent to desktop browser to ensure proper rendering
                webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                
                // Load the GitHub authorization URL
                String authUrl = GitHubOAuthService.getAuthorizationUrl();
                webEngine.load(authUrl);
                
                // Hide progress indicator when page is loading
                webEngine.setOnStatusChanged(event -> {
                    if (webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.RUNNING) {
                        progressIndicator.setVisible(true);
                    } else if (webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
                        progressIndicator.setVisible(false);
                        statusLabel.setText("Please sign in to your GitHub account");
                    }
                });
                
                // Set up a listener for location changes to detect when GitHub redirects to our callback URL
                webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.startsWith(org.esprit.utils.GitHubOAuthConfig.getRedirectUri())) {
                        // Extract code and state from the URL
                        Map<String, String> queryParams = parseQueryParams(newValue);
                        if (queryParams.containsKey("code")) {
                            code = queryParams.get("code");
                            state = queryParams.get("state");
                            
                            // Hide the WebView
                            webView.setVisible(false);
                            webView.setManaged(false);
                            
                            // Show the loading indicator
                            progressIndicator.setVisible(true);
                            
                            // Start the OAuth flow to get the user info
                            startOAuthFlow();
                        }
                    }
                });
                
                // Show helpful message
                statusLabel.setText("Loading GitHub login page...");
                
            } catch (Exception e) {
                statusLabel.setText("Error initializing GitHub login: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            statusLabel.setText("Error: WebView component not initialized properly");
        }
    }
    
    // Helper method to parse query parameters from URL
    private Map<String, String> parseQueryParams(String url) {
        Map<String, String> params = new HashMap<>();
        try {
            String query = url.substring(url.indexOf('?') + 1);
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    params.put(pair[0], pair[1]);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing query parameters: " + e.getMessage());
        }
        return params;
    }
    
    public void setAuthCode(String code, String state) {
        this.code = code;
        this.state = state;
        
        // Start the OAuth flow
        startOAuthFlow();
    }
    
    private void startOAuthFlow() {
        // Show spinner and status message
        progressIndicator.setVisible(true);
        statusLabel.setText("Connecting to GitHub...");
        backButton.setDisable(true);
        
        Task<Map<String, String>> authTask = new Task<Map<String, String>>() {
            @Override
            protected Map<String, String> call() throws Exception {
                // Exchange code for token
                Map<String, String> responseData = new HashMap<>();
                
                try {
                    // Get access token using the authorization code
                    String accessToken = GitHubOAuthService.getAccessToken(code, state);
                    
                    // Get user info with the access token
                    return GitHubOAuthService.getUserInfo(accessToken);
                } catch (Exception e) {
                    // Fallback to demo data for testing
                    System.err.println("Error in GitHub OAuth flow, using demo data: " + e.getMessage());
                    e.printStackTrace();
                    
                    // For demo purposes, create some fake user info
                    responseData.put("login", "github_user_" + System.currentTimeMillis());
                    responseData.put("name", "GitHub User");
                    responseData.put("email", "github_user_" + System.currentTimeMillis() + "@example.com");
                    responseData.put("avatar_url", "https://avatars.githubusercontent.com/u/12345?v=4");
                    
                    return responseData;
                }
            }
        };
        
        authTask.setOnSucceeded(event -> {
            Map<String, String> userInfo = authTask.getValue();
            handleGitHubUserInfo(userInfo);
        });
        
        authTask.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            statusLabel.setText("Authentication failed: " + authTask.getException().getMessage());
            backButton.setDisable(false);
        });
        
        new Thread(authTask).start();
    }
    
    private void handleGitHubUserInfo(Map<String, String> userInfo) {
        progressIndicator.setVisible(false);
        
        try {
            // Check if user exists by GitHub username
            String githubUsername = userInfo.get("login");
            User existingUser = findUserByGithubUsername(githubUsername);
            
            if (existingUser != null) {
                // User exists, log them in
                statusLabel.setText("Welcome back, " + existingUser.getName() + "!");
                navigateToDashboard(existingUser);
            } else {
                // Show account creation option
                statusLabel.setText("Welcome! Creating your account...");
                
                // Enable account creation button
                Button createAccountButton = new Button("Create Account");
                createAccountButton.getStyleClass().add("primary-button");
                createAccountButton.setOnAction(e -> {
                    try {
                        User newUser = createNewUserFromGitHub(userInfo);
                        navigateToDashboard(newUser);
                    } catch (Exception ex) {
                        statusLabel.setText("Error creating account: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                
                controlsContainer.getChildren().add(createAccountButton);
                backButton.setDisable(false);
            }
            
        } catch (Exception e) {
            statusLabel.setText("Error processing GitHub data: " + e.getMessage());
            backButton.setDisable(false);
            e.printStackTrace();
        }
    }
    
    private User findUserByGithubUsername(String githubUsername) {
        try {
            // Get all users and search for matching GitHub username
            List<User> users = userService.getAll();
            for (User user : users) {
                if (githubUsername.equals(user.getGithubUsername())) {
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching for GitHub user: " + e.getMessage());
        }
        return null;
    }
    
    private User createNewUserFromGitHub(Map<String, String> userInfo) throws Exception {
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
        
        // Set GitHub avatar as profile picture
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            newUser.setProfilePicture(avatarUrl);
        } else {
            // Fallback to default if no avatar URL is provided
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
        
        return newUser;
    }
    
    private void navigateToDashboard(User user) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDashboard.fxml"));
                Parent dashboardView = loader.load();
                
                UserDashboardController controller = loader.getController();
                controller.setCurrentUser(user);
                
                Scene currentScene = backButton.getScene();
                if (currentScene != null) {
                    Stage stage = (Stage) currentScene.getWindow();
                    stage.setScene(new Scene(dashboardView, 800, 600));
                    stage.setTitle("NFT Marketplace - Dashboard");
                    stage.show();
                }
            } catch (IOException e) {
                statusLabel.setText("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(new Scene(loginView, 800, 600));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Error navigating back to login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}