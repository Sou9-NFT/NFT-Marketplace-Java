package org.esprit.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Utility class for text translation services.
 * Provides caching and rate limiting to optimize API usage.
 */
public class TranslationService {
    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private static final int MAX_REQUESTS_PER_HOUR = 1000; // MyMemory API limit
    private static final long REQUEST_INTERVAL = TimeUnit.HOURS.toMillis(1) / MAX_REQUESTS_PER_HOUR;
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^\\p{L}\\p{N}\\s.,!?-]");
    
    private final HttpClient httpClient;
    private final Map<String, String> languageCodes;
    private final Map<String, String> translationCache;
    private long lastRequestTime;

    private static TranslationService instance;

    /**
     * Private constructor for singleton pattern
     */
    private TranslationService() {
        this.httpClient = HttpClient.newHttpClient();
        this.translationCache = new ConcurrentHashMap<>();
        this.lastRequestTime = 0;
        this.languageCodes = Map.of(
            "French", "fr",
            "Spanish", "es",
            "German", "de", 
            "Italian", "it",
            "Arabic", "ar",
            "English", "en"
        );
    }

    /**
     * Get singleton instance of TranslationService
     * @return TranslationService instance
     */
    public static synchronized TranslationService getInstance() {
        if (instance == null) {
            instance = new TranslationService();
        }
        return instance;
    }

    /**
     * Clean text by removing or replacing problematic characters
     * @param text Text to clean
     * @return Cleaned text
     */
    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
          // Replace smart quotes with regular quotes
        text = text.replace('\u201C', '"').replace('\u201D', '"')
                  .replace('\u2018', '\'').replace('\u2019', '\'');
                  
        // Replace em dash and en dash with regular dash
        text = text.replace('—', '-').replace('–', '-');
                  
        // Replace other special characters
        return SPECIAL_CHARS.matcher(text).replaceAll(" ");
    }

    /**
     * Translate text to target language with rate limiting and caching
     * @param text Text to translate
     * @param targetLang Target language code
     * @param sourceLang Source language code
     * @return Translated text
     * @throws RuntimeException if translation fails
     */
    public String translate(String text, String targetLang, String sourceLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Clean the text before translation
        String cleanedText = cleanText(text.trim());
        
        // Generate cache key
        String cacheKey = cleanedText + "|" + sourceLang + "|" + targetLang;
        
        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null) {
            return cachedTranslation;
        }

        // Apply rate limiting
        applyRateLimit();        try {
            // Encode each parameter separately
            String encodedText = URLEncoder.encode(cleanedText, StandardCharsets.UTF_8);
            String encodedLangPair = URLEncoder.encode(sourceLang + "|" + targetLang, StandardCharsets.UTF_8);
            
            // Build URL with properly encoded parameters
            String url = API_URL + "?q=" + encodedText + "&langpair=" + encodedLangPair;

            // Print URL for debugging
            System.out.println("Translation URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "NFTMarketplace/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());            // Print response for debugging
            System.out.println("Translation API Response: " + response.body());
            
            JSONObject jsonResponse = new JSONObject(response.body());
            
            // Check HTTP response code
            if (response.statusCode() != 200) {
                throw new TranslationException("HTTP error " + response.statusCode() + 
                    ": " + response.body());
            }
            
            // Check for translation errors
            if (jsonResponse.has("responseStatus")) {
                int status = jsonResponse.getInt("responseStatus");
                if (status != 200) {
                    String errorMsg = jsonResponse.optString("responseDetails", "Unknown translation error");
                    throw new TranslationException("Translation API error " + status + ": " + errorMsg);
                }
            }

            // Validate response structure
            if (!jsonResponse.has("responseData") || 
                !jsonResponse.getJSONObject("responseData").has("translatedText")) {
                throw new TranslationException("Invalid API response format: " + response.body());
            }

            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            String translation = responseData.getString("translatedText");
            
            // Cache the successful translation
            translationCache.put(cacheKey, translation);
            
            return translation;
        } catch (IOException e) {
            throw new TranslationException("Network error while translating: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation was interrupted", e);
        } catch (Exception e) {
            throw new TranslationException("Unexpected error during translation: " + e.getMessage(), e);
        }
    }

    /**
     * Get language code for a given language name
     * @param languageName Full language name
     * @return ISO language code, defaults to "en" if not found
     */
    public String getLanguageCode(String languageName) {
        return languageCodes.getOrDefault(languageName, "en");
    }

    /**
     * Enforce rate limiting between API requests
     */
    private void applyRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < REQUEST_INTERVAL) {
            try {
                Thread.sleep(REQUEST_INTERVAL - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TranslationException("Rate limiting interrupted", e);
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Clear the translation cache
     */
    public void clearCache() {
        translationCache.clear();
    }
}

/**
 * Custom exception for translation errors
 */
class TranslationException extends RuntimeException {
    public TranslationException(String message) {
        super(message);
    }
    
    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
