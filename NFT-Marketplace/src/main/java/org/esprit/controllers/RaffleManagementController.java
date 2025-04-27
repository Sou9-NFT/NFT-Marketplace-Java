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
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.Map;

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
    @FXML private Button statisticsButton;

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
        
        // Setup statistics button
        if (statisticsButton != null) {
            statisticsButton.setOnAction(event -> showStatistics());
        }
    }

    private void showStatistics() {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Raffle Statistics");
            dialog.setHeaderText(null);

            VBox content = new VBox(25);  // Increased spacing between sections
            content.setPadding(new Insets(25));
            content.setPrefWidth(600);    // Increased dialog width
            content.getStyleClass().add("statistics-dialog");

            // Winners section
            VBox winnersSection = new VBox(15);  // Increased internal spacing
            winnersSection.getStyleClass().add("statistics-section");
            
            Label winnersTitle = new Label("Top Winners");
            winnersTitle.getStyleClass().add("statistics-title");
            
            FlowPane winnerStats = new FlowPane(20, 20);  // Increased spacing between items
            winnerStats.setAlignment(Pos.CENTER);
            List<Map<String, Object>> winnerStatistics = raffleService.getWinnerStatistics();
            
            if (winnerStatistics.isEmpty()) {
                Label noWinnersLabel = new Label("No winners found");
                noWinnersLabel.getStyleClass().add("stat-label");
                winnerStats.getChildren().add(noWinnersLabel);
            } else {
                for (Map<String, Object> stat : winnerStatistics) {
                    VBox winnerBox = new VBox(5);
                    winnerBox.setAlignment(Pos.CENTER);
                    winnerBox.getStyleClass().add("statistics-item");
                    
                    ProgressIndicator progress = new ProgressIndicator();
                    progress.getStyleClass().add("stat-progress");
                    double winCount = ((Integer)stat.get("winCount")).doubleValue();
                    progress.setProgress(winCount / Math.max(winCount, 10.0));
                    
                    Label nameLabel = new Label(stat.get("userName").toString());
                    nameLabel.getStyleClass().add("winner-stat");
                    
                    Label countLabel = new Label(stat.get("winCount") + " wins");
                    countLabel.getStyleClass().add("stat-count");
                    
                    winnerBox.getChildren().addAll(progress, nameLabel, countLabel);
                    winnerStats.getChildren().add(winnerBox);
                }
            }

            // Time statistics section
            VBox timeSection = new VBox(10);
            timeSection.getStyleClass().add("statistics-section");
            
            Label timeTitle = new Label("Most Active Days");
            timeTitle.getStyleClass().add("statistics-title");
            
            FlowPane timeStats = new FlowPane(10, 10);
            timeStats.setAlignment(Pos.CENTER);
            List<Map<String, Object>> timeStatistics = raffleService.getCreationTimeStatistics();
            
            if (timeStatistics.isEmpty()) {
                Label noTimeStatsLabel = new Label("No time statistics available");
                noTimeStatsLabel.getStyleClass().add("stat-label");
                timeStats.getChildren().add(noTimeStatsLabel);
            } else {
                // Find max count for percentage calculation
                int maxCount = timeStatistics.stream()
                    .mapToInt(stat -> (Integer)stat.get("count"))
                    .max()
                    .orElse(1);
                
                for (Map<String, Object> stat : timeStatistics) {
                    VBox timeBox = new VBox(5);
                    timeBox.setAlignment(Pos.CENTER);
                    timeBox.getStyleClass().add("statistics-item");
                    
                    ProgressIndicator progress = new ProgressIndicator();
                    progress.getStyleClass().add("stat-progress");
                    double count = ((Integer)stat.get("count")).doubleValue();
                    progress.setProgress(count / maxCount);
                    
                    String dayName = (String)stat.get("dayName");
                    Label dayLabel = new Label(dayName);
                    dayLabel.getStyleClass().add("time-stat");
                    
                    Label countLabel = new Label(stat.get("count") + " raffles");
                    countLabel.getStyleClass().add("stat-count");
                    
                    timeBox.getChildren().addAll(progress, dayLabel, countLabel);
                    timeStats.getChildren().add(timeBox);
                }
            }

            // Add sections to content
            winnersSection.getChildren().addAll(winnersTitle, winnerStats);
            timeSection.getChildren().addAll(timeTitle, timeStats);
            content.getChildren().addAll(winnersSection, timeSection);

            // Add close button
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/raffle-management.css").toExternalForm());
            dialog.showAndWait();

        } catch (SQLException e) {
            AlertUtils.showError("Error", "Could not load statistics: " + e.getMessage());
        }
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
        
        // Update winner status column logic
        winnerStatusColumn.setCellValueFactory(cellData -> {
            Participant participant = cellData.getValue();
            Raffle raffle = participant.getRaffle();
            String status;
            
            if (raffle.getStatus().equals("active")) {
                status = "Pending";
            } else if (raffle.getStatus().equals("ended")) {
                if (raffle.getWinnerId() != null && raffle.getWinnerId().equals(participant.getUser().getId())) {
                    status = "Winner";
                } else if (raffle.getWinnerId() != null) {
                    status = "Not Winner";
                } else {
                    status = "No Winner";
                }
            } else {
                status = "Cancelled";
            }
            return new SimpleStringProperty(status);
        });

        // Style the winner status cells
        winnerStatusColumn.setCellFactory(column -> new TableCell<Participant, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-winner", "status-pending", "status-not-winner", "status-no-winner", "status-cancelled");
                } else {
                    setText(status);
                    getStyleClass().removeAll("status-winner", "status-pending", "status-not-winner", "status-no-winner", "status-cancelled");
                    
                    switch (status) {
                        case "Winner":
                            getStyleClass().add("status-winner");
                            setStyle("-fx-text-fill: #2E7D32;"); // Green color for winners
                            break;
                        case "Pending":
                            getStyleClass().add("status-pending");
                            setStyle("-fx-text-fill: #1976D2;"); // Blue color for pending
                            break;
                        case "Not Winner":
                            getStyleClass().add("status-not-winner");
                            setStyle("-fx-text-fill: #D32F2F;"); // Red color for non-winners
                            break;
                        case "No Winner":
                            getStyleClass().add("status-no-winner");
                            setStyle("-fx-text-fill: #757575;"); // Gray color for no winners
                            break;
                        case "Cancelled":
                            getStyleClass().add("status-cancelled");
                            setStyle("-fx-text-fill: #616161;"); // Dark gray for cancelled
                            break;
                    }
                }
            }
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
            "All", "active", "ended"
        ));
        statusFilter.setValue("All");
        
        // Add listeners for filter changes
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (rafflesToggle.isSelected()) {
                filterRaffles();
            } else {
                filterParticipants();
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (rafflesToggle.isSelected()) {
                filterRaffles();
            } else {
                filterParticipants();
            }
        });
    }

    public void loadRaffles() {
        try {
            List<Raffle> raffles = raffleService.getAllRaffles();
            
            // Ensure each raffle has a creator object
            for (Raffle raffle : raffles) {
                if (raffle.getCreator() == null) {
                    raffle.setLoadingFromDatabase(true);
                    User tempCreator = new User();
                    tempCreator.setId(currentUser != null ? currentUser.getId() : 0);
                    tempCreator.setName("Unknown Creator");
                    raffle.setCreator(tempCreator);
                    raffle.setLoadingFromDatabase(false);
                }
            }
            
            raffleList = FXCollections.observableArrayList(raffles);
            raffleTable.setItems(raffleList);
        } catch (Exception e) {
            System.err.println("Error loading raffles: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Error", "Could not load raffles: " + e.getMessage());
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
            controller.setParentController(this); // Set this controller as parent
            
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