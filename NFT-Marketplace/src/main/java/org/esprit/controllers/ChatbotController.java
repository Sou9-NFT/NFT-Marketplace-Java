package org.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
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
    private boolean animationInProgress = false;
    
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
        
        // Ensure the container doesn't block mouse events
        chatbotContainer.setPickOnBounds(false);
        
        // But the toggle button itself should always capture clicks
        toggleChatButton.setPickOnBounds(true);
        
        // Setup event handlers
        setupEventHandlers();
        
        // Add welcome message
        Platform.runLater(() -> {
            addBotMessage("Hello! I'm your NFT Marketplace assistant. I can help you with raffles, NFTs, and marketplace features. How can I assist you today?");
        });
    }
    
    private void setupEventHandlers() {
        // Handle toggle button directly with mouse click
        toggleChatButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                System.out.println("Toggle button clicked!");
                toggleChatPanel();
                event.consume();
            }
        });
        
        // Send button click handler
        sendButton.setOnAction(event -> {
            handleSendMessageManually();
            event.consume();
        });
        
        // Enter key in message input
        messageInput.setOnAction(event -> {
            handleSendMessageManually();
            event.consume();
        });
        
        // Minimize button click handler
        if (minimizeButton != null) {
            minimizeButton.setOnAction(event -> {
                toggleChatPanel();
                event.consume();
            });
        }
        
        // Auto-scroll when new messages are added
        chatMessagesBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
        
        // Ensure chat panel events don't propagate
        chatPanel.addEventFilter(MouseEvent.ANY, Event::consume);
    }
    
    @FXML
    private void handleSendMessage(ActionEvent event) {
        processSendMessage();
        if (event != null) {
            event.consume();
        }
    }
    
    private void handleSendMessageManually() {
        processSendMessage();
    }
    
    private void processSendMessage() {
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
    
    private void toggleChatPanel() {
        System.out.println("Toggle chat panel called. Current state: " + 
                           (chatVisible ? "visible" : "hidden") +
                           ", Animation in progress: " + animationInProgress);
        
        if (animationInProgress) {
            return; // Prevent multiple clicks during animation
        }
        
        animationInProgress = true;
        
        if (!chatVisible) {
            // Show chat panel
            chatPanel.setVisible(true);
            chatPanel.setManaged(true);
            chatPanel.setOpacity(0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), chatPanel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                chatVisible = true;
                animationInProgress = false;
                System.out.println("Fade in complete. Chat is now visible.");
                
                // Focus on input field
                Platform.runLater(() -> messageInput.requestFocus());
            });
            fadeIn.play();
        } else {
            // Hide chat panel
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), chatPanel);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                chatPanel.setVisible(false);
                chatPanel.setManaged(false);
                chatVisible = false;
                animationInProgress = false;
                System.out.println("Fade out complete. Chat is now hidden.");
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