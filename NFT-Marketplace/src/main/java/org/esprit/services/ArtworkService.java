package org.esprit.services;

import org.esprit.models.Artwork;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ArtworkService implements IService<Artwork> {
    private Connection connection;
    
    public ArtworkService() {
        connection = DatabaseConnection.getInstance().getConnection();
    }
    
    @Override
    public void add(Artwork artwork) throws SQLException {
        String query = "INSERT INTO artwork (creator_id, owner_id, title, description, image_name) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, artwork.getCreatorId());
        ps.setInt(2, artwork.getOwnerId());
        ps.setString(3, artwork.getTitle());
        ps.setString(4, artwork.getDescription());
        ps.setString(5, artwork.getImageName());
        
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            artwork.setId(rs.getInt(1));
        }
    }
    
    @Override
    public void update(Artwork artwork) throws SQLException {
        String query = "UPDATE artwork SET owner_id=?, title=?, description=?, image_name=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, artwork.getOwnerId());
        ps.setString(2, artwork.getTitle());
        ps.setString(3, artwork.getDescription());
        ps.setString(4, artwork.getImageName());
        ps.setInt(5, artwork.getId());
        
        ps.executeUpdate();
    }
    
    @Override
    public void delete(Artwork artwork) throws SQLException {
        String query = "DELETE FROM artwork WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, artwork.getId());
        ps.executeUpdate();
    }
    
    @Override
    public List<Artwork> getAll() throws SQLException {
        List<Artwork> artworks = new ArrayList<>();
        String query = "SELECT a.*, c.name as creator_name, o.name as owner_name FROM artwork a " +
                      "LEFT JOIN user c ON a.creator_id = c.id " +
                      "LEFT JOIN user o ON a.owner_id = o.id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        
        while (rs.next()) {
            artworks.add(extractArtworkFromResultSet(rs));
        }
        
        return artworks;
    }
    
    @Override
    public Artwork getOne(int id) throws SQLException {
        String query = "SELECT a.*, c.name as creator_name, o.name as owner_name FROM artwork a " +
                      "LEFT JOIN user c ON a.creator_id = c.id " +
                      "LEFT JOIN user o ON a.owner_id = o.id " +
                      "WHERE a.id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return extractArtworkFromResultSet(rs);
        }
        return null;
    }
    
    public List<Artwork> getByOwnerId(int ownerId) throws SQLException {
        List<Artwork> artworks = new ArrayList<>();
        String query = "SELECT a.*, c.name as creator_name, o.name as owner_name FROM artwork a " +
                      "LEFT JOIN user c ON a.creator_id = c.id " +
                      "LEFT JOIN user o ON a.owner_id = o.id " +
                      "WHERE a.owner_id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            artworks.add(extractArtworkFromResultSet(rs));
        }
        
        return artworks;
    }
    
    private Artwork extractArtworkFromResultSet(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setId(rs.getInt("id"));
        artwork.setCreatorId(rs.getInt("creator_id"));
        artwork.setOwnerId(rs.getInt("owner_id"));
        artwork.setTitle(rs.getString("title"));
        artwork.setDescription(rs.getString("description"));
        artwork.setImageName(rs.getString("image_name"));
        
        // Create and set the creator User object
        User creator = new User();
        creator.setId(rs.getInt("creator_id"));
        creator.setName(rs.getString("creator_name"));
        artwork.setCreator(creator);
        
        // Create and set the owner User object
        User owner = new User();
        owner.setId(rs.getInt("owner_id"));
        owner.setName(rs.getString("owner_name"));
        artwork.setOwner(owner);
        
        return artwork;
    }
}