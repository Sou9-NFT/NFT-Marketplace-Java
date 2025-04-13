package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class ParticipantService implements IService<Participant> {
    private Connection connection;
    private UserService userService;

    public ParticipantService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
    }

    @Override
    public void add(Participant participant) throws Exception {
        String query = "INSERT INTO participant (raffle_id, user_id, name, joined_at) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        
        ps.setInt(1, participant.getRaffle().getId());
        ps.setInt(2, participant.getUser().getId());
        ps.setString(3, participant.getName());
        ps.setTimestamp(4, Timestamp.valueOf(participant.getJoinedAt()));
        
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            participant.setId(rs.getInt(1));
        }
    }

    @Override
    public void update(Participant participant) throws Exception {
        String query = "UPDATE participant SET raffle_id=?, user_id=?, name=?, joined_at=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        
        ps.setInt(1, participant.getRaffle().getId());
        ps.setInt(2, participant.getUser().getId());
        ps.setString(3, participant.getName());
        ps.setTimestamp(4, Timestamp.valueOf(participant.getJoinedAt()));
        ps.setInt(5, participant.getId());
        
        ps.executeUpdate();
    }

    @Override
    public void delete(Participant participant) throws Exception {
        String query = "DELETE FROM participant WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, participant.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Participant> getAll() throws Exception {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT p.*, u.name as user_name, u.email as user_email FROM participant p " +
                      "JOIN user u ON p.user_id = u.id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs, false));
        }
        
        return participants;
    }

    @Override
    public Participant getOne(int id) throws Exception {
        String query = "SELECT p.*, u.name as user_name, u.email as user_email FROM participant p " +
                      "JOIN user u ON p.user_id = u.id " +
                      "WHERE p.id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return extractParticipantFromResultSet(rs, false);
        }
        return null;
    }

    public List<Participant> getByRaffle(Raffle raffle) throws Exception {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT p.*, u.name as user_name, u.email as user_email FROM participant p " +
                      "JOIN user u ON p.user_id = u.id " +
                      "WHERE p.raffle_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, raffle.getId());
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs, false));
        }
        
        return participants;
    }

    public List<Participant> getByUser(User user) throws Exception {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT p.*, r.title as raffle_title, u.name as user_name, u.email as user_email FROM participant p " +
                      "JOIN raffle r ON p.raffle_id = r.id " +
                      "JOIN user u ON p.user_id = u.id " +
                      "WHERE p.user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, user.getId());
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs, true));
        }
        
        return participants;
    }

    private Participant extractParticipantFromResultSet(ResultSet rs, boolean includeRaffleDetails) throws Exception {
        Participant participant = new Participant();
        participant.setId(rs.getInt("id"));
        
        // Create minimal Raffle object
        Raffle raffle = new Raffle();
        raffle.setId(rs.getInt("raffle_id"));
        if (includeRaffleDetails) {
            raffle.setTitle(rs.getString("raffle_title"));
        }
        participant.setRaffle(raffle);
        
        // Create User object with available details
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setName(rs.getString("user_name"));
        user.setEmail(rs.getString("user_email"));
        participant.setUser(user);
        
        participant.setName(rs.getString("name"));
        
        Timestamp joinedAt = rs.getTimestamp("joined_at");
        if (joinedAt != null) {
            participant.setJoinedAt(joinedAt.toLocalDateTime());
        }
        
        return participant;
    }
}