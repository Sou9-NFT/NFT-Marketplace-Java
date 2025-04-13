package org.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Singleton Design Pattern
public class DatabaseConnection {

    private final String URL = "jdbc:mysql://localhost:3306/sou9_nft";
    private final String USER = "root";
    private final String PASS = "";
    private Connection connection;
    private static DatabaseConnection instance;

    private DatabaseConnection(){
        try {
            // Load the JDBC driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish connection with timeout and additional parameters
            connection = DriverManager.getConnection(
                URL + "?connectTimeout=5000&useSSL=false&allowPublicKeyRetrieval=true", 
                USER, 
                PASS
            );
            System.out.println("Database connection established successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error establishing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance(){
        if(instance == null)
            instance = new DatabaseConnection();
        return instance;
    }

    public Connection getConnection() {
        try {
            // Check if connection is closed or invalid, and reconnect if needed
            if (connection == null || connection.isClosed()) {
                System.out.println("Connection was null or closed, attempting to reconnect...");
                instance = new DatabaseConnection();
                connection = instance.connection;
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection status: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}