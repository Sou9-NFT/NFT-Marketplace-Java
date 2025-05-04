package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.beans.property.SimpleStringProperty;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.services.TradeDisputeService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.Scene;

public class TradeDisputeListController {
    @FXML
    private TableView<TradeDispute> disputeTable;
    
    @FXML
    private TableColumn<TradeDispute, String> reporterColumn;
    
    @FXML
    private TableColumn<TradeDispute, Integer> tradeIdColumn;
    
    @FXML
    private TableColumn<TradeDispute, String> offeredItemColumn;
    
    @FXML
    private TableColumn<TradeDispute, String> receivedItemColumn;
    
    @FXML
    private TableColumn<TradeDispute, String> reasonColumn;
    
    @FXML
    private TableColumn<TradeDispute, String> statusColumn;
    
    @FXML
    private TableColumn<TradeDispute, String> timestampColumn;
    
    @FXML
    private TableColumn<TradeDispute, Void> actionsColumn;
    
    @FXML
    private Button statsButton;
    
    private User currentUser;
    private final TradeDisputeService disputeService = new TradeDisputeService();
    private final ObservableList<TradeDispute> disputes = FXCollections.observableArrayList();
    
    public void initialize() {
        // Configure column widths and alignment
        tradeIdColumn.setPrefWidth(70);
        reporterColumn.setPrefWidth(120);
        offeredItemColumn.setPrefWidth(150);
        receivedItemColumn.setPrefWidth(150);
        reasonColumn.setPrefWidth(200);
        statusColumn.setPrefWidth(100);
        timestampColumn.setPrefWidth(150);
        actionsColumn.setPrefWidth(250);  // Make actions column wider to fit all buttons
        
        // Center align specific columns
        tradeIdColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setStyle("-fx-alignment: CENTER;");
        actionsColumn.setStyle("-fx-alignment: CENTER;");
        timestampColumn.setStyle("-fx-alignment: CENTER;");
        
        // Make reason column wrap text
        reasonColumn.setCellFactory(tc -> {
            TableCell<TradeDispute, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(reasonColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });

        setupTableColumns();
        
        // Columns will be shown/hidden based on user role in setUser method
        // We'll call loadDisputes() after setting the user
    }
    
    private void setupTableColumns() {
        // Set up reporter column to show user name
        reporterColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getReporterName())
        );
        
        tradeIdColumn.setCellValueFactory(new PropertyValueFactory<>("tradeId"));
        
        // Set up offered item column to show artwork title
        offeredItemColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getOfferedItemTitle())
        );
        
        // Set up received item column to show artwork title
        receivedItemColumn.setCellValueFactory(cellData -> {
            String title = cellData.getValue().getReceivedItemTitle();
            return new SimpleStringProperty(title != null ? title : "N/A");
        });
        
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return new SimpleStringProperty(
                cellData.getValue().getTimestamp().format(formatter)
            );
        });
        
        // Setup actions column
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            
            {
                editButton.setOnAction(event -> {
                    TradeDispute dispute = getTableView().getItems().get(getIndex());
                    handleEditDispute(dispute);
                });
                
                deleteButton.setOnAction(event -> {
                    TradeDispute dispute = getTableView().getItems().get(getIndex());
                    handleDeleteDispute(dispute);
                });
                
                // Style the buttons
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    public void loadDisputes() {
        System.out.println("Starting to load disputes");
        disputes.clear();
        
        List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        boolean isAdmin = currentUser.getRoles().containsAll(adminRoles) && currentUser.getRoles().size() == adminRoles.size();
        
        List<TradeDispute> loadedDisputes = disputeService.getAllDisputes();
        System.out.println("Loaded " + loadedDisputes.size() + " disputes from service");

        if (!isAdmin) {
            // Filter disputes to only show ones where current user is the reporter
            loadedDisputes.removeIf(dispute -> 
                currentUser == null || 
                dispute.getReporter() != currentUser.getId()
            );
            System.out.println("Filtered to " + loadedDisputes.size() + " disputes for user");
        }

        disputes.addAll(loadedDisputes);
        System.out.println("Added disputes to observable list. Size: " + disputes.size());
        disputeTable.setItems(disputes);
        System.out.println("Set items in table. Table items size: " + disputeTable.getItems().size());
    }
    
    private void handleViewDispute(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViewTradeDispute.fxml"));
            Parent viewDisputeView = loader.load();
            
            ViewTradeDisputeController controller = loader.getController();
            controller.setDispute(dispute);
            controller.setUser(currentUser);
            
            disputeTable.getScene().setRoot(viewDisputeView);
        } catch (IOException e) {
            showAlert("Error", "Could not load dispute details: " + e.getMessage());
        }
    }
    
    private void handleResolveDispute(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResolveTradeDispute.fxml"));
            Parent resolveDisputeView = loader.load();
            
            ResolveTradeDisputeController controller = loader.getController();
            controller.setDispute(dispute);
            controller.setUser(currentUser);
            
            disputeTable.getScene().setRoot(resolveDisputeView);
        } catch (IOException e) {
            showAlert("Error", "Could not load resolve dispute form: " + e.getMessage());
        }
    }
    
    private void handleDeleteDispute(TradeDispute dispute) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Trade Dispute");
        confirmDialog.setContentText("Are you sure you want to delete this trade dispute? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (disputeService.deleteDispute(dispute.getId())) {
                    showAlert("Success", "Trade dispute deleted successfully");
                    loadDisputes(); // Refresh the table
                } else {
                    showAlert("Error", "Failed to delete trade dispute");
                }
            }
        });
    }
    
    private void handleEditDispute(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditTradeDispute.fxml"));
            Parent editDisputeView = loader.load();
            
            EditTradeDisputeController controller = loader.getController();
            controller.setDispute(dispute);
            controller.setUser(currentUser);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(editDisputeView));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Edit Trade Dispute");
            stage.showAndWait();
            
            // Refresh the table after editing
            loadDisputes();
        } catch (IOException e) {
            showAlert("Error", "Could not load edit dispute form: " + e.getMessage());
        }
    }
    
    private void handleEditStatus(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditTradeDisputeStatus.fxml"));
            Parent editStatusView = loader.load();
            
            EditTradeDisputeStatusController controller = loader.getController();
            controller.setDispute(dispute);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(editStatusView));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Edit Status");
            
            // Add a listener to the stage that will refresh the table when the window closes
            stage.setOnHidden(e -> loadDisputes());
            
            stage.showAndWait();
            
        } catch (IOException e) {
            showAlert("Error", "Could not load status edit form: " + e.getMessage());
        }
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        
        // Check if user has admin role
        List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        boolean isAdmin = user.getRoles().containsAll(adminRoles) && user.getRoles().size() == adminRoles.size();
        
        // Show or hide elements based on admin status
        tradeIdColumn.setVisible(isAdmin);
        reporterColumn.setVisible(isAdmin);
        statsButton.setVisible(isAdmin);
        
        // Load disputes after setting columns visibility
        loadDisputes();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showTradeStatistics() {
        List<TradeDispute> allDisputes = disputeService.getAllDisputes();
        
        // Calculate statistics
        int totalTrades = allDisputes.size();
        long acceptedTrades = allDisputes.stream().filter(d -> "accepted".equalsIgnoreCase(d.getStatus())).count();
        long rejectedTrades = allDisputes.stream().filter(d -> "rejected".equalsIgnoreCase(d.getStatus())).count();
        long pendingTrades = allDisputes.stream().filter(d -> "pending".equalsIgnoreCase(d.getStatus())).count();

        // Create statistics message
        StringBuilder stats = new StringBuilder();
        stats.append("Trade Statistics:\n\n");
        stats.append("Total Trades: ").append(totalTrades).append("\n\n");
        stats.append("Accepted Trades: ").append(acceptedTrades).append("\n");
        stats.append("Rejected Trades: ").append(rejectedTrades).append("\n");
        stats.append("Pending Trades: ").append(pendingTrades).append("\n\n");
        
        // Calculate and append percentages if there are any trades
        if (totalTrades > 0) {
            double acceptedPercent = (acceptedTrades * 100.0) / totalTrades;
            double rejectedPercent = (rejectedTrades * 100.0) / totalTrades;
            double pendingPercent = (pendingTrades * 100.0) / totalTrades;
            
            stats.append(String.format("Accepted: %.1f%%\n", acceptedPercent));
            stats.append(String.format("Rejected: %.1f%%\n", rejectedPercent));
            stats.append(String.format("Pending: %.1f%%", pendingPercent));
        }

        // Show statistics in a dialog
        Alert statsDialog = new Alert(Alert.AlertType.INFORMATION);
        statsDialog.setTitle("Trade Statistics");
        statsDialog.setHeaderText(null);
        statsDialog.setContentText(stats.toString());
        
        // Make the dialog resizable
        statsDialog.getDialogPane().setMinHeight(300);
        statsDialog.getDialogPane().setMinWidth(400);
        
        statsDialog.showAndWait();
    }
    
    @FXML
    private void handleShowStatistics() {
        showTradeStatistics();
    }
}