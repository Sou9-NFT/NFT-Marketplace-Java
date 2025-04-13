package org.esprit.main;

import org.esprit.models.Participant;
import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.services.ParticipantService;
import org.esprit.services.RaffleService;
import org.esprit.services.UserService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Calendar;

public class
TestCrudRaffle {
    public static void main(String[] args) {
        try {
            // Initialize services
            RaffleService raffleService = new RaffleService();
            UserService userService = new UserService();
            ParticipantService participantService = new ParticipantService();

            // Get a user to be the creator (assuming there's at least one user in the database)
            User creator = userService.getOne(1); // Get user with ID 1
            if (creator == null) {
                System.out.println("No user found to be creator. Please create a user first.");
                return;
            }

            // Get another user to be a participant (assuming there's at least two users)
            User participant = userService.getOne(2);
            if (participant == null) {
                System.out.println("No second user found to be participant. Please create another user.");
                return;
            }

            // Test Create
            System.out.println("Testing Raffle Creation...");
            Raffle raffle = new Raffle();
            raffle.setTitle("Test Raffle");
            raffle.setRaffleDescription("This is a test raffle description");
            raffle.setStartTime(new Date());
            raffle.setArtworkId(1); // Set an artwork ID (make sure this ID exists in your artwork table)
            
            // Set end time to 7 days from now
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            raffle.setEndTime(calendar.getTime());
            
            raffle.setCreator(creator);
            raffleService.add(raffle);
            System.out.println("Created raffle with ID: " + raffle.getId());

            // Add a participant to the raffle
            System.out.println("\nTesting Participant Addition...");
            Participant newParticipant = new Participant(raffle, participant);
            participantService.add(newParticipant);
            System.out.println("Added participant: " + newParticipant);

            // Test Read
            System.out.println("\nTesting Raffle Retrieval...");
            Raffle retrievedRaffle = raffleService.getOne(raffle.getId());
            System.out.println("Retrieved raffle: " + retrievedRaffle);

            // Get participants for the raffle
            List<Participant> raffleParticipants = participantService.getByRaffle(retrievedRaffle);
            System.out.println("Raffle participants: " + raffleParticipants.size());
            for (Participant p : raffleParticipants) {
                System.out.println("- " + p);
            }

            // Test Update
            System.out.println("\nTesting Raffle Update...");
            retrievedRaffle.setTitle("Updated Test Raffle");
            retrievedRaffle.setStatus("completed");
            raffleService.update(retrievedRaffle);
            
            // Verify update
            Raffle updatedRaffle = raffleService.getOne(retrievedRaffle.getId());
            System.out.println("Updated raffle: " + updatedRaffle);

            // Test Get All
            System.out.println("\nTesting Get All Raffles...");
            List<Raffle> allRaffles = raffleService.getAll();
            System.out.println("Total raffles found: " + allRaffles.size());
            for (Raffle r : allRaffles) {
                System.out.println(r);
                // Show participants for each raffle
                List<Participant> participants = participantService.getByRaffle(r);
                System.out.println("  Participants: " + participants.size());
                for (Participant p : participants) {
                    System.out.println("  - " + p);
                }
            }

            // Test Delete
            System.out.println("\nTesting Raffle Deletion...");
            // First delete all participants
            for (Participant p : raffleParticipants) {
                participantService.delete(p);
            }
            // Then delete the raffle
            raffleService.delete(retrievedRaffle);
            
            // Verify deletion
            Raffle deletedRaffle = raffleService.getOne(retrievedRaffle.getId());
            if (deletedRaffle == null) {
                System.out.println("Raffle and its participants successfully deleted!");
            } else {
                System.out.println("Failed to delete raffle!");
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
