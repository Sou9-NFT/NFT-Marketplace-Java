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

    public RaffleService() {
        connection = DatabaseConnection.getInstance().getConnection();
        artworkService = new ArtworkService();
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
            System.out.println("Raffle has expired during update: " + raffle.getId() + " - " + raffle.getTitle());
            try {
                // Set status to ended before calling selectWinner
                raffle.setStatus("ended");
                selectWinner(raffle);
                return; // Winner has been selected and raffle updated, no need to continue
            } catch (Exception e) {
                System.err.println("Error selecting winner during update: " + e.getMessage());
                e.printStackTrace();
                // Continue with normal update if selectWinner fails
            }
        }

        // Normal update for raffle that doesn't need to be ended or if selectWinner failed
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
        
        ps.executeUpdate();
    }

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
                System.out.println("Found expired raffle: " + raffle.getTitle() + " (ID: " + raffle.getId() + ")");
                
                // Instead of handling winner selection here, use the selectWinner method
                // which properly handles the artwork transfer
                raffle.setStatus("ended");
                selectWinner(raffle);
                
            } catch (Exception e) {
                System.err.println("Error processing expired raffle " + raffle.getId() + ": " + e.getMessage());
                e.printStackTrace();
                // Continue with next raffle even if one fails
            }
        }
    }

    private boolean transferArtworkOwnership(Artwork artwork, User winner) throws Exception {
        if (winner == null || artwork == null) {
            System.err.println("Cannot transfer ownership: winner or artwork is null");
            return false;
        }
        
        // Begin transaction
        connection.setAutoCommit(false);
        
        try {
            // Store original owner ID for logging
            int originalOwnerId = artwork.getOwnerId();
            
            System.out.println("Transferring artwork ownership - Artwork ID: " + artwork.getId() + 
                ", Current owner ID: " + originalOwnerId + 
                ", New owner (winner) ID: " + winner.getId());
            
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
                
                System.out.println("Database update successful. " + rowsAffected + " row(s) affected.");
            }
            
            // Verify the update by querying the database
            String verifySql = "SELECT owner_id FROM artwork WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(verifySql)) {
                stmt.setInt(1, artwork.getId());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int updatedOwnerId = rs.getInt("owner_id");
                    if (updatedOwnerId != winner.getId()) {
                        System.err.println("ERROR: Database verification failed. Expected owner_id: " + 
                            winner.getId() + ", Actual owner_id: " + updatedOwnerId);
                    } else {
                        System.out.println("Database verification successful. owner_id is now: " + updatedOwnerId);
                    }
                }
            }
            
            // Update the artwork object
            artwork.setOwnerId(winner.getId());
            artwork.setUpdatedAt(LocalDateTime.now());
            
            // Log the ownership transfer
            System.out.println(String.format(
                "Artwork ownership transferred - Artwork ID: %d, Title: %s, From User ID: %d, To Winner ID: %d, Winner Name: %s",
                artwork.getId(), 
                artwork.getTitle(),
                originalOwnerId,
                winner.getId(),
                winner.getName()
            ));
            
            // Commit the transaction
            connection.commit();
            return true;
            
        } catch (Exception e) {
            System.err.println("Error transferring artwork ownership: " + e.getMessage());
            e.printStackTrace();
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void selectWinner(Raffle raffle) throws Exception {
        // Begin transaction
        connection.setAutoCommit(false);
        System.out.println("Selecting winner for raffle: " + raffle.getTitle() + " (ID: " + raffle.getId() + ")");
        
        try {
            // Load participants first
            List<Participant> participants = getParticipantService().getByRaffle(raffle);
            if (participants.isEmpty()) {
                // If no participants, just mark as ended without a winner
                System.out.println("No participants in raffle, ending without a winner");
                
                // Make sure the raffle is updated as ended
                String updateRaffleSql = "UPDATE raffle SET status = 'ended' WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(updateRaffleSql)) {
                    stmt.setInt(1, raffle.getId());
                    stmt.executeUpdate();
                }
                
                connection.commit();
                return;
            }

            System.out.println("Number of participants: " + participants.size());
            
            // Select random winner
            Random random = new Random();
            Participant winnerParticipant = participants.get(random.nextInt(participants.size()));
            User winner = winnerParticipant.getUser();
            
            System.out.println("Selected winner: " + winner.getName() + " (ID: " + winner.getId() + ")");

            // Get the artwork
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            if (artwork == null) {
                throw new Exception("Artwork not found");
            }
            
            System.out.println("Artwork being transferred: " + artwork.getTitle() + " (ID: " + artwork.getId() + ")");
            System.out.println("Current owner ID: " + artwork.getOwnerId());

            // Transfer ownership to winner using our method
            boolean transferred = transferArtworkOwnership(artwork, winner);
            if (!transferred) {
                throw new Exception("Failed to transfer artwork ownership to winner");
            }

            // Update raffle with winner
            raffle.setWinnerId(winner.getId());
            
            // Make sure the database is updated with winner and status
            String updateRaffleSql = "UPDATE raffle SET status = 'ended', winner_id = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateRaffleSql)) {
                stmt.setInt(1, winner.getId());
                stmt.setInt(2, raffle.getId());
                stmt.executeUpdate();
            }
            
            System.out.println("Raffle ended successfully. Artwork ownership transferred to: " + winner.getName());

            connection.commit();
        } catch (Exception e) {
            System.err.println("Error selecting winner: " + e.getMessage());
            e.printStackTrace();
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<Raffle> getAllRaffles() {
        List<Raffle> raffles = new ArrayList<>();
        String query = "SELECT r.*, a.title as artwork_title, u.id as user_id, u.name as user_name FROM raffle r " +
                      "LEFT JOIN artwork a ON r.artwork_id = a.id " +
                      "LEFT JOIN user u ON r.creator_id = u.id " +
                      "ORDER BY r.created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Raffle raffle = new Raffle();
                raffle.setId(rs.getInt("id"));
                raffle.setTitle(rs.getString("title"));
                raffle.setRaffleDescription(rs.getString("raffle_description"));
                raffle.setArtworkId(rs.getInt("artwork_id"));
                raffle.setArtworkTitle(rs.getString("artwork_title"));
                raffle.setStartTime(rs.getTimestamp("start_time"));
                raffle.setEndTime(rs.getTimestamp("end_time"));
                raffle.setStatus(rs.getString("status"));
                raffle.setCreatedAt(rs.getTimestamp("created_at"));
                
                // Set creator information
                int creatorId = rs.getInt("creator_id");
                if (!rs.wasNull()) {
                    User creator = new User();
                    creator.setId(creatorId);
                    creator.setName(rs.getString("creator_name"));
                    raffle.setCreator(creator);
                }
                
                raffles.add(raffle);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return raffles;
    }

    public Raffle getRaffleById(int raffleId) {
        String query = "SELECT r.*, a.title as artwork_title FROM raffle r " +
                      "LEFT JOIN artwork a ON r.artwork_id = a.id " +
                      "WHERE r.id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, raffleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Raffle raffle = new Raffle();
                raffle.setId(rs.getInt("id"));
                raffle.setTitle(rs.getString("title"));
                raffle.setRaffleDescription(rs.getString("raffle_description"));
                raffle.setArtworkId(rs.getInt("artwork_id"));
                raffle.setArtworkTitle(rs.getString("artwork_title"));
                raffle.setStartTime(rs.getTimestamp("start_time"));
                raffle.setEndTime(rs.getTimestamp("end_time"));
                raffle.setStatus(rs.getString("status"));
                raffle.setCreatedAt(rs.getTimestamp("created_at"));
                return raffle;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteRaffle(int raffleId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // First delete all participants
            String deleteParticipants = "DELETE FROM participant WHERE raffle_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteParticipants)) {
                stmt.setInt(1, raffleId);
                stmt.executeUpdate();
            }

            // Then delete the raffle
            String deleteRaffle = "DELETE FROM raffle WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteRaffle)) {
                stmt.setInt(1, raffleId);
                stmt.executeUpdate();
            }
            
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw new SQLException("Error deleting raffle: " + e.getMessage());
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
