package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class BetSessionService {
    
    private Connection connection;
    private UserService userService;
    private ArtworkService artworkService;
    
    public BetSessionService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        artworkService = new ArtworkService();
    }
    
    public void addBetSession(BetSession betSession) throws SQLException {
        String query = "INSERT INTO bet_session (author_id, artwork_id, created_at, start_time, end_time, initial_price, current_price, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, betSession.getAuthor().getId());
            ps.setInt(2, betSession.getArtwork().getId());
            ps.setTimestamp(3, Timestamp.valueOf(betSession.getCreatedAt()));
            ps.setTimestamp(4, Timestamp.valueOf(betSession.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(betSession.getEndTime()));
            ps.setDouble(6, betSession.getInitialPrice());
            ps.setDouble(7, betSession.getCurrentPrice());
            ps.setString(8, betSession.getStatus());
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating bet session failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    betSession.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating bet session failed, no ID obtained.");
                }
            }
        }
    }
    
    public void updateBetSession(BetSession betSession) throws SQLException {
        String query = "UPDATE bet_session SET author_id = ?, artwork_id = ?, start_time = ?, end_time = ?, initial_price = ?, current_price = ?, status = ? WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, betSession.getAuthor().getId());
            ps.setInt(2, betSession.getArtwork().getId());
            ps.setTimestamp(3, Timestamp.valueOf(betSession.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(betSession.getEndTime()));
            ps.setDouble(5, betSession.getInitialPrice());
            ps.setDouble(6, betSession.getCurrentPrice());
            ps.setString(7, betSession.getStatus());
            ps.setInt(8, betSession.getId());
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Updating bet session failed, no rows affected.");
            }
        }
    }
    
    public void deleteBetSession(int id) throws SQLException {
        String query = "DELETE FROM bet_session WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    
    public BetSession getOne(int id) throws SQLException, Exception {
        String query = "SELECT * FROM bet_session WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractBetSessionFromResultSet(rs);
            }
        }
        
        return null;
    }
    
    public List<BetSession> getAllBetSessions() throws SQLException, Exception {
        List<BetSession> betSessions = new ArrayList<>();
        String query = "SELECT * FROM bet_session ORDER BY created_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                betSessions.add(extractBetSessionFromResultSet(rs));
            }
        }
        
        return betSessions;
    }
    
    /**
     * Retrieves all bet sessions associated with a specific author (user)
     * @param authorId The ID of the author/user
     * @return List of bet sessions created by the specified author
     * @throws SQLException If a database error occurs
     */
    public List<BetSession> getSessionsByAuthor(int authorId) throws SQLException, Exception {
        String query = "SELECT * FROM bet_session WHERE author_id = ?";
        List<BetSession> betSessions = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, authorId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    betSessions.add(extractBetSessionFromResultSet(rs));
                }
            }
        }
        
        return betSessions;
    }
    
    /**
     * Retrieves active bet sessions NOT created by the specified author
     * @param authorId The ID of the author/user to exclude
     * @return List of active bet sessions from other authors
     * @throws SQLException If a database error occurs
     */
    public List<BetSession> getActiveSessionsNotByAuthor(int authorId) throws SQLException, Exception {
        String query = "SELECT * FROM bet_session WHERE author_id != ? AND status = 'active'";
        List<BetSession> betSessions = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, authorId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    betSessions.add(extractBetSessionFromResultSet(rs));
                }
            }
        }
        
        return betSessions;
    }
    
    private BetSession extractBetSessionFromResultSet(ResultSet rs) throws SQLException, Exception {
        BetSession betSession = new BetSession();
        
        betSession.setId(rs.getInt("id"));
        
        // Load author from UserService
        User author = userService.getOne(rs.getInt("author_id"));
        betSession.setAuthor(author);
        
        // Load artwork from ArtworkService
        Artwork artwork = artworkService.getOne(rs.getInt("artwork_id"));
        betSession.setArtwork(artwork);
        
        // Convert Timestamp to LocalDateTime
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            betSession.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }
        
        Timestamp startTimeTimestamp = rs.getTimestamp("start_time");
        if (startTimeTimestamp != null) {
            betSession.setStartTime(startTimeTimestamp.toLocalDateTime());
        }
        
        Timestamp endTimeTimestamp = rs.getTimestamp("end_time");
        if (endTimeTimestamp != null) {
            betSession.setEndTime(endTimeTimestamp.toLocalDateTime());
        }
        
        betSession.setInitialPrice(rs.getDouble("initial_price"));
        betSession.setCurrentPrice(rs.getDouble("current_price"));
        betSession.setStatus(rs.getString("status"));
        
        return betSession;
    }
    
    // Method to automatically update the status of bet sessions based on time
    public void updateSessionStatuses() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        
        // Update pending sessions to active if current time is after start time
        String activateQuery = "UPDATE bet_session SET status = 'active' WHERE status = 'pending' AND start_time <= ?";
        try (PreparedStatement ps = connection.prepareStatement(activateQuery)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
        
        // Update active sessions to completed if current time is after end time
        String completeQuery = "UPDATE bet_session SET status = 'completed' WHERE status = 'active' AND end_time <= ?";
        try (PreparedStatement ps = connection.prepareStatement(completeQuery)) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }
}
