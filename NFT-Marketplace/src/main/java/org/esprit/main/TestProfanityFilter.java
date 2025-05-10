package org.esprit.main;

import org.esprit.utils.ProfanityFilter;

public class TestProfanityFilter {
    public static void main(String[] args) {
        try {
            System.out.println("========== TESTING PROFANITY FILTER ==========");

            // Test case 1: Clean text
            String cleanText = "Hello world, this is a nice day!";
            System.out.println("\nTesting clean text: " + cleanText);
            System.out.println("Contains profanity: " + ProfanityFilter.containsProfanity(cleanText));
            System.out.println("Filtered text: " + ProfanityFilter.filterText(cleanText));

            // Test case 2: Text with profanity
            String textWithProfanity = "What the hell, this stuff is damn good!";
            System.out.println("\nTesting text with profanity: " + textWithProfanity);
            System.out.println("Contains profanity: " + ProfanityFilter.containsProfanity(textWithProfanity));
            System.out.println("Filtered text: " + ProfanityFilter.filterText(textWithProfanity));

            // Test case 3: Edge cases
            System.out.println("\nTesting edge cases:");
            System.out.println("Empty string - Contains profanity: " + 
                ProfanityFilter.containsProfanity(""));
            System.out.println("Null string - Contains profanity: " + 
                ProfanityFilter.containsProfanity(null));

            System.out.println("\n========== PROFANITY FILTER TESTING COMPLETED ==========");

        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
