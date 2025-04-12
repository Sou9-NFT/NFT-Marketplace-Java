package org.esprit.services;

import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BlogService implements IService<Blog> {
    
    private Connection connection;
    private UserService userService;
    
    public BlogService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
    }
    
    @Override
    public void add(Blog blog) throws Exception {
        String sql = "INSERT INTO blog (user_id, title, translated_title, content, date, " +
                    "translated_content, translation_language, image_filename) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, blog.getUser().getId());
            stmt.setString(2, blog.getTitle());
            stmt.setString(3, blog.getTranslatedTitle());
            stmt.setString(4, blog.getContent());
            stmt.setDate(5, java.sql.Date.valueOf(blog.getDate()));
            stmt.setString(6, blog.getTranslatedContent());
            stmt.setString(7, blog.getTranslationLanguage());
            stmt.setString(8, blog.getImageFilename());
            
            stmt.executeUpdate();
            
            // Set the generated ID back to the blog object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    blog.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
    
    @Override
    public void update(Blog blog) throws Exception {
        String sql = "UPDATE blog SET user_id = ?, title = ?, translated_title = ?, " +
                    "content = ?, date = ?, translated_content = ?, translation_language = ?, " +
                    "image_filename = ? WHERE id = ?";
                    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, blog.getUser().getId());
            stmt.setString(2, blog.getTitle());
            stmt.setString(3, blog.getTranslatedTitle());
            stmt.setString(4, blog.getContent());
            stmt.setDate(5, java.sql.Date.valueOf(blog.getDate()));
            stmt.setString(6, blog.getTranslatedContent());
            stmt.setString(7, blog.getTranslationLanguage());
            stmt.setString(8, blog.getImageFilename());
            stmt.setInt(9, blog.getId());
            
            stmt.executeUpdate();
        }
    }
    
    @Override
    public void delete(Blog blog) throws Exception {
        String sql = "DELETE FROM blog WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, blog.getId());
            stmt.executeUpdate();
        }
    }
    
    @Override
    public List<Blog> getAll() throws Exception {
        List<Blog> blogs = new ArrayList<>();
        String sql = "SELECT * FROM blog";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                blogs.add(mapResultSetToBlog(rs));
            }
        }
        
        return blogs;
    }
    
    public Blog getById(int id) throws Exception {
        String sql = "SELECT * FROM blog WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBlog(rs);
                }
            }
        }
        
        return null;
    }
    
    public List<Blog> getByUser(User user) throws Exception {
        List<Blog> blogs = new ArrayList<>();
        String sql = "SELECT * FROM blog WHERE user_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    blogs.add(mapResultSetToBlog(rs));
                }
            }
        }
        
        return blogs;
    }
    
    private Blog mapResultSetToBlog(ResultSet rs) throws Exception {
        Blog blog = new Blog();
        blog.setId(rs.getInt("id"));
        blog.setTitle(rs.getString("title"));
        blog.setTranslatedTitle(rs.getString("translated_title"));
        blog.setContent(rs.getString("content"));
        
        Date sqlDate = rs.getDate("date");
        if (sqlDate != null) {
            blog.setDate(sqlDate.toLocalDate());
        }
        
        // Get the user object using UserService
        int userId = rs.getInt("user_id");
        User user = userService.getById(userId);
        blog.setUser(user);
        
        blog.setTranslatedContent(rs.getString("translated_content"));
        blog.setTranslationLanguage(rs.getString("translation_language"));
        blog.setImageFilename(rs.getString("image_filename"));
        
        return blog;
    }
}
