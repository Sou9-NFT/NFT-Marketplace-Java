package org.esprit.utils;

import java.util.Random;

public class CaptchaGenerator {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    
    public static String generateCaptchaText() {
        StringBuilder captcha = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            captcha.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return captcha.toString();
    }
    
    public static void drawCaptcha(javafx.scene.canvas.Canvas canvas, String text) {
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Clear the canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw background with noise
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            gc.setStroke(javafx.scene.paint.Color.rgb(
                200 + random.nextInt(55),
                200 + random.nextInt(55),
                200 + random.nextInt(55)
            ));
            gc.strokeLine(
                random.nextDouble() * canvas.getWidth(),
                random.nextDouble() * canvas.getHeight(),
                random.nextDouble() * canvas.getWidth(),
                random.nextDouble() * canvas.getHeight()
            );
        }
        
        // Draw the text
        gc.setFill(javafx.scene.paint.Color.rgb(50, 50, 50));
        gc.setFont(new javafx.scene.text.Font("Arial Bold", 24));
        
        // Add some random rotation to each character
        double x = 20;
        for (char c : text.toCharArray()) {
            gc.save();
            gc.translate(x, canvas.getHeight() / 2);
            gc.rotate(-15 + random.nextDouble() * 30);
            gc.fillText(String.valueOf(c), 0, 0);
            gc.restore();
            x += 20;
        }
        
        // Add some random lines over text
        gc.setStroke(javafx.scene.paint.Color.rgb(100, 100, 100, 0.5));
        for (int i = 0; i < 3; i++) {
            gc.strokeLine(
                0, random.nextDouble() * canvas.getHeight(),
                canvas.getWidth(), random.nextDouble() * canvas.getHeight()
            );
        }
    }
}
