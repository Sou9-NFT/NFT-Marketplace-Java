package org.esprit.main;

import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ParticipantService;
import org.esprit.services.RaffleService;
import org.esprit.services.UserService;

import java.sql.SQLException;
import java.util.List;

public class TestCrudParticipant {
    public static void main(String[] args) {
        try {
            // Initialize services
            ParticipantService participantService = new ParticipantService();
            UserService userService = new UserService();
            RaffleService raffleService = new RaffleService();

            // Get a user and raffle for testing (assuming they exist)
            User user = userService.getOne(1);
            Raffle raffle = raffleService.getOne(1);

            if (user == null || raffle == null) {
                System.out.println("Please ensure there's at least one user and raffle in the database.");
                return;
            }

            // Test Create
            System.out.println("Testing Participant Creation...");
            Participant participant = new Participant(raffle, user);
            participantService.add(participant);
            System.out.println("Created participant with ID: " + participant.getId());

            // Test Read
            System.out.println("\nTesting Participant Retrieval...");
            Participant retrievedParticipant = participantService.getOne(participant.getId());
            System.out.println("Retrieved participant: " + retrievedParticipant);

            // Test Update
            System.out.println("\nTesting Participant Update...");
            retrievedParticipant.setName("Updated Name");
            participantService.update(retrievedParticipant);
            
            // Verify update
            Participant updatedParticipant = participantService.getOne(retrievedParticipant.getId());
            System.out.println("Updated participant: " + updatedParticipant);

            // Test Get By Raffle
            System.out.println("\nTesting Get Participants by Raffle...");
            List<Participant> raffleParticipants = participantService.getByRaffle(raffle);
            System.out.println("Participants in raffle " + raffle.getId() + ": " + raffleParticipants.size());

            // Test Get By User
            System.out.println("\nTesting Get Participants by User...");
            List<Participant> userParticipations = participantService.getByUser(user);
            System.out.println("Participations by user " + user.getId() + ": " + userParticipations.size());

            // Test Get All
            System.out.println("\nTesting Get All Participants...");
            List<Participant> allParticipants = participantService.getAll();
            System.out.println("Total participants found: " + allParticipants.size());
            for (Participant p : allParticipants) {
                System.out.println(p);
            }

            // Test Delete
            System.out.println("\nTesting Participant Deletion...");
            participantService.delete(retrievedParticipant);

            // Verify deletion
            Participant deletedParticipant = participantService.getOne(retrievedParticipant.getId());
            if (deletedParticipant == null) {
                System.out.println("Participant successfully deleted!");
            } else {
                System.out.println("Failed to delete participant!");
            }

        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}