package org.esprit.main;

import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.CategoryService;
import org.esprit.services.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;

public class TestCrudCategory {
    private static CategoryService categoryService = new CategoryService();
    private static UserService userService = new UserService();
    
    public static void main(String[] args) {
        try {
            System.out.println("========== CATEGORY CRUD TESTING ==========");
            
            // First, make sure we have a manager user in the system
            User manager = ensureManagerExists();
            
            // 1. Create or get first category
            System.out.println("\n----- Creating/Getting First Category -----");
            String categoryName1 = "Digital Art";
            Category category1 = getCategoryByNameOrCreate(
                categoryName1,
                manager.getId(),
                "image",
                "Digital artwork including illustrations and 2D designs",
                Arrays.asList("image/jpeg", "image/png", "image/gif")
            );
            System.out.println("Category 1: " + category1.getName() + " with ID: " + category1.getId());
            
            // 2. Create or get second category
            System.out.println("\n----- Creating/Getting Second Category -----");
            String categoryName2 = "3D Models";
            Category category2 = getCategoryByNameOrCreate(
                categoryName2,
                manager.getId(),
                "model",
                "3D models and sculptures",
                Arrays.asList("model/gltf-binary", "model/obj", "application/octet-stream")
            );
            System.out.println("Category 2: " + category2.getName() + " with ID: " + category2.getId());
            
            // 3. Display all categories
            System.out.println("\n----- All Categories After Creation/Retrieval -----");
            List<Category> allCategories = categoryService.getAll();
            System.out.println("Total categories: " + allCategories.size());
            for (Category category : allCategories) {
                printCategoryDetails(category);
            }
            
            // 4. Update category
            System.out.println("\n----- Updating First Category -----");
            // Generate unique name for update to avoid duplicates
            String updatedName = "Digital Art Updated " + System.currentTimeMillis();
            category1.setName(updatedName);
            category1.setDescription("Premium digital artwork including illustrations and 2D designs");
            
            // Check if category already has MIME types before adding more
            List<String> updatedMimeTypes = new ArrayList<>();
            if (category1.getAllowedMimeTypes() != null) {
                updatedMimeTypes.addAll(category1.getAllowedMimeTypes());
            }
            if (!updatedMimeTypes.contains("image/webp")) {
                updatedMimeTypes.add("image/webp");
            }
            category1.setAllowedMimeTypes(updatedMimeTypes);
            
            try {
                categoryService.update(category1);
                System.out.println("Category 1 updated successfully to: " + updatedName);
            } catch (Exception e) {
                System.err.println("Failed to update category: " + e.getMessage());
                // Revert the name change for further testing
                category1.setName(categoryName1);
            }
            
            // 5. Search by name
            System.out.println("\n----- Searching for Categories by Name 'Digital' -----");
            List<Category> searchResults = categoryService.searchByName("Digital");
            System.out.println("Found " + searchResults.size() + " results:");
            for (Category category : searchResults) {
                printCategoryDetails(category);
            }
            
            // 6. Get categories by type
            System.out.println("\n----- Getting Categories by Type 'image' -----");
            List<Category> typeResults = categoryService.getByType("image");
            System.out.println("Found " + typeResults.size() + " categories with type 'image':");
            for (Category category : typeResults) {
                printCategoryDetails(category);
            }
            
            // 7. Check if mime type is allowed
            System.out.println("\n----- Checking MIME Type Permissions -----");
            boolean isJpegAllowed = categoryService.isMimeTypeAllowed(category1.getId(), "image/jpeg");
            boolean isWebpAllowed = categoryService.isMimeTypeAllowed(category1.getId(), "image/webp");
            boolean isPdfAllowed = categoryService.isMimeTypeAllowed(category1.getId(), "application/pdf");
            
            System.out.println("Is image/jpeg allowed in category 1? " + isJpegAllowed);
            System.out.println("Is image/webp allowed in category 1? " + isWebpAllowed);
            System.out.println("Is application/pdf allowed in category 1? " + isPdfAllowed);
            
            // 8. Create a temporary category just for deletion testing
            System.out.println("\n----- Creating Temporary Category for Deletion Test -----");
            Category tempCategory = new Category();
            tempCategory.setManagerId(manager.getId());
            tempCategory.setName("Temp Category For Deletion " + System.currentTimeMillis());
            tempCategory.setType("temp");
            tempCategory.setDescription("Temporary category for deletion testing");
            tempCategory.setAllowedMimeTypes(Arrays.asList("text/plain"));
            
            try {
                categoryService.add(tempCategory);
                System.out.println("Temporary category created with ID: " + tempCategory.getId());
                
                // 9. Delete the temporary category
                System.out.println("\n----- Deleting Temporary Category -----");
                categoryService.delete(tempCategory);
                System.out.println("Temporary category deleted");
            } catch (Exception e) {
                System.err.println("Failed to create or delete temporary category: " + e.getMessage());
            }
            
            // 10. Verify final state
            System.out.println("\n----- Final Categories List -----");
            List<Category> finalCategories = categoryService.getAll();
            System.out.println("Total categories: " + finalCategories.size());
            for (Category category : finalCategories) {
                printCategoryDetails(category);
            }
            
            System.out.println("\n========== CATEGORY CRUD TESTING COMPLETED ==========");
            
        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Category getCategoryByNameOrCreate(String name, int managerId, String type, 
                                                    String description, List<String> mimeTypes) {
        try {
            // Search for category with the given name
            List<Category> existingCategories = categoryService.searchByName(name);
            for (Category cat : existingCategories) {
                if (cat.getName().equals(name)) {
                    System.out.println("Category with name '" + name + "' already exists with ID: " + cat.getId());
                    return cat;
                }
            }
            
            // If not found, create a new one
            Category newCategory = new Category();
            newCategory.setManagerId(managerId);
            newCategory.setName(name);
            newCategory.setType(type);
            newCategory.setDescription(description);
            newCategory.setAllowedMimeTypes(mimeTypes);
            
            categoryService.add(newCategory);
            System.out.println("Category '" + name + "' created with ID: " + newCategory.getId());
            return newCategory;
            
        } catch (Exception e) {
            System.err.println("Error creating/retrieving category '" + name + "': " + e.getMessage());
            e.printStackTrace();
            
            // Return a placeholder category as fallback
            Category fallbackCategory = new Category();
            fallbackCategory.setName(name);
            return fallbackCategory;
        }
    }
    
    private static User ensureManagerExists() {
        try {
            List<User> users = userService.getAll();
            if (!users.isEmpty()) {
                System.out.println("Using existing user as manager: " + users.get(0).getName());
                return users.get(0);
            } else {
                System.out.println("No users found. Creating a manager user...");
                User manager = new User("manager@example.com", "password", "Category Manager");
                manager.getRoles().add("ROLE_ADMIN");
                userService.add(manager);
                System.out.println("Created manager user with ID: " + manager.getId());
                return manager;
            }
        } catch (Exception e) {
            System.err.println("Error ensuring manager exists: " + e.getMessage());
            e.printStackTrace();
            // Return a placeholder user with ID 1 as fallback
            User fallbackUser = new User();
            fallbackUser.setId(1);
            return fallbackUser;
        }
    }
    
    private static void printCategoryDetails(Category category) {
        System.out.println("\nID: " + category.getId());
        System.out.println("Name: " + category.getName());
        System.out.println("Type: " + category.getType());
        System.out.println("Description: " + category.getDescription());
        System.out.println("Manager ID: " + category.getManagerId());
        System.out.print("Allowed MIME Types: ");
        if (category.getAllowedMimeTypes() != null) {
            System.out.println(String.join(", ", category.getAllowedMimeTypes()));
        } else {
            System.out.println("None");
        }
        System.out.println("----------------------------------------");
    }
}
