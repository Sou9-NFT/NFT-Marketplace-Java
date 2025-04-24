package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

/**
 * Service class for handling bet session questions and providing information about bet sessions
 */
public class BetSessionAssistantService {
    
    private Connection connection;
    private BetSessionService betSessionService;
    private UserService userService;
    
    // Common bet session questions and their predefined answers
    private static final Map<String, String> COMMON_QUESTIONS = new HashMap<>();
    
    // Greetings and responses
    private static final Map<String, String> GREETINGS = new HashMap<>();
    
    // Thank you responses
    private static final Map<String, String> THANK_YOU_RESPONSES = new HashMap<>();
    
    // Bet session related keywords
    private static final String[] BET_SESSION_KEYWORDS = {
        "bet", "session", "auction", "bid", "bidding", "price", "artwork", 
        "current", "initial", "join", "participate", "how", "active", "ended", 
        "status", "rules", "start", "end", "time", "process", "winner",
        "highest", "value", "author", "creator", "artist", "step", "help", 
        "guide", "when", "where", "what", "need", "requirement", "cost",
        "enter", "participation", "cancel", "confirm", "submit", "button"
    };
    
    // Non-bet session keywords
    private static final String[] NON_BET_SESSION_KEYWORDS = {
        "raffle", "blog", "post", "article", "comment", "write", "read",
        "trade", "trading", "exchange", "swap", "offer", "dispute", 
        "profile", "account", "wallet", "balance", "inventory", 
        "collection", "category", "marketplace"
    };
    
    static {
        // Initialize common questions and answers
        COMMON_QUESTIONS.put("process", "Here's the participation process for bet sessions:\n" +
            "1. Find an active bet session\n" +
            "2. Review the artwork and current price\n" +
            "3. Enter your bid amount (must be higher than current price)\n" +
            "4. Confirm your bid\n\n" +
            "Remember: You must be logged in to place bids! ✨");
            
        COMMON_QUESTIONS.put("participate", "Want to join a bet session? Here's how:\n" + 
            "1. Look for active bet sessions\n" +
            "2. Check the current price\n" +
            "3. Enter a bid higher than the current price\n" +
            "4. Confirm your bid\n\n" +
            "Make sure you're logged in first! 🎯");
            
        COMMON_QUESTIONS.put("how to bid", "Here's how to place a bid:\n" +
            "1. Open an active bet session\n" +
            "2. Enter a bid amount above the current price\n" +
            "3. Click 'Place Bid'\n" +
            "4. Confirm your bid\n\n" +
            "The highest bidder when the session ends wins! 💰");
            
        COMMON_QUESTIONS.put("rules", "📜 Bet Session Rules:\n" +
            "- Must be logged in to participate\n" +
            "- Bids must exceed the current price\n" +
            "- Active sessions accept bids\n" +
            "- Ended sessions no longer accept bids\n" +
            "- Highest bidder when session ends wins");
            
        COMMON_QUESTIONS.put("status", "🎯 Bet Session Status:\n" +
            "- Pending: Scheduled to start soon\n" +
            "- Active: Currently accepting bids\n" +
            "- Ended: No longer accepting bids\n" +
            "- Cancelled: Session was cancelled\n\n" +
            "Only active sessions accept bids!");
            
        COMMON_QUESTIONS.put("winner", "🏆 Winner Selection:\n" +
            "- The highest bidder when the session ends wins\n" +
            "- In case of equal bids, the earlier bid wins\n" +
            "- Check session status to see if it has ended\n" +
            "- Winners are notified automatically");
            
        COMMON_QUESTIONS.put("start", "Starting times:\n" +
            "- Each bet session shows start and end times\n" +
            "- You can only bid during the active period\n" +
            "- Pending sessions haven't started yet\n" +
            "- Active sessions are currently accepting bids ⏰");
            
        COMMON_QUESTIONS.put("active", "Active bet sessions:\n" +
            "- These are currently accepting bids\n" +
            "- You can place multiple bids on the same session\n" +
            "- Each bid must be higher than the current price\n" +
            "- Sessions automatically end at their end time 🎯");
            
        COMMON_QUESTIONS.put("ended", "Ended bet sessions:\n" +
            "- No longer accept bids\n" +
            "- The highest bidder is the winner\n" +
            "- Cannot be reactivated\n" +
            "- Artwork goes to the highest bidder ⌛");
        
        // Initialize greetings
        GREETINGS.put("hi", "👋 Hello! I'm your bet session assistant. How can I help you today?");
        GREETINGS.put("hello", "Hi there! 👋 I'm here to help you with bet sessions. What would you like to know?");
        GREETINGS.put("hey", "Hey! 😊 Ready to help you with any bet session questions!");
        GREETINGS.put("good morning", "Good morning! 🌅 Ready to help you explore our bet sessions!");
        GREETINGS.put("good afternoon", "Good afternoon! ☀️ Looking to join a bet session today?");
        GREETINGS.put("good evening", "Good evening! 🌙 Need help with our bet sessions?");
        
        // Initialize thank you responses
        THANK_YOU_RESPONSES.put("thank", "You're welcome! 😊 Happy to help with your bet session questions!");
        THANK_YOU_RESPONSES.put("thanks", "You're welcome! 🌟 Let me know if you need anything else about bet sessions!");
        THANK_YOU_RESPONSES.put("thx", "No problem at all! 👍 Feel free to ask more about bet sessions!");
        THANK_YOU_RESPONSES.put("ty", "You're welcome! 🎉 Good luck with your bidding!");
    }
    
    /**
     * Constructor initializing the service with necessary dependencies
     */
    public BetSessionAssistantService() {
        connection = DatabaseConnection.getInstance().getConnection();
        betSessionService = new BetSessionService();
        userService = new UserService();
    }
    
    /**
     * Checks if a message is related to bet sessions
     * @param message The user message
     * @return true if the message is related to bet sessions, false otherwise
     */
    private boolean isBetSessionRelated(String message) {
        message = message.toLowerCase();
        
        // First check if it's explicitly about non-bet session topics
        for (String keyword : NON_BET_SESSION_KEYWORDS) {
            if (message.contains(keyword)) {
                return false;
            }
        }
        
        // Then check if it's about bet sessions
        for (String keyword : BET_SESSION_KEYWORDS) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Find a matching predefined answer for common questions
     * @param message The user message
     * @return The predefined answer if found, null otherwise
     */
    private String findMatchingPredefinedAnswer(String message) {
        message = message.toLowerCase();
        String[] words = message.split("\\s+");
        
        for (Map.Entry<String, String> entry : COMMON_QUESTIONS.entrySet()) {
            String keyword = entry.getKey();
            String answer = entry.getValue();
            String[] keywordParts = keyword.split("\\s+");
            int matches = 0;
            
            // Check each part of the keyword against each word in the message
            for (String part : keywordParts) {
                for (String word : words) {
                    if (word.contains(part) || part.contains(word)) {
                        matches++;
                        break;
                    }
                }
            }
            
            // If all parts of the keyword are found, return the answer
            if (matches == keywordParts.length) {
                return answer;
            }
        }
        
        // Check for single keyword matches if no full phrase match
        for (String word : words) {
            for (Map.Entry<String, String> entry : COMMON_QUESTIONS.entrySet()) {
                String keyword = entry.getKey();
                String answer = entry.getValue();
                
                if (word.contains(keyword) || keyword.contains(word)) {
                    return answer;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a message is a greeting
     * @param message The user message
     * @return true if the message is a greeting, false otherwise
     */
    private boolean isGreeting(String message) {
        message = message.toLowerCase();
        for (String greeting : GREETINGS.keySet()) {
            if (message.contains(greeting)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the appropriate greeting response
     * @param message The user message
     * @return The greeting response
     */
    private String getGreetingResponse(String message) {
        message = message.toLowerCase();
        for (Map.Entry<String, String> entry : GREETINGS.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return GREETINGS.get("hello"); // Default greeting
    }
    
    /**
     * Check if a message is a thank you message
     * @param message The user message
     * @return true if the message is a thank you, false otherwise
     */
    private boolean isThankYou(String message) {
        message = message.toLowerCase();
        for (String keyword : THANK_YOU_RESPONSES.keySet()) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the appropriate thank you response
     * @param message The user message
     * @return The thank you response
     */
    private String getThankYouResponse(String message) {
        message = message.toLowerCase();
        for (Map.Entry<String, String> entry : THANK_YOU_RESPONSES.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return THANK_YOU_RESPONSES.get("thank"); // Default thank you response
    }
    
    /**
     * Get information about active bet sessions
     * @return List of active bet sessions with their information
     */
    private List<Map<String, Object>> getActiveBetSessionsData() {
        List<Map<String, Object>> betSessionsInfo = new ArrayList<>();
        
        try {
            List<BetSession> activeBetSessions = betSessionService.getAllBetSessions()
                .stream()
                .filter(session -> "active".equals(session.getStatus()))
                .toList();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (BetSession session : activeBetSessions) {
                Map<String, Object> sessionInfo = new HashMap<>();
                Artwork artwork = session.getArtwork();
                User author = session.getAuthor();
                
                sessionInfo.put("id", session.getId());
                sessionInfo.put("artworkTitle", artwork != null ? artwork.getTitle() : "Unknown Artwork");
                sessionInfo.put("author", author != null ? author.getName() : "Unknown Author");
                sessionInfo.put("currentPrice", session.getCurrentPrice());
                sessionInfo.put("initialPrice", session.getInitialPrice());
                sessionInfo.put("endTime", session.getEndTime() != null ? session.getEndTime().format(formatter) : "Unknown");
                sessionInfo.put("numberOfBids", session.getNumberOfBids());
                
                betSessionsInfo.add(sessionInfo);
            }
        } catch (Exception e) {
            System.err.println("Error getting bet session data: " + e.getMessage());
        }
        
        return betSessionsInfo;
    }
    
    /**
     * Check if the message is a query about bet sessions by a specific author
     * @param message The user message
     * @return true if the message is an author query, false otherwise
     */
    private boolean isAuthorQuery(String message) {
        message = message.toLowerCase();
        String[] patterns = {
            "bet sessions? (created|made) by",
            "show.*bet sessions?.*(by|from)",
            "list.*bet sessions?.*(by|from)",
            "find.*bet sessions?.*(by|from)",
            "(created|made).*(by|from).*bet sessions?"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract the author name from an author query
     * @param message The user message
     * @return The extracted author name, or null if not found
     */
    private String extractAuthorName(String message) {
        message = message.toLowerCase();
        String[] patterns = {
            "(?:bet sessions? (?:created|made) by) ([a-zA-Z0-9@._-]+)",
            "(?:show.*bet sessions?.*(?:by|from)) ([a-zA-Z0-9@._-]+)",
            "(?:list.*bet sessions?.*(?:by|from)) ([a-zA-Z0-9@._-]+)",
            "(?:find.*bet sessions?.*(?:by|from)) ([a-zA-Z0-9@._-]+)",
            "(?:created|made).*(?:by|from) ([a-zA-Z0-9@._-]+).*bet sessions?"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }
    
    /**
     * Find bet sessions created by a specific author
     * @param authorName The name of the author
     * @return List of bet sessions created by the author
     */
    private List<BetSession> findBetSessionsByAuthor(String authorName) {
        List<BetSession> authorSessions = new ArrayList<>();
        
        try {
            // Get all users matching the name pattern
            String query = "SELECT id FROM user WHERE username LIKE ? OR email LIKE ?";
            List<Integer> authorIds = new ArrayList<>();
            
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, "%" + authorName + "%");
                ps.setString(2, "%" + authorName + "%");
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        authorIds.add(rs.getInt("id"));
                    }
                }
            }
            
            // Get bet sessions for each author
            for (int authorId : authorIds) {
                authorSessions.addAll(betSessionService.getSessionsByAuthor(authorId));
            }
        } catch (Exception e) {
            System.err.println("Error finding bet sessions by author: " + e.getMessage());
        }
        
        return authorSessions;
    }
    
    /**
     * Check if the message is a query about a specific bet session status
     * @param message The user message
     * @return true if the message is a status query, false otherwise
     */
    private boolean isBetSessionStatusQuery(String message) {
        message = message.toLowerCase();
        String[] patterns = {
            "bet session (?:number |#)?(\\d+)",
            "is bet session (?:number |#)?(\\d+)",
            "status of bet session (?:number |#)?(\\d+)",
            "check bet session (?:number |#)?(\\d+)",
            "bet session (?:named |called |titled )?[\"\'](.+?)[\"\']",
            "is (.+?) bet session",
            "status of (.+?) bet session"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract the bet session identifier from a status query
     * @param message The user message
     * @return Map containing the type and value of the identifier
     */
    private Map<String, Object> extractBetSessionIdentifier(String message) {
        message = message.toLowerCase();
        Map<String, Object> result = new HashMap<>();
        result.put("type", null);
        result.put("value", null);
        
        // Check for ID patterns
        String[] idPatterns = {
            "bet session (?:number |#)?(\\d+)",
            "is bet session (?:number |#)?(\\d+)",
            "status of bet session (?:number |#)?(\\d+)",
            "check bet session (?:number |#)?(\\d+)"
        };
        
        for (String patternStr : idPatterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                result.put("type", "id");
                result.put("value", Integer.parseInt(matcher.group(1)));
                return result;
            }
        }
        
        // Check for title patterns
        String[] titlePatterns = {
            "bet session (?:named |called |titled )?[\"\'](.+?)[\"\']",
            "is (.+?) bet session",
            "status of (.+?) bet session"
        };
        
        for (String patternStr : titlePatterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                result.put("type", "title");
                result.put("value", matcher.group(1).trim());
                return result;
            }
        }
        
        return result;
    }
    
    /**
     * Find a bet session by ID
     * @param id The ID of the bet session
     * @return The bet session, or null if not found
     */
    private BetSession findBetSessionById(int id) {
        try {
            return betSessionService.getOne(id);
        } catch (Exception e) {
            System.err.println("Error finding bet session by ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a formatted response with information about a bet session
     * @param session The bet session
     * @return Formatted string with bet session information
     */
    private String getBetSessionStatusResponse(BetSession session) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String status = "🎯 Bet Session for: " + (session.getArtwork() != null ? session.getArtwork().getTitle() : "Unknown Artwork") + " (ID: " + session.getId() + ")\n";
        status += "   • Status: " + getStatusEmoji(session.getStatus()) + " " + capitalizeFirstLetter(session.getStatus()) + "\n";
        status += "   • Created by: " + (session.getAuthor() != null ? session.getAuthor().getName() : "Unknown") + "\n";
        status += "   • Initial Price: $" + session.getInitialPrice() + "\n";
        status += "   • Current Price: $" + session.getCurrentPrice() + "\n";
        status += "   • Number of Bids: " + session.getNumberOfBids() + "\n";
        
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime endTime = session.getEndTime();
        
        if (startTime != null) {
            status += "   • Start Time: " + startTime.format(formatter) + "\n";
        }
        
        if (endTime != null) {
            status += "   • End Time: " + endTime.format(formatter) + "\n";
        }
        
        if ("active".equals(session.getStatus())) {
            status += "\nThis bet session is active! You can place a bid above the current price of $" + session.getCurrentPrice() + ".";
        } else if ("pending".equals(session.getStatus())) {
            status += "\nThis bet session hasn't started yet. It will begin at " + (startTime != null ? startTime.format(formatter) : "a scheduled time") + ".";
        } else if ("ended".equals(session.getStatus())) {
            status += "\nThis bet session has ended. No more bids can be placed.";
        } else if ("cancelled".equals(session.getStatus())) {
            status += "\nThis bet session was cancelled.";
        }
        
        return status;
    }
    
    /**
     * Get an emoji based on bet session status
     * @param status The status string
     * @return An emoji representing the status
     */
    private String getStatusEmoji(String status) {
        switch (status.toLowerCase()) {
            case "active": return "🟢";
            case "pending": return "🟡";
            case "ended": return "⚫";
            case "cancelled": return "🔴";
            default: return "❓";
        }
    }
    
    /**
     * Capitalize the first letter of a string
     * @param input The input string
     * @return The input string with the first letter capitalized
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
    
    /**
     * Generate a response to a user message
     * @param userMessage The message from the user
     * @return A response to the user message
     */
    public String generateResponse(String userMessage) {
        // First check if the message is about non-bet session topics
        String message = userMessage.toLowerCase();
        for (String keyword : NON_BET_SESSION_KEYWORDS) {
            if (message.contains(keyword)) {
                return "I apologize, but I'm specialized in bet session questions only. For questions about " + keyword + "s, please check the relevant section of our application or contact support. 🎯\n\nI'd be happy to help you with any bet session-related questions though!";
            }
        }
        
        // Check for specific bet session status query
        if (isBetSessionStatusQuery(userMessage)) {
            Map<String, Object> identifier = extractBetSessionIdentifier(userMessage);
            BetSession session = null;
            
            if ("id".equals(identifier.get("type"))) {
                session = findBetSessionById((Integer) identifier.get("value"));
            }
            
            if (session != null) {
                return getBetSessionStatusResponse(session);
            } else {
                return "🔍 Sorry, I couldn't find that bet session. Please check the ID and try again.";
            }
        }
        
        // Check if this is an author query
        if (isAuthorQuery(userMessage)) {
            String authorName = extractAuthorName(userMessage);
            if (authorName != null) {
                List<BetSession> sessions = findBetSessionsByAuthor(authorName);
                if (!sessions.isEmpty()) {
                    StringBuilder response = new StringBuilder("📋 Bet Sessions created by " + authorName + ":\n\n");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    
                    for (BetSession session : sessions) {
                        response.append("🎯 ID: ").append(session.getId());
                        response.append(" - ");
                        response.append(session.getArtwork() != null ? session.getArtwork().getTitle() : "Unknown Artwork").append("\n");
                        response.append("   • Status: ").append(getStatusEmoji(session.getStatus())).append(" ");
                        response.append(capitalizeFirstLetter(session.getStatus())).append("\n");
                        response.append("   • Current Price: $").append(session.getCurrentPrice()).append("\n");
                        if (session.getCreatedAt() != null) {
                            response.append("   • Created: ").append(session.getCreatedAt().format(formatter)).append("\n");
                        }
                        response.append("\n");
                    }
                    return response.toString();
                } else {
                    return "🔍 No bet sessions found created by " + authorName + ". Make sure the name is correct!";
                }
            }
        }
        
        // Get current bet session data for the standard response
        List<Map<String, Object>> betSessionData = getActiveBetSessionsData();
        StringBuilder betSessionStatus = new StringBuilder("📊 Current Active Bet Sessions:\n\n");
        
        if (betSessionData.isEmpty()) {
            betSessionStatus.append("No active bet sessions at the moment.\n\n");
        } else {
            for (Map<String, Object> session : betSessionData) {
                betSessionStatus.append("🎯 ")
                    .append(session.get("artworkTitle"))
                    .append(" (ID: ")
                    .append(session.get("id"))
                    .append(")\n");
                betSessionStatus.append("   • Current Price: $").append(session.get("currentPrice")).append("\n");
                betSessionStatus.append("   • Bids: ").append(session.get("numberOfBids")).append("\n");
                betSessionStatus.append("   • Ends: ").append(session.get("endTime")).append("\n");
                betSessionStatus.append("\n");
            }
        }
        
        // Check for thank you messages first
        if (isThankYou(userMessage)) {
            return betSessionStatus.toString() + getThankYouResponse(userMessage);
        }
        
        // Check for greetings next
        if (isGreeting(userMessage)) {
            return betSessionStatus.toString() + getGreetingResponse(userMessage);
        }
        
        if (!isBetSessionRelated(userMessage)) {
            return betSessionStatus.toString() + "👋 Hi! While I'd love to chat, I'm specifically here to help with bet sessions. Feel free to ask me about joining bet sessions, rules, or anything bet session-related!";
        }
        
        // Check for predefined answers first
        String predefinedAnswer = findMatchingPredefinedAnswer(userMessage);
        if (predefinedAnswer != null) {
            return betSessionStatus.toString() + predefinedAnswer;
        }
        
        // Default response if no specific pattern matched
        return betSessionStatus.toString() + "To participate in a bet session, find an active one from the list above, review the current price, and place a bid higher than the current price. Let me know if you have specific questions about bet sessions!";
    }
}
