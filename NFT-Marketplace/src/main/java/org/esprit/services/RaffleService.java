package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class RaffleService implements IService<Raffle> {
    private Connection connection;

    public RaffleService() {
        connection = DatabaseConnection.getInstance().getConnection();
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
        String query = "DELETE FROM raffle WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, raffle.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Raffle> getAll() throws SQLException {
        // Update expired raffles before getting the list
        updateExpiredRaffles();
        
        List<Raffle> raffles = new ArrayList<>();
        String query = "SELECT r.*, u.* FROM raffle r LEFT JOIN user u ON r.creator_id = u.id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            raffles.add(extractRaffleFromResultSet(rs));
        }
        
        return raffles;
    }

    @Override
    public Raffle getOne(int id) throws SQLException {
        String query = "SELECT r.*, u.* FROM raffle r LEFT JOIN user u ON r.creator_id = u.id WHERE r.id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return extractRaffleFromResultSet(rs);
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
        
        // Create and set the creator User object
        User creator = new User();
        creator.setId(rs.getInt("creator_id"));
        creator.setName(rs.getString("name")); // Assuming the user table has a name column
        raffle.setCreator(creator);
        
        Integer winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            raffle.setWinnerId(winnerId);
        }
        
        return raffle;
    }

    public void updateExpiredRaffles() throws SQLException {
        String query = "UPDATE raffle SET status='ended' WHERE status='active' AND end_time <= NOW()";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.executeUpdate();
    }
}
