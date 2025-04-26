package org.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import org.esprit.components.ChatMessage;
import org.esprit.components.TypingIndicator;
import org.esprit.utils.GeminiChatbot;

/**
 * Controller for the Gemini chatbot
 */
public class ChatbotController {
    
    @FXML
    private StackPane chatbotContainer;
    
    @FXML
    private VBox chatPanel;
    
    @FXML
    private ScrollPane chatScrollPane;
    
    @FXML
    private VBox chatMessagesBox;
    
    @FXML
    private TextField messageInput;
    
    @FXML
    private Button sendButton;
    
    @FXML
    private Button toggleChatButton;
    
    @FXML
    private Button minimizeButton;
    
    private GeminiChatbot geminiChatbot;
    private TypingIndicator typingIndicator;
    private boolean chatVisible = false;
    
    @FXML
    public void initialize() {
        geminiChatbot = new GeminiChatbot();
        typingIndicator = new TypingIndicator();
        
        // Configure chat messages container
        chatMessagesBox.setSpacing(10);
        chatMessagesBox.setPadding(new Insets(10));
        
        // Configure scroll pane
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVvalue(1.0); // Scroll to bottom
        
        // Hide chat panel initially
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        
        // Configure toggle button with a robot icon
        Text chatIcon = new Text("🤖");
        chatIcon.setStyle("-fx-font-size: 20px;");
        toggleChatButton.setGraphic(chatIcon);
        toggleChatButton.setPickOnBounds(true); // Make sure the button can be clicked
        
        // Setup event handlers
        setupEventHandlers();
        
        // Add welcome message
        Platform.runLater(() -> {
            addBotMessage("Hello! I'm your NFT Marketplace assistant. I can help you with raffles, NFTs, and marketplace features. How can I assist you today?");
        });
    }
    
    private void setupEventHandlers() {
        // Send button click handler
        sendButton.setOnAction(this::handleSendMessage);
        
        // Enter key in message input
        messageInput.setOnAction(this::handleSendMessage);
        
        // Toggle chat visibility
        toggleChatButton.setOnAction(event -> toggleChatVisibility());
        
        // Minimize button (same as toggle button for hiding)
        if (minimizeButton != null) {
            minimizeButton.setOnAction(event -> toggleChatVisibility());
        }
        
        // Auto-scroll when new messages are added
        chatMessagesBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
        
        // Ensure the button never becomes mouse transparent
        toggleChatButton.setMouseTransparent(false);
        
        // For the chat panel, we want to consume events to prevent them from leaking to parent
        chatPanel.addEventFilter(MouseEvent.ANY, event -> {
            event.consume();
        });
        
        // Make sure chatbot container doesn't interfere with parent scrolling
        chatbotContainer.setPickOnBounds(false);
    }
    
    /**
     * Setup event propagation to prevent chat components from blocking parent scrolling
     */
    private void setupEventPropagation() {
        // This method is no longer needed since we're handling it in setupEventHandlers
    }
    
    @FXML
    private void handleSendMessage(ActionEvent event) {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addUserMessage(message);
        
        // Clear input field
        messageInput.clear();
        
        // Show typing indicator
        showTypingIndicator();
        
        // Send message to Gemini API
        geminiChatbot.sendMessage(message, response -> {
            Platform.runLater(() -> {
                // Hide typing indicator
                hideTypingIndicator();
                
                // Add bot response
                addBotMessage(response);
            });
        });
    }
    
    private void toggleChatVisibility() {
        chatVisible = !chatVisible;
        
        if (chatVisible) {
            // Show chat panel with animation
            chatPanel.setVisible(true);
            chatPanel.setManaged(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), chatPanel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // Focus on input field
            Platform.runLater(() -> messageInput.requestFocus());
        } else {
            // Hide chat panel with animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), chatPanel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                chatPanel.setVisible(false);
                chatPanel.setManaged(false);
            });
            fadeOut.play();
        }
    }
    
    private void addUserMessage(String message) {
        ChatMessage userMessage = new ChatMessage(message, true);
        chatMessagesBox.getChildren().add(userMessage);
        
        // Scroll to the bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    private void addBotMessage(String message) {
        ChatMessage botMessage = new ChatMessage(message, false);
        chatMessagesBox.getChildren().add(botMessage);
        
        // Scroll to the bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    private void showTypingIndicator() {
        // Position it at the bottom of the chat
        typingIndicator.start();
        chatMessagesBox.getChildren().add(typingIndicator);
        
        // Scroll to the bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    private void hideTypingIndicator() {
        typingIndicator.stop();
        chatMessagesBox.getChildren().remove(typingIndicator);
    }
    
    /**
     * Cleanup resources when controller is no longer needed
     */
    public void cleanup() {
        // Nothing to cleanup for now
    }
} 