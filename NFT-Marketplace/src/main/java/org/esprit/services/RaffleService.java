package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.esprit.models.Artwork;
import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class RaffleService implements IService<Raffle> {
    private Connection connection;
    private ArtworkService artworkService;
    private ParticipantService participantService;
    private UserService userService;

    public RaffleService() {
        connection = DatabaseConnection.getInstance().getConnection();
        artworkService = new ArtworkService();
        userService = new UserService();
    }

    private ParticipantService getParticipantService() {
        if (participantService == null) {
            participantService = new ParticipantService();
        }
        return participantService;
    }

    @Override
    public void add(Raffle raffle) throws Exception {
        String query = "INSERT INTO raffle (title, raffle_description, start_time, end_time, status, creator_id, created_at, creator_name, artwork_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        
        ps.setString(1, raffle.getTitle());
        ps.setString(2, raffle.getRaffleDescription());
        ps.setTimestamp(3, new Timestamp(raffle.getStartTime().getTime()));
        ps.setTimestamp(4, new Timestamp(raffle.getEndTime().getTime()));
        ps.setString(5, raffle.getStatus());
        ps.setInt(6, raffle.getCreator().getId());
        ps.setTimestamp(7, new Timestamp(raffle.getCreatedAt().getTime()));
        ps.setString(8, raffle.getCreatorName());
        ps.setInt(9, raffle.getArtworkId());
        
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            raffle.setId(rs.getInt(1));
        }    }
    
    public void update(Raffle raffle) throws SQLException {
        // First check if this raffle needs to be ended
        if (raffle.getStatus().equals("active") && raffle.getEndTime().before(new Date())) {
            raffle.setStatus("ended");
            try {
                selectWinner(raffle);
                return; // Winner has been selected and raffle updated
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String query = "UPDATE raffle SET title=?, raffle_description=?, end_time=?, status=?, winner_id=?, artwork_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, raffle.getTitle());
        ps.setString(2, raffle.getRaffleDescription());
        ps.setTimestamp(3, new Timestamp(raffle.getEndTime().getTime()));
        ps.setString(4, raffle.getStatus());
        if (raffle.getWinnerId() != null) {
            ps.setInt(5, raffle.getWinnerId());
        } else {
            ps.setNull(5, Types.INTEGER);
        }
        ps.setInt(6, raffle.getArtworkId());
        ps.setInt(7, raffle.getId());
        
        ps.executeUpdate();    }

    public void delete(Raffle raffle) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // First delete all participants
            List<Participant> participants = getParticipantService().getByRaffle(raffle);
            for (Participant participant : participants) {
                getParticipantService().delete(participant);
            }

            // Then delete the raffle
            String query = "DELETE FROM raffle WHERE id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, raffle.getId());
            ps.executeUpdate();
            
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw new SQLException("Error deleting raffle: " + e.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Override
    public List<Raffle> getAll() throws SQLException {
        // Update expired raffles before getting the list
        updateExpiredRaffles();
        
        List<Raffle> raffles = new ArrayList<>();
        String query = "SELECT r.* FROM raffle r ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            Raffle raffle = extractRaffleFromResultSet(rs);
            // Load participants for each raffle
            try {
                List<Participant> participants = getParticipantService().getByRaffle(raffle);
                for (Participant p : participants) {
                    raffle.addParticipant(p.getUser());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            raffles.add(raffle);
        }
        
        return raffles;
    }

    @Override
    public Raffle getOne(int id) throws SQLException {
        // Check expired raffles first
        updateExpiredRaffles();
        
        String query = "SELECT r.* FROM raffle r WHERE r.id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            Raffle raffle = extractRaffleFromResultSet(rs);
            // Load participants
            try {
                List<Participant> participants = getParticipantService().getByRaffle(raffle);
                for (Participant p : participants) {
                    raffle.addParticipant(p.getUser());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return raffle;
        }
        return null;
    }

    private Raffle extractRaffleFromResultSet(ResultSet rs) throws SQLException {
        Raffle raffle = new Raffle();
        raffle.setId(rs.getInt("id"));
        raffle.setTitle(rs.getString("title"));
        raffle.setRaffleDescription(rs.getString("raffle_description"));
        raffle.setStartTime(rs.getTimestamp("start_time"));
        raffle.setEndTime(rs.getTimestamp("end_time"));
        raffle.setStatus(rs.getString("status"));
        raffle.setCreatedAt(rs.getTimestamp("created_at"));
        raffle.setCreatorName(rs.getString("creator_name"));
        raffle.setArtworkId(rs.getInt("artwork_id"));
        
        // Create and set the creator User object with minimal information
        User creator = new User();
        creator.setId(rs.getInt("creator_id"));
        creator.setName(rs.getString("creator_name"));
        raffle.setCreator(creator);
        
        Integer winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            raffle.setWinnerId(winnerId);
        }
        
        return raffle;
    }

    public void updateExpiredRaffles() throws SQLException {
        // Get all expired active raffles that need winners
        String getExpiredRafflesQuery = "SELECT * FROM raffle WHERE status='active' AND end_time <= NOW()";
        PreparedStatement ps = connection.prepareStatement(getExpiredRafflesQuery);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Raffle raffle = extractRaffleFromResultSet(rs);
            try {
                // Get participants for the raffle
                List<Participant> participants = getParticipantService().getByRaffle(raffle);
                
                // Set status to ended
                raffle.setStatus("ended");
                
                // Select winner if there are participants
                if (!participants.isEmpty()) {
                    // Random selection of winner
                    Random random = new Random();
                    Participant winner = participants.get(random.nextInt(participants.size()));
                    raffle.setWinnerId(winner.getUser().getId());
                }
                
                // Update raffle in database
                update(raffle);
                
            } catch (Exception e) {
                e.printStackTrace();
                // Continue with next raffle even if one fails
            }
        }
    }

    private boolean transferArtworkOwnership(Artwork artwork, User winner) throws Exception {
        if (winner == null || artwork == null) {
            return false;
        }
        
        // Begin transaction
        connection.setAutoCommit(false);
        
        try {
            // Update artwork ownership in database
            String updateArtworkSql = "UPDATE artwork SET owner_id = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateArtworkSql)) {
                stmt.setInt(1, winner.getId());
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(3, artwork.getId());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to update artwork ownership");
                }
            }
            
            // Update the artwork object
            artwork.setOwnerId(winner.getId());
            artwork.setUpdatedAt(LocalDateTime.now());
            
            // Commit the transaction
            connection.commit();
            return true;
            
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void selectWinner(Raffle raffle) throws Exception {
        // Begin transaction
        connection.setAutoCommit(false);
        try {
            // Load participants first
            List<Participant> participants = getParticipantService().getByRaffle(raffle);
            if (participants.isEmpty()) {
                // If no participants, just mark as ended without a winner
                raffle.setStatus("ended");
                update(raffle);
                connection.commit();
                return;
            }

            // Select random winner
            Random random = new Random();
            Participant winnerParticipant = participants.get(random.nextInt(participants.size()));
            User winner = winnerParticipant.getUser();

            // Get the artwork
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            if (artwork == null) {
                throw new Exception("Artwork not found");
            }

            // Transfer ownership to winner using our new method
            boolean transferred = transferArtworkOwnership(artwork, winner);
            if (!transferred) {
                throw new Exception("Failed to transfer artwork ownership to winner");
            }

            // Update raffle with winner
            raffle.setWinnerId(winner.getId());
            raffle.setStatus("ended");
            update(raffle);

            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<Raffle> getAllRaffles() {
        List<Raffle> raffles = new ArrayList<>();
        String query = "SELECT r.*, a.title as artwork_title FROM raffles r " +
                      "LEFT JOIN artworks a ON r.artwork_id = a.id " +
                      "ORDER BY r.created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Raffle raffle = new Raffle();
                raffle.setId(rs.getInt("id"));
                raffle.setTitle(rs.getString("title"));
                raffle.setRaffleDescription(rs.getString("description"));
                raffle.setArtworkId(rs.getInt("artwork_id"));
                raffle.setArtworkTitle(rs.getString("artwork_title"));
                raffle.setStartTime(rs.getTimestamp("start_date"));
                raffle.setEndTime(rs.getTimestamp("end_date"));
                raffle.setStatus(rs.getString("status"));
                raffle.setTicketPrice(rs.getDouble("ticket_price"));
                raffle.setCreatedAt(rs.getTimestamp("created_at"));
                raffles.add(raffle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return raffles;
    }

    public void createRaffle(Raffle raffle) throws SQLException {
        String query = "INSERT INTO raffles (title, description, artwork_id, start_date, end_date, " +
                      "status, ticket_price, total_tickets, sold_tickets) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, raffle.getTitle());
            stmt.setInt(3, raffle.getArtworkId());
            stmt.setTimestamp(4, new Timestamp(raffle.getStartTime().getTime()));
            stmt.setTimestamp(5, new Timestamp(raffle.getEndTime().getTime()));
            stmt.setString(6, raffle.getStatus());
            stmt.setDouble(7, raffle.getTicketPrice());
            stmt.setInt(9, 0); // Initially no tickets sold

            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                raffle.setId(rs.getInt(1));
            }
        }
    }

    public void updateRaffle(Raffle raffle) throws SQLException {
        String query = "UPDATE raffles SET title = ?, description = ?, artwork_id = ?, " +
                      "start_date = ?, end_date = ?, status = ?, ticket_price = ?, " +
                      "total_tickets = ?, updated_at = CURRENT_TIMESTAMP " +
                      "WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, raffle.getTitle());
            stmt.setInt(3, raffle.getArtworkId());
            stmt.setTimestamp(4, new Timestamp(raffle.getStartTime().getTime()));
            stmt.setTimestamp(5, new Timestamp(raffle.getEndTime().getTime()));
            stmt.setString(6, raffle.getStatus());
            stmt.setDouble(7, raffle.getTicketPrice());
            stmt.setInt(9, raffle.getId());

            stmt.executeUpdate();
        }
    }

    public void deleteRaffle(int raffleId) throws SQLException {
        // First delete related records in tickets table
        String deleteTickets = "DELETE FROM tickets WHERE raffle_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteTickets)) {
            stmt.setInt(1, raffleId);
            stmt.executeUpdate();
        }

        // Then delete the raffle
        String deleteRaffle = "DELETE FROM raffles WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteRaffle)) {
            stmt.setInt(1, raffleId);
            stmt.executeUpdate();
        }
    }

    public Raffle getRaffleById(int raffleId) {
        String query = "SELECT r.*, a.title as artwork_title FROM raffles r " +
                      "LEFT JOIN artworks a ON r.artwork_id = a.id " +
                      "WHERE r.id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, raffleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Raffle raffle = new Raffle();
                raffle.setId(rs.getInt("id"));
                raffle.setTitle(rs.getString("title"));
                raffle.setRaffleDescription(rs.getString("description"));
                raffle.setArtworkId(rs.getInt("artwork_id"));
                raffle.setArtworkTitle(rs.getString("artwork_title")); // Fixed: using correct setter
                raffle.setStartTime(rs.getTimestamp("start_date"));
                raffle.setEndTime(rs.getTimestamp("end_date"));
                raffle.setStatus(rs.getString("status"));
                raffle.setTicketPrice(rs.getDouble("ticket_price"));
                raffle.setCreatedAt(rs.getTimestamp("created_at"));
                return raffle;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateRaffleStatus(int raffleId, String status) throws SQLException {
        String query = "UPDATE raffles SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, raffleId);
            stmt.executeUpdate();
        }
    }
}
