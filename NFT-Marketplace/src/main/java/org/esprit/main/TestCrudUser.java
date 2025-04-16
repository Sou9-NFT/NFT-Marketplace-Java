package org.esprit.main;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.esprit.models.User;
import org.esprit.services.UserService;
import org.esprit.utils.DatabaseConnection;
import org.esprit.utils.PasswordHasher;

public class TestCrudUser {

    public static void main(String[] args) {
        try {
            Connection connection = DatabaseConnection.getInstance().getConnection();
            System.out.println("========== TRUNCATING TABLES ==========");
            
            // Define the tables to truncate (excluding migration and messenger tables)
            String[] tablesToTruncate = {
                "artwork", "bet_session", "bid", "blog", "category", "comment",
                "notification", "participant", "raffle", "top_up_request",
                "trade_dispute", "trade_offer", "trade_state", "vote", "user"
            };
            
            // Disable foreign key checks to allow truncating tables with foreign key constraints
            try (PreparedStatement disableChecks = connection.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")) {
                disableChecks.executeUpdate();
                System.out.println("Foreign key checks disabled");
            }
            
            // Truncate each table
            for (String table : tablesToTruncate) {
                try (PreparedStatement truncateStmt = connection.prepareStatement("TRUNCATE TABLE " + table)) {
                    truncateStmt.executeUpdate();
                    System.out.println("Table " + table + " truncated");
                } catch (SQLException e) {
                    System.out.println("Error truncating table " + table + ": " + e.getMessage());
                }
            }
            
            // Re-enable foreign key checks
            try (PreparedStatement enableChecks = connection.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                enableChecks.executeUpdate();
                System.out.println("Foreign key checks re-enabled");
            }
            
            System.out.println("========== TABLES TRUNCATED ==========");
            
            // Now proceed with user creation
            UserService userService = new UserService();

            System.out.println("========== USER CRUD TESTING ==========");
            
            // Create admin user with hashed password
            System.out.println("\n----- Creating Admin User -----");
            String adminEmail = "admin@admin.com";
            String adminPlainPassword = "123456";
            String hashedAdminPassword = PasswordHasher.hashPassword(adminPlainPassword);
            User adminUser = new User(adminEmail, hashedAdminPassword, "Administrator");
            adminUser.getRoles().add("ROLE_ADMIN"); // Adding ROLE_ADMIN in addition to default ROLE_USER
            adminUser.setBalance(new BigDecimal("500.000"));
            adminUser.setWalletAddress("0xadmin123456789");
            userService.add(adminUser);
            System.out.println("Admin user created with ID: " + adminUser.getId());
            System.out.println("Admin plain password: " + adminPlainPassword);
            System.out.println("Admin hashed password: " + hashedAdminPassword);

            // Create regular user with hashed password
            System.out.println("\n----- Creating Regular User -----");
            String userEmail = "user@user.com";
            String userPlainPassword = "Aymen123456";
            String hashedUserPassword = PasswordHasher.hashPassword(userPlainPassword);
            User regularUser = new User(userEmail, hashedUserPassword, "Regular User");
            regularUser.setBalance(new BigDecimal("100.000"));
            regularUser.setWalletAddress("0xuser123456789");
            userService.add(regularUser);
            System.out.println("Regular user created with ID: " + regularUser.getId());
            System.out.println("User plain password: " + userPlainPassword);
            System.out.println("User hashed password: " + hashedUserPassword);

            // Create first dummy user
            System.out.println("\n----- Creating First Dummy User -----");
            String email1 = "john.doe@example.com";
            String plainPassword1 = "Password123";
            String hashedPassword1 = PasswordHasher.hashPassword(plainPassword1);
            User user1 = new User(email1, hashedPassword1, "John Doe");
            user1.setBalance(new BigDecimal("100.000"));
            user1.setWalletAddress("0x123456789abcdef");
            userService.add(user1);
            System.out.println("User 1 created with ID: " + user1.getId());
            System.out.println("User 1 plain password: " + plainPassword1);
            System.out.println("User 1 hashed password: " + hashedPassword1);

            // Create second dummy user
            System.out.println("\n----- Creating Second Dummy User -----");
            String email2 = "jane.smith@example.com";
            String plainPassword2 = "SecurePass456";
            String hashedPassword2 = PasswordHasher.hashPassword(plainPassword2);
            User user2 = new User(email2, hashedPassword2, "Jane Smith");
            user2.getRoles().add("ROLE_ADMIN"); // Adding ROLE_ADMIN in addition to default ROLE_USER
            user2.setBalance(new BigDecimal("200.000"));
            user2.setWalletAddress("0xabcdef123456789");
            user2.setGithubUsername("janesmith");
            userService.add(user2);
            System.out.println("User 2 created with ID: " + user2.getId());
            System.out.println("User 2 plain password: " + plainPassword2);
            System.out.println("User 2 hashed password: " + hashedPassword2);

            // Verify all users are in the system
            System.out.println("\n----- All Users After Creation -----");
            List<User> allUsers = userService.getAll();
            System.out.println("Total users: " + allUsers.size());
            for (User user : allUsers) {
                System.out.println("User: " + user.getId() + " - " + user.getName() + " - " + user.getEmail() + " - Roles: " + user.getRoles());
            }
            
            // Verify password hashing is working correctly
            System.out.println("\n----- Password Verification Tests -----");
            // Test admin password
            boolean adminPasswordValid = PasswordHasher.verifyPassword(adminPlainPassword, hashedAdminPassword);
            System.out.println("Admin password verification: " + (adminPasswordValid ? "PASSED ✓" : "FAILED ✗"));
            
            // Test user password
            boolean userPasswordValid = PasswordHasher.verifyPassword(userPlainPassword, hashedUserPassword);
            System.out.println("User password verification: " + (userPasswordValid ? "PASSED ✓" : "FAILED ✗"));
            
            // Test wrong password
            boolean wrongPasswordTest = !PasswordHasher.verifyPassword("wrong_password", hashedAdminPassword);
            System.out.println("Wrong password rejection test: " + (wrongPasswordTest ? "PASSED ✓" : "FAILED ✗"));

            System.out.println("\n========== USER CRUD TESTING COMPLETED ==========");

        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
