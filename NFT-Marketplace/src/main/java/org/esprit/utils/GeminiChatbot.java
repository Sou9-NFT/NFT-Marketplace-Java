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

/**
 * Utility class for interacting with the Gemini API
 */
public class GeminiChatbot {
    private static final String API_KEY = "AIzaSyCc52REiSD_mLhuIoADxvJhDNVHzN60f3Y";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    
    // Detailed knowledge base about the raffle system functionality
    private static final String APP_KNOWLEDGE = 
            "Here are specific details about how the NFT Marketplace Raffle system works:\n\n" +
            "1. VIEWING RAFFLES:\n" +
            "   - All available raffles are displayed on the Raffles page\n" +
            "   - Each raffle card shows the artwork image, title, creator, and end date\n" +
            "   - Use the search field to find specific raffles by title or description\n" +
            "   - Filter raffles by status (active or ended) using the dropdown\n\n" +
            
            "2. JOINING A RAFFLE:\n" +
            "   - Click 'View Details' on any raffle card\n" +
            "   - In the raffle details page, click 'Participate'\n" +
            "   - Complete the CAPTCHA verification\n" +
            "   - Confirm your entry by clicking 'Join Raffle'\n" +
            "   - You'll receive a confirmation message when successfully entered\n\n" +
            
            "3. CREATING A RAFFLE:\n" +
            "   - Click 'Create New Raffle' at the top of the Raffles page\n" +
            "   - Upload your NFT artwork and fill in all required details\n" +
            "   - Set the raffle duration and any participation requirements\n" +
            "   - Click 'Create' to publish your raffle\n\n" +
            
            "4. MANAGING YOUR RAFFLES:\n" +
            "   - Navigate to 'My Raffles' in the user dashboard\n" +
            "   - You can view, edit or cancel raffles you've created\n" +
            "   - For active raffles, you can see the current participant count\n\n" +
            
            "5. WINNING A RAFFLE:\n" +
            "   - Winners are selected automatically when the raffle end time is reached\n" +
            "   - If you win, you'll receive a notification\n" +
            "   - The NFT will be transferred to your account\n" +
            "   - You can view your won NFTs in the 'My Collection' section\n\n" +
            
            "6. COMMON ISSUES:\n" +
            "   - If you can't join a raffle, ensure you're logged in and meet the requirements\n" +
            "   - If the CAPTCHA doesn't load, try refreshing the page\n" +
            "   - If images don't load, check your internet connection\n";
    
    private final List<JSONObject> conversationHistory = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Sends a message to the Gemini API and returns the response asynchronously
     */
    public CompletableFuture<String> sendMessage(String userMessage, Consumer<String> onResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
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
    
    /**
     * Clears the conversation history
     */
    public void clearConversation() {
        conversationHistory.clear();
    }
} 