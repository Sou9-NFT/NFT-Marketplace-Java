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
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class ArtworkService implements IService<Artwork> {
    
    private Connection connection;
    private UserService userService;
    
    public ArtworkService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
    }
    
    @Override
    public void add(Artwork artwork) throws Exception {
        String sql = "INSERT INTO artwork (creator_id, owner_id, category_id, title, description, " +
                     "price, image_name, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, artwork.getCreatorId());
            stmt.setInt(2, artwork.getOwnerId());
            stmt.setInt(3, artwork.getCategoryId());
            stmt.setString(4, artwork.getTitle());
            stmt.setString(5, artwork.getDescription());
            stmt.setDouble(6, artwork.getPrice());
            stmt.setString(7, artwork.getImageName());
            
            // Set timestamps for created_at and updated_at
            LocalDateTime now = LocalDateTime.now();
            stmt.setTimestamp(8, artwork.getCreatedAt() != null ? 
                             Timestamp.valueOf(artwork.getCreatedAt()) : Timestamp.valueOf(now));
            stmt.setTimestamp(9, artwork.getUpdatedAt() != null ? 
                             Timestamp.valueOf(artwork.getUpdatedAt()) : Timestamp.valueOf(now));
            
            stmt.executeUpdate();
            
            // Set the generated ID back to the artwork object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    artwork.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
    
    @Override
    public void update(Artwork artwork) throws Exception {
        String sql = "UPDATE artwork SET creator_id = ?, owner_id = ?, category_id = ?, " +
                     "title = ?, description = ?, price = ?, image_name = ?, updated_at = ? " +
                     "WHERE id = ?";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, artwork.getCreatorId());
            stmt.setInt(2, artwork.getOwnerId());
            stmt.setInt(3, artwork.getCategoryId());
            stmt.setString(4, artwork.getTitle());
            stmt.setString(5, artwork.getDescription());
            stmt.setDouble(6, artwork.getPrice());
            stmt.setString(7, artwork.getImageName());
            
            // Set timestamp for updated_at to current time
            LocalDateTime now = LocalDateTime.now();
            stmt.setTimestamp(8, Timestamp.valueOf(now));
            
            stmt.setInt(9, artwork.getId());
            
            stmt.executeUpdate();
            
            // Update the updatedAt field in the object
            artwork.setUpdatedAt(now);
        }
    }
    
    @Override
    public void delete(Artwork artwork) throws Exception {
        String sql = "DELETE FROM artwork WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, artwork.getId());
            stmt.executeUpdate();
        }
    }
    
    @Override
    public List<Artwork> getAll() throws Exception {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artwork";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                artworks.add(mapResultSetToArtwork(rs));
            }
        }
        
        return artworks;
    }
    
    @Override
    public Artwork getOne(int id) throws Exception {
        return getById(id);
    }
    
    public Artwork getById(int id) throws Exception {
        String sql = "SELECT * FROM artwork WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToArtwork(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<Artwork> getByCreator(int creatorId) throws Exception {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artwork WHERE creator_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, creatorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        }
        
        return artworks;
    }
    
    public List<Artwork> getByOwner(int ownerId) throws Exception {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artwork WHERE owner_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        }
        
        return artworks;
    }
    
    public List<Artwork> getByCategory(int categoryId) throws Exception {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artwork WHERE category_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        }
        
        return artworks;
    }
    
    public List<Artwork> searchByTitle(String searchTerm) throws Exception {
        List<Artwork> artworks = new ArrayList<>();
        String sql = "SELECT * FROM artwork WHERE title LIKE ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapResultSetToArtwork(rs));
                }
            }
        }
        
        return artworks;
    }
    
    public boolean transferOwnership(Artwork artwork, User newOwner, double salePrice) throws Exception {
        // Get current owner for balance update
        User currentOwner = userService.getById(artwork.getOwnerId());
        if (currentOwner == null || newOwner == null) {
            return false;
        }
        
        // Check if buyer has enough balance
        if (newOwner.getBalance().doubleValue() < salePrice) {
            return false;
        }
        
        // Begin transaction
        connection.setAutoCommit(false);
        
        try {
            // Update artwork ownership
            String updateArtworkSql = "UPDATE artwork SET owner_id = ?, price = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateArtworkSql)) {
                stmt.setInt(1, newOwner.getId());
                stmt.setDouble(2, salePrice); // Update price to the sale price
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(4, artwork.getId());
                stmt.executeUpdate();
            }
            
            // Update balances
            // Deduct from buyer
            newOwner.setBalance(newOwner.getBalance().subtract(java.math.BigDecimal.valueOf(salePrice)));
            userService.update(newOwner);
            
            // Add to seller
            currentOwner.setBalance(currentOwner.getBalance().add(java.math.BigDecimal.valueOf(salePrice)));
            userService.update(currentOwner);
            
            // Commit transaction
            connection.commit();
            
            // Update the artwork object
            artwork.setOwnerId(newOwner.getId());
            artwork.setPrice(salePrice);
            artwork.setUpdatedAt(LocalDateTime.now());
            
            return true;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    private Artwork mapResultSetToArtwork(ResultSet rs) throws SQLException {
        Artwork artwork = new Artwork();
        artwork.setId(rs.getInt("id"));
        artwork.setCreatorId(rs.getInt("creator_id"));
        artwork.setOwnerId(rs.getInt("owner_id"));
        artwork.setCategoryId(rs.getInt("category_id"));
        artwork.setTitle(rs.getString("title"));
        artwork.setDescription(rs.getString("description"));
        artwork.setPrice(rs.getDouble("price"));
        artwork.setImageName(rs.getString("image_name"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            artwork.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            artwork.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return artwork;
    }
}
