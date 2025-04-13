package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class BetSessionService implements IBetSessionService {

    private final Connection connection;

    public BetSessionService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public void addBetSession(BetSession betSession) {
        String query = "INSERT INTO bet_session (author_id, artwork_id, created_at, start_time, end_time, initial_price, current_price, status, number_of_bids, mysterious_mode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, betSession.getAuthor().getId());
            statement.setInt(2, betSession.getArtwork().getId());
            statement.setTimestamp(3, Timestamp.valueOf(betSession.getCreatedAt()));
            statement.setTimestamp(4, Timestamp.valueOf(betSession.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(betSession.getEndTime()));
            statement.setDouble(6, betSession.getInitialPrice());
            statement.setDouble(7, betSession.getCurrentPrice());
            statement.setString(8, betSession.getStatus());
            statement.setInt(9, 0); // Initialize with 0 bids
            statement.setBoolean(10, betSession.isMysteriousMode()); // Add mysterious_mode field
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }    
    }

    public void updateBetSession(BetSession betSession) {
        String query = "UPDATE bet_session SET author_id = ?, artwork_id = ?, start_time = ?, end_time = ?, initial_price = ?, current_price = ?, status = ?, number_of_bids = ?, mysterious_mode = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, betSession.getAuthor().getId());
            statement.setInt(2, betSession.getArtwork().getId());
            statement.setTimestamp(3, Timestamp.valueOf(betSession.getStartTime()));
            statement.setTimestamp(4, Timestamp.valueOf(betSession.getEndTime()));
            statement.setDouble(5, betSession.getInitialPrice());
            statement.setDouble(6, betSession.getCurrentPrice());
            statement.setString(7, betSession.getStatus());
            
            // Get current number_of_bids to preserve it (or set to 0 if new)
            int numberOfBids = 0;
            try (PreparedStatement getStatement = connection.prepareStatement("SELECT number_of_bids FROM bet_session WHERE id = ?")) {
                getStatement.setInt(1, betSession.getId());
                ResultSet rs = getStatement.executeQuery();
                if (rs.next()) {
                    numberOfBids = rs.getInt("number_of_bids");
                }
            }
            statement.setInt(8, numberOfBids);
            statement.setBoolean(9, betSession.isMysteriousMode()); // Add mysterious_mode field
            statement.setInt(10, betSession.getId());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }    
    }

    public void deleteBetSession(int id) {
        String query = "DELETE FROM bet_session WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }    
    }

    public BetSession getBetSessionById(int id) {
        String query = "SELECT * FROM bet_session WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                BetSession betSession = new BetSession();
                betSession.setId(resultSet.getInt("id"));
                // Populate other fields as needed
                return betSession;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }        
        return null;
    }    
    
    public List<BetSession> getAllBetSessions() {
        List<BetSession> betSessions = new ArrayList<>();
        String query = "SELECT bs.*, u.name as author_name, a.title as artwork_title FROM bet_session bs " +
                      "LEFT JOIN user u ON bs.author_id = u.id " +
                      "LEFT JOIN artwork a ON bs.artwork_id = a.id";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                BetSession betSession = new BetSession();
                betSession.setId(resultSet.getInt("id"));
                
                // Set timestamps
                if (resultSet.getTimestamp("created_at") != null) {
                    betSession.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                }
                if (resultSet.getTimestamp("start_time") != null) {
                    betSession.setStartTime(resultSet.getTimestamp("start_time").toLocalDateTime());
                }
                if (resultSet.getTimestamp("end_time") != null) {
                    betSession.setEndTime(resultSet.getTimestamp("end_time").toLocalDateTime());
                }
                
                // Set prices
                betSession.setInitialPrice(resultSet.getDouble("initial_price"));
                betSession.setCurrentPrice(resultSet.getDouble("current_price"));
                
                // Set status
                betSession.setStatus(resultSet.getString("status"));
                
                // Set author
                int authorId = resultSet.getInt("author_id");
                if (!resultSet.wasNull()) {
                    User author = new User();
                    author.setId(authorId);
                    // Get the author name from the join
                    String authorName = resultSet.getString("author_name");
                    if (authorName != null) {
                        author.setName(authorName);
                    } else {
                        author.setName("User #" + authorId);
                    }
                    betSession.setAuthor(author);
                }
                
                // Set artwork
                int artworkId = resultSet.getInt("artwork_id");
                if (!resultSet.wasNull()) {
                    Artwork artwork = new Artwork();
                    artwork.setId(artworkId);
                    // Get the artwork title from the join
                    String artworkTitle = resultSet.getString("artwork_title");
                    if (artworkTitle != null) {
                        artwork.setTitle(artworkTitle);
                    } else {
                        artwork.setTitle("Artwork #" + artworkId);
                    }
                    betSession.setArtwork(artwork);
                }
                
                betSessions.add(betSession);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return betSessions;
    }
}
