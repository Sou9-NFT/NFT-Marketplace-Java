package org.esprit.utils;

/**
 * Configuration class for GitHub OAuth
 * You need to register your application on GitHub:
 * 1. Go to GitHub -> Settings -> Developer settings -> OAuth Apps -> New OAuth App
 * 2. Fill in application details:
 *    - Application name: sou9 nft
 *    - Homepage URL: http://127.0.0.1:8000
 *    - Authorization callback URL: http://127.0.0.1:8000/connect/github/check
 * 3. After registration, GitHub will provide a Client ID and Client Secret
 * 4. Replace the placeholder values below with your actual credentials
 */
public class GitHubOAuthConfig {
    // GitHub OAuth credentials
    private static final String CLIENT_ID = "Ov23lioUw7aR32FF64FW";
    private static final String CLIENT_SECRET = "2df12a6dcd34cba9ce2612d673a55a47e68869b4";
    private static final String REDIRECT_URI = "http://127.0.0.1:8000/connect/github/check";
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_API_URL = "https://api.github.com/user";
    
    public static String getClientId() {
        return CLIENT_ID;
    }
    
    public static String getClientSecret() {
        return CLIENT_SECRET;
    }
    
    public static String getRedirectUri() {
        return REDIRECT_URI;
    }
    
    public static String getAuthorizeUrl() {
        return AUTHORIZE_URL;
    }
    
    public static String getTokenUrl() {
        return TOKEN_URL;
    }
    
    public static String getUserApiUrl() {
        return USER_API_URL;
    }
}