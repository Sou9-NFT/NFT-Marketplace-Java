package org.esprit.main;

import java.io.IOException;

import org.esprit.controllers.RaffleListController;
import org.esprit.models.User;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NftMarketplaceApplication extends Application {
    private static Stage primaryStage;
    private static RaffleListController currentRaffleController;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        showLoginView();
        
        // Add window close handler
        primaryStage.setOnCloseRequest(e -> {
            if (currentRaffleController != null) {
                currentRaffleController.cleanup();
            }
        });
        
        primaryStage.show();
    }

    public static void showLoginView() throws IOException {
        currentRaffleController = null; // Reset controller reference
        FXMLLoader loader = new FXMLLoader(NftMarketplaceApplication.class.getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(NftMarketplaceApplication.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("NFT Marketplace - Login");
    }

    public static void showRaffleList(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(NftMarketplaceApplication.class.getResource("/fxml/RaffleList.fxml"));
        Parent root = loader.load();
        
        RaffleListController controller = loader.getController();
        currentRaffleController = controller; // Store controller reference
        controller.setUser(user);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(NftMarketplaceApplication.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("NFT Marketplace - Raffles");
    }

    public static void main(String[] args) {
        launch();
    }
}