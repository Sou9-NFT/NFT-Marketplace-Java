package org.esprit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for managing application configuration and API keys
 */
public class ConfigManager {
    
    private static final Properties properties = new Properties();
    private static ConfigManager instance = null;
    
    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Unable to find config.properties file");
            }
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
        }
    }
    
    private ConfigManager() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get the singleton instance of ConfigManager
     * @return The ConfigManager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * Get a configuration property by key
     * @param key The property key
     * @return The property value, or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get a configuration property by key with a default value
     * @param key The property key
     * @param defaultValue The default value to return if the key is not found
     * @return The property value, or the default value if not found
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get the Gemini API key
     * @return The Gemini API key
     */
    public String getGeminiApiKey() {
        return getProperty("gemini.api.key");
    }
    
    /**
     * Get the OpenAI API key
     * @return The OpenAI API key
     */
    public String getOpenAiApiKey() {
        return getProperty("openai.api.key");
    }
}
