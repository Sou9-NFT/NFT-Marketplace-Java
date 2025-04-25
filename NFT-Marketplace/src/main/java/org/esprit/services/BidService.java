package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.BetSession;
import org.esprit.models.Bid;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class BidService {
    
    private Connection connection;
    private UserService userService;
    private BetSessionService betSessionService;
    
    public BidService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        betSessionService = new BetSessionService();
    }
    
    /**
     * Adds a new bid to the database
     * @param bid The bid to add
     * @throws SQLException If a database error occurs
     */
    public void addBid(Bid bid) throws SQLException, Exception {
        String query = "INSERT INTO bid (bid_value, bid_time, bet_session_id, author_id) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, bid.getBidValue());
            ps.setTimestamp(2, Timestamp.valueOf(bid.getBidTime()));
            ps.setInt(3, bid.getBetSession().getId());
            ps.setInt(4, bid.getAuthor().getId());
            
            int affectedRows = ps.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating bid failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating bid failed, no ID obtained.");
                }
            }
              // Update the current price and increment the number of bids in the bet session
            BetSession session = bid.getBetSession();
            session.setCurrentPrice(bid.getBidValue());
            
            // Increment the number of bids
            int currentBids = session.getNumberOfBids();
            session.setNumberOfBids(currentBids + 1);
            
            betSessionService.updateBetSession(session);
        }
    }
    
    /**
     * Retrieves a bid by its ID
     * @param id The ID of the bid
     * @return The bid with the specified ID, or null if not found
     * @throws SQLException If a database error occurs
     */
    public Bid getBid(int id) throws SQLException, Exception {
        String query = "SELECT * FROM bid WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBidFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Retrieves all bids for a specific bet session
     * @param betSessionId The ID of the bet session
     * @return List of bids for the specified bet session
     * @throws SQLException If a database error occurs
     */
    public List<Bid> getBidsByBetSession(int betSessionId) throws SQLException, Exception {
        String query = "SELECT * FROM bid WHERE bet_session_id = ? ORDER BY bid_time DESC";
        List<Bid> bids = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, betSessionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(extractBidFromResultSet(rs));
                }
            }
        }
        
        return bids;
    }
    
    /**
     * Retrieves all bids made by a specific user
     * @param userId The ID of the user
     * @return List of bids made by the specified user
     * @throws SQLException If a database error occurs
     */
    public List<Bid> getBidsByUser(int userId) throws SQLException, Exception {
        String query = "SELECT * FROM bid WHERE author_id = ? ORDER BY bid_time DESC";
        List<Bid> bids = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(extractBidFromResultSet(rs));
                }
            }
        }
        
        return bids;
    }
    
    /**
     * Retrieves the highest bid for a specific bet session
     * @param betSessionId The ID of the bet session
     * @return The highest bid for the specified bet session, or null if no bids exist
     * @throws SQLException If a database error occurs
     */
    public Bid getHighestBidForBetSession(int betSessionId) throws SQLException, Exception {
        String query = "SELECT * FROM bid WHERE bet_session_id = ? ORDER BY bid_value DESC LIMIT 1";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, betSessionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBidFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extracts a Bid object from a ResultSet
     * @param rs The ResultSet containing bid data
     * @return A Bid object
     * @throws SQLException If a database error occurs
     */
    private Bid extractBidFromResultSet(ResultSet rs) throws SQLException, Exception {
        Bid bid = new Bid();
        
        bid.setId(rs.getInt("id"));
        bid.setBidValue(rs.getDouble("bid_value"));
        
        Timestamp bidTimeTimestamp = rs.getTimestamp("bid_time");
        if (bidTimeTimestamp != null) {
            bid.setBidTime(bidTimeTimestamp.toLocalDateTime());
        }
        
        // Load bet session from BetSessionService
        BetSession betSession = betSessionService.getOne(rs.getInt("bet_session_id"));
        bid.setBetSession(betSession);
        
        // Load author from UserService
        User author = userService.getOne(rs.getInt("author_id"));
        bid.setAuthor(author);
        
        return bid;
    }
}
