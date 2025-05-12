package org.esprit.main;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.esprit.models.Artwork;
import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.CategoryService;
import org.esprit.services.UserService;
import org.esprit.utils.PasswordHasher;

public class TestCrudArtwork {
    private static ArtworkService artworkService = new ArtworkService();
    private static UserService userService = new UserService();
    private static CategoryService categoryService = new CategoryService();
    
    public static void main(String[] args) {
        try {
            System.out.println("========== ARTWORK CRUD TESTING ==========");
            
            // First, make sure we have users in the system
            User creator = ensureUsersExist();
            
            // Make sure we have categories in the system
            List<Category> categories = ensureCategoriesExist(creator.getId());
            if (categories.isEmpty()) {
                System.out.println("Failed to create or retrieve categories. Exiting test.");
                return;
            }
            
            Category category1 = categories.get(0);
            Category category2 = categories.size() > 1 ? categories.get(1) : category1;
            
            // Get all users for reference
            List<User> allUsers = userService.getAll();
            if (allUsers.isEmpty()) {
                System.out.println("No users found in the system. Please run TestCrudUser first.");
                return;
            }
            
            User buyer = allUsers.size() > 1 ? allUsers.get(1) : creator;
            
            // 1. Create artwork
            System.out.println("\n----- Creating New Artwork -----");
            Artwork artwork1 = new Artwork();
            artwork1.setCreatorId(creator.getId());
            artwork1.setOwnerId(creator.getId());
            artwork1.setCategoryId(category1.getId());  // Use our validated category ID
            artwork1.setTitle("Digital Universe");
            artwork1.setDescription("An expansive digital art piece showing the universe");
            artwork1.setPrice(50.00);
            artwork1.setImageName("digital_universe.jpg");
            artwork1.setCreatedAt(LocalDateTime.now());
            artwork1.setUpdatedAt(LocalDateTime.now());
            
            artworkService.add(artwork1);
            System.out.println("Artwork 1 created with ID: " + artwork1.getId());
            
            // 2. Create another artwork
            System.out.println("\n----- Creating Second Artwork -----");
            Artwork artwork2 = new Artwork();
            artwork2.setCreatorId(creator.getId());
            artwork2.setOwnerId(creator.getId());
            artwork2.setCategoryId(category2.getId());  // Use our validated category ID
            artwork2.setTitle("Abstract Dreams");
            artwork2.setDescription("A colorful abstract composition");
            artwork2.setPrice(75.00);
            artwork2.setImageName("abstract_dreams.jpg");
            artwork2.setCreatedAt(LocalDateTime.now());
            artwork2.setUpdatedAt(LocalDateTime.now());
            
            artworkService.add(artwork2);
            System.out.println("Artwork 2 created with ID: " + artwork2.getId());
            
            // 3. Display all artworks
            System.out.println("\n----- All Artworks After Creation -----");
            List<Artwork> allArtworks = artworkService.getAll();
            System.out.println("Total artworks: " + allArtworks.size());
            for (Artwork artwork : allArtworks) {
                printArtworkDetails(artwork);
            }
            
            // 4. Update artwork
            System.out.println("\n----- Updating First Artwork -----");
            artwork1.setTitle("Digital Universe - Remastered");
            artwork1.setDescription("An expansive digital art piece showing the universe with enhanced colors");
            artwork1.setPrice(65.00);
            artwork1.setUpdatedAt(LocalDateTime.now());
            artworkService.update(artwork1);
            System.out.println("Artwork 1 updated successfully");
            
            // 5. Search by title
            System.out.println("\n----- Searching for Artwork by Title 'Digital' -----");
            List<Artwork> searchResults = artworkService.searchByTitle("Digital");
            System.out.println("Found " + searchResults.size() + " results:");
            for (Artwork artwork : searchResults) {
                printArtworkDetails(artwork);
            }
            
            // 6. Get artworks by category
            System.out.println("\n----- Getting Artworks by Category -----");
            List<Artwork> categoryResults = artworkService.getByCategory(category1.getId());
            System.out.println("Found " + categoryResults.size() + " artworks in category '" + category1.getName() + "':");
            for (Artwork artwork : categoryResults) {
                printArtworkDetails(artwork);
            }
            
            // 7. Transfer artwork ownership (if we have at least 2 users)
            if (allUsers.size() > 1) {
                System.out.println("\n----- Transferring Artwork Ownership -----");
                
                // Make sure buyer has enough balance
                if (buyer.getBalance().doubleValue() < artwork2.getPrice()) {
                    buyer.setBalance(new BigDecimal("1000.00"));
                    userService.update(buyer);
                    System.out.println("Updated buyer's balance to ensure sufficient funds.");
                }
                
                boolean success = artworkService.transferOwnership(artwork2, buyer, 80.00);
                
                if (success) {
                    System.out.println("Ownership transferred successfully to user with ID: " + buyer.getId());
                    Artwork updatedArtwork = artworkService.getById(artwork2.getId());
                    printArtworkDetails(updatedArtwork);
                } else {
                    System.out.println("Ownership transfer failed. Check that the buyer has sufficient balance.");
                }
            }
            
            // 8. Delete artwork
            System.out.println("\n----- Deleting Second Artwork -----");
            artworkService.delete(artwork2);
            System.out.println("Artwork 2 deleted");
            
            // 9. Verify final state
            System.out.println("\n----- Final Artworks List -----");
            List<Artwork> finalArtworks = artworkService.getAll();
            System.out.println("Total artworks: " + finalArtworks.size());
            for (Artwork artwork : finalArtworks) {
                printArtworkDetails(artwork);
            }
            
            System.out.println("\n========== ARTWORK CRUD TESTING COMPLETED ==========");
            
        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
      private static User ensureUsersExist() {
        User firstUser = null;
        List<User> users = new ArrayList<>();
        try {
            users = userService.getAll();
            if (users.isEmpty()) {
                System.out.println("No users found in the system. Running basic user creation...");
                // Create a test user if none exists
                try {
                    String password1 = "Password123";
                    String hashedPassword1 = PasswordHasher.hashPassword(password1);
                    User testUser = new User("test@example.com", hashedPassword1, "Test User");
                    testUser.setBalance(new BigDecimal("1000.00"));
                    userService.add(testUser);
                    System.out.println("Created test user with ID: " + testUser.getId());
                    firstUser = testUser;
                    
                    // Create a second user for transfer testing
                    String password2 = "SecurePass456";
                    String hashedPassword2 = PasswordHasher.hashPassword(password2);
                    User secondUser = new User("buyer@example.com", hashedPassword2, "Buyer User");
                    secondUser.setBalance(new BigDecimal("2000.00"));
                    userService.add(secondUser);
                    System.out.println("Created second user with ID: " + secondUser.getId());
                } catch (Exception e) {
                    System.err.println("Failed to create test users: " + e.getMessage());
                }
            } else {
                System.out.println("Found " + users.size() + " existing users in the system.");
                firstUser = users.get(0);
            }
        } catch (Exception e) {
            System.err.println("Error checking existing users: " + e.getMessage());
        }
        return firstUser;
    }
    
    private static List<Category> ensureCategoriesExist(int managerId) {
        List<Category> categories = new ArrayList<>();
        try {
            // Check for existing categories
            categories = categoryService.getAll();
            
            if (categories.isEmpty()) {
                System.out.println("No categories found. Creating test categories...");
                
                // Create first category
                Category digitalArt = new Category();
                digitalArt.setManagerId(managerId);
                digitalArt.setName("Digital Art");
                digitalArt.setType("image");
                digitalArt.setDescription("Digital artwork including illustrations and 2D designs");
                digitalArt.setAllowedMimeTypes(Arrays.asList("image/jpeg", "image/png", "image/gif"));
                
                try {
                    categoryService.add(digitalArt);
                    System.out.println("Created 'Digital Art' category with ID: " + digitalArt.getId());
                    categories.add(digitalArt);
                } catch (Exception e) {
                    System.err.println("Failed to create 'Digital Art' category: " + e.getMessage());
                }
                
                // Create second category
                Category models3D = new Category();
                models3D.setManagerId(managerId);
                models3D.setName("3D Models");
                models3D.setType("model");
                models3D.setDescription("3D models and sculptures");
                models3D.setAllowedMimeTypes(Arrays.asList("model/gltf-binary", "model/obj", "application/octet-stream"));
                
                try {
                    categoryService.add(models3D);
                    System.out.println("Created '3D Models' category with ID: " + models3D.getId());
                    categories.add(models3D);
                } catch (Exception e) {
                    System.err.println("Failed to create '3D Models' category: " + e.getMessage());
                }
            } else {
                System.out.println("Found " + categories.size() + " existing categories:");
                for (Category category : categories) {
                    System.out.println(" - " + category.getName() + " (ID: " + category.getId() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring categories exist: " + e.getMessage());
            e.printStackTrace();
        }
        
        return categories;
    }
    
    private static void printArtworkDetails(Artwork artwork) {
        System.out.println("\nID: " + artwork.getId());
        System.out.println("Title: " + artwork.getTitle());
        System.out.println("Description: " + artwork.getDescription());
        System.out.println("Price: " + artwork.getPrice());
        System.out.println("Creator ID: " + artwork.getCreatorId());
        System.out.println("Owner ID: " + artwork.getOwnerId());
        System.out.println("Category ID: " + artwork.getCategoryId());
        System.out.println("Image: " + artwork.getImageName());
        System.out.println("Created: " + artwork.getCreatedAt());
        System.out.println("Updated: " + artwork.getUpdatedAt());
        System.out.println("----------------------------------------");
    }
}