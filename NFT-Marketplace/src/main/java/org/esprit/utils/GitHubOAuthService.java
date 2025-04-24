package org.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

/**
 * Service class for handling GitHub OAuth flow
 */
public class GitHubOAuthService {
    
    private static final String CHARSET = StandardCharsets.UTF_8.name();
    private static String stateToken; // Used to prevent CSRF attacks
    
    /**
     * Generate the authorization URL to redirect the user to GitHub
     * @return The authorization URL
     */
    public static String getAuthorizationUrl() {
        // Generate a random state token to prevent CSRF attacks
        stateToken = generateStateToken();
        
        try {
            return GitHubOAuthConfig.getAuthorizeUrl() +
                   "?client_id=" + GitHubOAuthConfig.getClientId() +
                   "&redirect_uri=" + URLEncoder.encode(GitHubOAuthConfig.getRedirectUri(), CHARSET) +
                   "&scope=user" +
                   "&state=" + stateToken;
        } catch (Exception e) {
            throw new RuntimeException("Error generating authorization URL", e);
        }
    }
    
    /**
     * Exchange the authorization code for an access token
     * @param code The authorization code returned by GitHub
     * @param state The state parameter returned by GitHub
     * @return The access token
     * @throws Exception If the state token doesn't match or there's an error in the request
     */
    public static String getAccessToken(String code, String state) throws Exception {
        // Verify the state token to prevent CSRF attacks
        if (!state.equals(stateToken)) {
            throw new Exception("Invalid state parameter");
        }
        
        URL url = new URL(GitHubOAuthConfig.getTokenUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        
        String params = "client_id=" + GitHubOAuthConfig.getClientId() +
                        "&client_secret=" + GitHubOAuthConfig.getClientSecret() +
                        "&code=" + code +
                        "&redirect_uri=" + URLEncoder.encode(GitHubOAuthConfig.getRedirectUri(), CHARSET);
        
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(params);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("access_token");
    }
    
    /**
     * Get the user information from GitHub API
     * @param accessToken The access token
     * @return A map containing user information
     * @throws Exception If there's an error in the request
     */
    public static Map<String, String> getUserInfo(String accessToken) throws Exception {
        URL url = new URL(GitHubOAuthConfig.getUserApiUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        JSONObject jsonResponse = new JSONObject(response.toString());
        Map<String, String> userInfo = new HashMap<>();
        
        // Extract relevant user information with safer handling of potentially missing or null fields
        userInfo.put("id", jsonResponse.has("id") ? jsonResponse.get("id").toString() : "");
        userInfo.put("login", jsonResponse.has("login") ? jsonResponse.optString("login", "") : "");
        userInfo.put("name", jsonResponse.has("name") ? jsonResponse.optString("name", "") : "");
        
        // Handle email field more carefully - GitHub might not return this if it's not public
        if (jsonResponse.has("email") && !jsonResponse.isNull("email")) {
            userInfo.put("email", jsonResponse.getString("email"));
        } else {
            // Use null or empty string to indicate missing email
            userInfo.put("email", "");
        }
        
        userInfo.put("avatar_url", jsonResponse.has("avatar_url") ? jsonResponse.optString("avatar_url", "") : "");
        
        // Print debug info to console
        System.out.println("GitHub user info received: " + jsonResponse.toString(2));
        
        return userInfo;
    }
    
    /**
     * Generate a random state token
     * @return A random string
     */
    private static String generateStateToken() {
        return Long.toString(new Random().nextLong());
    }
}