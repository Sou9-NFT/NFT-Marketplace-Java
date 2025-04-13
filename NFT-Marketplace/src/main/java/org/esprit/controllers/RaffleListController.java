package org.esprit.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.RaffleService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RaffleListController {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilter;
    
    @FXML
    private GridPane rafflesGrid;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private RaffleService raffleService;
    
    @FXML
    public void initialize() {
        raffleService = new RaffleService();
        
        // Initialize the controller
        statusFilter.getItems().addAll("All", "active", "ended");
        statusFilter.setValue("All");
        
        // Add listener for search field and status filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshRaffles());

        // Initial load of raffles
        refreshRaffles();
    }

    public void setUser(User user) {
        this.currentUser = user;
        // Refresh raffles again after user is set
        refreshRaffles();
    }
    
    @FXML
    private void handleCreateRaffle(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateRaffle.fxml"));
            Parent createRaffleView = loader.load();
            
            CreateRaffleController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(createRaffleView));
            stage.setTitle("Create New Raffle");
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open create raffle view: " + e.getMessage());
        }
    }
    
    public void refreshRaffles() {
        try {
            // Clear the current grid
            rafflesGrid.getChildren().clear();
            rafflesGrid.getRowConstraints().clear();
            rafflesGrid.getColumnConstraints().clear();
            
            // Get all raffles
            List<Raffle> raffles = raffleService.getAll();
            
            // Filter based on search text and status
            String searchText = searchField.getText().toLowerCase();
            String statusValue = statusFilter.getValue();
            
            raffles = raffles.stream()
                .filter(raffle -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                        raffle.getTitle().toLowerCase().contains(searchText) ||
                        raffle.getRaffleDescription().toLowerCase().contains(searchText);
                        
                    boolean matchesStatus = statusValue.equals("All") ||
                        raffle.getStatus().equalsIgnoreCase(statusValue);
                        
                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());
            
            // Display raffles in grid
            int col = 0;
            int row = 0;
            final int MAX_COLUMNS = 3;
            
            for (Raffle raffle : raffles) {
                VBox raffleCard = createRaffleCard(raffle);
                rafflesGrid.add(raffleCard, col, row);
                
                col++;
                if (col >= MAX_COLUMNS) {
                    col = 0;
                    row++;
                }
            }
            
            statusLabel.setVisible(false);
            
        } catch (SQLException e) {
            showError("Error loading raffles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private VBox createRaffleCard(Raffle raffle) {
        VBox card = new VBox(10);
        card.getStyleClass().add("raffle-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        
        Label titleLabel = new Label(raffle.getTitle());
        titleLabel.getStyleClass().add("raffle-title");
        titleLabel.setWrapText(true);
        
        Label descLabel = new Label(raffle.getRaffleDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);
        
        Label statusLabel = new Label("Status: " + raffle.getStatus());
        Label creatorLabel = new Label("Creator: " + raffle.getCreatorName());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Label endDateLabel = new Label("Ends: " + dateFormat.format(raffle.getEndTime()));
        
        Button detailsButton = new Button("View Details");
        detailsButton.getStyleClass().add("primary-button");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        
        detailsButton.setOnAction(e -> showRaffleDetails(raffle));
        
        card.getChildren().addAll(
            titleLabel,
            descLabel,
            statusLabel,
            creatorLabel,
            endDateLabel,
            detailsButton
        );
        
        return card;
    }
    
    private void showRaffleDetails(Raffle raffle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleDetails.fxml"));
            Parent detailsView = loader.load();
            
            RaffleDetailsController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(detailsView));
            stage.setTitle("Raffle Details - " + raffle.getTitle());
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open raffle details: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }

    /**
     * Cleanup resources before controller is destroyed
     * Called when application is closing to handle any necessary cleanup
     */
    public void cleanup() {
        // Close any open dialogs
        if (searchField != null && searchField.getScene() != null && searchField.getScene().getWindow() != null) {
            searchField.getScene().getWindow().hide();
        }
        
        // Clear references
        currentUser = null;
        raffleService = null;
        
        // Clear UI elements
        if (rafflesGrid != null) {
            rafflesGrid.getChildren().clear();
        }
        if (statusFilter != null) {
            statusFilter.getItems().clear();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Return to the user dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDashboard.fxml"));
            Parent dashboardView = loader.load();
            
            UserDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Scene scene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) scene.getWindow();
            
            scene.setRoot(dashboardView);
            stage.setTitle("NFT Marketplace - User Dashboard");
        } catch (IOException e) {
            showError("Failed to return to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}