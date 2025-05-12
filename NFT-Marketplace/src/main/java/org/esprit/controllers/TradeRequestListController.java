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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.esprit.models.TradeState;
import org.esprit.models.User;
import org.esprit.models.TradeOffer;
import org.esprit.models.Artwork;
import org.esprit.services.TradeStateService;
import org.esprit.services.ArtworkService;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.List;

public class TradeRequestListController {
    @FXML private TableView<TradeState> tradeTable;
    @FXML private TableColumn<TradeState, String> idColumn;
    @FXML private TableColumn<TradeState, String> senderColumn;
    @FXML private TableColumn<TradeState, String> receiverColumn;
    @FXML private TableColumn<TradeState, String> offeredItemColumn;
    @FXML private TableColumn<TradeState, String> requestedItemColumn;
    @FXML private TableColumn<TradeState, String> descriptionColumn;
    @FXML private TableColumn<TradeState, Void> actionColumn;
    
    private User currentUser;
    private TradeStateService tradeStateService;
    private ObservableList<TradeState> tradeList;
    private ArtworkService artworkService;
    
    @FXML
    public void initialize() {
        tradeStateService = new TradeStateService();
        artworkService = new ArtworkService();
        tradeList = FXCollections.observableArrayList();
        
        // Configure column widths and alignment
        idColumn.setPrefWidth(60);
        senderColumn.setPrefWidth(120);
        receiverColumn.setPrefWidth(120);
        offeredItemColumn.setPrefWidth(150);
        requestedItemColumn.setPrefWidth(150);
        descriptionColumn.setPrefWidth(200);
        actionColumn.setPrefWidth(300);
        
        // Center align appropriate columns
        idColumn.setStyle("-fx-alignment: CENTER;");
        actionColumn.setStyle("-fx-alignment: CENTER;");
        
        // Make description column wrap text
        descriptionColumn.setStyle("-fx-alignment: TOP-LEFT; -fx-wrap-text: true;");
        descriptionColumn.setCellFactory(tc -> {
            TableCell<TradeState, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            text.wrappingWidthProperty().bind(descriptionColumn.widthProperty());
            text.textProperty().bind(cell.itemProperty());
            return cell;
        });

        // Setup column cell value factories
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        senderColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getSender() != null ? data.getValue().getSender().getName() : ""));
        receiverColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getReceiver() != null ? data.getValue().getReceiver().getName() : ""));
        offeredItemColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getOfferedItem() != null ? data.getValue().getOfferedItem().getTitle() : ""));
        requestedItemColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getReceivedItem() != null ? data.getValue().getReceivedItem().getTitle() : ""));
        descriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getTradeOffer() != null ? data.getValue().getTradeOffer().getDescription() : ""));

        setupActionColumn();
        
        // Set items to the table
        tradeTable.setItems(tradeList);
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<TradeState, Void>() {
            private final Button acceptButton = new Button("Accept");
            private final Button rejectButton = new Button("Reject");
            private final Button pdfButton = new Button("PDF");
            private final HBox buttonsBox = new HBox(5, acceptButton, rejectButton, pdfButton);
              {
                // Style the buttons
                acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 60px;");
                rejectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 60px;");
                pdfButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 60px;");
                
                // Set up button actions
                acceptButton.setOnAction(event -> {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    handleAcceptTrade(tradeState);
                });
                
                rejectButton.setOnAction(event -> {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    handleRejectTrade(tradeState);
                });

                pdfButton.setOnAction(event -> {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    generateTradeOfferPdf(tradeState);
                });
                  // Set button sizes to be consistent - wider buttons for better readability
                acceptButton.setPrefWidth(80);
                rejectButton.setPrefWidth(80);
                pdfButton.setPrefWidth(80);
                
                // Add some padding and spacing around the buttons
                buttonsBox.setStyle("-fx-padding: 5px; -fx-spacing: 10px;");
                // Add hover effects to buttons
                String defaultStyle = "-fx-text-fill: white; -fx-min-width: 80px;";
                String hoverStyle = "-fx-text-fill: white; -fx-min-width: 80px; -fx-opacity: 0.9;";
                
                acceptButton.setStyle("-fx-background-color: #4CAF50; " + defaultStyle);
                rejectButton.setStyle("-fx-background-color: #f44336; " + defaultStyle);
                pdfButton.setStyle("-fx-background-color: #2196F3; " + defaultStyle);
                
                acceptButton.setOnMouseEntered(e -> acceptButton.setStyle("-fx-background-color: #45a049; " + hoverStyle));
                acceptButton.setOnMouseExited(e -> acceptButton.setStyle("-fx-background-color: #4CAF50; " + defaultStyle));
                
                rejectButton.setOnMouseEntered(e -> rejectButton.setStyle("-fx-background-color: #da190b; " + hoverStyle));
                rejectButton.setOnMouseExited(e -> rejectButton.setStyle("-fx-background-color: #f44336; " + defaultStyle));
                
                pdfButton.setOnMouseEntered(e -> pdfButton.setStyle("-fx-background-color: #0b7dda; " + hoverStyle));
                pdfButton.setOnMouseExited(e -> pdfButton.setStyle("-fx-background-color: #2196F3; " + defaultStyle));}
              @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TradeState tradeState = getTableView().getItems().get(getIndex());
                    
                    // Show buttons only if the current user is the receiver of this trade request
                    // This applies to both admin and regular users
                    if (currentUser != null && 
                        tradeState.getReceiver() != null && 
                        currentUser.getId() == tradeState.getReceiver().getId()) {
                        setGraphic(buttonsBox);
                    } else {
                        // For trades where the user is not the receiver, just show the PDF button
                        Button pdfOnlyButton = new Button("PDF");
                        pdfOnlyButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 80px;");
                        pdfOnlyButton.setOnAction(event -> generateTradeOfferPdf(tradeState));
                        setGraphic(pdfOnlyButton);
                    }
                }
            }
        });
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        
        // Check if user has admin role
        List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
        boolean isAdmin = user.getRoles().containsAll(adminRoles) && user.getRoles().size() == adminRoles.size();
          // Control visibility of admin-only elements
        idColumn.setVisible(false);  // Always hide ID column
        receiverColumn.setVisible(isAdmin);
        
        refreshTrades();
    }    public void refreshTrades() {
        try {
            tradeList.clear();
            List<TradeState> trades = tradeStateService.getAllPendingTrades();
            
            // Admin can see all pending trades, regular users only see their own
            List<String> adminRoles = List.of("ROLE_USER", "ROLE_ADMIN");
            boolean isAdmin = currentUser != null && 
                             currentUser.getRoles().containsAll(adminRoles) && 
                             currentUser.getRoles().size() == adminRoles.size();            // For regular users, only show trades where they are the receiver
            if (!isAdmin) {
                trades.removeIf(trade -> 
                    currentUser == null || 
                    trade.getReceiver() == null ||
                    trade.getReceiver().getId() != currentUser.getId()
                );
            }
            // For admins, show all trades but they can only accept/reject their own
            // The accept/reject buttons will only be shown for trades where they are the receiver

            System.out.println("Loading " + trades.size() + " trade requests for " + 
                (isAdmin ? "admin" : "user " + currentUser.getName()));
            
            tradeList.addAll(trades);
            tradeTable.setItems(tradeList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load trades from database: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load trades: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleAcceptTrade(TradeState tradeState) {
        try {
            // Validate the trade state
            if (tradeState == null || tradeState.getTradeOffer() == null) {
                showAlert("Error", "Invalid trade state", Alert.AlertType.ERROR);
                return;
            }

            // Security check - ensure current user is the receiver
            if (currentUser == null || tradeState.getReceiver() == null || 
                currentUser.getId() != tradeState.getReceiver().getId()) {
                showAlert("Error", "You are not authorized to accept this trade", Alert.AlertType.ERROR);
                return;
            }

            // Get the items involved in the trade
            Artwork receivedItem = tradeState.getReceivedItem();
            Artwork offeredItem = tradeState.getOfferedItem();

            // Security checks for items
            if (receivedItem == null || offeredItem == null) {
                showAlert("Error", "Invalid trade items", Alert.AlertType.ERROR);
                return;
            }

            // Get the users involved
            User sender = tradeState.getSender();
            User receiver = tradeState.getReceiver();

            // Swap ownership of the items
            int tempOwnerId = offeredItem.getOwnerId();
            offeredItem.setOwnerId(receiver.getId());
            receivedItem.setOwnerId(tempOwnerId);

            // Update items in database
            artworkService.update(receivedItem);
            artworkService.update(offeredItem);

            // Update trade offer status
            TradeOffer tradeOffer = tradeState.getTradeOffer();
            tradeOffer.setStatus("accepted");
            tradeStateService.updateTradeOfferStatus(tradeOffer.getId(), "accepted");

            // Show success message
            showAlert("Success", "Trade accepted and items swapped successfully!", Alert.AlertType.INFORMATION);

            // Refresh the trades list
            refreshTrades();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to accept trade: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleRejectTrade(TradeState tradeState) {
        try {
            // Validate the trade state
            if (tradeState == null || tradeState.getTradeOffer() == null) {
                showAlert("Error", "Invalid trade state", Alert.AlertType.ERROR);
                return;
            }

            // Security check - ensure current user is the receiver
            if (currentUser == null || tradeState.getReceiver() == null || 
                currentUser.getId() != tradeState.getReceiver().getId()) {
                showAlert("Error", "You are not authorized to reject this trade", Alert.AlertType.ERROR);
                return;
            }

            // Update trade offer status
            TradeOffer tradeOffer = tradeState.getTradeOffer();
            tradeOffer.setStatus("rejected");
            tradeStateService.updateTradeOfferStatus(tradeOffer.getId(), "rejected");

            // Show success message
            showAlert("Success", "Trade offer rejected successfully", Alert.AlertType.INFORMATION);

            // Refresh the trades list
            refreshTrades();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to reject trade: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void generateTradeOfferPdf(TradeState tradeState) {
        try {
            if (tradeState == null || tradeState.getTradeOffer() == null) {
                showAlert("Error", "Invalid trade state", Alert.AlertType.ERROR);
                return;
            }

            // Create unique filename using timestamp
            String fileName = "trade_offer_" + tradeState.getId() + "_" + System.currentTimeMillis() + ".pdf";
            String filePath = "reports/" + fileName;

            // Ensure reports directory exists
            new File("reports").mkdirs();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("Trade Request Details", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add sender name
            document.add(new Paragraph("From: " + tradeState.getSender().getName(), boldFont));
            document.add(Chunk.NEWLINE);            // Add offered item details
            document.add(new Paragraph("Offered Item:", boldFont));
            document.add(new Paragraph(tradeState.getOfferedItem().getTitle(), normalFont));
            document.add(Chunk.NEWLINE);

            // Add received item details
            document.add(new Paragraph("Requested Item:", boldFont));
            document.add(new Paragraph(tradeState.getReceivedItem().getTitle(), normalFont));
            document.add(Chunk.NEWLINE);

            // Add trade description if exists
            if (tradeState.getDescription() != null && !tradeState.getDescription().isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Trade Description:", boldFont));
                document.add(new Paragraph(tradeState.getDescription(), normalFont));
            }

            document.close();

            // Show success message with file location
            showAlert("Success", "PDF generated successfully at: " + filePath, Alert.AlertType.INFORMATION);

            // Open the PDF
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) {
                Desktop.getDesktop().open(pdfFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to generate PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
