package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.esprit.models.Artwork;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.RaffleService;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class RaffleDetailsController {
    @FXML
    private ImageView artworkImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label creatorLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label artworkIdLabel;
    @FXML
    private ListView<String> participantsListView;
    @FXML
    private Button participateButton;

    private Raffle raffle;
    private User currentUser;
    private RaffleService raffleService;
    private ArtworkService artworkService;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private RaffleListController parentController;

    public void initialize() {
        raffleService = new RaffleService();
        artworkService = new ArtworkService();
    }

    public void setRaffle(Raffle raffle) {
        this.raffle = raffle;
        loadRaffleDetails();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateParticipateButton();
    }

    public void setParentController(RaffleListController controller) {
        this.parentController = controller;
    }

    private void loadRaffleDetails() {
        titleLabel.setText(raffle.getTitle());
        descriptionArea.setText(raffle.getRaffleDescription());
        creatorLabel.setText(raffle.getCreatorName());
        startDateLabel.setText(dateFormat.format(raffle.getStartTime()));
        endDateLabel.setText(dateFormat.format(raffle.getEndTime()));
        statusLabel.setText(raffle.getStatus());
        artworkIdLabel.setText(String.valueOf(raffle.getArtworkId()));

        // Load artwork image
        try {
            Artwork artwork = artworkService.getOne(raffle.getArtworkId());
            loadArtworkImage(artwork);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Load participants
        participantsListView.getItems().clear();
        for (User participant : raffle.getParticipants()) {
            participantsListView.getItems().add(participant.getName());
        }
    }

    private void loadArtworkImage(Artwork artwork) {
        if (artwork != null && artwork.getImageName() != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/uploads/" + artwork.getImageName()));
                artworkImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading artwork image " + artwork.getImageName() + ": " + e.getMessage());
            }
        }
    }

    private void updateParticipateButton() {
        if (currentUser != null) {
            boolean isCreator = raffle.getCreator().getId() == currentUser.getId();
            boolean isParticipant = raffle.getParticipants().contains(currentUser);
            participateButton.setDisable(isCreator || isParticipant);
            participateButton.setText(isParticipant ? "Already Participating" : "Participate");
        }
    }

    @FXML
    private void handleParticipate() {
        try {
            raffle.addParticipant(currentUser);
            raffleService.update(raffle);
            updateParticipateButton();
            loadRaffleDetails(); // Refresh the view
            
            if (parentController != null) {
                parentController.refreshRaffles();
            }
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error joining raffle: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}