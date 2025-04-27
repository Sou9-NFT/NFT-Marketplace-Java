package org.esprit.components;

import java.util.List;

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
import javafx.util.Duration;

/**
 * Component for displaying a scrolling cryptocurrency ticker
 */
public class CryptoTickerComponent {
    private final ScrollPane scrollPane;
    private final HBox cryptoTickerBox;
    private final CryptoTickerService service;
    private Timeline scrollingTimeline;
    private Timeline refreshTimeline;
    
    public CryptoTickerComponent() {
        this.service = new CryptoTickerService();
        
        cryptoTickerBox = new HBox();
        cryptoTickerBox.setAlignment(Pos.CENTER_LEFT);
        cryptoTickerBox.setSpacing(20);
        cryptoTickerBox.getStyleClass().add("crypto-ticker");
        
        scrollPane = new ScrollPane(cryptoTickerBox);
        scrollPane.getStyleClass().add("crypto-ticker-scroll");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        
        // Display loading message initially
        Label loadingLabel = new Label("Loading cryptocurrency data...");
        loadingLabel.getStyleClass().add("crypto-loading");
        cryptoTickerBox.getChildren().add(loadingLabel);
        
        // Load crypto data
        loadCryptoData();
        
        // Set up auto refresh every 2 minutes
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.minutes(2), event -> loadCryptoData())
        );
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }
    
    /**
     * Get the ScrollPane containing the ticker
     * @return ScrollPane with the crypto ticker
     */
    public ScrollPane getView() {
        return scrollPane;
    }
    
    /**
     * Load cryptocurrency data from the service
     */
    private void loadCryptoData() {
        service.fetchTopCryptocurrencies(5)
                .thenAccept(this::updateTickerUI);
    }
    
    /**
     * Update the ticker UI with cryptocurrency data
     * @param cryptoList List of CryptoCurrency objects
     */
    private void updateTickerUI(List<CryptoCurrency> cryptoList) {
        Platform.runLater(() -> {
            cryptoTickerBox.getChildren().clear();
            
            // Add cryptocurrency items to the ticker
            for (CryptoCurrency crypto : cryptoList) {
                HBox cryptoItem = createCryptoTickerItem(crypto);
                cryptoTickerBox.getChildren().add(cryptoItem);
            }
            
            // Start scrolling animation after items are loaded
            startScrollingAnimation();
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
        item.setSpacing(8);
        item.getStyleClass().add("crypto-item");
        
        // Symbol and name
        Label nameLabel = new Label(crypto.getSymbol());
        nameLabel.getStyleClass().add("crypto-name");
        
        // Price
        Label priceLabel = new Label(crypto.getFormattedPrice());
        priceLabel.getStyleClass().add("crypto-price");
        
        // Change percentage with up/down arrow
        Label changeLabel = new Label(crypto.getFormattedChange());
        if (crypto.isPositiveChange()) {
            changeLabel.getStyleClass().add("crypto-change-up");
            // Add up arrow icon
            ImageView upArrow = createArrowIcon("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='%2316c784' viewBox='0 0 16 16'><path d='M8 4a.5.5 0 0 1 .5.5v5.793l2.146-2.147a.5.5 0 0 1 .708.708l-3 3a.5.5 0 0 1-.708 0l-3-3a.5.5 0 1 1 .708-.708L7.5 10.293V4.5A.5.5 0 0 1 8 4z'/></svg>");
            changeLabel.setGraphic(upArrow);
        } else if (crypto.isNegativeChange()) {
            changeLabel.getStyleClass().add("crypto-change-down");
            // Add down arrow icon
            ImageView downArrow = createArrowIcon("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='%23ea3943' viewBox='0 0 16 16'><path d='M8 4a.5.5 0 0 1 .5.5v5.793l2.146-2.147a.5.5 0 0 1 .708.708l-3 3a.5.5 0 0 1-.708 0l-3-3a.5.5 0 1 1 .708-.708L7.5 10.293V4.5A.5.5 0 0 1 8 4z'/></svg>");
            changeLabel.setGraphic(downArrow);
        } else {
            changeLabel.getStyleClass().add("crypto-change-neutral");
        }
        
        item.getChildren().addAll(nameLabel, priceLabel, changeLabel);
        return item;
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
        arrow.getStyleClass().add("crypto-icon");
        return arrow;
    }
    
    /**
     * Start or restart the scrolling animation
     */
    private void startScrollingAnimation() {
        // Stop any existing animation
        if (scrollingTimeline != null) {
            scrollingTimeline.stop();
        }
        
        // Reset scroll position
        scrollPane.setHvalue(0);
        
        // Create new scrolling animation
        scrollingTimeline = new Timeline(
            new KeyFrame(Duration.millis(50), event -> {
                double currentScroll = scrollPane.getHvalue();
                double step = 0.001; // Slow scrolling speed
                
                // When reaching the end, loop back to start
                if (currentScroll >= 0.99) {
                    scrollPane.setHvalue(0);
                } else {
                    scrollPane.setHvalue(currentScroll + step);
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
        
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        
        service.shutdown();
    }
}