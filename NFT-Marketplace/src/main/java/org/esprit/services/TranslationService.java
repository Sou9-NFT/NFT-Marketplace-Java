package org.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;

public class TranslationService {
    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private final HttpClient httpClient;
    private final Map<String, String> languageCodes;

    public TranslationService() {
        this.httpClient = HttpClient.newHttpClient();
        this.languageCodes = Map.of(
            "French", "fr",
            "Spanish", "es",
            "German", "de",
            "Italian", "it",
            "Arabic", "ar"
        );
    }

    public String translate(String text, String targetLang, String sourceLang) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&langpair=%s|%s", 
                API_URL, encodedText, sourceLang, targetLang);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            
            // Check for translation errors
            if (jsonResponse.has("responseStatus") && jsonResponse.getInt("responseStatus") != 200) {
                throw new RuntimeException("Translation failed: " + 
                    jsonResponse.getString("responseDetails"));
            }

            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            return responseData.getString("translatedText");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to translate text: " + e.getMessage(), e);
        }
    }

    public String getLanguageCode(String languageName) {
        return languageCodes.getOrDefault(languageName, "en");
    }
}
