package org.esprit.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.esprit.models.Artwork;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for interacting with the Gemini API to generate descriptions for artworks
 */
public class GeminiApi {

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
    /**
     * Generates a mysterious and funny description for an artwork in mysterious mode
     * 
     * @param artwork The artwork to generate a description for
     * @return The generated mysterious description
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public static String generateMysteriousDescription(Artwork artwork) throws IOException, InterruptedException {
        ConfigManager configManager = ConfigManager.getInstance();
        String apiKey = configManager.getGeminiApiKey();
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "This bewildering artwork conceals a humorous secret! What appears ordinary at first glance hides an unexpected twist that only the highest bidder will discover. Are you curious enough to unveil its true nature?";
        }
        
        String prompt = buildPrompt(artwork);
        String url = API_BASE_URL + "?key=" + apiKey;
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", prompt);
        
        content.put("parts", new Object[]{part});
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.8); // Slightly higher temperature for more creative outputs
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 200);
        
        requestBody.put("contents", new Object[]{content});
        requestBody.put("generationConfig", generationConfig);
        
        // Convert request body to JSON
        JSONObject jsonBody = new JSONObject(requestBody);
        
        // Build and send request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse response
        String description = parseResponse(response.body());
        
        // Fallback description if parsing fails
        if (description == null || description.isEmpty()) {
            return "This whimsical mystery artwork hides a playful surprise! The colors dance with delight, teasing a story that only the auction winner will fully understand. Bid now to unlock its humorous secrets!";
        }
        
        return description;
    }
    
    /**
     * Build a prompt for the Gemini API based on the artwork
     * 
     * @param artwork The artwork to generate a description for
     * @return A prompt for the Gemini API
     */
    private static String buildPrompt(Artwork artwork) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("Create a funny, mysterious, and witty description for an artwork in an NFT auction. ");
        promptBuilder.append("The description should be enigmatic but with a touch of humor that creates curiosity without revealing too much. ");
        promptBuilder.append("It should entice bidders to participate in the auction by making them curious. ");
          if (artwork != null) {
            promptBuilder.append("Here are some details about the artwork: ");
            
            if (artwork.getTitle() != null && !artwork.getTitle().isEmpty()) {
                promptBuilder.append("Title: ").append(artwork.getTitle()).append(". ");
            }
            
            // Use creatorId instead of artist
            promptBuilder.append("Creator ID: ").append(artwork.getCreatorId()).append(". ");
            
            if (artwork.getDescription() != null && !artwork.getDescription().isEmpty()) {
                promptBuilder.append("Original Description: ").append(artwork.getDescription()).append(". ");
            }
            
            // Include price as additional context
            promptBuilder.append("Price: ").append(artwork.getPrice()).append(". ");
            
            // Include category ID
            promptBuilder.append("Category ID: ").append(artwork.getCategoryId()).append(". ");
        }
        
        promptBuilder.append("Keep the description between 2-3 sentences and make it sound both mysterious and slightly amusing. ");
        promptBuilder.append("Include a hint of something surprising or unexpected about the artwork. ");
        promptBuilder.append("Don't explicitly mention NFTs or blockchain. ");
        promptBuilder.append("End with a question or a teasing statement that makes bidders want to own the piece.");
        
        return promptBuilder.toString();
    }
    
    /**
     * Parse the response from the Gemini API
     * 
     * @param responseBody The response body from the Gemini API
     * @return The generated description
     */
    private static String parseResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                
                if (candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            
                            if (parts.length() > 0) {
                                JSONObject part = parts.getJSONObject(0);
                                
                                if (part.has("text")) {
                                    return part.getString("text");
                                }
                            }
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing Gemini API response: " + e.getMessage());
            return null;
        }
    }
}
