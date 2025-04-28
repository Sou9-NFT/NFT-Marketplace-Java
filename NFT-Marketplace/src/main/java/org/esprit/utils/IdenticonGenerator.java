package org.esprit.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.imageio.ImageIO;

/**
 * Generates GitHub-style identicon avatars based on user data.
 * Creates a unique geometric pattern based on hash of user email or other identifier.
 */
public class IdenticonGenerator {
    private static final int GRID_SIZE = 5; // 5x5 grid for the identicon
    private static final int BLOCK_SIZE = 70; // Size of each block in pixels
    private static final int PADDING = 20; // Padding around the grid
    
    /**
     * Generates an identicon for a user based on their email or identifier
     * @param identifier Usually email or username to generate a unique pattern from
     * @param outputPath Path to save the generated image
     * @return Path to the generated image
     */
    public static String generateIdenticon(String identifier, String outputDir) throws IOException, NoSuchAlgorithmException {
        // Create output directory if it doesn't exist
        Path outputDirPath = Paths.get(outputDir);
        if (!Files.exists(outputDirPath)) {
            Files.createDirectories(outputDirPath);
        }
        
        // Calculate hash from identifier
        byte[] hash = calculateHash(identifier);
        
        // Determine the foreground color from the hash
        int r = Math.abs(hash[0] & 0xFF) % 130 + 80; // Range 80-210 for better visibility
        int g = Math.abs(hash[1] & 0xFF) % 130 + 80;
        int b = Math.abs(hash[2] & 0xFF) % 130 + 80;
        Color foregroundColor = new Color(r, g, b);
        
        // Use white as the background
        Color backgroundColor = Color.WHITE;
        
        // Total size of the image including padding
        int size = GRID_SIZE * BLOCK_SIZE + 2 * PADDING;
        
        // Create a Java AWT image
        BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        
        // Fill background
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, size, size);
        
        // Generate the pattern - we only need to determine the left half due to symmetry
        boolean[][] pattern = new boolean[GRID_SIZE][GRID_SIZE];
        
        // Fill the left half of the grid based on hash
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < Math.ceil(GRID_SIZE / 2.0); x++) {
                // Get a bit from the hash
                int index = y * 3 + x;
                boolean bit = ((hash[index % hash.length] >> (index / hash.length)) & 1) == 1;
                pattern[y][x] = bit;
                
                // Mirror for the right side (except the middle column in a 5x5 grid)
                if (x < GRID_SIZE / 2) {
                    pattern[y][GRID_SIZE - 1 - x] = bit;
                }
            }
        }
        
        // Draw the pattern
        g2d.setColor(foregroundColor);
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (pattern[y][x]) {
                    g2d.fillRect(
                        PADDING + x * BLOCK_SIZE,
                        PADDING + y * BLOCK_SIZE,
                        BLOCK_SIZE,
                        BLOCK_SIZE
                    );
                }
            }
        }
        
        g2d.dispose(); // Clean up graphics resources
        
        // Create filename based on hash of identifier
        String fileName = "identicon_" + Math.abs(identifier.hashCode()) + ".png";
        File outputFile = new File(outputDir, fileName);
        
        // Save the image
        ImageIO.write(bufferedImage, "png", outputFile);
        
        return outputFile.getName();
    }
    
    private static byte[] calculateHash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(input.getBytes());
    }
}