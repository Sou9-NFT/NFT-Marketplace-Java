package org.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.esprit.models.Artwork;
import org.esprit.models.TradeOffer;
import org.esprit.models.User;
import org.esprit.services.TradeOfferService;
import org.esprit.models.TradeDispute;
import org.esprit.services.TradeDisputeService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javafx.util.Pair;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.util.Base64;

public class TradeOfferListController {
    
    @FXML
    private TableView<TradeOffer> tradeTable;
    
    @FXML
    private TableColumn<TradeOffer, String> idColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> senderColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> receiverColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> offeredItemColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> requestedItemColumn;
    
    @FXML
    private TableColumn<TradeOffer, String> statusColumn;
    
    @FXML
    private TableColumn<TradeOffer, Void> actionColumn;
    
    @FXML
    private Button createTradeButton;
    
    private User currentUser;
    private TradeOfferService tradeService;
    private ObservableList<TradeOffer> tradeList;
    private byte[] selectedImageData;
    
    @FXML
    public void initialize() {
        System.out.println("Initializing TradeOfferListController...");
        try {
            tradeService = new TradeOfferService();
            tradeList = FXCollections.observableArrayList();

            // Initialize table columns
            idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
            
            senderColumn.setCellValueFactory(data -> {
                User sender = data.getValue().getSender();
                return new SimpleStringProperty(sender != null ? sender.getName() : "");
            });
            
            receiverColumn.setCellValueFactory(data -> {
                User receiver = data.getValue().getReceiverName();
                return new SimpleStringProperty(receiver != null ? receiver.getName() : "");
            });
            
            offeredItemColumn.setCellValueFactory(data -> {
                Artwork offered = data.getValue().getOfferedItem();
                return new SimpleStringProperty(offered != null ? offered.getTitle() : "");
            });
            
            requestedItemColumn.setCellValueFactory(data -> {
                Artwork requested = data.getValue().getReceivedItem();
                return new SimpleStringProperty(requested != null ? requested.getTitle() : "");
            });
            
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            // Set up action column with delete button
            actionColumn.setCellFactory(param -> new TableCell<>() {
                private final Button editButton = new Button("Edit");
                private final Button deleteButton = new Button("Delete");
                private final Button reportButton = new Button("Report");
                
                {
                    editButton.setOnAction(event -> {
                        TradeOffer trade = getTableView().getItems().get(getIndex());
                        handleEditTrade(trade);
                    });
                    
                    deleteButton.setOnAction(event -> {
                        TradeOffer trade = getTableView().getItems().get(getIndex());
                        handleDeleteTrade(trade);
                    });

                    reportButton.setOnAction(event -> {
                        TradeOffer trade = getTableView().getItems().get(getIndex());
                        handleReportTrade(trade);
                    });
                    
                    // Style the buttons
                    editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                    reportButton.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white;");
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5);
                        TradeOffer trade = getTableView().getItems().get(getIndex());
                        if ("pending".equals(trade.getStatus())) {
                            buttons.getChildren().addAll(editButton, deleteButton);
                        }
                        buttons.getChildren().add(reportButton);
                        setGraphic(buttons);
                    }
                }
            });

            tradeTable.setItems(tradeList);
            System.out.println("TradeOfferListController initialized successfully.");
        } catch (Exception e) {
            System.err.println("Error initializing TradeOfferListController: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setUser(User user) {
        System.out.println("Setting user in TradeOfferListController: " + (user != null ? user.getName() : "null"));
        this.currentUser = user;
        refreshTrades();
    }
    
    @FXML
    private void handleCreateTrade(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateTrade.fxml"));
            Parent createTradeView = loader.load();
            
            CreateTradeController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(createTradeView));
            stage.setTitle("Create New Trade Offer");
            stage.show();
            
        } catch (IOException e) {
            showError("Could not open create trade view: " + e.getMessage());
        }
    }
    
    public void refreshTrades() {
        try {
            tradeList.clear();
            // Only get trades for the current user
            List<TradeOffer> trades = tradeService.getUserTradeOffers(currentUser.getId());
            tradeList.addAll(trades);
            tradeTable.setItems(tradeList);
            System.out.println("Loaded " + trades.size() + " trades for user: " + currentUser.getName());
        } catch (SQLException e) {
            showAlert("Error", "Failed to load trades: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleEditTrade(TradeOffer tradeOffer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditTrade.fxml"));
            Parent editTradeView = loader.load();

            EditTradeController controller = loader.getController();
            controller.setTradeOffer(tradeOffer);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(editTradeView));
            stage.setTitle("Edit Trade Offer");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error opening edit window: " + e.getMessage());
        }
    }

    private void handleDeleteTrade(TradeOffer trade) {
        try {
            // Delete the trade offer
            tradeService.delete(trade);
            
            // Refresh the table
            refreshTrades();
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Trade offer deleted successfully");
            alert.showAndWait();
        } catch (Exception e) {
            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error deleting trade offer: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleReportTrade(TradeOffer trade) {
        try {
            // Create a new trade dispute with the trade offer details
            TradeDispute dispute = new TradeDispute();
            dispute.setTradeId(trade.getId());
            dispute.setOfferedItem(String.valueOf(trade.getOfferedItem().getId()));
            dispute.setReceivedItem(String.valueOf(trade.getReceivedItem().getId()));
            dispute.setReporter(currentUser.getId());
            dispute.setStatus("pending");
            
            // Show a dialog to get the reason and evidence
            Dialog<Pair<String, byte[]>> dialog = new Dialog<>();
            dialog.setTitle("Report Trade");
            dialog.setHeaderText("Please provide details about the issue");

            // Set the button types
            ButtonType reportButtonType = new ButtonType("Report", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(reportButtonType, ButtonType.CANCEL);

            // Create the reason and evidence fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextArea reasonField = new TextArea();
            reasonField.setPromptText("Reason for reporting");
            
            // Create image upload section
            VBox imageUploadBox = new VBox(10);
            Button uploadButton = new Button("Upload Evidence Image");
            ImageView imagePreview = new ImageView();
            imagePreview.setFitWidth(200);
            imagePreview.setFitHeight(200);
            imagePreview.setPreserveRatio(true);
            
            uploadButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Evidence Image");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
                );
                File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
                if (selectedFile != null) {
                    try {
                        // Read the image file
                        selectedImageData = Files.readAllBytes(selectedFile.toPath());
                        
                        // Display preview
                        Image image = new Image(selectedFile.toURI().toString());
                        imagePreview.setImage(image);
                    } catch (IOException ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Error loading image: " + ex.getMessage());
                        alert.showAndWait();
                    }
                }
            });
            
            imageUploadBox.getChildren().addAll(uploadButton, imagePreview);

            grid.add(new Label("Reason:"), 0, 0);
            grid.add(reasonField, 1, 0);
            grid.add(new Label("Evidence:"), 0, 1);
            grid.add(imageUploadBox, 1, 1);

            dialog.getDialogPane().setContent(grid);

            // Convert the result to a pair of strings
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == reportButtonType) {
                    return new Pair<>(reasonField.getText(), selectedImageData);
                }
                return null;
            });

            // Show the dialog and handle the result
            Optional<Pair<String, byte[]>> result = dialog.showAndWait();
            result.ifPresent(reasonAndEvidence -> {
                dispute.setReason(reasonAndEvidence.getKey());
                // Convert byte array to Base64 string
                if (reasonAndEvidence.getValue() != null) {
                    String base64Image = Base64.getEncoder().encodeToString(reasonAndEvidence.getValue());
                    dispute.setEvidence(base64Image);
                } else {
                    dispute.setEvidence("");
                }

                // Create the dispute in the database
                TradeDisputeService disputeService = new TradeDisputeService();
                if (disputeService.createDispute(dispute)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Trade reported successfully");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to report trade");
                    alert.showAndWait();
                }
            });
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error reporting trade: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}