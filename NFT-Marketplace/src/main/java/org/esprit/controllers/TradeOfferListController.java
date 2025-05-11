package org.esprit.controllers;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
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
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.esprit.models.Artwork;
import org.esprit.models.TradeOffer;
import org.esprit.models.User;
import org.esprit.services.TradeOfferService;
import org.esprit.models.TradeDispute;
import org.esprit.services.TradeDisputeService;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javafx.util.Pair;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Chunk;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class TradeOfferListController {
    
    @FXML private TableView<TradeOffer> tradeTable;
    @FXML private TableColumn<TradeOffer, String> idColumn;
    @FXML private TableColumn<TradeOffer, String> senderColumn;
    @FXML private TableColumn<TradeOffer, String> receiverColumn;
    @FXML private TableColumn<TradeOffer, String> offeredItemColumn;
    @FXML private TableColumn<TradeOffer, String> requestedItemColumn;
    @FXML private TableColumn<TradeOffer, String> statusColumn;
    @FXML private TableColumn<TradeOffer, Void> actionColumn;
    @FXML private Button createTradeButton;
    @FXML private Button statsButton;
    @FXML private TextField searchField;
    
    private static final double ID_COL_WIDTH = 60;
    private static final double NAME_COL_WIDTH = 120;
    private static final double ITEM_COL_WIDTH = 150;
    private static final double STATUS_COL_WIDTH = 100;
    private static final double ACTION_COL_WIDTH = 300;
    
    private User currentUser;
    private TradeOfferService tradeService;
    private TradeDisputeService disputeService;
    private ObservableList<TradeOffer> tradeList;
    private byte[] selectedImageData;
    private ObservableList<TradeOffer> masterData;
    
    @FXML
    public void initialize() {
        System.out.println("Initializing TradeOfferListController...");
        try {
            tradeService = new TradeOfferService();
            disputeService = new TradeDisputeService();
            tradeList = FXCollections.observableArrayList();
            
            // Configure search field for instant filtering
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterTradeOffers(newValue);
            });
            
            // Configure column widths and alignment
            idColumn.setPrefWidth(ID_COL_WIDTH);
            senderColumn.setPrefWidth(NAME_COL_WIDTH);
            receiverColumn.setPrefWidth(NAME_COL_WIDTH);
            offeredItemColumn.setPrefWidth(ITEM_COL_WIDTH);
            requestedItemColumn.setPrefWidth(ITEM_COL_WIDTH);
            statusColumn.setPrefWidth(STATUS_COL_WIDTH);
            actionColumn.setPrefWidth(ACTION_COL_WIDTH);
            
            // Center align appropriate columns
            idColumn.setStyle("-fx-alignment: CENTER;");
            statusColumn.setStyle("-fx-alignment: CENTER;");
            actionColumn.setStyle("-fx-alignment: CENTER;");

            setupColumns();
            setupActionColumn();
            
            tradeTable.setItems(tradeList);
            System.out.println("TradeOfferListController initialized successfully.");
        } catch (Exception e) {
            System.err.println("Error initializing TradeOfferListController: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        
        senderColumn.setCellValueFactory(data -> {
            User sender = data.getValue().getSender();  // Show the sender's name
            return new SimpleStringProperty(sender != null ? sender.getName() : "");
        });
        
        receiverColumn.setCellValueFactory(data -> {
            User receiver = data.getValue().getReceiverName();  // Show the receiver's name
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
    }
    
    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<TradeOffer, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Button reportButton = new Button("Report");
            private final Button pdfButton = new Button("PDF");
            private final HBox buttonsBox = new HBox(5);
            
            {
                // Style the buttons
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 60px");
                deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-min-width: 60px");
                reportButton.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white; -fx-min-width: 60px");
                pdfButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 60px");

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

                pdfButton.setOnAction(event -> {
                    TradeOffer trade = getTableView().getItems().get(getIndex());
                    generateTradeOfferPdf(trade);
                });
                
                // Add some padding around the buttons
                buttonsBox.setStyle("-fx-padding: 2px");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    buttonsBox.getChildren().clear();
                    TradeOffer trade = getTableView().getItems().get(getIndex());
                    if (trade != null) {
                        List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
                        boolean isAdmin = currentUser.getRoles().containsAll(adminRoles) && currentUser.getRoles().size() == adminRoles.size();
                        
                        if (isAdmin) {
                            // Admin sees all buttons for all trades
                            buttonsBox.getChildren().addAll(editButton, deleteButton, pdfButton);
                            
                            // Add visual indicator if trade has been reported
                            try {
                                List<TradeDispute> disputes = disputeService.getAllDisputes();
                                boolean isReported = disputes.stream()
                                    .anyMatch(d -> d.getTradeId() == trade.getId());
                                    
                                if (isReported) {
                                    Button reportedButton = new Button("Has Reports");
                                    reportedButton.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white; -fx-min-width: 60px");
                                    buttonsBox.getChildren().add(reportedButton);
                                }
                            } catch (Exception e) {
                                System.err.println("Error checking trade dispute status: " + e.getMessage());
                            }
                        } else {
                            // Regular user flow
                            if ("pending".equals(trade.getStatus())) {
                                // For pending trades, show edit and delete
                                buttonsBox.getChildren().addAll(editButton, deleteButton);
                                buttonsBox.getChildren().add(pdfButton);
                            } else {
                                // For accepted/rejected trades, show delete and report options
                                Button reportedButton = new Button("Reported");
                                reportedButton.setStyle("-fx-background-color: #808080; -fx-text-fill: white; -fx-min-width: 60px");
                                reportedButton.setDisable(true);
                                
                                // Always show delete button for accepted/rejected trades
                                buttonsBox.getChildren().add(deleteButton);
                                
                                try {
                                    // Check if this trade has been reported by the current user
                                    List<TradeDispute> disputes = disputeService.getAllDisputes();
                                    boolean isReported = disputes.stream()
                                        .anyMatch(d -> d.getTradeId() == trade.getId() && d.getReporter() == currentUser.getId());
                                    
                                    if (isReported) {
                                        // If reported, show disabled "Reported" button with PDF
                                        buttonsBox.getChildren().addAll(deleteButton, reportedButton, pdfButton);
                                    } else {
                                        // If not reported, show Report button with PDF
                                        buttonsBox.getChildren().addAll(deleteButton, reportButton, pdfButton);
                                    }
                                } catch (Exception e) {
                                    // In case of error, default to showing the report button
                                    buttonsBox.getChildren().addAll(reportButton, pdfButton);
                                    System.err.println("Error checking trade dispute status: " + e.getMessage());
                                }
                            }
                        }
                        setGraphic(buttonsBox);
                    }
                }
            }
        });
    }    public void setUser(User user) {
        System.out.println("Setting user in TradeOfferListController: " + (user != null ? user.getName() : "null"));
        this.currentUser = user;
        
        // Check if user has admin role and show/hide stats button
        List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        boolean isAdmin = user.getRoles().containsAll(adminRoles) && user.getRoles().size() == adminRoles.size();
        
        // Control visibility of admin-only elements
        statsButton.setVisible(isAdmin);
        
        // Always hide ID column and receiver column, use sender column to show receiver name
        idColumn.setVisible(false);
        receiverColumn.setVisible(false);
        senderColumn.setText("Receiver");  // Always show receiver name in this column
        
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
            // Get a fresh service instance to ensure a new connection
            tradeService = new TradeOfferService();
            
            List<TradeOffer> trades;
            // If user has both ROLE_USER and ROLE_ADMIN roles, get all trades
            List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
            boolean isAdmin = currentUser.getRoles().containsAll(adminRoles) && currentUser.getRoles().size() == adminRoles.size();
            
            if (isAdmin) {
                trades = tradeService.getAllTradeOffers();
                System.out.println("Admin: Loaded all " + trades.size() + " trades");
            } else {
                trades = tradeService.getAllTradeOffers();
                // Filter trades to show only where current user is the sender
                trades.removeIf(trade -> 
                    currentUser == null || 
                    trade.getSender() == null ||
                    trade.getSender().getId() != currentUser.getId()
                );
                System.out.println("Loaded " + trades.size() + " trades where user is sender: " + currentUser.getName());
            }
            
            tradeList.addAll(trades);
            
            // Log the trades that were added
            for (TradeOffer trade : trades) {
                System.out.println("Added trade to list - ID: " + trade.getId() + ", Status: " + trade.getStatus() + 
                                 ", Sender: " + (trade.getSender() != null ? trade.getSender().getName() : "null") + 
                                 ", Receiver: " + (trade.getReceiverName() != null ? trade.getReceiverName().getName() : "null"));
            }
            
        } catch (SQLException e) {
            showError("Failed to load trades: " + e.getMessage());
            e.printStackTrace();
        }
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
            // Create a confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Delete");
            confirmDialog.setHeaderText("Delete Trade Offer");
            confirmDialog.setContentText("Are you sure you want to delete this trade offer? This action cannot be undone.");
            
            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Get a fresh service instance to ensure a new connection
                        TradeOfferService freshTradeService = new TradeOfferService();
                        freshTradeService.delete(trade);
                        refreshTrades();
                        showAlert("Success", "Trade offer deleted successfully", Alert.AlertType.INFORMATION);
                    } catch (Exception ex) {
                        showError("Error deleting trade offer: " + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            showError("Error showing confirmation dialog: " + e.getMessage());
        }
    }

    private void handleReportTrade(TradeOffer trade) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateTradeDispute.fxml"));
            Parent reportTradeView = loader.load();
            
            CreateTradeDisputeController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setTradeOffer(trade);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(reportTradeView));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Report Trade");
            
            // After the window closes, refresh the table with a new database connection
            stage.setOnHidden(e -> refreshTrades());
            
            stage.show();
        } catch (IOException e) {
            showError("Could not open report trade window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateTradeOfferPdf(TradeOffer tradeOffer) {
        try {
            if (tradeOffer == null) {
                showError("Invalid trade offer");
                return;
            }

            String fileName = "reports/trade_offer_" + tradeOffer.getId() + "_" + System.currentTimeMillis() + ".pdf";
            new File("reports").mkdirs();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Trade Offer Details", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            // Add creation date
            document.add(new Paragraph("Created on: " + tradeOffer.getCreationDate(), normalFont));
            document.add(Chunk.NEWLINE);

            // Add sender name
            document.add(new Paragraph("From: " + tradeOffer.getSender().getName(), boldFont));
            document.add(Chunk.NEWLINE);

            // Add offered item details
            document.add(new Paragraph("Offered Item:", boldFont));
            Artwork offered = tradeOffer.getOfferedItem();
            document.add(new Paragraph("Title: " + offered.getTitle(), normalFont));
            document.add(new Paragraph("Description: " + offered.getDescription(), normalFont));
            document.add(Chunk.NEWLINE);

            // Add received item details
            document.add(new Paragraph("Requested Item:", boldFont));
            Artwork requested = tradeOffer.getReceivedItem();
            document.add(new Paragraph("Title: " + requested.getTitle(), normalFont));
            document.add(new Paragraph("Description: " + requested.getDescription(), normalFont));

            document.close();

            showAlert("Success", "PDF generated successfully at: " + fileName, Alert.AlertType.INFORMATION);

            // Open the PDF
            Desktop.getDesktop().open(new File(fileName));

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to generate PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowStatistics() {
        showTradeStatistics();
    }

    private void showTradeStatistics() {
        try {
            // Create a new service instance to ensure a fresh connection
            TradeOfferService freshTradeService = new TradeOfferService();
            List<TradeOffer> allTrades = freshTradeService.getAllTradeOffers();
            
            // Calculate statistics
            int totalTrades = allTrades.size();
            long acceptedTrades = allTrades.stream().filter(t -> "accepted".equalsIgnoreCase(t.getStatus())).count();
            long rejectedTrades = allTrades.stream().filter(t -> "rejected".equalsIgnoreCase(t.getStatus())).count();
            long pendingTrades = allTrades.stream().filter(t -> "pending".equalsIgnoreCase(t.getStatus())).count();

            // Create pie chart
            javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
            pieChart.setTitle("Trade Offer Statistics");
            
            // Create pie chart data with custom colors
            javafx.scene.chart.PieChart.Data acceptedSlice = new javafx.scene.chart.PieChart.Data("Accepted", acceptedTrades);
            javafx.scene.chart.PieChart.Data rejectedSlice = new javafx.scene.chart.PieChart.Data("Rejected", rejectedTrades);
            javafx.scene.chart.PieChart.Data pendingSlice = new javafx.scene.chart.PieChart.Data("Pending", pendingTrades);
            
            pieChart.getData().addAll(acceptedSlice, rejectedSlice, pendingSlice);
            
            // Add colors and percentage labels
            String[] colors = {"#4CAF50", "#f44336", "#FFA500"}; // Green for accepted, Red for rejected, Orange for pending
            int colorIndex = 0;
            
            for (javafx.scene.chart.PieChart.Data data : pieChart.getData()) {
                // Calculate percentage
                double percentage = (data.getPieValue() / totalTrades) * 100;
                
                // Create label with percentage
                String text = String.format("%s: %.1f%%", data.getName(), percentage);
                data.setName(text);
                
                // Set slice color
                data.getNode().setStyle("-fx-pie-color: " + colors[colorIndex++] + ";");
            }
            
            // Style the chart
            pieChart.setLabelsVisible(true);
            pieChart.setLegendVisible(true);
            pieChart.setStartAngle(90);
            pieChart.setClockwise(false);
            pieChart.setAnimated(true);
            pieChart.setLabelLineLength(20);
            
            // Create a dialog to show the chart
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Trade Statistics");
            dialog.setHeaderText("Total Trades: " + totalTrades);
            
            // Make the dialog resizable
            dialog.setResizable(true);
            dialog.getDialogPane().setPrefSize(500, 500);
            
            // Add the chart to the dialog
            dialog.getDialogPane().setContent(pieChart);
            
            // Add close button
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            
            dialog.showAndWait();
        } catch (SQLException e) {
            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
            errorDialog.setTitle("Error");
            errorDialog.setHeaderText(null);
            errorDialog.setContentText("Error fetching trade statistics: " + e.getMessage());
            errorDialog.showAndWait();
        }
    }

    private void showError(String message) {
        showAlert("Error", message, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void filterTradeOffers(String searchText) {
        if (masterData == null) {
            // Store the original list first time
            masterData = FXCollections.observableArrayList(tradeList);
        }
        
        if (searchText == null || searchText.isEmpty()) {
            tradeTable.setItems(masterData);
        } else {
            ObservableList<TradeOffer> filteredList = FXCollections.observableArrayList();
            String lowerCaseFilter = searchText.toLowerCase();
            
            for (TradeOffer offer : masterData) {
                if (offer.getSender() != null && 
                    offer.getSender().getName().toLowerCase().contains(lowerCaseFilter)) {
                    filteredList.add(offer);
                }
            }
            tradeTable.setItems(filteredList);
        }
    }
}