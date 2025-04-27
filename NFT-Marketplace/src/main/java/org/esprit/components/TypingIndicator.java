package org.esprit.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Custom component that shows a "typing" animation with three dots
 */
public class TypingIndicator extends HBox {
    
    private final Circle dot1 = new Circle(4);
    private final Circle dot2 = new Circle(4);
    private final Circle dot3 = new Circle(4);
    
    private final Timeline animation;
    
    public TypingIndicator() {
        getStyleClass().add("typing-indicator");
        setSpacing(5);
        
        // Add styling to dots
        dot1.getStyleClass().add("typing-dot");
        dot2.getStyleClass().add("typing-dot");
        dot3.getStyleClass().add("typing-dot");
        
        getChildren().addAll(dot1, dot2, dot3);
        
        // Create animation
        animation = createAnimation();
    }
    
    /**
     * Creates the bouncing animation for the dots
     */
    private Timeline createAnimation() {
        // Create timeline for dots animation
        Timeline timeline = new Timeline();
        
        // Dot 1 animation
        KeyValue kv1a = new KeyValue(dot1.translateYProperty(), -5);
        KeyValue kv1b = new KeyValue(dot1.translateYProperty(), 0);
        KeyFrame kf1a = new KeyFrame(Duration.ZERO, kv1b);
        KeyFrame kf1b = new KeyFrame(Duration.millis(300), kv1a);
        KeyFrame kf1c = new KeyFrame(Duration.millis(600), kv1b);
        
        // Dot 2 animation (delayed start)
        KeyValue kv2a = new KeyValue(dot2.translateYProperty(), -5);
        KeyValue kv2b = new KeyValue(dot2.translateYProperty(), 0);
        KeyFrame kf2a = new KeyFrame(Duration.ZERO, kv2b);
        KeyFrame kf2b = new KeyFrame(Duration.millis(400), kv2a);
        KeyFrame kf2c = new KeyFrame(Duration.millis(700), kv2b);
        
        // Dot 3 animation (more delayed start)
        KeyValue kv3a = new KeyValue(dot3.translateYProperty(), -5);
        KeyValue kv3b = new KeyValue(dot3.translateYProperty(), 0);
        KeyFrame kf3a = new KeyFrame(Duration.ZERO, kv3b);
        KeyFrame kf3b = new KeyFrame(Duration.millis(500), kv3a);
        KeyFrame kf3c = new KeyFrame(Duration.millis(800), kv3b);
        
        // Add all keyframes to timeline
        timeline.getKeyFrames().addAll(
                kf1a, kf1b, kf1c,
                kf2a, kf2b, kf2c,
                kf3a, kf3b, kf3c
        );
        
        // Set timeline to cycle indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        return timeline;
    }
    
    /**
     * Start the animation
     */
    public void start() {
        animation.play();
    }
    
    /**
     * Stop the animation
     */
    public void stop() {
        animation.stop();
    }
} 