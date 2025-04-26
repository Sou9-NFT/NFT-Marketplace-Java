package org.esprit.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

/**
 * Service for cryptocurrency-related operations
 * Provides methods to fetch current prices and convert between currencies
 */
public class CryptoService {
    
    // Default conversion rate: 1 Dannous = 0.0000005 ETH
    private static final double DANNOUS_TO_ETH_RATE = 0.0000005;
    
    private static final HttpClient client = HttpClient.newHttpClient();
    
    /**
     * Fetches current Ethereum price from CoinGecko API
     * @return Current ETH price in USD, or -1 if there was an error
     */
    public double fetchEthereumPrice() {
        try {
            // CoinGecko free API for ETH price in USD
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONObject ethereum = jsonResponse.getJSONObject("ethereum");
                double ethPriceInUsd = ethereum.getDouble("usd");
                
                System.out.println("Current ETH price: $" + ethPriceInUsd);
                return ethPriceInUsd;
            } else {
                System.err.println("API Error: " + response.statusCode());
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Error fetching ETH price: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Converts Dannous value to Ethereum equivalent
     * @param dannousAmount The amount in Dannous
     * @return The equivalent amount in ETH
     */
    public double convertDannousToEth(double dannousAmount) {
        // Calculate ETH equivalent using the fixed rate
        return dannousAmount * DANNOUS_TO_ETH_RATE;
    }
    
    /**
     * Formats a price with both Dannous and ETH equivalent
     * @param dannousAmount The amount in Dannous
     * @return Formatted string with both currencies
     */
    public String formatPriceWithEth(double dannousAmount) {
        String priceText = String.format("%.2f Dannous", dannousAmount);
        
        // Fetch current ETH price
        double ethPrice = fetchEthereumPrice();
        
        // Only add ETH equivalent if API fetch was successful
        if (ethPrice > 0) {
            double ethAmount = convertDannousToEth(dannousAmount);
            priceText += String.format(" (≈ %.8f ETH)", ethAmount);
        }
        
        return priceText;
    }
}
