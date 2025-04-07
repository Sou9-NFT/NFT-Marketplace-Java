package org.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class TopupController implements Initializable {

    @FXML
    private Label currentBalanceLabel;
    
    @FXML
    private TextField amountField;
    
    @FXML
    private ComboBox<String> paymentMethodCombo;
    
    @FXML
    private TextField transactionIdField;
    
    @FXML
    private TextArea notesTextArea;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private TableView<TopupRequest> requestsTableView;
    
    @FXML
    private TableColumn<TopupRequest, String> dateColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> amountColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> statusColumn;
    
    @FXML
    private TableColumn<TopupRequest, String> paymentMethodColumn;
    
    private UserService userService;
    private User currentUser;
    
    // In a real application, this would be a proper entity and service
    public static class TopupRequest {
        private LocalDateTime date;
        private BigDecimal amount;
        private String status;
        private String paymentMethod;
        
        public TopupRequest(LocalDateTime date, BigDecimal amount, String status, String paymentMethod) {
            this.date = date;
            this.amount = amount;
            this.status = status;
            this.paymentMethod = paymentMethod;
        }
        
        public String getDateString() {
            return date.toString().replace("T", " ").substring(0, 19);
        }
        
        public String getAmountString() {
            return amount.toString() + " ETH";
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getPaymentMethod() {
            return paymentMethod;
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        currentUser = MainViewController.getCurrentUser();
        
        if (currentUser != null) {
            // Set current balance
            currentBalanceLabel.setText(currentUser.getBalance() + " ETH");
            
            // Set up payment method options
            ObservableList<String> paymentMethods = FXCollections.observableArrayList(
                "Credit Card",
                "PayPal",
                "Bank Transfer",
                "Cryptocurrency"
            );
            paymentMethodCombo.setItems(paymentMethods);
            paymentMethodCombo.getSelectionModel().selectFirst();
            
            // Set up the table columns
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateString"));
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("amountString"));
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
            
            // For demo purposes, load some sample data
            // In a real app, these would be loaded from a database
            loadSampleTopupRequests();
        } else {
            // Handle error - no user logged in
            errorLabel.setText("No user logged in");
        }
    }
    
    private void loadSampleTopupRequests() {
        // In a real application, you would load these from the database
        ObservableList<TopupRequest> sampleData = FXCollections.observableArrayList(
            new TopupRequest(LocalDateTime.now().minusDays(5), new BigDecimal("0.5"), "Approved", "Credit Card"),
            new TopupRequest(LocalDateTime.now().minusDays(2), new BigDecimal("1.0"), "Pending", "Bank Transfer")
        );
        
        requestsTableView.setItems(sampleData);
    }
    
    @FXML
    private void handleSubmitRequest() {
        // Clear previous error message
        errorLabel.setText("");
        
        // Validate input
        String amountText = amountField.getText().trim();
        String paymentMethod = paymentMethodCombo.getValue();
        
        if (amountText.isEmpty()) {
            errorLabel.setText("Amount is required");
            return;
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setText("Amount must be greater than zero");
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid amount format");
            return;
        }
        
        // In a real application, save the topup request to the database
        // For this demo, we'll just add it to the table
        
        // Create and add new request
        TopupRequest newRequest = new TopupRequest(
            LocalDateTime.now(),
            amount,
            "Pending",
            paymentMethod
        );
        
        requestsTableView.getItems().add(0, newRequest);
        
        // Clear form fields
        amountField.clear();
        transactionIdField.clear();
        notesTextArea.clear();
        
        // Show confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Submitted");
        alert.setHeaderText("Top-up Request Submitted");
        alert.setContentText("Your request for " + amount + " ETH has been submitted and is pending approval.");
        alert.showAndWait();
    }
    
    @FXML
    private void handleCancel() {
        // Clear form fields
        amountField.clear();
        transactionIdField.clear();
        notesTextArea.clear();
        errorLabel.setText("");
        
        // Return to main view
        MainViewController mainController = (MainViewController) 
            amountField.getScene().getWindow().getUserData();
        if (mainController != null) {
            try {
                mainController.updateUIForUser();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}