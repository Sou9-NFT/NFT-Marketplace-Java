package org.esprit.services;

import org.esprit.models.TradeDispute;
import org.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeDisputeService {
    
    public TradeDisputeService() {
        alterTableForLongEvidence();
    }
    
    public List<TradeDispute> getAllDisputes() {
        List<TradeDispute> disputes = new ArrayList<>();
        String query = "SELECT d.*, " +
                      "u.name as reporter_name, " +
                      "o.title as offered_title, " +
                      "r.title as received_title " +
                      "FROM " + TradeDispute.TABLE_NAME + " d " +
                      "LEFT JOIN user u ON d.reporter = u.id " +
                      "LEFT JOIN artwork o ON CAST(d.offered_item AS SIGNED) = o.id " +
                      "LEFT JOIN artwork r ON CAST(d.received_item AS SIGNED) = r.id";

        System.out.println("Executing query: " + query);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println("Created statement");
            ResultSet rs = stmt.executeQuery(query);
            System.out.println("Executed query successfully");

            while (rs.next()) {
                System.out.println("Found a dispute record");
                TradeDispute dispute = new TradeDispute();
                
                // Log each field as we read it
                int id = rs.getInt("id");
                System.out.println("Dispute ID: " + id);
                dispute.setId(id);
                
                int reporter = rs.getInt("reporter");
                System.out.println("Reporter ID: " + reporter);
                dispute.setReporter(reporter);
                
                int tradeId = rs.getInt("trade_id");
                System.out.println("Trade ID: " + tradeId);
                dispute.setTradeId(tradeId);
                
                String offeredItem = rs.getString("offered_item");
                System.out.println("Offered Item: " + offeredItem);
                dispute.setOfferedItem(offeredItem);
                
                String receivedItem = rs.getString("received_item");
                System.out.println("Received Item: " + receivedItem);
                dispute.setReceivedItem(receivedItem);
                
                String reason = rs.getString("reason");
                System.out.println("Reason: " + reason);
                dispute.setReason(reason);
                
                String evidence = rs.getString("evidence");
                System.out.println("Evidence: " + evidence);
                dispute.setEvidence(evidence);
                
                Timestamp timestamp = rs.getTimestamp("timestamp");
                System.out.println("Timestamp: " + timestamp);
                dispute.setTimestamp(timestamp.toLocalDateTime());
                
                String status = rs.getString("status");
                System.out.println("Status: " + status);
                dispute.setStatus(status);
                
                // Store the names and titles in the dispute object
                String reporterName = rs.getString("reporter_name");
                System.out.println("Reporter Name: " + reporterName);
                dispute.setReporterName(reporterName);
                
                String offeredTitle = rs.getString("offered_title");
                System.out.println("Offered Title: " + offeredTitle);
                dispute.setOfferedItemTitle(offeredTitle);
                
                String receivedTitle = rs.getString("received_title");
                System.out.println("Received Title: " + receivedTitle);
                dispute.setReceivedItemTitle(receivedTitle);
                
                disputes.add(dispute);
                System.out.println("Added dispute to list. Total disputes: " + disputes.size());
            }
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Returning " + disputes.size() + " disputes");
        return disputes;
    }
    
    public Map<Integer, String> getUserNames(List<Integer> userIds) {
        Map<Integer, String> userNames = new HashMap<>();
        
        if (userIds.isEmpty()) {
            return userNames;
        }
        
        // Create a comma-separated list of user IDs for the IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < userIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        
        String query = "SELECT id, name FROM user WHERE id IN (" + placeholders.toString() + ")";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < userIds.size(); i++) {
                pstmt.setInt(i + 1, userIds.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                userNames.put(rs.getInt("id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return userNames;
    }
    
    public Map<Integer, String> getArtworkTitles(List<Integer> artworkIds) {
        Map<Integer, String> artworkTitles = new HashMap<>();
        
        if (artworkIds.isEmpty()) {
            return artworkTitles;
        }
        
        // Create a comma-separated list of artwork IDs for the IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < artworkIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        
        String query = "SELECT id, title FROM artwork WHERE id IN (" + placeholders.toString() + ")";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < artworkIds.size(); i++) {
                pstmt.setInt(i + 1, artworkIds.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                artworkTitles.put(rs.getInt("id"), rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return artworkTitles;
    }

    public TradeDispute getDisputeById(int id) {
        String query = "SELECT * FROM " + TradeDispute.TABLE_NAME + " WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                TradeDispute dispute = new TradeDispute();
                dispute.setId(rs.getInt("id"));
                dispute.setReporter(rs.getInt("reporter"));
                dispute.setTradeId(rs.getInt("trade_id"));
                dispute.setOfferedItem(rs.getString("offered_item"));
                dispute.setReceivedItem(rs.getString("received_item"));
                dispute.setReason(rs.getString("reason"));
                dispute.setEvidence(rs.getString("evidence"));
                dispute.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                dispute.setStatus(rs.getString("status"));
                return dispute;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean createDispute(TradeDispute dispute) {
        String query = "INSERT INTO " + TradeDispute.TABLE_NAME + 
                      " (reporter, trade_id, offered_item, received_item, reason, evidence, status, timestamp) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        System.out.println("Creating dispute with values:");
        System.out.println("Reporter: " + dispute.getReporter());
        System.out.println("Trade ID: " + dispute.getTradeId());
        System.out.println("Offered Item: " + dispute.getOfferedItem());
        System.out.println("Received Item: " + dispute.getReceivedItem());
        System.out.println("Reason: " + dispute.getReason());
        System.out.println("Evidence length: " + (dispute.getEvidence() != null ? dispute.getEvidence().length() : "null"));
        System.out.println("Status: " + dispute.getStatus());
        System.out.println("Timestamp: " + dispute.getTimestamp());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, dispute.getReporter());
            pstmt.setInt(2, dispute.getTradeId());
            pstmt.setString(3, dispute.getOfferedItem());
            pstmt.setString(4, dispute.getReceivedItem());
            pstmt.setString(5, dispute.getReason());
            pstmt.setString(6, dispute.getEvidence());
            pstmt.setString(7, dispute.getStatus());
            pstmt.setTimestamp(8, Timestamp.valueOf(dispute.getTimestamp()));

            int result = pstmt.executeUpdate();
            System.out.println("Execute update result: " + result);
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error creating dispute: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDisputeStatus(int id, String status) {
        String query = "UPDATE " + TradeDispute.TABLE_NAME + " SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDispute(int id) {
        String query = "DELETE FROM " + TradeDispute.TABLE_NAME + " WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting dispute: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDispute(int id, String reason, String evidence) {
        String query = "UPDATE " + TradeDispute.TABLE_NAME + 
                      " SET reason = ?, evidence = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, reason);
            pstmt.setString(2, evidence);
            pstmt.setInt(3, id);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating dispute: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void alterTableForLongEvidence() {
        String sql = "ALTER TABLE trade_dispute MODIFY COLUMN evidence LONGTEXT COLLATE utf8mb4_unicode_ci NOT NULL";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
            System.out.println("Successfully altered trade_dispute table for long evidence");
            
        } catch (SQLException e) {
            System.err.println("Error altering trade_dispute table: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 