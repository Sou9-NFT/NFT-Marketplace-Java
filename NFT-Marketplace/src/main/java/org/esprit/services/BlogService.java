package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

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
        String sql = "SELECT * FROM blog ORDER BY date DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                blogs.add(mapResultSetToBlog(rs));
            }
        }
        
        return blogs;
    }
    
    @Override
    public Blog getOne(int id) throws Exception {
        return getById(id);
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
        String sql = "SELECT * FROM blog WHERE user_id = ? ORDER BY date DESC";
        
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

    public List<Blog> search(String searchText) throws Exception {
        List<Blog> blogs = new ArrayList<>();
        String sql = "SELECT * FROM blog WHERE LOWER(title) LIKE ? OR LOWER(content) LIKE ? " +
                    "OR EXISTS (SELECT 1 FROM user WHERE id = blog.user_id AND LOWER(name) LIKE ?) " +
                    "ORDER BY date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchText.toLowerCase() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
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
        blog.setTranslatedContent(rs.getString("translated_content"));
        blog.setDate(rs.getDate("date").toLocalDate());
        blog.setImageFilename(rs.getString("image_filename"));
        blog.setTranslationLanguage(rs.getString("translation_language"));
        
        // Get the user from UserService
        int userId = rs.getInt("user_id");
        User user = userService.getById(userId);
        blog.setUser(user);
        
        return blog;
    }

    public List<Blog> readAll() throws Exception {
        return getAll();
    }
}
