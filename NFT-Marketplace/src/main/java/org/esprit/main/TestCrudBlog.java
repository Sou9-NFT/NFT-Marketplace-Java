package org.esprit.main;

import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import org.esprit.services.UserService;

import java.time.LocalDate;
import java.util.List;

public class TestCrudBlog {

    public static void main(String[] args) {
        try {
            BlogService blogService = new BlogService();
            UserService userService = new UserService();

            System.out.println("========== BLOG CRUD TESTING ==========");

            // First, get a user to be the author of our test blogs
            User author = userService.getByEmail("john.doe@example.com");
            if (author == null) {
                System.out.println("Creating test user...");
                author = new User("john.doe@example.com", "password123", "John Doe");
                userService.add(author);
            }

            // Create first test blog
            System.out.println("\n----- Creating First Blog -----");
            Blog blog1 = new Blog(
                "My First NFT Experience",
                "Today I bought my first NFT and it was an amazing experience...",
                author
            );
            blog1.setImageFilename("first_nft.jpg");
            blogService.add(blog1);
            System.out.println("Blog 1 created with ID: " + blog1.getId());

            // Create second test blog
            System.out.println("\n----- Creating Second Blog -----");
            Blog blog2 = new Blog(
                "The Future of Digital Art",
                "NFTs are revolutionizing how we think about digital art ownership...",
                author
            );
            blog2.setImageFilename("digital_art.jpg");
            blogService.add(blog2);
            System.out.println("Blog 2 created with ID: " + blog2.getId());

            // Update first blog
            System.out.println("\n----- Updating First Blog -----");
            blog1.setTitle("My Updated NFT Experience");
            blog1.setContent("I've learned so much more about NFTs since my first purchase...");
            blogService.update(blog1);
            System.out.println("Blog 1 updated");

            // List all blogs
            System.out.println("\n----- All Blogs -----");
            List<Blog> allBlogs = blogService.getAll();
            for (Blog blog : allBlogs) {
                System.out.println(blog);
            }

            // Get blogs by user
            System.out.println("\n----- Blogs by User -----");
            List<Blog> userBlogs = blogService.getByUser(author);
            System.out.println("Found " + userBlogs.size() + " blogs by " + author.getName());

            // Delete second blog
            System.out.println("\n----- Deleting Second Blog -----");
            blogService.delete(blog2);
            System.out.println("Blog 2 deleted");

            // Verify deletion
            System.out.println("\n----- Final Blog List -----");
            List<Blog> finalBlogs = blogService.getAll();
            System.out.println("Total blogs: " + finalBlogs.size());
            for (Blog blog : finalBlogs) {
                System.out.println(blog);
            }

            // Test blog validation
            System.out.println("\n----- Testing Blog Validation -----");
            Blog invalidBlog = new Blog();
            Blog.ValidationResult validationResult = invalidBlog.validate();
            if (!validationResult.isValid()) {
                System.out.println("Validation errors:");
                for (String field : validationResult.getErrors().keySet()) {
                    System.out.println(field + ": " + validationResult.getErrors().get(field));
                }
            }

            System.out.println("\n========== BLOG CRUD TESTING COMPLETED ==========");

        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
