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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleGroup;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.models.Participant;
import org.esprit.services.RaffleService;
import org.esprit.services.ParticipantService;
import org.esprit.utils.AlertUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;

public class RaffleManagementController {
    // Existing raffle table fields
    @FXML private TableView<Raffle> raffleTable;
    @FXML private TableColumn<Raffle, Integer> idColumn;
    @FXML private TableColumn<Raffle, String> titleColumn;
    @FXML private TableColumn<Raffle, String> artworkColumn;
    @FXML private TableColumn<Raffle, String> startDateColumn;
    @FXML private TableColumn<Raffle, String> endDateColumn;
    @FXML private TableColumn<Raffle, String> statusColumn;
    @FXML private TableColumn<Raffle, Void> actionsColumn;
    
    // New participant table fields
    @FXML private TableView<Participant> participantTable;
    @FXML private TableColumn<Participant, Integer> participantIdColumn;
    @FXML private TableColumn<Participant, String> participantNameColumn;
    @FXML private TableColumn<Participant, String> raffleNameColumn;
    @FXML private TableColumn<Participant, Date> participationDateColumn;
    @FXML private TableColumn<Participant, String> winnerStatusColumn;
    @FXML private TableColumn<Participant, Void> participantActionsColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ToggleButton rafflesToggle;
    @FXML private ToggleButton participantsToggle;
    
    private RaffleService raffleService;
    private ParticipantService participantService;
    private User currentUser;
    private ObservableList<Raffle> raffleList;
    private ObservableList<Participant> participantList;

    public void initialize() {
        raffleService = new RaffleService();
        participantService = new ParticipantService();
        
        setupToggles();
        setupTable();
        setupParticipantTable();
        setupFilters();
        loadRaffles();
        loadParticipants();
    }

    private void setupToggles() {
        // Create toggle group
        ToggleGroup viewToggle = new ToggleGroup();
        rafflesToggle.setToggleGroup(viewToggle);
        participantsToggle.setToggleGroup(viewToggle);

        // Handle toggle changes
        viewToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rafflesToggle) {
                raffleTable.setVisible(true);
                raffleTable.setManaged(true);
                participantTable.setVisible(false);
                participantTable.setManaged(false);
                loadRaffles();
            } else {
                raffleTable.setVisible(false);
                raffleTable.setManaged(false);
                participantTable.setVisible(true);
                participantTable.setManaged(true);
                loadParticipants();
            }
        });
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        artworkColumn.setCellValueFactory(new PropertyValueFactory<>("artworkTitle"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Apply custom styling to status column
        statusColumn.setCellFactory(column -> new TableCell<Raffle, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-active", "status-upcoming", "status-completed", "status-cancelled");
                } else {
                    setText(item);
                    getStyleClass().removeAll("status-active", "status-upcoming", "status-completed", "status-cancelled");
                    switch (item.toLowerCase()) {
                        case "active":
                            getStyleClass().add("status-active");
                            break;
                        case "upcoming":
                            getStyleClass().add("status-upcoming");
                            break;
                        case "completed":
                            getStyleClass().add("status-completed");
                            break;
                        case "cancelled":
                            getStyleClass().add("status-cancelled");
                            break;
                    }
                }
            }
        });
        
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

    private void setupParticipantTable() {
        participantIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        participantNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getUser().getName()));
        raffleNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRaffle().getTitle()));
            
        // Format the date for better readability
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        participationDateColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(Date.from(cellData.getValue().getJoinedAt().atZone(ZoneId.systemDefault()).toInstant())));
        participationDateColumn.setCellFactory(column -> new TableCell<Participant, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormat.format(item));
                }
            }
        });
        
        // Apply custom styling to winner status column
        winnerStatusColumn.setCellFactory(column -> new TableCell<Participant, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-winner", "pending-status", "not-winner-status");
                } else {
                    Participant participant = getTableView().getItems().get(getIndex());
                    Raffle raffle = participant.getRaffle();
                    
                    String winnerStatus = "Pending";
                    
                    if (raffle.getStatus().equals("ended") && raffle.getWinnerId() != null) {
                        if (raffle.getWinnerId().equals(participant.getUser().getId())) {
                            winnerStatus = "Winner";
                            getStyleClass().removeAll("pending-status", "not-winner-status");
                            getStyleClass().add("status-winner");
                        } else {
                            winnerStatus = "Not Winner";
                            getStyleClass().removeAll("status-winner", "pending-status");
                            getStyleClass().add("not-winner-status");
                        }
                    } else {
                        getStyleClass().removeAll("status-winner", "not-winner-status");
                        getStyleClass().add("pending-status");
                    }
                    
                    setText(winnerStatus);
                }
            }
        });
        
        winnerStatusColumn.setCellValueFactory(cellData -> {
            Participant participant = cellData.getValue();
            Raffle raffle = participant.getRaffle();
            if (raffle.getStatus().equals("ended") && raffle.getWinnerId() != null) {
                return new SimpleStringProperty(raffle.getWinnerId().equals(participant.getUser().getId()) ? 
                    "Winner" : "Not Winner");
            }
            return new SimpleStringProperty("Pending");
        });
        
        setupParticipantActions();
    }

    private void setupParticipantActions() {
        participantActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final HBox buttons = new HBox(5, viewButton);
            
            {
                viewButton.setOnAction(event -> {
                    Participant participant = getTableRow().getItem();
                    if (participant != null) {
                        showRaffleDetails(participant.getRaffle());
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
        try {
            List<Raffle> raffles = raffleService.getAllRaffles();
            
            // Ensure each raffle has a creator object
            for (Raffle raffle : raffles) {
                // If the raffle has no creator, set the current user as creator
                // This is a temporary fix to prevent NullPointerException
                if (raffle.getCreator() == null) {
                    // Enable loading mode to prevent validation during this temporary fix
                    raffle.setLoadingFromDatabase(true);
                    
                    User tempCreator = new User();
                    tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
                    tempCreator.setName("Unknown Creator");
                    raffle.setCreator(tempCreator);
                    
                    // Disable loading mode after setting the property
                    raffle.setLoadingFromDatabase(false);
                }
            }
            
            raffleList = FXCollections.observableArrayList(raffles);
            raffleTable.setItems(raffleList);
        } catch (Exception e) {
            System.err.println("Error loading raffles: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not load raffles: " + e.getMessage());
            // Create empty list if there was an error to avoid NPE
            raffleList = FXCollections.observableArrayList();
            raffleTable.setItems(raffleList);
        }
    }

    private void loadParticipants() {
        try {
            List<Participant> participants = participantService.getAll();
            participantList = FXCollections.observableArrayList(participants);
            filterParticipants();
        } catch (Exception e) {
            AlertUtils.showError("Error", "Could not load participants: " + e.getMessage());
        }
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

    private void filterParticipants() {
        String searchText = searchField.getText().toLowerCase();
        String status = statusFilter.getValue();
        
        ObservableList<Participant> filteredList = participantList.filtered(participant -> 
            (searchText.isEmpty() || 
             participant.getUser().getName().toLowerCase().contains(searchText) ||
             participant.getRaffle().getTitle().toLowerCase().contains(searchText)) &&
            (status.equals("All") || participant.getRaffle().getStatus().equals(status))
        );
        
        participantTable.setItems(filteredList);
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
            // Set loading flag to prevent validation during this temporary fix
            raffle.setLoadingFromDatabase(true);
            
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
            
            // Reset loading flag
            raffle.setLoadingFromDatabase(false);
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
            // Set loading flag to prevent validation during this temporary fix
            raffle.setLoadingFromDatabase(true);
            
            // Create a temporary creator for this raffle for data consistency
            User tempCreator = new User();
            tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
            tempCreator.setName("Unknown Creator");
            raffle.setCreator(tempCreator);
            
            // Reset loading flag
            raffle.setLoadingFromDatabase(false);
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

    private void showRaffleDetails(Raffle raffle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleDetails.fxml"));
            Parent detailsView = loader.load();
            
            RaffleDetailsController controller = loader.getController();
            controller.setRaffle(raffle);
            controller.setCurrentUser(currentUser);
            
            Stage stage = new Stage();
            Scene scene = new Scene(detailsView);
            stage.setScene(scene);
            stage.setTitle("Raffle Details: " + raffle.getTitle());
            stage.setMinWidth(600);
            stage.setMinHeight(650);
            stage.show();
        } catch (IOException e) {
            AlertUtils.showError("Error", "Could not open raffle details: " + e.getMessage());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}