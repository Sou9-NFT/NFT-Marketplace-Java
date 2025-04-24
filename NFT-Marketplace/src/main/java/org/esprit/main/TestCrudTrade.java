package org.esprit.main;

import org.esprit.models.*;
import java.time.LocalDateTime;

public class TestCrudTrade {
    public static void main(String[] args) {
        // Create users
        User sender = new User();
        sender.setId(1);
        sender.setName("Alice");

        User receiver = new User();
        receiver.setId(2);
        receiver.setName("Bob");

        // Create artworks
        Artwork offeredItem = new Artwork();
        offeredItem.setId(101);
        offeredItem.setTitle("Mona Lisa");

        Artwork receivedItem = new Artwork();
        receivedItem.setId(202);
        receivedItem.setTitle("Starry Night");

        // Create trade offer
        TradeOffer tradeOffer = new TradeOffer();
        tradeOffer.setId(1);
        tradeOffer.setSender(sender);
        tradeOffer.setReceiverName(receiver);
        tradeOffer.setOfferedItem(offeredItem);
        tradeOffer.setReceivedItem(receivedItem);
        tradeOffer.setDescription("Trade Mona Lisa for Starry Night");
        tradeOffer.setCreationDate(LocalDateTime.now());

        // Print trade offer details
        System.out.println(tradeOffer);

        // Check if trade offer has disputes
        //System.out.println("Has disputes: " + tradeOffer.hasDisputes());
    }
}
