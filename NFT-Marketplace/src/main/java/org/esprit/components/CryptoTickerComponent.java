package org.esprit.components;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.esprit.services.CryptoTickerService;
import org.esprit.services.CryptoTickerService.CryptoCurrency;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Component for displaying a scrolling cryptocurrency ticker
 * Uses only local icons from the crypto folder with infinite scrolling like a news ticker
 */
public class CryptoTickerComponent {
    private static final Logger LOGGER = Logger.getLogger(CryptoTickerComponent.class.getName());
    private final ScrollPane scrollPane;
    private final HBox cryptoTickerBox;
    private final CryptoTickerService service;
    private Timeline scrollingTimeline;
    private final Map<String, Image> cryptoIcons = new HashMap<>();
    private boolean isFirstLoad = true;
    
    public CryptoTickerComponent() {
        this.service = new CryptoTickerService();
        
        // Initialize the crypto icons map with available icons from the crypto folder
        loadLocalCryptoIcons();
        
        cryptoTickerBox = new HBox();
        cryptoTickerBox.setAlignment(Pos.CENTER_LEFT);
        // Increase spacing between crypto items for better readability
        cryptoTickerBox.setSpacing(120);
        cryptoTickerBox.getStyleClass().add("crypto-ticker");
        
        scrollPane = new ScrollPane(cryptoTickerBox);
        scrollPane.getStyleClass().add("crypto-ticker-scroll");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        
        // Create fancy container with gradient background
        StackPane container = new StackPane(scrollPane);
        container.getStyleClass().add("crypto-ticker-container");
        scrollPane.setContent(cryptoTickerBox);
        scrollPane.setFitToHeight(true);
        
        // Display loading message initially
        Label loadingLabel = new Label("Loading cryptocurrency data...");
        loadingLabel.getStyleClass().add("crypto-loading");
        cryptoTickerBox.getChildren().add(loadingLabel);
        
        // Load crypto data once based on the local icons
        loadCryptoData();
    }
    
    /**
     * Get the ScrollPane containing the ticker
     * @return ScrollPane with the crypto ticker
     */
    public ScrollPane getView() {
        return scrollPane;
    }
    
    /**
     * Load cryptocurrency data from the service - called only once
     */
    private void loadCryptoData() {
        service.fetchTopCryptocurrencies(cryptoIcons.size())
                .thenAccept(this::updateTickerUI);
    }
    
    /**
     * Update the ticker UI with cryptocurrency data
     * @param cryptoList List of CryptoCurrency objects
     */
    private void updateTickerUI(List<CryptoCurrency> cryptoList) {
        Platform.runLater(() -> {
            cryptoTickerBox.getChildren().clear();
            
            // Add items twice to ensure continuous scrolling when it loops back
            for (int i = 0; i < 2; i++) {
                for (CryptoCurrency crypto : cryptoList) {
                    HBox cryptoItem = createCryptoTickerItem(crypto);
                    cryptoTickerBox.getChildren().add(cryptoItem);
                }
            }
            
            // Start scrolling animation after items are loaded
            if (isFirstLoad) {
                startScrollingAnimation();
                isFirstLoad = false;
            }
        });
    }
    
    /**
     * Create a ticker item for a cryptocurrency
     * @param crypto CryptoCurrency object
     * @return HBox containing the cryptocurrency ticker item
     */
    private HBox createCryptoTickerItem(CryptoCurrency crypto) {
        HBox item = new HBox();
        item.setAlignment(Pos.CENTER);
        // Increase spacing within each crypto item
        item.setSpacing(15);
        item.getStyleClass().add("crypto-item");
        // Set minimum width to ensure text has enough space
        item.setMinWidth(220);
        item.setPrefWidth(220);
        
        // Cryptocurrency icon
        StackPane iconContainer = createCryptoIconContainer(crypto.getId(), crypto.getSymbol());
        
        // Symbol with more space
        Label symbolLabel = new Label(crypto.getSymbol());
        symbolLabel.getStyleClass().add("crypto-symbol");
        symbolLabel.setMinWidth(50);
        
        // Price
        Label priceLabel = new Label(crypto.getFormattedPrice());
        priceLabel.getStyleClass().add("crypto-price");
        priceLabel.setMinWidth(80);
        
        // Change percentage with up/down arrow
        HBox changeContainer = new HBox(5);
        changeContainer.setAlignment(Pos.CENTER);
        changeContainer.setMinWidth(70);
        
        Label changeLabel = new Label(crypto.getFormattedChange());
        
        if (crypto.isPositiveChange()) {
            changeLabel.getStyleClass().add("crypto-change-up");
            // Add up arrow icon
            ImageView upArrow = createArrowIcon("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiIgZmlsbD0iIzE2Yzc4NCIgdmlld0JveD0iMCAwIDE2IDE2Ij48cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik04IDVhLjUuNSAwIDAgMSAuNS41djUuNzkzbDIuMTQ2LTIuMTQ3YS41LjUgMCAwIDEgLjcwOC43MDhsLTMgM2EuNS41IDAgMCAxLS43MDggMGwtMy0zYS41LjUgMCAwIDEgLjcwOC0uNzA4TDcuNSAxMS4yOTNWNS41QS41LjUgMCAwIDEgOCA1eiIvPjwvc3ZnPg==");
            changeContainer.getChildren().addAll(upArrow, changeLabel);
        } else if (crypto.isNegativeChange()) {
            changeLabel.getStyleClass().add("crypto-change-down");
            // Add down arrow icon
            ImageView downArrow = createArrowIcon("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiIgZmlsbD0iI2VhMzk0MyIgdmlld0JveD0iMCAwIDE2IDE2Ij48cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik04IDEwYS41LjUgMCAwIDEtLjUtLjVWMy43MDdMNS4zNTQgNS44NTRhLjUuNSAwIDEgMS0uNzA4LS43MDhsMy0zYS41LjUgMCAwIDEgLjcwOCAwbDMgM2EuNS41IDAgMCAxLS43MDguNzA4TDguNSAzLjcwN1Y5LjVhLjUuNSAwIDAgMS0uNS41eiIvPjwvc3ZnPg==");
            changeContainer.getChildren().addAll(downArrow, changeLabel);
        } else {
            changeLabel.getStyleClass().add("crypto-change-neutral");
            changeContainer.getChildren().add(changeLabel);
        }
        
        // Simplify the layout - just icon, symbol, price and change
        item.getChildren().addAll(iconContainer, symbolLabel, priceLabel, changeContainer);
        return item;
    }
    
    /**
     * Create a container with cryptocurrency icon
     * @param cryptoId Cryptocurrency ID
     * @param symbol Cryptocurrency symbol as fallback
     * @return StackPane containing the icon
     */
    private StackPane createCryptoIconContainer(String cryptoId, String symbol) {
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("crypto-icon-bg");
        
        ImageView iconView;
        if (cryptoIcons.containsKey(cryptoId.toLowerCase())) {
            iconView = new ImageView(cryptoIcons.get(cryptoId.toLowerCase()));
        } else if (cryptoIcons.containsKey(symbol.toLowerCase())) {
            iconView = new ImageView(cryptoIcons.get(symbol.toLowerCase()));
        } else {
            // Create a fallback icon with the first letter of the symbol
            Label symbolIcon = new Label(symbol.substring(0, 1).toUpperCase());
            symbolIcon.getStyleClass().add("crypto-symbol-fallback");
            iconContainer.getChildren().add(symbolIcon);
            return iconContainer;
        }
        
        iconView.setFitWidth(24);
        iconView.setFitHeight(24);
        iconView.getStyleClass().add("crypto-icon");
        
        // Add a clip to make the icon circular
        Rectangle clip = new Rectangle(24, 24);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        iconView.setClip(clip);
        
        iconContainer.getChildren().add(iconView);
        return iconContainer;
    }
    
    /**
     * Create an arrow icon for price change indication
     * @param svgUrl SVG data URL for the arrow
     * @return ImageView with the arrow icon
     */
    private ImageView createArrowIcon(String svgUrl) {
        ImageView arrow = new ImageView(new Image(svgUrl));
        arrow.setFitWidth(12);
        arrow.setFitHeight(12);
        arrow.getStyleClass().add("crypto-change-arrow");
        return arrow;
    }
    
    /**
     * Load cryptocurrency icons from the crypto folder
     */
    private void loadLocalCryptoIcons() {
        LOGGER.info("Loading cryptocurrency icons from assets/crypto folder");
        
        // Define the mapping of filenames to crypto IDs and symbols for our available icons
        String[][] iconMappings = {
            // filename, id, symbol
            {"bitcoin.png", "bitcoin", "btc"},
            {"eth.png", "ethereum", "eth"},
            {"tether.png", "tether", "usdt"},
            {"BNB.png", "binancecoin", "bnb"},
            {"SOL.png", "solana", "sol"},
            {"xrp.png", "ripple", "xrp"}
        };
        
        // Load each icon
        for (String[] mapping : iconMappings) {
            try {
                String iconPath = "/assets/crypto/" + mapping[0];
                InputStream is = getClass().getResourceAsStream(iconPath);
                if (is != null) {
                    Image icon = new Image(is);
                    // Add by ID (lowercase for consistent lookup)
                    cryptoIcons.put(mapping[1].toLowerCase(), icon);
                    // Also add by symbol (lowercase for consistent lookup)
                    cryptoIcons.put(mapping[2].toLowerCase(), icon);
                    
                    LOGGER.info("Loaded icon: " + mapping[0] + " for " + mapping[1]);
                } else {
                    LOGGER.warning("Could not find icon: " + mapping[0]);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error loading icon: " + mapping[0], e);
            }
        }
        
        LOGGER.info("Loaded " + (cryptoIcons.size() / 2) + " cryptocurrency icons");
    }
    
    /**
     * Start or restart the scrolling animation for infinite scrolling effect
     * News ticker style - continuous scrolling without user interaction
     */
    private void startScrollingAnimation() {
        // Stop any existing animation
        if (scrollingTimeline != null) {
            scrollingTimeline.stop();
        }
        
        // Reset scroll position
        scrollPane.setHvalue(0);
        
        // Create a true infinite scrolling animation (news ticker style)
        scrollingTimeline = new Timeline(
            new KeyFrame(Duration.millis(30), event -> {
                // Only perform scrolling calculation if component is visible and sized
                if (scrollPane.getWidth() <= 0) {
                    return;
                }

                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double contentWidth = cryptoTickerBox.getBoundsInLocal().getWidth();
                
                // If content doesn't need scrolling, don't scroll
                if (contentWidth <= viewportWidth) {
                    return;
                }
                
                // Calculate halfway point - when we reach half the content, jump back to start
                // This creates a seamless loop since we duplicated all content
                double halfwayPoint = 0.5;
                
                // Get current scroll position
                double currentHValue = scrollPane.getHvalue();
                
                // If we've reached halfway, loop back to start for seamless scrolling
                if (currentHValue >= halfwayPoint) {
                    // Jump back to start without animation for perfect loop
                    scrollPane.setHvalue(0);
                } else {
                    // Continue smooth scrolling
                    // Adjust speed based on content length for consistent pace
                    double scrollSpeed = Math.max(0.0003, 0.0007 / (contentWidth / 3000));
                    scrollPane.setHvalue(currentHValue + scrollSpeed);
                }
            })
        );
        
        scrollingTimeline.setCycleCount(Animation.INDEFINITE);
        scrollingTimeline.play();
    }
    
    /**
     * Clean up resources when component is no longer needed
     */
    public void dispose() {
        if (scrollingTimeline != null) {
            scrollingTimeline.stop();
        }
        
        service.shutdown();
    }
}