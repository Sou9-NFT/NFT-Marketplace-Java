package org.esprit.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

import org.esprit.utils.ConfigManager;
import org.json.JSONObject;

/**
 * Service for uploading images to Imgur API
 */
public class ImgurService {
    private static final String UPLOAD_ENDPOINT = "https://api.imgur.com/3/image";
    private final String clientId;
    
    /**
     * Constructor that loads the Imgur client ID from config
     */
    public ImgurService() {
        ConfigManager configManager = ConfigManager.getInstance();
        this.clientId = configManager.getProperty("imgur.client.id");
        
        if (clientId == null || clientId.isEmpty()) {
            System.err.println("Warning: Imgur client ID is not configured. Profile picture uploads will use local storage.");
        }
    }
    
    /**
     * Check if Imgur integration is configured
     * @return true if Imgur client ID is configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty();
    }
    
    /**
     * Upload an image to Imgur
     * @param imageFile The image file to upload
     * @return The URL of the uploaded image
     * @throws IOException If there's an error reading the file or communicating with Imgur API
     */
    public String uploadImage(File imageFile) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Imgur client ID is not configured");
        }
        
        // Read file into byte array
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        
        // Base64 encode the image data
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // Set up the connection
        URL url = new URL(UPLOAD_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Client-ID " + clientId);
        conn.setRequestProperty("Content-Type", "application/json");
        
        // Create the request JSON
        JSONObject requestJson = new JSONObject();
        requestJson.put("image", base64Image);
        requestJson.put("type", "base64");
        
        // Send the request
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(requestJson.toString());
        writer.flush();
        writer.close();
        
        // Get the response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to upload image to Imgur. Response code: " + responseCode);
        }
        
        // Read the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());
        
        if (!jsonResponse.getBoolean("success")) {
            throw new IOException("Imgur API returned failure: " + jsonResponse.toString());
        }
        
        // Get the image URL
        JSONObject data = jsonResponse.getJSONObject("data");
        String imageUrl = data.getString("link");
        
        return imageUrl;
    }
}
