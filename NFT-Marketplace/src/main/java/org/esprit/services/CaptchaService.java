package org.esprit.services;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.effect.DropShadow;
import java.util.Random;

public class CaptchaService {
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 6;
    private static final Random random = new Random();

    public static String generateCaptchaText() {
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            captcha.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return captcha.toString();
    }

    public static Canvas generateCaptchaImage(String captchaText) {
        Canvas canvas = new Canvas(300, 100);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Set background with gradient
        gc.setFill(Color.rgb(25, 25, 25)); // Dark background
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Add some noise/pattern
        for (int i = 0; i < 50; i++) {
            gc.setStroke(Color.rgb(
                40 + random.nextInt(40),
                40 + random.nextInt(40),
                40 + random.nextInt(40),
                0.5
            ));
            gc.strokeLine(
                random.nextDouble() * canvas.getWidth(),
                random.nextDouble() * canvas.getHeight(),
                random.nextDouble() * canvas.getWidth(),
                random.nextDouble() * canvas.getHeight()
            );
        }

        // Set text properties
        gc.setEffect(new DropShadow(10, Color.CYAN));
        gc.setFill(Color.WHITE);
        gc.setFont(Font.loadFont(CaptchaService.class.getResourceAsStream("/styles/fonts/cyberpunk.ttf"), 48));
        if (gc.getFont() == null) {
            gc.setFont(Font.font("Impact", 48)); // Fallback font
        }

        // Draw each character with slight rotation and position variation
        double x = 40;
        for (char c : captchaText.toCharArray()) {
            // Rotate the canvas slightly for each character
            double rotation = -15 + random.nextDouble() * 30;
            gc.save();
            gc.translate(x, 60 + random.nextDouble() * 10);
            gc.rotate(rotation);
            gc.fillText(String.valueOf(c), 0, 0);
            gc.restore();
            x += 35 + random.nextDouble() * 10;
        }

        // Add some glowing lines
        gc.setStroke(Color.CYAN.deriveColor(1, 1, 1, 0.3));
        gc.setLineWidth(2);
        for (int i = 0; i < 3; i++) {
            gc.strokeLine(0, random.nextDouble() * canvas.getHeight(),
                         canvas.getWidth(), random.nextDouble() * canvas.getHeight());
        }

        return canvas;
    }
}