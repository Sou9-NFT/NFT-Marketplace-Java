package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.esprit.models.TradeDispute;
import org.esprit.models.User;
import org.esprit.services.TradeDisputeService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminTradeDisputesController {
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
    
    private User currentUser;
    private final TradeDisputeService disputeService = new TradeDisputeService();
    private final ObservableList<TradeDispute> disputes = FXCollections.observableArrayList();    public void initialize() {
        // Configure column widths and alignment
        tradeIdColumn.setPrefWidth(70);
        tradeIdColumn.setVisible(false);  // Hide the trade ID column
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
        loadDisputes();
    }
    
    private void setupTableColumns() {
        reporterColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getReporterName())
        );
        
        tradeIdColumn.setCellValueFactory(new PropertyValueFactory<>("tradeId"));
        
        offeredItemColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getOfferedItemTitle())
        );
        
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
            private final HBox buttonsContainer = new HBox(5);
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            
            {
                viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                
                viewButton.setOnAction(event -> {
                    TradeDispute dispute = getTableView().getItems().get(getIndex());
                    handleViewDispute(dispute);
                });
                
                editButton.setOnAction(event -> {
                    TradeDispute dispute = getTableView().getItems().get(getIndex());
                    handleEditDispute(dispute);
                });
                
                deleteButton.setOnAction(event -> {
                    TradeDispute dispute = getTableView().getItems().get(getIndex());
                    handleDeleteDispute(dispute);
                });
                
                buttonsContainer.getChildren().addAll(viewButton, editButton, deleteButton);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsContainer);
                }
            }
        });
    }
    
    private void handleDeleteDispute(TradeDispute dispute) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Trade Dispute");
        confirmation.setContentText("Are you sure you want to delete this trade dispute? This action cannot be undone.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                disputeService.deleteDispute(dispute.getId());
                loadDisputes(); // Refresh the table
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setHeaderText(null);
                success.setContentText("Trade dispute deleted successfully.");
                success.showAndWait();
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("Failed to delete trade dispute");
                error.setContentText("An error occurred while deleting the trade dispute: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
    
    private void handleViewDispute(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ViewTradeDispute.fxml"));
            Parent root = loader.load();
            
            ViewTradeDisputeController controller = loader.getController();
            controller.setDispute(dispute);
            
            Stage stage = new Stage();
            stage.setTitle("Trade Dispute Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Failed to open dispute details");
            error.setContentText("An error occurred while opening the dispute details: " + e.getMessage());
            error.showAndWait();
        }
    }
    
    private void handleEditDispute(TradeDispute dispute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditTradeDisputeStatus.fxml"));
            Parent root = loader.load();
            
            EditTradeDisputeStatusController controller = loader.getController();
            controller.setDispute(dispute);
            
            Stage stage = new Stage();
            stage.setTitle("Edit Trade Dispute Status");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Failed to open edit window");
            error.setContentText("An error occurred while opening the edit window: " + e.getMessage());
            error.showAndWait();
        }
    }
    
    public void loadDisputes() {
        disputes.clear();
        List<TradeDispute> loadedDisputes = disputeService.getAllDisputes();
        disputes.addAll(loadedDisputes);
        disputeTable.setItems(disputes);
    }
    
    public void setUser(User user) {
        this.currentUser = user;
    }
} 