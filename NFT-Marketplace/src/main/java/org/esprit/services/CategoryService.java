package org.esprit.services;

import org.esprit.models.Category;
import org.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class CategoryService implements IService<Category> {
    
    private Connection connection;
    
    public CategoryService() {
        connection = DatabaseConnection.getInstance().getConnection();
    }
    
    @Override
    public void add(Category category) throws Exception {
        String sql = "INSERT INTO category (manager_id, name, type, description, allowed_mime_types) " +
                     "VALUES (?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, category.getManagerId());
            stmt.setString(2, category.getName());
            stmt.setString(3, category.getType());
            stmt.setString(4, category.getDescription());
            
            // Convert MIME types list to JSON array format
            String mimeTypesJson = null;
            if (category.getAllowedMimeTypes() != null && !category.getAllowedMimeTypes().isEmpty()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < category.getAllowedMimeTypes().size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(category.getAllowedMimeTypes().get(i)).append("\"");
                }
                jsonBuilder.append("]");
                mimeTypesJson = jsonBuilder.toString();
            }
            stmt.setString(5, mimeTypesJson);
            
            stmt.executeUpdate();
            
            // Set the generated ID back to the category object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    category.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
    
    @Override
    public void update(Category category) throws Exception {
        String sql = "UPDATE category SET manager_id = ?, name = ?, type = ?, description = ?, " +
                     "allowed_mime_types = ? WHERE id = ?";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, category.getManagerId());
            stmt.setString(2, category.getName());
            stmt.setString(3, category.getType());
            stmt.setString(4, category.getDescription());
            
            // Convert MIME types list to JSON array format
            String mimeTypesJson = null;
            if (category.getAllowedMimeTypes() != null && !category.getAllowedMimeTypes().isEmpty()) {
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < category.getAllowedMimeTypes().size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(category.getAllowedMimeTypes().get(i)).append("\"");
                }
                jsonBuilder.append("]");
                mimeTypesJson = jsonBuilder.toString();
            }
            stmt.setString(5, mimeTypesJson);
            
            stmt.setInt(6, category.getId());
            
            stmt.executeUpdate();
        }
    }
    
    @Override
    public void delete(Category category) throws Exception {
        String sql = "DELETE FROM category WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, category.getId());
            stmt.executeUpdate();
        }
    }
    
    @Override
    public List<Category> getAll() throws Exception {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM category";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        }
        
        return categories;
    }
    
    public Category getById(int id) throws Exception {
        String sql = "SELECT * FROM category WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<Category> getByManager(int managerId) throws Exception {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM category WHERE manager_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, managerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        }
        
        return categories;
    }
    
    public List<Category> getByType(String type) throws Exception {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM category WHERE type = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, type);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        }
        
        return categories;
    }
    
    public List<Category> searchByName(String searchTerm) throws Exception {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM category WHERE name LIKE ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        }
        
        return categories;
    }
    
    public boolean isMimeTypeAllowed(int categoryId, String mimeType) throws Exception {
        Category category = getById(categoryId);
        if (category == null || category.getAllowedMimeTypes() == null) {
            return false;
        }
        
        return category.getAllowedMimeTypes().contains(mimeType);
    }
    
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setManagerId(rs.getInt("manager_id"));
        category.setName(rs.getString("name"));
        category.setType(rs.getString("type"));
        category.setDescription(rs.getString("description"));
        
        // Parse JSON array format for allowed MIME types
        String mimeTypesJson = rs.getString("allowed_mime_types");
        if (mimeTypesJson != null && !mimeTypesJson.isEmpty()) {
            List<String> mimeTypesList = new ArrayList<>();
            
            // Simple parsing of JSON array format: ["image/jpeg","image/png"]
            if (mimeTypesJson.startsWith("[") && mimeTypesJson.endsWith("]")) {
                String[] jsonParts = mimeTypesJson.substring(1, mimeTypesJson.length() - 1).split(",");
                for (String part : jsonParts) {
                    if (!part.isEmpty()) {
                        // Remove quotes from MIME type values
                        String mimeType = part.trim();
                        if (mimeType.startsWith("\"") && mimeType.endsWith("\"")) {
                            mimeType = mimeType.substring(1, mimeType.length() - 1);
                        }
                        mimeTypesList.add(mimeType);
                    }
                }
            }
            
            category.setAllowedMimeTypes(mimeTypesList);
        }
        
        return category;
    }
}
