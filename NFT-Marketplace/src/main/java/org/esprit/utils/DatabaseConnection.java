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
            connection = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static DatabaseConnection getInstance(){
        if(instance == null)
            instance = new DatabaseConnection();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}