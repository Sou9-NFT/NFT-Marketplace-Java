package org.esprit.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service class for interacting with the Stability AI API to generate images.
 */
public class StabilityAIService {
    private final String API_URL = "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image";
    private String apiKey;
    private OkHttpClient httpClient;
    private final String UPLOAD_DIRECTORY = "src/main/resources/uploads/";
    
    /**
     * Available art styles for AI generation
     */
    public static final String[] ART_STYLES = {
        "Photorealistic",
        "Digital Art", 
        "Oil Painting", 
        "Watercolor", 
        "3D Rendering", 
        "Pencil Sketch", 
        "Pixel Art",
        "Comic Book", 
        "Anime", 
        "Abstract",
        "Impressionism",
        "Surrealism",
        "Pop Art",
        "Minimalist"
    };
    
    public StabilityAIService() {
        httpClient = new OkHttpClient();
        loadApiKey();
    }
    
    /**
     * Load API key from properties file
     */
    private void loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            
            if (input != null) {
                props.load(input);
                apiKey = props.getProperty("stability.api.key");
                input.close();
            } else {
                System.err.println("Unable to find config.properties");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Generate image asynchronously based on prompt and style
     * @param prompt The text prompt to generate image from
     * @param style The art style to apply
     * @return CompletableFuture containing the path to the generated image
     */
    public CompletableFuture<String> generateImage(String prompt, String style) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create full prompt with style
                String fullPrompt = prompt;
                if (style != null && !style.isEmpty() && !style.equals("Photorealistic")) {
                    fullPrompt = prompt + ", in " + style + " style";
                }
                
                // Create request JSON - fixing the JSON structure to match Stability API requirements
                JsonObject requestJson = new JsonObject();
                
                // Create text_prompts array properly
                JsonArray textPrompts = new JsonArray();
                JsonObject promptObj = new JsonObject();
                promptObj.addProperty("text", fullPrompt);
                promptObj.addProperty("weight", 1);
                textPrompts.add(promptObj);
                
                requestJson.add("text_prompts", textPrompts);
                requestJson.addProperty("cfg_scale", 7);
                requestJson.addProperty("height", 1024);
                requestJson.addProperty("width", 1024);
                requestJson.addProperty("samples", 1);
                requestJson.addProperty("steps", 50);
                
                // For debugging - print the request JSON
                System.out.println("Stability API Request: " + requestJson.toString());
                
                // Build request
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), requestJson.toString());
                
                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();
                
                // Execute request
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        System.err.println("API Error: " + response.code() + " " + response.message());
                        System.err.println("Error details: " + errorBody);
                        throw new IOException("Unexpected response: " + response.code() + " " + response.message() + " - " + errorBody);
                    }
                    
                    String responseBody = response.body().string();
                    return saveImage(responseBody);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Save the generated image to disk
     * @param responseJson JSON response from the API
     * @return Path to saved image file
     */
    private String saveImage(String responseJson) throws IOException {
        // Parse response JSON
        JsonObject jsonResponse = JsonParser.parseString(responseJson).getAsJsonObject();
        JsonArray artifacts = jsonResponse.getAsJsonArray("artifacts");
        
        if (artifacts == null || artifacts.size() == 0) {
            throw new IOException("No artifacts found in API response");
        }
        
        // Get base64-encoded image
        String base64Image = artifacts.get(0).getAsJsonObject().get("base64").getAsString();
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        
        // Create upload directory if needed
        File uploadDir = new File(UPLOAD_DIRECTORY);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // Generate unique filename
        String uniqueFileName = "ai_generated_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path destination = Paths.get(UPLOAD_DIRECTORY + uniqueFileName);
        
        // Write image to file
        try (FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            fos.write(imageBytes);
        }
        
        return uniqueFileName;
    }
    
    /**
     * Get predefined art styles
     * @return Array of available art styles
     */
    public String[] getArtStyles() {
        return ART_STYLES;
    }
}