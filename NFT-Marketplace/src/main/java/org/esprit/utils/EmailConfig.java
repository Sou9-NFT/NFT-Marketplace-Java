package org.esprit.utils;

/**
 * EmailConfig contains the configuration for sending emails via SMTP.
 */
public class EmailConfig {
    // Email server settings
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    public static final String SMTP_AUTH = "true";
    public static final String SMTP_STARTTLS_ENABLE = "true";
    
    // Email credentials from the provided MAILER_DSN
    public static final String SENDER_EMAIL = "linuxattack69@gmail.com";
    public static final String SENDER_PASSWORD = "rkxp ynem idog mnre"; // App password for Gmail
    
    // Application settings
    public static final String APP_NAME = "NFT Marketplace";
    public static final String BASE_URL = "http://localhost"; // Base URL for password reset links
}