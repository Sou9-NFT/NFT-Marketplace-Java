package org.esprit.components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying chat messages
 */
public class ChatMessage extends VBox {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final String message;
    private final boolean isUserMessage;
    private final LocalDateTime timestamp;
    
    public ChatMessage(String message, boolean isUserMessage) {
        this.message = message;
        this.isUserMessage = isUserMessage;
        this.timestamp = LocalDateTime.now();
        
        initialize();
    }
    
    private void initialize() {
        // Create message label
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("message-bubble");
        if (isUserMessage) {
            messageLabel.getStyleClass().add("user-message");
        } else {
            messageLabel.getStyleClass().add("bot-message");
        }
        
        // Create timestamp label
        Label timeLabel = new Label(timestamp.format(TIME_FORMATTER));
        timeLabel.getStyleClass().add("timestamp");
        
        // Add children to VBox
        getChildren().addAll(messageLabel, timeLabel);
        
        // Set VBox alignment and styling
        setSpacing(2);
        setPadding(new Insets(5, 10, 5, 10));
        setAlignment(isUserMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setMaxWidth(USE_PREF_SIZE);
    }
} 