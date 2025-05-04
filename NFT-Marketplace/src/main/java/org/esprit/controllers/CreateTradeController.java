package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.esprit.models.Artwork;
import org.esprit.models.TradeOffer;
import org.esprit.models.TradeState;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.TradeOfferService;
import org.esprit.services.TradeStateService;
import org.esprit.services.UserService;
import org.esprit.services.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.esprit.utils.CaptchaGenerator;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.time.LocalDateTime;

public class CreateTradeController {
    @FXML private ComboBox<Artwork> offeredArtworkCombo;
    @FXML private ComboBox<Artwork> requestedArtworkCombo;
    @FXML private ComboBox<User> receiverCombo;
    @FXML private TextArea descriptionField;
    @FXML private Label statusLabel;
    @FXML private Canvas captchaCanvas;
    @FXML private TextField captchaField;
    
    private String currentCaptcha;
    private User currentUser;
    private TradeOfferService tradeService;
    private ArtworkService artworkService;
    private UserService userService;
    private TradeOfferListController parentController;
    private SmsService smsService;
    
    @FXML
    public void initialize() {
        tradeService = new TradeOfferService();
        artworkService = new ArtworkService();
        userService = new UserService();
        smsService = new SmsService(); // Initialize SMS service with default credentials
        refreshCaptcha(); // Initialize first CAPTCHA
        loadData();
    }
    
    public void setUser(User user) {
        this.currentUser = user;
        loadData();
    }
    
    private void loadData() {
        try {
            if (currentUser == null) return;
            
            // Load user's artworks for offered artwork combo
            ObservableList<Artwork> userArtworks = FXCollections.observableArrayList(
                artworkService.getByOwner(currentUser.getId())
            );
            offeredArtworkCombo.setItems(userArtworks);
            
            // Load all users except current user for receiver combo
            ObservableList<User> allUsers = FXCollections.observableArrayList(
                userService.getAll().stream()
                    .filter(user -> user.getId() != currentUser.getId())
                    .toList()
            );
            receiverCombo.setItems(allUsers);
            
            setupComboBoxCellFactories();
            
        } catch (Exception e) {
            showStatus("Error loading data: " + e.getMessage(), true);
        }
    }
    
    private void setupComboBoxCellFactories() {
        // Set cell factories for all combo boxes
        setupArtworkComboBox(offeredArtworkCombo);
        setupArtworkComboBox(requestedArtworkCombo);
        setupUserComboBox(receiverCombo);
        
        // Update requested artwork combo when receiver is selected
        receiverCombo.setOnAction(event -> {
            User selectedUser = receiverCombo.getValue();
            if (selectedUser != null) {
                try {
                    ObservableList<Artwork> receiverArtworks = FXCollections.observableArrayList(
                        artworkService.getByOwner(selectedUser.getId())
                    );
                    requestedArtworkCombo.setItems(receiverArtworks);
                } catch (Exception e) {
                    showStatus("Error loading receiver's artworks: " + e.getMessage(), true);
                }
            }
        });
    }
    
    private void setupArtworkComboBox(ComboBox<Artwork> comboBox) {
        comboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
    }
    
    private void setupUserComboBox(ComboBox<User> comboBox) {
        comboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }
    
    public void setParentController(TradeOfferListController controller) {
        this.parentController = controller;
    }
    
    @FXML
    private void handleCreateTrade(ActionEvent event) {
        try {
            if (!validateCaptcha()) {
                showStatus("Invalid CAPTCHA. Please try again.", true);
                refreshCaptcha();
                captchaField.clear();
                return;
            }

            validateInputs();
            TradeOffer tradeOffer = createTradeOffer();
            saveTradeOffer(tradeOffer);
            sendNotification(tradeOffer);
            handleSuccess();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }
    
    private void validateInputs() {
        Artwork offeredArtwork = offeredArtworkCombo.getValue();
        Artwork requestedArtwork = requestedArtworkCombo.getValue();
        User receiver = receiverCombo.getValue();
        
        if (offeredArtwork == null || requestedArtwork == null || receiver == null) {
            throw new IllegalStateException("Please select all required fields");
        }
    }
    
    private TradeOffer createTradeOffer() {
        TradeOffer tradeOffer = new TradeOffer();
        tradeOffer.setSender(currentUser);
        tradeOffer.setReceiverName(receiverCombo.getValue());
        tradeOffer.setOfferedItem(offeredArtworkCombo.getValue());
        tradeOffer.setReceivedItem(requestedArtworkCombo.getValue());
        tradeOffer.setDescription(descriptionField.getText().trim());
        tradeOffer.setCreationDate(LocalDateTime.now());
        tradeOffer.setStatus("pending");
        return tradeOffer;
    }
    
    private void saveTradeOffer(TradeOffer tradeOffer) throws Exception {
        // Save trade offer
        tradeService.add(tradeOffer);
        
        // Create and save trade state
        TradeState tradeState = new TradeState();
        tradeState.setTradeOffer(tradeOffer);
        tradeState.setReceivedItem(tradeOffer.getReceivedItem());
        tradeState.setOfferedItem(tradeOffer.getOfferedItem());
        tradeState.setSender(tradeOffer.getSender());
        tradeState.setReceiver(tradeOffer.getReceiverName());
        tradeState.setDescription(tradeOffer.getDescription());
        
        TradeStateService tradeStateService = new TradeStateService();
        tradeStateService.add(tradeState);
    }
    
    private void sendNotification(TradeOffer tradeOffer) {
        try {
            String message = String.format(
                "NFT Trade Alert: %s wants to trade!\n" +
                "Offering: %s\n" +
                "Requesting: %s\n" +
                "Status: Pending\n" +
                "Login to review the trade offer.",
                currentUser.getName(),
                tradeOffer.getOfferedItem().getTitle(),
                tradeOffer.getReceivedItem().getTitle()
            );

            // Initialize Twilio directly
            Properties props = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
                props.load(input);
                String accountSid = props.getProperty("twilio.account.sid");
                String authToken = props.getProperty("twilio.auth.token");
                String fromNumber = props.getProperty("twilio.phone.number");
                
                Twilio.init(accountSid, authToken);
                
                Message.creator(
                    new PhoneNumber("+21696396731"),  // To number with country code
                    new PhoneNumber(fromNumber),      // From number
                    message                           // Message content
                ).create();
                
                System.out.println("SMS notification sent successfully");
            } catch (IOException e) {
                System.err.println("Error loading Twilio configuration: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
            showStatus("Trade created but notification failed", true);        }
    }
    
    private void handleSuccess() {
        showStatus("Trade offer created successfully!", false);
        if (parentController != null) {
            parentController.refreshTrades();
            // Add a small delay before closing the window to ensure the refresh completes
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            ((Stage) offeredArtworkCombo.getScene().getWindow()).close();
                        });
                    }
                },
                500 // 500ms delay
            );
        } else {
            ((Stage) offeredArtworkCombo.getScene().getWindow()).close();
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) offeredArtworkCombo.getScene().getWindow()).close();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }

    @FXML
    private void refreshCaptcha() {
        currentCaptcha = CaptchaGenerator.generateCaptchaText();
        CaptchaGenerator.drawCaptcha(captchaCanvas, currentCaptcha);
    }

    private boolean validateCaptcha() {
        String userInput = captchaField.getText();
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        return userInput.equalsIgnoreCase(currentCaptcha);
    }
}