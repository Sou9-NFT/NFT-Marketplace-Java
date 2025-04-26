package org.esprit.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Utility class for registering custom URL protocol handlers to allow the application
 * to be launched from URLs like "nftmarketplace://reset-password?token=xyz123"
 */
public class UrlProtocolHandler {
    
    private static final String PROTOCOL_NAME = "nftmarketplace";
    
    /**
     * Registers the application as a handler for the custom URL protocol.
     * Currently only supports Windows.
     * 
     * @return true if registration was successful, false otherwise
     */
    public static boolean registerProtocolHandler() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return registerWindowsProtocolHandler();
        } else if (osName.contains("mac")) {
            // macOS protocol handler registration (not implemented yet)
            System.out.println("macOS protocol handler registration not implemented yet");
            return false;
        } else if (osName.contains("linux")) {
            // Linux protocol handler registration (not implemented yet)
            System.out.println("Linux protocol handler registration not implemented yet");
            return false;
        }
        
        return false;
    }
    
    /**
     * Registers the application as a URL protocol handler on Windows
     * 
     * @return true if registration was successful, false otherwise
     */
    private static boolean registerWindowsProtocolHandler() {
        try {
            // Get the absolute path to the JAR file
            String jarPath = new File(UrlProtocolHandler.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            
            // If running from a class file rather than a JAR, use the classpath
            if (!jarPath.endsWith(".jar")) {
                jarPath = Paths.get(System.getProperty("user.dir"), "target", "NFT-Marketplace-1.0-SNAPSHOT.jar")
                        .toAbsolutePath().toString();
            }
            
            // Create registry commands
            String command = String.format(
                "reg add \"HKCU\\Software\\Classes\\%s\" /ve /d \"URL:NFT Marketplace Protocol\" /f & " +
                "reg add \"HKCU\\Software\\Classes\\%s\" /v \"URL Protocol\" /d \"\" /f & " +
                "reg add \"HKCU\\Software\\Classes\\%s\\shell\\open\\command\" /ve /d \"javaw -jar \\\"%s\\\" %%1\" /f",
                PROTOCOL_NAME, PROTOCOL_NAME, PROTOCOL_NAME, jarPath);
            
            // Execute registry commands
            Process process = Runtime.getRuntime().exec(new String[] { "cmd.exe", "/c", command });
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("Successfully registered URL protocol handler: " + PROTOCOL_NAME);
                return true;
            } else {
                System.err.println("Failed to register URL protocol handler: Exit code " + exitCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error registering URL protocol handler: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Parses a custom URL into component parts.
     * 
     * @param url the URL to parse (e.g. "nftmarketplace://reset-password?token=xyz123")
     * @return an array containing [action, token] or null if the URL is invalid
     */
    public static String[] parseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Format: nftmarketplace://action?token=xyz123
        String prefix = PROTOCOL_NAME + "://";
        
        if (!url.startsWith(prefix)) {
            return null;
        }
        
        try {
            // Remove the protocol prefix
            String remaining = url.substring(prefix.length());
            
            // Split by the query parameter delimiter
            String[] parts = remaining.split("\\?", 2);
            
            if (parts.length < 1) {
                return null;
            }
            
            String action = parts[0];
            String token = "";
            
            // Parse query parameters
            if (parts.length > 1 && parts[1].startsWith("token=")) {
                token = parts[1].substring(6);
            }
            
            return new String[] { action, token };
        } catch (Exception e) {
            System.err.println("Error parsing URL: " + e.getMessage());
            return null;
        }
    }
}