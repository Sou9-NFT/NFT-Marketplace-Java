package org.esprit.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class TwitterService {
    private static final String API_KEY = "y6jLitsabt0pVGDLOl4pPV7mI";
    private static final String API_SECRET = "AYwfMZczk7L3kg1yRPebJBkHHqgrBvZxD4pvvOnTcE7YIpK2Fy";
    private static final String ACCESS_TOKEN = "1916427087151521792-hFzP8Sii00uVyln6aRtr4grA9YRQXy";
    private static final String ACCESS_TOKEN_SECRET = "CYrIp2NWP5scQY3OEFDHK7zIfuNeNF6pYs2awP1VuzZ10";
    
    private static final String TWITTER_API_URL = "https://api.twitter.com/2/tweets";
    private static final String TWITTER_UPLOAD_URL = "https://upload.twitter.com/1.1/media/upload.json";
    
    private final HttpClient client;
    
    public TwitterService() {
        client = HttpClient.newHttpClient();
    }

    public String shareWin(String raffleTitle, String artworkTitle, String artworkImagePath) throws Exception {
        // First upload the image
        String mediaId = uploadImage(artworkImagePath);
        
        if (mediaId == null) {
            throw new IOException("Failed to upload image");
        }

        String message = String.format("🎉 I just won \"%s\" in a raffle on NFT Marketplace! Check out this amazing artwork: %s 🎨 #NFT #RaffleWinner", 
            raffleTitle, 
            artworkTitle);

        // Create OAuth 1.0a parameters for the tweet
        TreeMap<String, String> params = createOAuthParams();
        
        // Create parameter string
        String paramString = params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));
        
        // Generate OAuth signature
        String signatureBaseString = "POST" + "&" + 
            encode(TWITTER_API_URL) + "&" + 
            encode(paramString);
        
        String signingKey = encode(API_SECRET) + "&" + encode(ACCESS_TOKEN_SECRET);
        String oauthSignature = generateSignature(signatureBaseString, signingKey);
        params.put("oauth_signature", oauthSignature);
        
        // Create Authorization header
        String authHeader = "OAuth " + params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=\"" + encode(e.getValue()) + "\"")
            .collect(Collectors.joining(", "));

        // Create tweet request body with media
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", message);
        
        // Add media to the tweet
        JSONObject media = new JSONObject();
        media.put("media_ids", new String[]{mediaId});
        requestBody.put("media", media);
        
        // Send tweet request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TWITTER_API_URL))
            .header("Authorization", authHeader)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new IOException("Failed to share on Twitter. Status code: " + response.statusCode() + 
                ". Response: " + response.body());
        }
    }
    
    private String uploadImage(String imagePath) throws Exception {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("Image file not found: " + imagePath);
        }

        // Read image file as bytes and encode as base64
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Create OAuth parameters for media upload
        TreeMap<String, String> params = createOAuthParams();
        
        // Generate OAuth signature for media upload
        String paramString = params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));

        String signatureBaseString = "POST" + "&" + 
            encode(TWITTER_UPLOAD_URL) + "&" + 
            encode(paramString);

        String signingKey = encode(API_SECRET) + "&" + encode(ACCESS_TOKEN_SECRET);
        String oauthSignature = generateSignature(signatureBaseString, signingKey);
        params.put("oauth_signature", oauthSignature);

        // Create Authorization header
        String authHeader = "OAuth " + params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=\"" + encode(e.getValue()) + "\"")
            .collect(Collectors.joining(", "));

        // Create form data for image upload
        String boundary = "boundary" + System.currentTimeMillis();
        StringBuilder formData = new StringBuilder();
        formData.append("--").append(boundary).append("\r\n");
        formData.append("Content-Disposition: form-data; name=\"media_data\"\r\n\r\n");
        formData.append(base64Image).append("\r\n");
        formData.append("--").append(boundary).append("--\r\n");

        // Send upload request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TWITTER_UPLOAD_URL))
            .header("Authorization", authHeader)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofString(formData.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getString("media_id_string");
        } else {
            throw new IOException("Failed to upload image. Status code: " + response.statusCode() + 
                ". Response: " + response.body());
        }
    }
    
    private TreeMap<String, String> createOAuthParams() {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("oauth_consumer_key", API_KEY);
        params.put("oauth_nonce", generateNonce());
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", String.valueOf(Instant.now().getEpochSecond()));
        params.put("oauth_token", ACCESS_TOKEN);
        params.put("oauth_version", "1.0");
        return params;
    }
    
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }
    
    private String generateNonce() {
        byte[] nonce = new byte[32];
        new Random().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce)
            .replaceAll("[^a-zA-Z0-9]", "");
    }
    
    private String generateSignature(String data, String key) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha1HMAC = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        sha1HMAC.init(secretKey);
        byte[] signatureBytes = sha1HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}