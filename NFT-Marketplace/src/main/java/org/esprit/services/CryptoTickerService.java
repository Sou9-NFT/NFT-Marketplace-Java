package org.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service for fetching cryptocurrency data from CoinCap API v3
 */
public class CryptoTickerService {
    private static final Logger LOGGER = Logger.getLogger(CryptoTickerService.class.getName());
    
    // Using the correct endpoint for CoinCap API v3 with apiKey as URL parameter
    private static final String API_BASE_URL = "https://rest.coincap.io/v3/assets";
    
    // Your API Key
    private static final String API_KEY = "312723c86e41e4cac9b9d536a4869d6c934b4240c24ed84d6f42e998a65ee3a0"; 
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    /**
     * Fetches the top cryptocurrencies asynchronously using CoinCap API
     * @param limit Number of cryptocurrencies to fetch (default 5)
     * @return CompletableFuture with list of CryptoCurrency objects
     */
    public CompletableFuture<List<CryptoCurrency>> fetchTopCryptocurrencies(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // Construct the URL with apiKey parameter and limit
            String apiUrl = String.format("%s?apiKey=%s&limit=%d", API_BASE_URL, API_KEY, limit);
            List<CryptoCurrency> result = fetchFromApi(apiUrl, limit);
            
            if (result.isEmpty()) {
                System.out.println("API call failed, returning mock data");
                return createMockData();
            }
            
            return result;
        }, executorService);
    }
    
    /**
     * Attempts to fetch cryptocurrency data from the API
     * @param apiUrl The complete API URL including parameters
     * @param limit Number of cryptocurrencies to fetch
     * @return List of CryptoCurrency objects or empty list on failure
     */
    private List<CryptoCurrency> fetchFromApi(String apiUrl, int limit) {
        // Debug output
        System.out.println("\n====== CoinCap API Request ======");
        System.out.println("API URL: " + apiUrl.replace(API_KEY, API_KEY.substring(0, 5) + "..." + API_KEY.substring(API_KEY.length() - 5)));
        
        List<CryptoCurrency> cryptoList = new ArrayList<>();
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            
            // Debug request headers
            System.out.println("\n== Request Headers ==");
            connection.getRequestProperties().forEach((key, values) -> 
                System.out.println(key + ": " + String.join(", ", values)));
            
            // Make the connection
            connection.connect();
            int responseCode = connection.getResponseCode();
            System.out.println("\n== Response Code: " + responseCode + " ==");
            
            // Debug response headers
            System.out.println("== Response Headers ==");
            connection.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    System.out.println(key + ": " + String.join(", ", values));
                }
            });
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // Print the first 500 chars of response for debugging
                    String responseStr = response.toString();
                    System.out.println("\n== Response Preview (truncated) ==");
                    System.out.println(responseStr.length() > 500 
                        ? responseStr.substring(0, 500) + "..." 
                        : responseStr);
                    
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    
                    // Parse the JSON response based on the v3 API structure
                    JSONArray dataArray = null;
                    
                    // Check for various potential response structures
                    if (jsonResponse.has("data")) {
                        Object dataObj = jsonResponse.get("data");
                        if (dataObj instanceof JSONArray) {
                            dataArray = (JSONArray) dataObj;
                            System.out.println("Found data array with " + dataArray.length() + " items");
                        } else if (dataObj instanceof JSONObject) {
                            JSONObject dataObject = (JSONObject) dataObj;
                            // Look for arrays within the data object
                            for (String key : dataObject.keySet()) {
                                if (dataObject.get(key) instanceof JSONArray) {
                                    System.out.println("Found array in data." + key + " with " + 
                                                     dataObject.getJSONArray(key).length() + " items");
                                    dataArray = dataObject.getJSONArray(key);
                                    break;
                                }
                            }
                            
                            if (dataArray == null) {
                                System.out.println("No array found in data object. Keys: " + dataObject.keySet());
                                return new ArrayList<>();
                            }
                        }
                    } else {
                        // Look for arrays at the top level
                        for (String key : jsonResponse.keySet()) {
                            if (jsonResponse.get(key) instanceof JSONArray) {
                                System.out.println("Found top-level array in " + key + " with " + 
                                                 jsonResponse.getJSONArray(key).length() + " items");
                                dataArray = jsonResponse.getJSONArray(key);
                                break;
                            }
                        }
                        
                        if (dataArray == null) {
                            System.out.println("No array found in response. Keys: " + jsonResponse.keySet());
                            return new ArrayList<>();
                        }
                    }
                    
                    // Process the data array
                    for (int i = 0; i < dataArray.length() && i < limit; i++) {
                        JSONObject crypto = dataArray.getJSONObject(i);
                        
                        // Debug the structure of one crypto object
                        if (i == 0) {
                            System.out.println("\n== Sample crypto object structure ==");
                            System.out.println("Keys: " + crypto.keySet());
                        }
                        
                        // Extract cryptocurrency data with fallbacks for different field names
                        String id = crypto.optString("id", crypto.optString("assetId", ""));
                        String symbol = crypto.optString("symbol", "");
                        String name = crypto.optString("name", "");
                        
                        // Handle different price field formats
                        double price = 0.0;
                        if (crypto.has("priceUsd")) {
                            try {
                                price = Double.parseDouble(crypto.optString("priceUsd", "0.0"));
                            } catch (NumberFormatException e) {
                                price = crypto.optDouble("priceUsd", 0.0);
                            }
                        } else if (crypto.has("price")) {
                            price = crypto.optDouble("price", 0.0);
                        } else if (crypto.has("market_data") && crypto.getJSONObject("market_data").has("price_usd")) {
                            price = crypto.getJSONObject("market_data").optDouble("price_usd", 0.0);
                        }
                        
                        // Handle different change percent field formats
                        double changePercent = 0.0;
                        if (crypto.has("changePercent24Hr")) {
                            try {
                                changePercent = Double.parseDouble(crypto.optString("changePercent24Hr", "0.0"));
                            } catch (NumberFormatException e) {
                                changePercent = crypto.optDouble("changePercent24Hr", 0.0);
                            }
                        } else if (crypto.has("change24h")) {
                            changePercent = crypto.optDouble("change24h", 0.0);
                        } else if (crypto.has("percent_change_24h")) {
                            changePercent = crypto.optDouble("percent_change_24h", 0.0);
                        }
                        
                        System.out.println(name + " (" + symbol + ") - Price: $" + price + 
                                          ", Change: " + String.format("%.2f%%", changePercent));
                        
                        cryptoList.add(new CryptoCurrency(id, symbol, name, price, changePercent));
                    }
                    
                    System.out.println("\n== Successfully processed " + cryptoList.size() + 
                                     " cryptocurrencies ==");
                    return cryptoList;
                }
            } else {
                LOGGER.log(Level.WARNING, "Error response - Code: " + responseCode);
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    String errorDetails = errorResponse.toString();
                    System.out.println("== API Error Response ==\n" + errorDetails);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception with API call", e);
            System.out.println("== Exception with API call ==");
            e.printStackTrace(System.out);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            System.out.println("====== End of API Request ======\n");
        }
        
        // Return empty list to indicate failure
        return new ArrayList<>();
    }
    
    /**
     * Creates mock cryptocurrency data in case the API call fails
     * @return List of mock CryptoCurrency objects (Top 5)
     */
    private List<CryptoCurrency> createMockData() {
        System.out.println("Generating mock cryptocurrency data...");
        List<CryptoCurrency> mockList = new ArrayList<>();
        mockList.add(new CryptoCurrency("bitcoin", "BTC", "Bitcoin", 68250.55, 1.25));
        mockList.add(new CryptoCurrency("ethereum", "ETH", "Ethereum", 3540.12, -0.85));
        mockList.add(new CryptoCurrency("tether", "USDT", "Tether", 1.0, 0.01));
        mockList.add(new CryptoCurrency("binancecoin", "BNB", "Binance Coin", 605.75, 2.43));
        mockList.add(new CryptoCurrency("solana", "SOL", "Solana", 149.34, 4.67));
        return mockList;
    }
    
    /**
     * Shuts down the executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Class representing cryptocurrency data
     */
    public static class CryptoCurrency {
        private final String id;
        private final String symbol;
        private final String name;
        private final double price;
        private final double changePercent24h;
        
        public CryptoCurrency(String id, String symbol, String name, double price, double changePercent24h) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.changePercent24h = changePercent24h;
        }
        
        // Getters and other methods unchanged
        public String getId() { return id; }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public double getChangePercent24h() { return changePercent24h; }
        public boolean isPositiveChange() { return changePercent24h > 0; }
        public boolean isNegativeChange() { return changePercent24h < 0; }
        
        public String getFormattedPrice() {
            if (price < 1.0) {
                return String.format("$%.4f", price);
            } else if (price < 10.0) {
                return String.format("$%.2f", price);
            } else {
                return String.format("$%.2f", price);
            }
        }
        
        public String getFormattedChange() {
            return String.format("%.2f%%", changePercent24h);
        }
    }
}