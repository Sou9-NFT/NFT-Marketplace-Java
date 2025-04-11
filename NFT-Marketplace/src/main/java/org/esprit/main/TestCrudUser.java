package org.esprit.main;

import java.math.BigDecimal;
import java.util.List;

import org.esprit.models.User;
import org.esprit.services.UserService;

public class TestCrudUser {

    public static void main(String[] args) {
        try {
            UserService userService = new UserService();

            System.out.println("========== USER CRUD TESTING ==========");

            // Check and create first dummy user
            System.out.println("\n----- Creating First User -----");
            String email1 = "john.doe@example.com";
            User existingUser1 = userService.getByEmail(email1);
            User user1;
            
            if (existingUser1 != null) {
                System.out.println("User with email " + email1 + " already exists with ID: " + existingUser1.getId());
                user1 = existingUser1;
            } else {
                user1 = new User(email1, "password123", "John Doe");
                user1.setBalance(new BigDecimal("100.000"));
                user1.setWalletAddress("0x123456789abcdef");
                userService.add(user1);
                System.out.println("User 1 created with ID: " + user1.getId());
            }

            // Check and create second dummy user
            System.out.println("\n----- Creating Second User -----");
            String email2 = "jane.smith@example.com";
            User existingUser2 = userService.getByEmail(email2);
            User user2;
            
            if (existingUser2 != null) {
                System.out.println("User with email " + email2 + " already exists with ID: " + existingUser2.getId());
                user2 = existingUser2;
            } else {
                user2 = new User(email2, "securepass456", "Jane Smith");
                user2.getRoles().add("ROLE_ADMIN"); // Adding ROLE_ADMIN in addition to default ROLE_USER
                user2.setBalance(new BigDecimal("200.000"));
                user2.setWalletAddress("0xabcdef123456789");
                user2.setGithubUsername("janesmith");
                userService.add(user2);
                System.out.println("User 2 created with ID: " + user2.getId());
            }

            // Check and create admin user
            System.out.println("\n----- Creating Admin User -----");
            String adminEmail = "admin@admin.com";
            User existingAdmin = userService.getByEmail(adminEmail);
            User adminUser;
            
            if (existingAdmin != null) {
                System.out.println("Admin user with email " + adminEmail + " already exists with ID: " + existingAdmin.getId());
                adminUser = existingAdmin;
            } else {
                adminUser = new User(adminEmail, "123456", "Administrator");
                adminUser.getRoles().add("ROLE_ADMIN"); // Adding ROLE_ADMIN in addition to default ROLE_USER
                adminUser.setBalance(new BigDecimal("500.000"));
                adminUser.setWalletAddress("0xadmin123456789");
                userService.add(adminUser);
                System.out.println("Admin user created with ID: " + adminUser.getId());
            }

            // Verify all users were added
            System.out.println("\n----- All Users After Creation -----");
            List<User> allUsers = userService.getAll();
            System.out.println("Total users: " + allUsers.size());
            for (User user : allUsers) {
                System.out.println("User: " + user.getId() + " - " + user.getName() + " - " + user.getEmail());
            }

            // Update first user
            System.out.println("\n----- Updating First User -----");
            user1.setName("John Modified Doe");
            user1.setGithubUsername("johndoe");
            userService.update(user1);
            System.out.println("User 1 updated");

            // Delete second user
            System.out.println("\n----- Deleting Second User -----");
            userService.delete(user2);
            System.out.println("User 2 deleted");

            // Verify user1 exists and user2 is deleted
            System.out.println("\n----- Final Users List -----");
            List<User> finalUsers = userService.getAll();
            System.out.println("Total users: " + finalUsers.size());
            for (User user : finalUsers) {
                System.out.println("User: " + user.getId() + " - " + user.getName() + " - " + user.getEmail());
            }

            // Verify user1 details
            User updatedUser1 = userService.getById(user1.getId());
            if (updatedUser1 != null) {
                System.out.println("\nUser 1 details after update:");
                System.out.println("Name: " + updatedUser1.getName());
                System.out.println("GitHub: " + updatedUser1.getGithubUsername());
                System.out.println("Roles: " + updatedUser1.getRoles());
            }

            // Verify user2 no longer exists
            User deletedUser2 = userService.getById(user2.getId());
            if (deletedUser2 == null) {
                System.out.println("\nUser 2 was successfully deleted");
            } else {
                System.out.println("\nUser 2 was not deleted correctly");
            }

            System.out.println("\n========== USER CRUD TESTING COMPLETED ==========");

        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
