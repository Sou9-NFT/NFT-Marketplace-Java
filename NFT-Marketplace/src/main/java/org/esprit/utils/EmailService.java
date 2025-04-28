package org.esprit.utils;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * EmailService handles sending emails for the application.
 */
public class EmailService {
    
    /**
     * Sends an email with the specified details.
     * 
     * @param to The recipient email address
     * @param subject The email subject
     * @param htmlContent The HTML content of the email
     * @return True if the email was sent successfully, false otherwise
     */
    public static boolean sendEmail(String to, String subject, String htmlContent) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", EmailConfig.SMTP_AUTH);
        props.put("mail.smtp.starttls.enable", EmailConfig.SMTP_STARTTLS_ENABLE);
        props.put("mail.smtp.host", EmailConfig.SMTP_HOST);
        props.put("mail.smtp.port", EmailConfig.SMTP_PORT);
        
        // Create an authenticated session
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailConfig.SENDER_EMAIL, EmailConfig.SENDER_PASSWORD);
            }
        });
        
        try {
            // Create a new message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.SENDER_EMAIL, EmailConfig.APP_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Send the message
            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Sends a password reset email to the user with the reset code directly.
     * 
     * @param to The user's email address
     * @param name The user's name
     * @param resetToken The password reset token
     * @return True if the email was sent successfully, false otherwise
     */
    public static boolean sendPasswordResetEmail(String to, String name, String resetToken) {
        String subject = "Reset Your Password - " + EmailConfig.APP_NAME;
        
        // Create the email content with the token directly instead of a link
        String htmlContent = 
            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
            "<h2>Hello " + name + ",</h2>" +
            "<p>We received a request to reset your password for your " + EmailConfig.APP_NAME + " account.</p>" +
            "<p>Please use the following reset code to reset your password. This code will expire in 1 hour.</p>" +
            "<div style='text-align: center; margin: 30px 0;'>" +
            "<div style='background-color: #f5f5f5; padding: 15px; border: 1px solid #ddd; border-radius: 4px; " +
            "font-family: monospace; font-size: 16px; letter-spacing: 1px; word-break: break-all;'>" + resetToken + "</div>" +
            "</div>" +
            "<p>Return to the application and paste this code when prompted.</p>" +
            "<p>If you did not request a password reset, you can safely ignore this email.</p>" +
            "<p>Best regards,<br>The " + EmailConfig.APP_NAME + " Team</p>" +
            "</div>";
        
        return sendEmail(to, subject, htmlContent);
    }
}