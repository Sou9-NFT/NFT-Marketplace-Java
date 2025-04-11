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

            // Create first dummy user
            System.out.println("\n----- Creating First User -----");
            User user1 = new User("john.doe@example.com", "password123", "John Doe");
            user1.setBalance(new BigDecimal("100.000"));
            user1.setWalletAddress("0x123456789abcdef");
            userService.add(user1);
            System.out.println("User 1 created with ID: " + user1.getId());

            // Create second dummy user
            System.out.println("\n----- Creating Second User -----");
            User user2 = new User("jane.smith@example.com", "securepass456", "Jane Smith");
            user2.getRoles().add("ROLE_ADMIN"); // Adding ROLE_ADMIN in addition to default ROLE_USER
            user2.setBalance(new BigDecimal("200.000"));
            user2.setWalletAddress("0xabcdef123456789");
            user2.setGithubUsername("janesmith");
            userService.add(user2);
            System.out.println("User 2 created with ID: " + user2.getId());

            // Verify both users were added
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
