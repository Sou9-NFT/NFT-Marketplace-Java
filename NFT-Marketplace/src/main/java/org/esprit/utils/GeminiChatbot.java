package org.esprit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.esprit.services.ArtworkService;
import org.esprit.services.RaffleService;
import org.esprit.services.UserService;
import org.esprit.models.User;
import org.esprit.models.Artwork;
import org.esprit.models.Raffle;

/**
 * Utility class for interacting with the Gemini API
 */
public class GeminiChatbot {
    private static final String API_KEY = "AIzaSyCc52REiSD_mLhuIoADxvJhDNVHzN60f3Y";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    
    private final ArtworkService artworkService;
    private final RaffleService raffleService;
    private final UserService userService;
    
    private static final String CONVERSATIONAL_INSTRUCTIONS = 
            "Also respond naturally to gratitude and conversational expressions:\n" +
            "- When users say 'thanks', 'thank you', etc., respond with friendly acknowledgments like 'You're welcome!' or 'Happy to help!'\n" +
            "- For greetings like 'hi', 'hello', respond warmly while mentioning you're ready to help with the NFT marketplace\n" +
            "- If users say 'bye' or 'goodbye', respond with a friendly farewell and remind them you're here to help anytime\n" +
            "- For compliments or positive feedback, show appreciation while staying professional\n" +
            "When users ask about NFTs or raffles, check the database and provide specific information.\n" +
            "Remember to keep responses concise and natural while maintaining a helpful, professional tone.\n\n";
    
    // Detailed knowledge base about the raffle system functionality
    private static final String APP_KNOWLEDGE = 
            "Here are specific details about how the NFT Marketplace Raffle system works:\n\n" +
            "1. BEFORE PARTICIPATING IN RAFFLES:\n" +
            "   - You must own at least one NFT to participate in or create raffles\n" +
            "   - If you don't own an NFT, you need to create or acquire one first\n" +
            "   - You can create an NFT by going to the 'Artworks' section and clicking 'Create New'\n" +
            "   - Alternatively, you can purchase an NFT from the Marketplace\n\n" +
            
            "2. VIEWING RAFFLES:\n" +
            "   - All available raffles are displayed on the Raffles page\n" +
            "   - Each raffle card shows the artwork image, title, creator, and end date\n" +
            "   - Use the search field to find specific raffles by title or description\n" +
            "   - Filter raffles by status (active or ended) using the dropdown\n" +
            "   - I can show you current active or ended raffles from the database\n\n" +
            
            "3. NFT INFORMATION:\n" +
            "   - I can show you NFTs created by specific users\n" +
            "   - I can show you NFTs owned by specific users\n" +
            "   - Ask me about any NFT by creator name or owner name\n" +
            "   - I can search NFTs by title\n\n" +
            
            "4. JOINING A RAFFLE:\n" +
            "   - First ensure you own at least one NFT\n" +
            "   - Click 'View Details' on any raffle card\n" +
            "   - In the raffle details page, click 'Participate'\n" +
            "   - Complete the CAPTCHA verification\n" +
            "   - Confirm your entry by clicking 'Join Raffle'\n" +
            "   - You'll receive a confirmation message when successfully entered\n\n" +
            
            "5. CREATING A RAFFLE:\n" +
            "   - You must own the NFT you want to raffle\n" +
            "   - Click 'Create New Raffle' at the top of the Raffles page\n" +
            "   - Select your NFT artwork and fill in all required details\n" +
            "   - Set the raffle duration and any participation requirements\n" +
            "   - Click 'Create' to publish your raffle\n\n" +
            
            "6. MANAGING YOUR RAFFLES:\n" +
            "   - Navigate to 'My Raffles' in the user dashboard\n" +
            "   - You can view, edit or cancel raffles you've created\n" +
            "   - For active raffles, you can see the current participant count\n\n" +
            
            "7. WINNING A RAFFLE:\n" +
            "   - Winners are selected automatically when the raffle end time is reached\n" +
            "   - If you win, you'll receive a notification\n" +
            "   - The NFT will be transferred to your account\n" +
            "   - You can view your won NFTs in the 'My Collection' section\n" +
            "   - When you win, you can share your victory on X (Twitter) directly from the platform!\n\n" +
            
            "8. SHARING ON X (TWITTER):\n" +
            "   - After winning a raffle, you'll see a 'Share on X' button\n" +
            "   - Clicking it will create a post with your winning artwork\n" +
            "   - The post will include the raffle title and artwork image\n" +
            "   - Hashtags #NFT and #RaffleWinner are automatically added\n" +
            "   - The system handles all the authentication and posting for you\n\n" +
            
            "9. COMMON ISSUES:\n" +
            "   - If you can't join a raffle, ensure you're logged in and own at least one NFT\n" +
            "   - If the CAPTCHA doesn't load, try refreshing the page\n" +
            "   - If images don't load, check your internet connection\n" +
            "   - If sharing to X fails, try again in a few moments\n";
    
    private final List<JSONObject> conversationHistory = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GeminiChatbot() {
        this.artworkService = new ArtworkService();
        this.raffleService = new RaffleService();
        this.userService = new UserService();
    }

    /**
     * Sends a message to the Gemini API and returns the response asynchronously
     */
    public CompletableFuture<String> sendMessage(String userMessage, Consumer<String> onResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if the message is asking about database information
                if (userMessage.toLowerCase().contains("show") || userMessage.toLowerCase().contains("list")) {
                    String databaseResponse = handleDatabaseQuery(userMessage);
                    if (databaseResponse != null) {
                        onResponse.accept(databaseResponse);
                        return databaseResponse;
                    }
                }

                // Continue with normal API response if not a database query
                System.out.println("Sending message to Gemini API: " + userMessage);
                
                // Create the request body following the example format
                JSONObject requestBody = new JSONObject();
                
                // Create contents array with a single content object
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                
                // Prepare parts array
                JSONArray parts = new JSONArray();
                
                // First part will contain our knowledge base instructions
                JSONObject instructionPart = new JSONObject();
                instructionPart.put("text", 
                    "You are the assistant for the NFT Marketplace app's raffle feature. " +
                    "You must answer questions using ONLY the specific information provided below. " +
                    "If users ask about features not mentioned, say you don't have that information yet. " +
                    "Be specific, direct and helpful. Give exact step-by-step instructions when asked how to do something. " +
                    "Respond in a friendly, conversational tone. Always focus on providing clear guidance on how to use the features.\n\n" +
                    CONVERSATIONAL_INSTRUCTIONS +
                    "KNOWLEDGE BASE:\n" + APP_KNOWLEDGE);
                parts.put(instructionPart);
                
                // Second part will contain the user's message
                JSONObject userPart = new JSONObject();
                userPart.put("text", userMessage);
                parts.put(userPart);
                
                // Add parts to content
                content.put("parts", parts);
                
                // Add content to contents array
                contents.put(content);
                
                // Set the contents in the request body
                requestBody.put("contents", contents);
                
                // Add generation config
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.2); // Lower temperature for more factual responses
                generationConfig.put("topP", 0.95);
                generationConfig.put("topK", 40);
                generationConfig.put("maxOutputTokens", 800);
                requestBody.put("generationConfig", generationConfig);
                
                // Create URL with API key
                URL url = new URL(API_URL + "?key=" + API_KEY);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Debug info
                System.out.println("Connecting to URL: " + url.toString());
                
                // Send request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Process response
                int responseCode = connection.getResponseCode();
                System.out.println("Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        // Parse the response
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        try {
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject candidate = candidates.getJSONObject(0);
                                JSONObject contentObj = candidate.getJSONObject("content");
                                JSONArray responseParts = contentObj.getJSONArray("parts");
                                if (responseParts.length() > 0) {
                                    String responseText = responseParts.getJSONObject(0).getString("text");
                                    
                                    // Call the onResponse consumer
                                    onResponse.accept(responseText);
                                    return responseText;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing response: " + e.getMessage());
                            e.printStackTrace();
                            
                            // Try alternate response format
                            if (jsonResponse.has("text")) {
                                String responseText = jsonResponse.getString("text");
                                onResponse.accept(responseText);
                                return responseText;
                            }
                        }
                    }
                } else {
                    String errorDetails = "";
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        errorDetails = response.toString();
                        System.err.println("Error response from Gemini API: " + errorDetails);
                    }
                    String errorMessage = "Error: Unable to get response from AI (HTTP " + responseCode + ")";
                    if (!errorDetails.isEmpty()) {
                        try {
                            JSONObject errorJson = new JSONObject(errorDetails);
                            if (errorJson.has("error")) {
                                JSONObject error = errorJson.getJSONObject("error");
                                if (error.has("message")) {
                                    errorMessage += " - " + error.getString("message");
                                }
                            }
                        } catch (Exception e) {
                            // If we can't parse the error JSON, just use the raw error details
                            errorMessage += " - " + errorDetails;
                        }
                    }
                    onResponse.accept(errorMessage);
                    return errorMessage;
                }
            } catch (IOException e) {
                e.printStackTrace();
                String errorMessage = "Error: " + e.getMessage();
                onResponse.accept(errorMessage);
                return errorMessage;
            }
            
            String fallbackMessage = "Sorry, I couldn't process your request.";
            onResponse.accept(fallbackMessage);
            return fallbackMessage;
        }, executor);
    }

    private String handleDatabaseQuery(String userMessage) {
        try {
            String msg = userMessage.toLowerCase();
            StringBuilder response = new StringBuilder();

            // Handle NFT queries by creator
            if (msg.contains("nft") && msg.contains("created by")) {
                String creatorName = extractName(msg, "created by");
                User creator = userService.findByUsername(creatorName);
                if (creator != null) {
                    List<Artwork> artworks = artworkService.getByCreator(creator.getId());
                    if (!artworks.isEmpty()) {
                        response.append("Here are the NFTs created by ").append(creatorName).append(":\n\n");
                        for (Artwork art : artworks) {
                            response.append("- ").append(art.getTitle())
                                   .append(" (Price: $").append(art.getPrice()).append(")\n");
                        }
                    } else {
                        response.append("No NFTs found created by ").append(creatorName);
                    }
                    return response.toString();
                }
            }

            // Handle raffle status queries
            if (msg.contains("raffle")) {
                if (msg.contains("active") || msg.contains("current")) {
                    List<Raffle> raffles = raffleService.getAll();
                    response.append("Here are the active raffles:\n\n");
                    for (Raffle raffle : raffles) {
                        if (raffle.getStatus().equals("active")) {
                            response.append("- ").append(raffle.getTitle())
                                   .append(" (Creator: ").append(raffle.getCreatorName())
                                   .append(", Ends: ").append(raffle.getEndTime()).append(")\n");
                        }
                    }
                    return response.toString();
                } else if (msg.contains("ended") || msg.contains("completed")) {
                    List<Raffle> raffles = raffleService.getAll();
                    response.append("Here are the ended raffles:\n\n");
                    for (Raffle raffle : raffles) {
                        if (raffle.getStatus().equals("ended")) {
                            response.append("- ").append(raffle.getTitle())
                                   .append(" (Creator: ").append(raffle.getCreatorName())
                                   .append(", Winner: ").append(raffle.getWinnerId() != null ? "Selected" : "None").append(")\n");
                        }
                    }
                    return response.toString();
                }
            }

            return null; // Return null if no database query was handled
        } catch (Exception e) {
            return "Sorry, I encountered an error while fetching the information: " + e.getMessage();
        }
    }

    private String extractName(String message, String keyword) {
        int index = message.indexOf(keyword) + keyword.length();
        String name = message.substring(index).trim();
        // Remove any trailing punctuation or words
        name = name.split("[.,!?\\s]+")[0];
        return name;
    }
    
    /**
     * Clears the conversation history
     */
    public void clearConversation() {
        conversationHistory.clear();
    }
}