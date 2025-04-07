package org.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TopupRequestsController implements Initializable {

    @FXML
    private ComboBox<String> statusFilterCombo;
    
    @FXML
    private TableView<TopupRequest> requestsTableView;
    
    @FXML
    private TableColumn<TopupRequest, Integer> idColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> userColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> dateColumn;
    
    @FXML
    private TableColumn<TopupRequest, BigDecimal> amountColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> paymentMethodColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> statusColumn;
    
    @FXML
    private TableColumn<TopupRequest, Void> actionsColumn;
    
    @FXML
    private Label totalRequestsLabel;
    
    @FXML
    private VBox requestDetailPane;
    
    @FXML
    private TextField requestIdField;
    
    @FXML
    private TextField userField;
    
    @FXML
    private TextField currentBalanceField;
    
    @FXML
    private TextField amountField;
    
    @FXML
    private TextField paymentMethodField;
    
    @FXML
    private TextField transactionIdField;
    
    @FXML
    private TextField dateField;
    
    @FXML
    private TextArea notesTextArea;
    
    @FXML
    private TextArea adminNotesTextArea;
    
    @FXML
    private TextField statusField;
    
    @FXML
    private Button approveButton;
    
    @FXML
    private Button rejectButton;
    
    private UserService userService;
    private TopupRequest selectedRequest;
    
    // This is a mock class for demo purposes - in a real application, this would be a proper entity
    public static class TopupRequest {
        private int id;
        private User user;
        private LocalDateTime date;
        private BigDecimal amount;
        private String paymentMethod;
        private String transactionId;
        private String notes;
        private String adminNotes;
        private String status;
        
        public TopupRequest(int id, User user, LocalDateTime date, BigDecimal amount, 
                           String paymentMethod, String transactionId, String notes, 
                           String adminNotes, String status) {
            this.id = id;
            this.user = user;
            this.date = date;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.transactionId = transactionId;
            this.notes = notes;
            this.adminNotes = adminNotes;
            this.status = status;
        }
        
        // Getters and setters
        public int getId() { return id; }
        
        public User getUser() { return user; }
        
        public String getUserName() { 
            return user != null ? user.getName() : "Unknown"; 
        }
        
        public LocalDateTime getDate() { return date; }
        
        public String getDateString() {
            return date.toString().replace("T", " ").substring(0, 19);
        }
        
        public BigDecimal getAmount() { return amount; }
        
        public String getPaymentMethod() { return paymentMethod; }
        
        public String getTransactionId() { return transactionId; }
        
        public String getNotes() { return notes; }
        
        public String getAdminNotes() { return adminNotes; }
        
        public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
        
        public String getStatus() { return status; }
        
        public void setStatus(String status) { this.status = status; }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        
        // Set up status filter options
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "All",
            "Pending",
            "Approved",
            "Rejected"
        );
        statusFilterCombo.setItems(statusOptions);
        statusFilterCombo.getSelectionModel().selectFirst();
        
        // Set up table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateString"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Setup actions column
        setupActionsColumn();
        
        // Load requests
        loadTopupRequests();
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<TopupRequest, Void>() {
            private final Button viewBtn = new Button("View");
            
            {
                viewBtn.setOnAction(event -> {
                    TopupRequest request = getTableView().getItems().get(getIndex());
                    showRequestDetails(request);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(Pos.CENTER);
                    buttons.getChildren().add(viewBtn);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void loadTopupRequests() {
        try {
            // For demo purposes, we'll create some sample data
            // In a real application, these would be loaded from the database
            ObservableList<TopupRequest> sampleData = createSampleRequests();
            
            requestsTableView.setItems(sampleData);
            totalRequestsLabel.setText("Total Requests: " + sampleData.size());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load topup requests", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private ObservableList<TopupRequest> createSampleRequests() {
        try {
            // Get some users from the database
            List<User> users = userService.getAll();
            User user1 = users.size() > 0 ? users.get(0) : new User("user1@example.com", "password", "John Doe");
            User user2 = users.size() > 1 ? users.get(1) : new User("user2@example.com", "password", "Jane Smith");
            
            // Create sample requests
            ObservableList<TopupRequest> sampleData = FXCollections.observableArrayList(
                new TopupRequest(
                    1, 
                    user1, 
                    LocalDateTime.now().minusDays(2), 
                    new BigDecimal("1.5"), 
                    "Credit Card", 
                    "TXN12345",
                    "Please process my request ASAP",
                    "",
                    "Pending"
                ),
                new TopupRequest(
                    2, 
                    user2, 
                    LocalDateTime.now().minusDays(1), 
                    new BigDecimal("2.0"), 
                    "PayPal", 
                    "PP87654321",
                    "Made payment through PayPal",
                    "",
                    "Pending"
                ),
                new TopupRequest(
                    3, 
                    user1, 
                    LocalDateTime.now().minusDays(5), 
                    new BigDecimal("1.0"), 
                    "Bank Transfer", 
                    "BT98765",
                    "Bank transfer completed",
                    "Verified payment receipt",
                    "Approved"
                )
            );
            
            return sampleData;
        } catch (Exception e) {
            e.printStackTrace();
            return FXCollections.observableArrayList(); // Return empty list if error occurs
        }
    }
    
    @FXML
    private void handleApplyFilter() {
        String selectedStatus = statusFilterCombo.getValue();
        
        if (selectedStatus == null || selectedStatus.equals("All")) {
            loadTopupRequests();
            return;
        }
        
        // Filter the requests based on status
        ObservableList<TopupRequest> allRequests = createSampleRequests();
        ObservableList<TopupRequest> filteredRequests = FXCollections.observableArrayList();
        
        for (TopupRequest request : allRequests) {
            if (request.getStatus().equals(selectedStatus)) {
                filteredRequests.add(request);
            }
        }
        
        requestsTableView.setItems(filteredRequests);
        totalRequestsLabel.setText("Found Requests: " + filteredRequests.size());
    }
    
    @FXML
    private void handleClearFilter() {
        statusFilterCombo.getSelectionModel().selectFirst();
        loadTopupRequests();
    }
    
    private void showRequestDetails(TopupRequest request) {
        selectedRequest = request;
        
        // Populate form fields
        requestIdField.setText(String.valueOf(request.getId()));
        userField.setText(request.getUserName());
        
        // Get current balance
        User user = request.getUser();
        currentBalanceField.setText(user.getBalance() + " ETH");
        
        amountField.setText(request.getAmount() + " ETH");
        paymentMethodField.setText(request.getPaymentMethod());
        transactionIdField.setText(request.getTransactionId());
        dateField.setText(request.getDateString());
        notesTextArea.setText(request.getNotes());
        adminNotesTextArea.setText(request.getAdminNotes());
        statusField.setText(request.getStatus());
        
        // Enable/disable approve/reject buttons based on status
        boolean isPending = "Pending".equals(request.getStatus());
        approveButton.setDisable(!isPending);
        rejectButton.setDisable(!isPending);
        
        // Show detail pane
        requestDetailPane.setVisible(true);
    }
    
    @FXML
    private void handleApproveRequest() {
        if (selectedRequest == null) return;
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Approve Request");
        confirmation.setHeaderText("Approve Top-up Request #" + selectedRequest.getId());
        confirmation.setContentText("Are you sure you want to approve this request and add " 
                                 + selectedRequest.getAmount() + " ETH to " 
                                 + selectedRequest.getUserName() + "'s balance?");
                                 
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Update request status
                selectedRequest.setStatus("Approved");
                selectedRequest.setAdminNotes(adminNotesTextArea.getText());
                
                // Update user balance
                User user = selectedRequest.getUser();
                BigDecimal newBalance = user.getBalance().add(selectedRequest.getAmount());
                user.setBalance(newBalance);
                
                // In a real app, save changes to database
                userService.update(user);
                
                // Update UI
                statusField.setText("Approved");
                currentBalanceField.setText(newBalance + " ETH");
                approveButton.setDisable(true);
                rejectButton.setDisable(true);
                
                // Refresh table
                loadTopupRequests();
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Request Approved", 
                         "Top-up request has been approved and user's balance has been updated.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to approve request", e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleRejectRequest() {
        if (selectedRequest == null) return;
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Reject Request");
        confirmation.setHeaderText("Reject Top-up Request #" + selectedRequest.getId());
        confirmation.setContentText("Are you sure you want to reject this top-up request?");
                                 
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Update request status
                selectedRequest.setStatus("Rejected");
                selectedRequest.setAdminNotes(adminNotesTextArea.getText());
                
                // In a real app, save changes to database
                // requestService.update(selectedRequest);
                
                // Update UI
                statusField.setText("Rejected");
                approveButton.setDisable(true);
                rejectButton.setDisable(true);
                
                // Refresh table
                loadTopupRequests();
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Request Rejected", 
                         "Top-up request has been rejected.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject request", e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleCloseDetails() {
        requestDetailPane.setVisible(false);
        selectedRequest = null;
        adminNotesTextArea.clear();
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}