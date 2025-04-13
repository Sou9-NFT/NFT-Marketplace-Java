package org.esprit.services;

import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class UserService implements IService<User> {
    
    private Connection connection;
    
    public UserService() {
        connection = DatabaseConnection.getInstance().getConnection();
    }
    
    @Override
    public void add(User user) throws Exception {
        String sql = "INSERT INTO user (email, roles, balance, password, created_at, name, profile_picture, " +
                     "wallet_address, github_username, password_reset_token, password_reset_token_expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getEmail());
            
            // Convert roles list to JSON array format
            String rolesJson = null;
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < user.getRoles().size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(user.getRoles().get(i)).append("\"");
                }
                jsonBuilder.append("]");
                rolesJson = jsonBuilder.toString();
            }
            stmt.setString(2, rolesJson);
            
            stmt.setBigDecimal(3, user.getBalance());
            stmt.setString(4, user.getPassword());
            stmt.setTimestamp(5, user.getCreatedAt() != null ? 
                             Timestamp.valueOf(user.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(6, user.getName());
            stmt.setString(7, user.getProfilePicture());
            stmt.setString(8, user.getWalletAddress());
            stmt.setString(9, user.getGithubUsername());
            stmt.setString(10, user.getPasswordResetToken());
            stmt.setTimestamp(11, user.getPasswordResetTokenExpiresAt() != null ? 
                             Timestamp.valueOf(user.getPasswordResetTokenExpiresAt()) : null);
            
            stmt.executeUpdate();
            
            // Set the generated ID back to the user object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
    
    @Override
    public void update(User user) throws Exception {
        String sql = "UPDATE user SET email = ?, roles = ?, balance = ?, password = ?, " +
                     "name = ?, profile_picture = ?, wallet_address = ?, github_username = ?, " +
                     "password_reset_token = ?, password_reset_token_expires_at = ? WHERE id = ?";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            
            // Convert roles list to JSON array format
            String rolesJson = null;
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < user.getRoles().size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(user.getRoles().get(i)).append("\"");
                }
                jsonBuilder.append("]");
                rolesJson = jsonBuilder.toString();
            }
            stmt.setString(2, rolesJson);
            
            stmt.setBigDecimal(3, user.getBalance());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getName());
            stmt.setString(6, user.getProfilePicture());
            stmt.setString(7, user.getWalletAddress());
            stmt.setString(8, user.getGithubUsername());
            stmt.setString(9, user.getPasswordResetToken());
            stmt.setTimestamp(10, user.getPasswordResetTokenExpiresAt() != null ? 
                             Timestamp.valueOf(user.getPasswordResetTokenExpiresAt()) : null);
            stmt.setInt(11, user.getId());
            
            stmt.executeUpdate();
        }
    }
    
    @Override
    public void delete(User user) throws Exception {
        String sql = "DELETE FROM user WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.executeUpdate();
        }
    }
    
    @Override
    public List<User> getAll() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        
        return users;
    }

    @Override
    public User getOne(int id) throws Exception {
        return getById(id);
    }
    
    public User getById(int id) throws Exception {
        String sql = "SELECT * FROM user WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        
        return null;
    }
    
    public User getByEmail(String email) throws Exception {
        String sql = "SELECT * FROM user WHERE email = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        
        return null;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        
        // Parse JSON array format for roles
        String rolesJson = rs.getString("roles");
        if (rolesJson != null && !rolesJson.isEmpty()) {
            List<String> rolesList = new ArrayList<>();
            
            // Simple parsing of JSON array format: ["ROLE_USER","ROLE_ADMIN"]
            if (rolesJson.startsWith("[") && rolesJson.endsWith("]")) {
                String[] jsonParts = rolesJson.substring(1, rolesJson.length() - 1).split(",");
                for (String part : jsonParts) {
                    if (!part.isEmpty()) {
                        // Remove quotes from role values
                        String role = part.trim();
                        if (role.startsWith("\"") && role.endsWith("\"")) {
                            role = role.substring(1, role.length() - 1);
                        }
                        rolesList.add(role);
                    }
                }
            }
            
            user.setRoles(rolesList);
        }
        
        user.setBalance(rs.getBigDecimal("balance"));
        user.setPassword(rs.getString("password"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        user.setName(rs.getString("name"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setWalletAddress(rs.getString("wallet_address"));
        user.setGithubUsername(rs.getString("github_username"));
        user.setPasswordResetToken(rs.getString("password_reset_token"));
        
        Timestamp resetTokenExpiry = rs.getTimestamp("password_reset_token_expires_at");
        if (resetTokenExpiry != null) {
            user.setPasswordResetTokenExpiresAt(resetTokenExpiry.toLocalDateTime());
        }
        
        return user;
    }
}
