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
    private RaffleService raffleService;

    public ParticipantService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        raffleService = new RaffleService();
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
        String query = "SELECT * FROM participant";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs));
        }
        
        return participants;
    }

    @Override
    public Participant getOne(int id) throws Exception {
        String query = "SELECT * FROM participant WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return extractParticipantFromResultSet(rs);
        }
        return null;
    }

    public List<Participant> getByRaffle(Raffle raffle) throws Exception {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT * FROM participant WHERE raffle_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, raffle.getId());
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs));
        }
        
        return participants;
    }

    public List<Participant> getByUser(User user) throws Exception {
        List<Participant> participants = new ArrayList<>();
        String query = "SELECT * FROM participant WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, user.getId());
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            participants.add(extractParticipantFromResultSet(rs));
        }
        
        return participants;
    }

    private Participant extractParticipantFromResultSet(ResultSet rs) throws Exception {
        Participant participant = new Participant();
        
        participant.setId(rs.getInt("id"));
        participant.setRaffle(raffleService.getOne(rs.getInt("raffle_id")));
        participant.setUser(userService.getOne(rs.getInt("user_id")));
        participant.setName(rs.getString("name"));
        
        Timestamp joinedAt = rs.getTimestamp("joined_at");
        if (joinedAt != null) {
            participant.setJoinedAt(joinedAt.toLocalDateTime());
        }
        
        return participant;
    }
}