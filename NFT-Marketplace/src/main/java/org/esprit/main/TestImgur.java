package org.esprit.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * A simple utility to test downloading an image from Imgur
 */
public class TestImgur {
    
    public static void main(String[] args) {
        String imgurUrl = "https://imgur.com/oWDqVPa";
        
        // Convert to direct image URL
        String directUrl = convertToDirectImageUrl(imgurUrl);
        System.out.println("Direct Image URL: " + directUrl);
        
        // Create a directory to save the downloaded image
        String downloadDir = "downloaded_images";
        Path dirPath = Paths.get(downloadDir);
        
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Created directory: " + downloadDir);
            }
            
            // Download the image
            String imagePath = downloadDir + "/imgur_image.jpg";
            boolean success = downloadImage(directUrl, imagePath);
            
            if (success) {
                System.out.println("Image successfully downloaded to: " + imagePath);
                System.out.println("Full path: " + Paths.get(imagePath).toAbsolutePath());
            } else {
                System.out.println("Failed to download image.");
            }
            
            // Try with Imgur API if direct download fails
            tryWithImgurApi(imgurUrl, downloadDir);
            
        } catch (IOException e) {
            System.err.println("Error creating directory or downloading image: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Converts a standard Imgur URL to a direct image URL
     */
    private static String convertToDirectImageUrl(String imgurUrl) {
        // Extract the image ID
        String imageId = imgurUrl.substring(imgurUrl.lastIndexOf('/') + 1);
        // Create direct URL to the image
        return "https://i.imgur.com/" + imageId + ".jpg";
    }
    
    /**
     * Downloads an image from the given URL to the specified file path
     */
    private static boolean downloadImage(String imageUrl, String destinationPath) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    return true;
                }
            } else {
                System.err.println("Failed to download image. HTTP error code: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error downloading image: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Attempts to download the image using Imgur API credentials from config.properties
     */
    private static void tryWithImgurApi(String imgurUrl, String downloadDir) {
        try {
            // Load API credentials from config.properties
            Properties properties = new Properties();
            try (InputStream input = TestImgur.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input == null) {
                    System.err.println("Sorry, unable to find config.properties");
                    return;
                }
                properties.load(input);
            }
            
            String clientId = properties.getProperty("imgur.client.id");
            String clientSecret = properties.getProperty("imgur.client.secret");
            
            if (clientId == null || clientSecret == null) {
                System.err.println("Imgur API credentials not found in config.properties");
                return;
            }
            
            System.out.println("Found Imgur API credentials. Client ID: " + clientId);
            
            // Extract the image ID
            String imageId = imgurUrl.substring(imgurUrl.lastIndexOf('/') + 1);
            
            // Create URL for API request
            URL url = new URL("https://api.imgur.com/3/image/" + imageId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Client-ID " + clientId);
            
            int responseCode = connection.getResponseCode();
            System.out.println("API Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("API connection successful. You can implement parsing the response to get the direct image URL.");
                // To fully implement: Parse JSON response to get link to image
                // Then download the image
            } else {
                System.err.println("Failed to connect to Imgur API. HTTP error code: " + responseCode);
            }
            
        } catch (IOException e) {
            System.err.println("Error using Imgur API: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
