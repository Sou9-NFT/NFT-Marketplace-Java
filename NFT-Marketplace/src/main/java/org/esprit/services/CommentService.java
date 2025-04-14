package org.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.Comment;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.utils.DatabaseConnection;

public class CommentService implements IService<Comment> {

    private Connection connection;
    private UserService userService;
    private BlogService blogService;

    public CommentService() {
        connection = DatabaseConnection.getInstance().getConnection();
        userService = new UserService();
        blogService = new BlogService();
    }

    @Override
    public void add(Comment comment) throws Exception {
        String sql = "INSERT INTO comment (user_id, blog_id, content, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getUser().getId());
            stmt.setInt(2, comment.getBlog().getId());
            stmt.setString(3, comment.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(comment.getCreatedAt()));

            stmt.executeUpdate();

            // Set the generated ID back to the comment object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    comment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Comment comment) throws Exception {
        String sql = "UPDATE comment SET content = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, comment.getContent());
            stmt.setInt(2, comment.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Comment comment) throws Exception {
        String sql = "DELETE FROM comment WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, comment.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Comment> getAll() throws Exception {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comment ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                comments.add(mapResultSetToComment(rs));
            }
        }

        return comments;
    }

    @Override
    public Comment getOne(int id) throws Exception {
        return getById(id);
    }

    public Comment getById(int id) throws Exception {
        String sql = "SELECT * FROM comment WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToComment(rs);
                }
            }
        }

        return null;
    }

    public List<Comment> getByBlog(Blog blog) throws Exception {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comment WHERE blog_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, blog.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapResultSetToComment(rs));
                }
            }
        }

        return comments;
    }

    public List<Comment> getByUser(User user) throws Exception {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM comment WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapResultSetToComment(rs));
                }
            }
        }

        return comments;
    }

    private Comment mapResultSetToComment(ResultSet rs) throws Exception {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Get the user and blog from their respective services
        User user = userService.getById(rs.getInt("user_id"));
        Blog blog = blogService.getById(rs.getInt("blog_id"));

        comment.setUser(user);
        comment.setBlog(blog);

        return comment;
    }
}
