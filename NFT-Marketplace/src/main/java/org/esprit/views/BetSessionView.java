package org.esprit.views;

import java.time.LocalDateTime;

import org.esprit.models.Artwork;
import org.esprit.models.BetSession;
import org.esprit.models.User;
import org.esprit.services.BetSessionService;
import org.esprit.services.UserService;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class BetSessionView {
    private final BorderPane view;
    private final TableView<BetSession> tableView;
    private final BetSessionService betSessionService;
    private final UserService userService;

    public BetSessionView() {
        this.view = new BorderPane();
        this.tableView = new TableView<>();
        this.betSessionService = new BetSessionService();
        this.userService = new UserService();

        initializeUI();
    }
    
    private void initializeUI() {
        // TableView setup
        TableColumn<BetSession, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        
        // Author column - Display author username
        TableColumn<BetSession, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(cellData -> {
            User author = cellData.getValue().getAuthor();
            return author != null ? 
                   new javafx.beans.property.SimpleStringProperty(author.getName()) : 
                   new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Artwork column - Display artwork title
        TableColumn<BetSession, String> artworkColumn = new TableColumn<>("Artwork");
        artworkColumn.setCellValueFactory(cellData -> {
            Artwork artwork = cellData.getValue().getArtwork();
            return artwork != null ? 
                   new javafx.beans.property.SimpleStringProperty(artwork.getTitle()) : 
                   new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        // Created At column
        TableColumn<BetSession, LocalDateTime> createdAtColumn = new TableColumn<>("Created At");
        createdAtColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());
        
        // Start Time column
        TableColumn<BetSession, LocalDateTime> startTimeColumn = new TableColumn<>("Start Time");
        startTimeColumn.setCellValueFactory(cellData -> cellData.getValue().startTimeProperty());
        
        // End Time column
        TableColumn<BetSession, LocalDateTime> endTimeColumn = new TableColumn<>("End Time");
        endTimeColumn.setCellValueFactory(cellData -> cellData.getValue().endTimeProperty());
        
        // Initial Price column
        TableColumn<BetSession, Number> initialPriceColumn = new TableColumn<>("Initial Price");
        initialPriceColumn.setCellValueFactory(cellData -> cellData.getValue().initialPriceProperty());
        
        // Current Price column
        TableColumn<BetSession, Number> currentPriceColumn = new TableColumn<>("Current Price");
        currentPriceColumn.setCellValueFactory(cellData -> cellData.getValue().currentPriceProperty());
        
        // Status column
        TableColumn<BetSession, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        tableView.getColumns().addAll(
            idColumn, 
            authorColumn, 
            artworkColumn, 
            createdAtColumn, 
            startTimeColumn, 
            endTimeColumn, 
            initialPriceColumn, 
            currentPriceColumn, 
            statusColumn
        );

        // Buttons
        Button addButton = new Button("Add");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");

        addButton.setOnAction(e -> showAddDialog());
        updateButton.setOnAction(e -> showUpdateDialog());
        deleteButton.setOnAction(e -> deleteSelectedBetSession());

        HBox buttonBox = new HBox(10, addButton, updateButton, deleteButton);
        buttonBox.setPadding(new Insets(10));

        // Layout
        view.setCenter(tableView);
        view.setBottom(buttonBox);

        loadBetSessions();
    }

    private void loadBetSessions() {
        tableView.getItems().setAll(betSessionService.getAllBetSessions());
    }    private void showAddDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Add New Bet Session");
        
        // Create form components - using text fields for IDs instead of ComboBoxes
        javafx.scene.control.TextField authorIdField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField artworkIdField = new javafx.scene.control.TextField();
        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
        javafx.scene.control.TextField initialPriceField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField currentPriceField = new javafx.scene.control.TextField();
        javafx.scene.control.ComboBox<String> statusComboBox = new javafx.scene.control.ComboBox<>();
        
        // Set up status options
        statusComboBox.getItems().addAll("pending", "active", "completed", "cancelled");
        statusComboBox.setValue("pending");
        
        // Layout
        javafx.scene.layout.GridPane formLayout = new javafx.scene.layout.GridPane();
        formLayout.setPadding(new javafx.geometry.Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new javafx.scene.control.Label("Author ID:"), 0, 0);
        formLayout.add(authorIdField, 1, 0);
        formLayout.add(new javafx.scene.control.Label("Artwork ID:"), 0, 1);
        formLayout.add(artworkIdField, 1, 1);
        formLayout.add(new javafx.scene.control.Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new javafx.scene.control.Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new javafx.scene.control.Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new javafx.scene.control.Label("Current Price:"), 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        formLayout.add(new javafx.scene.control.Label("Status:"), 0, 6);
        formLayout.add(statusComboBox, 1, 6);
        
        // Buttons
        javafx.scene.control.Button saveButton = new javafx.scene.control.Button("Save");
        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("Cancel");
        
        javafx.scene.layout.HBox buttonLayout = new javafx.scene.layout.HBox(10, saveButton, cancelButton);
        buttonLayout.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new javafx.geometry.Insets(10));
        
        saveButton.setOnAction(e -> {
            try {
                // Create new BetSession from form data
                BetSession newBetSession = new BetSession();
                
                // Set Author and Artwork from ID fields
                try {
                    int authorId = Integer.parseInt(authorIdField.getText());
                    User author = new User();
                    author.setId(authorId);
                    newBetSession.setAuthor(author);
                    
                    int artworkId = Integer.parseInt(artworkIdField.getText());
                    Artwork artwork = new Artwork();
                    artwork.setId(artworkId);
                    newBetSession.setArtwork(artwork);
                } catch (NumberFormatException ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR, 
                            "Please enter valid numeric IDs for Author and Artwork.");
                    alert.showAndWait();
                    return;
                }
                
                // Get time values - set to noon by default
                java.time.LocalTime defaultTime = java.time.LocalTime.of(12, 0);
                
                if (startDatePicker.getValue() != null) {
                    newBetSession.setStartTime(startDatePicker.getValue().atTime(defaultTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    newBetSession.setEndTime(endDatePicker.getValue().atTime(defaultTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    newBetSession.setInitialPrice(initialPrice);
                    
                    // If current price is empty, use initial price
                    double currentPrice = currentPriceField.getText().isEmpty() ? 
                                         initialPrice : 
                                         Double.parseDouble(currentPriceField.getText());
                    newBetSession.setCurrentPrice(currentPrice);
                } catch (NumberFormatException ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR, 
                            "Please enter valid prices.");
                    alert.showAndWait();
                    return;
                }
                
                newBetSession.setStatus(statusComboBox.getValue());
                
                // Save to database
                betSessionService.addBetSession(newBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR, 
                        "Error saving bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, formLayout, buttonLayout);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }    private void showUpdateDialog() {
        BetSession selectedBetSession = tableView.getSelectionModel().getSelectedItem();
        if (selectedBetSession == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING, 
                    "Please select a bet session to update.");
            alert.showAndWait();
            return;
        }
        
        // Check if bet session is in 'pending' status
        if (!"pending".equals(selectedBetSession.getStatus())) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Only pending bet sessions can be updated.");
            alert.setTitle("Cannot Update");
            alert.setHeaderText("Bet Session Not Pending");
            alert.showAndWait();
            return;
        }
        
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Update Bet Session");
        
        // Create form components - only for editable fields
        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker();
        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker();
        javafx.scene.control.TextField initialPriceField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField currentPriceField = new javafx.scene.control.TextField();
        javafx.scene.control.ComboBox<String> statusComboBox = new javafx.scene.control.ComboBox<>();
        
        // Display author and artwork information (read-only)
        String authorInfo = selectedBetSession.getAuthor() != null ? 
                           "ID: " + selectedBetSession.getAuthor().getId() + 
                           (selectedBetSession.getAuthor().getName() != null ? 
                           ", Name: " + selectedBetSession.getAuthor().getName() : "") : 
                           "N/A";
                           
        String artworkInfo = selectedBetSession.getArtwork() != null ? 
                            "ID: " + selectedBetSession.getArtwork().getId() + 
                            (selectedBetSession.getArtwork().getTitle() != null ? 
                            ", Title: " + selectedBetSession.getArtwork().getTitle() : "") : 
                            "N/A";
        
        javafx.scene.control.Label authorLabel = new javafx.scene.control.Label(authorInfo);
        javafx.scene.control.Label artworkLabel = new javafx.scene.control.Label(artworkInfo);
        
        // Status options - restrict to relevant statuses
        statusComboBox.getItems().addAll("pending", "active");
        
        // Set values from selected bet session
        if (selectedBetSession.getStartTime() != null) {
            startDatePicker.setValue(selectedBetSession.getStartTime().toLocalDate());
        }
        
        if (selectedBetSession.getEndTime() != null) {
            endDatePicker.setValue(selectedBetSession.getEndTime().toLocalDate());
        }
        
        initialPriceField.setText(String.valueOf(selectedBetSession.getInitialPrice()));
        currentPriceField.setText(String.valueOf(selectedBetSession.getCurrentPrice()));
        statusComboBox.setValue(selectedBetSession.getStatus());
        
        // Layout
        javafx.scene.layout.GridPane formLayout = new javafx.scene.layout.GridPane();
        formLayout.setPadding(new javafx.geometry.Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);
        
        formLayout.add(new javafx.scene.control.Label("Author (read-only):"), 0, 0);
        formLayout.add(authorLabel, 1, 0);
        formLayout.add(new javafx.scene.control.Label("Artwork (read-only):"), 0, 1);
        formLayout.add(artworkLabel, 1, 1);
        formLayout.add(new javafx.scene.control.Label("Start Date:"), 0, 2);
        formLayout.add(startDatePicker, 1, 2);
        formLayout.add(new javafx.scene.control.Label("End Date:"), 0, 3);
        formLayout.add(endDatePicker, 1, 3);
        formLayout.add(new javafx.scene.control.Label("Initial Price:"), 0, 4);
        formLayout.add(initialPriceField, 1, 4);
        formLayout.add(new javafx.scene.control.Label("Current Price:"), 0, 5);
        formLayout.add(currentPriceField, 1, 5);
        formLayout.add(new javafx.scene.control.Label("Status:"), 0, 6);
        formLayout.add(statusComboBox, 1, 6);
        
        // Buttons
        javafx.scene.control.Button updateButton = new javafx.scene.control.Button("Update");
        javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("Cancel");
        
        javafx.scene.layout.HBox buttonLayout = new javafx.scene.layout.HBox(10, updateButton, cancelButton);
        buttonLayout.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonLayout.setPadding(new javafx.geometry.Insets(10));
        
        updateButton.setOnAction(e -> {
            try {
                // Author and Artwork remain unchanged
                // Only update time, price and status
                
                // Get time values - preserve existing time if available, otherwise use noon
                java.time.LocalTime defaultTime = java.time.LocalTime.of(12, 0);
                java.time.LocalTime existingStartTime = selectedBetSession.getStartTime() != null ? 
                                                      selectedBetSession.getStartTime().toLocalTime() : 
                                                      defaultTime;
                java.time.LocalTime existingEndTime = selectedBetSession.getEndTime() != null ? 
                                                    selectedBetSession.getEndTime().toLocalTime() : 
                                                    defaultTime;
                
                if (startDatePicker.getValue() != null) {
                    selectedBetSession.setStartTime(startDatePicker.getValue().atTime(existingStartTime));
                }
                
                if (endDatePicker.getValue() != null) {
                    selectedBetSession.setEndTime(endDatePicker.getValue().atTime(existingEndTime));
                }
                
                // Parse price values
                try {
                    double initialPrice = Double.parseDouble(initialPriceField.getText());
                    selectedBetSession.setInitialPrice(initialPrice);
                    
                    double currentPrice = Double.parseDouble(currentPriceField.getText());
                    selectedBetSession.setCurrentPrice(currentPrice);
                } catch (NumberFormatException ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR, 
                            "Please enter valid prices.");
                    alert.showAndWait();
                    return;
                }
                
                selectedBetSession.setStatus(statusComboBox.getValue());
                
                // Save to database
                betSessionService.updateBetSession(selectedBetSession);
                
                // Refresh table
                loadBetSessions();
                
                // Close dialog
                dialog.close();
                
            } catch (Exception ex) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR, 
                        "Error updating bet session: " + ex.getMessage());
                alert.showAndWait();
            }
        });
        
        cancelButton.setOnAction(e -> dialog.close());
        
        // Create scene
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, formLayout, buttonLayout);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }private void deleteSelectedBetSession() {
        BetSession selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Create confirmation dialog
            javafx.scene.control.Alert confirmDialog = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete this bet session?",
                    javafx.scene.control.ButtonType.YES,
                    javafx.scene.control.ButtonType.NO
            );
            confirmDialog.setTitle("Confirm Deletion");
            confirmDialog.setHeaderText("Delete Bet Session #" + selected.getId());
            
            // Show dialog and wait for response
            javafx.scene.control.ButtonType result = confirmDialog.showAndWait().orElse(javafx.scene.control.ButtonType.NO);
            
            // If user confirms, delete the bet session
            if (result == javafx.scene.control.ButtonType.YES) {
                betSessionService.deleteBetSession(selected.getId());
                loadBetSessions();
            }
        } else {
            // No bet session selected, show warning
            javafx.scene.control.Alert warning = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Please select a bet session to delete."
            );
            warning.setTitle("No Selection");
            warning.setHeaderText("No Bet Session Selected");
            warning.showAndWait();
        }
    }    public BorderPane getView() {
        return view;
    }
    
}
