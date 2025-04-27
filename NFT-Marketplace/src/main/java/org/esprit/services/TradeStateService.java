package org.esprit.services;

import org.esprit.models.TradeState;
import org.esprit.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TradeStateService {
    private final Connection connection;
    private final UserService userService;
    private final ArtworkService artworkService;

    public TradeStateService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        artworkService = new ArtworkService();
    }    public void add(TradeState tradeState) throws SQLException {
        String query = "INSERT INTO trade_state (trade_offer_id, received_item, offered_item, sender_id, receiver_id, description) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, tradeState.getTradeOffer().getId());
            stmt.setInt(2, tradeState.getReceivedItem().getId());
            stmt.setInt(3, tradeState.getOfferedItem().getId());
            stmt.setInt(4, tradeState.getSender().getId());
            stmt.setInt(5, tradeState.getReceiver().getId());
            stmt.setString(6, tradeState.getDescription());
            
            stmt.executeUpdate();
            
            // Get the generated ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    tradeState.setId(rs.getInt(1));
                }
            }
        }
    }    public List<TradeState> getAllPendingTrades() throws Exception {
        List<TradeState> tradeStates = new ArrayList<>();
        String query = "SELECT ts.*, t.id as trade_offer_id, t.status FROM trade_state ts " +
                      "JOIN trade_offer t ON ts.trade_offer_id = t.id " +
                      "WHERE t.status = 'pending'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                try {
                    tradeStates.add(mapResultSetToTradeState(rs));
                } catch (Exception e) {
                    System.err.println("Error loading trade state: " + e.getMessage());
                    // Continue loading other trades even if one fails
                    continue;
                }
            }
        }
        return tradeStates;
    }    private TradeState mapResultSetToTradeState(ResultSet rs) throws SQLException, Exception {
        TradeState tradeState = new TradeState();
        try {
            tradeState.setId(rs.getInt("id"));
            tradeState.setSender(userService.getOne(rs.getInt("sender_id")));
            tradeState.setReceiver(userService.getOne(rs.getInt("receiver_id")));
            tradeState.setOfferedItem(artworkService.getOne(rs.getInt("offered_item")));
            tradeState.setReceivedItem(artworkService.getOne(rs.getInt("received_item")));
            tradeState.setDescription(rs.getString("description"));
            
            // Get and set the trade offer
            TradeOfferService tradeOfferService = new TradeOfferService();
            tradeState.setTradeOffer(tradeOfferService.getOne(rs.getInt("trade_offer_id")));
        } catch (Exception e) {
            System.err.println("Error mapping trade state: " + e.getMessage());
            throw e;
        }
        return tradeState;
    }    public void updateTradeOfferStatus(int tradeOfferId, String status) throws SQLException {
        String query = "UPDATE trade_offer SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, tradeOfferId);
            stmt.executeUpdate();
        }
    }
}
