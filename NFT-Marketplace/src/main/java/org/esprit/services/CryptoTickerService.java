package org.esprit.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for providing cryptocurrency data for the ticker component
 * This version uses static data based on available icons rather than API calls
 */
public class CryptoTickerService {
    private static final Logger LOGGER = Logger.getLogger(CryptoTickerService.class.getName());

    /**
     * Provides cryptocurrency data asynchronously from static data
     * @param limit Number of cryptocurrencies to fetch (ignored as we return all available icons)
     * @return CompletableFuture with list of CryptoCurrency objects
     */
    public CompletableFuture<List<CryptoCurrency>> fetchTopCryptocurrencies(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Generating static cryptocurrency data based on available icons");
            return createStaticCryptoData();
        });
    }
    
    /**
     * Creates static cryptocurrency data based on available crypto icons
     * @return List of CryptoCurrency objects
     */
    private List<CryptoCurrency> createStaticCryptoData() {
        List<CryptoCurrency> cryptoList = new ArrayList<>();
        
        // Bitcoin
        cryptoList.add(new CryptoCurrency("bitcoin", "BTC", "Bitcoin", 68250.55, 1.25));
        
        // Ethereum
        cryptoList.add(new CryptoCurrency("ethereum", "ETH", "Ethereum", 3540.12, -0.85));
        
        // Tether
        cryptoList.add(new CryptoCurrency("tether", "USDT", "Tether", 1.0, 0.01));
        
        // Binance Coin
        cryptoList.add(new CryptoCurrency("binancecoin", "BNB", "Binance Coin", 605.75, 2.43));
        
        // Solana
        cryptoList.add(new CryptoCurrency("solana", "SOL", "Solana", 149.34, 4.67));
        
        // Ripple
        cryptoList.add(new CryptoCurrency("ripple", "XRP", "Ripple", 0.5452, -1.32));
        
        return cryptoList;
    }
    
    /**
     * No-op method as service doesn't need to be shutdown (no threads used)
     */
    public void shutdown() {
        // No-op since we're not using any executors or services that need shutdown
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