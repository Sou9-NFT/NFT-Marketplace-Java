package org.esprit.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.exception.TwilioException;
import com.twilio.exception.ApiException;

public class SmsService {
    // Live credentials from the account
    private static final String ACCOUNT_SID = "AC16b766cefb70c4dfdda1a5d2aaa55dcd";
    private static final String AUTH_TOKEN = "0384156b4e0875d251d4bef6c57a5150";
    private static final String FROM_NUMBER = "+19342390245"; // Twilio verified number
    private boolean initialized = false;

    public SmsService() {
        System.out.println("Initializing SMS notification service with live credentials");
        initializeTwilio();
    }

    private void initializeTwilio() {
        if (!initialized) {
            try {
                System.out.println("Initializing Twilio with Live Account SID: " + ACCOUNT_SID);
                Twilio.init(ACCOUNT_SID, AUTH_TOKEN);  // Fixed: passing credentials directly
                initialized = true;
                System.out.println("Twilio initialized successfully with live credentials");
            } catch (Exception e) {
                System.err.println("Failed to initialize Twilio: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Twilio", e);
            }
        }
    }

    public void sendTradeOfferNotification(String toNumber, String message) {
        try {
            // Format the recipient's number
            String formattedNumber = formatPhoneNumber(toNumber);
            
            System.out.println("Sending SMS message...");
            System.out.println("From: " + FROM_NUMBER);
            System.out.println("To: " + formattedNumber);
            System.out.println("Message: " + message);

            // Send the message
            Message messageResult = Message.creator(
                new PhoneNumber(formattedNumber),
                new PhoneNumber(FROM_NUMBER),
                message
            ).create();

            System.out.println("Message SID: " + messageResult.getSid());
            System.out.println("Message Status: " + messageResult.getStatus());
            System.out.println("SMS message sent successfully");
            
        } catch (ApiException e) {
            System.err.println("Twilio API Error: " + e.getMessage());
            System.err.println("Error Code: " + e.getCode());
            System.err.println("More Info: " + e.getMoreInfo());
            throw new RuntimeException("Failed to send SMS message", e);
        } catch (Exception e) {
            System.err.println("Error sending SMS message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send SMS message", e);
        }
    }

    private String formatPhoneNumber(String number) {
        // Remove any existing prefixes or formatting
        String cleanNumber = number.replaceAll("[^0-9]", "");
        
        // If number doesn't start with country code, add it
        if (!cleanNumber.startsWith("216")) {
            cleanNumber = "216" + cleanNumber;
        }
        
        // Add + prefix
        return "+" + cleanNumber;
    }
}
