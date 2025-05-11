package org.esprit.main;

import java.io.IOException;

import org.esprit.controllers.RaffleListController;
import org.esprit.controllers.ResetPasswordController;
import org.esprit.models.User;
import org.esprit.utils.UrlProtocolHandler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class NftMarketplaceApplication extends Application {
    private static Stage primaryStage;
    private static RaffleListController currentRaffleController;
    private static String resetToken = null;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        // Set application icon that will appear in the taskbar
        try {
            Image icon = new Image(getClass().getResourceAsStream("/kit/icon_2.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }
        
        // Check if we have a reset token to process
        if (resetToken != null) {
            showResetPasswordView(resetToken);
        } else {
            showLoginView();
        }
        
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
    
    public static void showResetPasswordView(String token) throws IOException {
        FXMLLoader loader = new FXMLLoader(NftMarketplaceApplication.class.getResource("/fxml/ResetPassword.fxml"));
        Parent root = loader.load();
        
        // Set the reset token in the controller
        ResetPasswordController controller = loader.getController();
        controller.setResetToken(token);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(NftMarketplaceApplication.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("NFT Marketplace - Reset Password");
    }    public static void showRaffleList(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(NftMarketplaceApplication.class.getResource("/fxml/RaffleList.fxml"));
        Parent root = loader.load();
        
        RaffleListController controller = loader.getController();
        currentRaffleController = controller; // Store controller reference
        controller.setUser(user);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(NftMarketplaceApplication.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("NFT Marketplace - Raffles");
        
        // Set the stage to fullscreen
        primaryStage.setMaximized(true);
    }

    public static void main(String[] args) {
        // Check if the application was launched with a password reset token
        if (args.length > 0) {
            // Check if it's a custom URL protocol
            String arg = String.join(" ", args);
            if (arg.startsWith("nftmarketplace://")) {
                // Parse the URL
                String[] parts = UrlProtocolHandler.parseUrl(arg);
                if (parts != null && parts.length >= 2 && "reset-password".equals(parts[0])) {
                    resetToken = parts[1];
                }
            } else {
                // Look for a token parameter (format: "token=xyz123")
                for (String argument : args) {
                    if (argument.startsWith("token=")) {
                        resetToken = argument.substring(6); // Extract token value
                        break;
                    }
                }
            }
        }
        
        // Register URL protocol handler on first run
        UrlProtocolHandler.registerProtocolHandler();
        
        launch(args);
    }
}