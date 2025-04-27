package org.esprit.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class GiphyService {
    private static final String API_KEY = "oYhxFl3EmvAIbVCAGZX1Wez29oeICkjq";
    private static final String API_URL = "https://api.giphy.com/v1/gifs/search";
    private static final int LIMIT = 20;  // Number of GIFs to return per search
    
    private final HttpClient httpClient;
    private static GiphyService instance;
    
    private GiphyService() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    public static synchronized GiphyService getInstance() {
        if (instance == null) {
            instance = new GiphyService();
        }
        return instance;
    }
    
    public JSONArray searchGifs(String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s?api_key=%s&q=%s&limit=%d&rating=g", 
            API_URL, API_KEY, encodedQuery, LIMIT);
            
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
                
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
                
        if (response.statusCode() != 200) {
            throw new IOException("Failed to search GIFs: " + response.body());
        }
        
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("data");
    }
}
