package org.esprit.main;

import org.esprit.utils.DatabaseConnection;
import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        if (conn != null) {
            System.out.println("Connected to MySQL successfully!");
        } else {
            System.out.println("Connection failed!");
        }
    }
}