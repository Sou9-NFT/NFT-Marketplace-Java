package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.RaffleService;
import org.esprit.utils.AlertUtils;

import java.io.IOException;
import java.util.List;

public class RaffleManagementController {
    @FXML private TableView<Raffle> raffleTable;
    @FXML private TableColumn<Raffle, Integer> idColumn;
    @FXML private TableColumn<Raffle, String> titleColumn;
    @FXML private TableColumn<Raffle, String> artworkColumn;
    @FXML private TableColumn<Raffle, String> startDateColumn;
    @FXML private TableColumn<Raffle, String> endDateColumn;
    @FXML private TableColumn<Raffle, String> statusColumn;
    @FXML private TableColumn<Raffle, Void> actionsColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    
    private RaffleService raffleService;
    private User currentUser;
    private ObservableList<Raffle> raffleList;

    public void initialize() {
        raffleService = new RaffleService();
        setupTable();
        setupFilters();
        loadRaffles();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        artworkColumn.setCellValueFactory(new PropertyValueFactory<>("artworkTitle"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        setupActionButtons();
    }

    private void setupActionButtons() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setOnAction(event -> {
                    Raffle raffle = getTableRow().getItem();
                    if (raffle != null) {
                        handleEditRaffle(raffle);
                    }
                });
                
                deleteButton.setOnAction(event -> {
                    Raffle raffle = getTableRow().getItem();
                    if (raffle != null) {
                        handleDeleteRaffle(raffle);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "Active", "Upcoming", "Completed", "Cancelled"
        ));
        statusFilter.setValue("All");
        
        // Add listeners for filter changes
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterRaffles());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterRaffles());
    }

    private void loadRaffles() {
        List<Raffle> raffles = raffleService.getAllRaffles();
        
        // Ensure each raffle has a creator object
        for (Raffle raffle : raffles) {
            // If the raffle has no creator, set the current user as creator
            // This is a temporary fix to prevent NullPointerException
            if (raffle.getCreator() == null) {
                User tempCreator = new User();
                tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
                tempCreator.setName("Unknown Creator");
                raffle.setCreator(tempCreator);
            }
        }
        
        raffleList = FXCollections.observableArrayList(raffles);
        raffleTable.setItems(raffleList);
    }

    private void filterRaffles() {
        String searchText = searchField.getText().toLowerCase();
        String status = statusFilter.getValue();
        
        ObservableList<Raffle> filteredList = raffleList.filtered(raffle -> 
            (searchText.isEmpty() || raffle.getTitle().toLowerCase().contains(searchText)) &&
            (status.equals("All") || raffle.getStatus().equals(status))
        );
        
        raffleTable.setItems(filteredList);
    }

    @FXML
    private void handleSearch() {
        filterRaffles();
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        statusFilter.setValue("All");
        raffleTable.setItems(raffleList);
    }

    @FXML
    private void handleCreateRaffle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateRaffle.fxml"));
            Parent root = loader.load();
            
            CreateRaffleController controller = loader.getController();
            controller.setUser(currentUser);
            
            Stage stage = new Stage();
            stage.setTitle("Create New Raffle");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the table after creating
            loadRaffles();
            
        } catch (IOException e) {
            AlertUtils.showError("Error", "Could not open create raffle window: " + e.getMessage());
        }
    }

    private void handleEditRaffle(Raffle raffle) {
        // Check if raffle has a creator - still create one if needed
        if (raffle.getCreator() == null) {
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageRaffle.fxml"));
            Parent root = loader.load();
            
            ManageRaffleController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setCurrentUser(currentUser);
            
            Stage stage = new Stage();
            stage.setTitle("Edit Raffle");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Refresh the table after editing
            loadRaffles();
            
        } catch (IOException e) {
            AlertUtils.showError("Error", "Could not open edit raffle window: " + e.getMessage());
        }
    }

    private void handleDeleteRaffle(Raffle raffle) {
        // Check if raffle has a creator - still create one if needed
        if (raffle.getCreator() == null) {
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Raffle");
        alert.setHeaderText("Delete Raffle");
        alert.setContentText("Are you sure you want to delete this raffle?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                raffleService.deleteRaffle(raffle.getId());
                loadRaffles(); // Refresh the table
                AlertUtils.showInformation("Success", "Raffle deleted successfully");
            } catch (Exception e) {
                AlertUtils.showError("Error", "Could not delete raffle: " + e.getMessage());
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}