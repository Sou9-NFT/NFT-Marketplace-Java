package org.esprit.utils;

import org.esprit.models.Raffle;
import org.esprit.models.User;
import org.esprit.models.Artwork;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Image;
import com.itextpdf.text.BaseColor;

/**
 * Utility class for generating PDF reports for raffles.
 */
public class PdfGenerator {
    
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    /**
     * Generates a PDF report for a raffle and returns the path to the generated file.
     * 
     * @param raffle The raffle to generate a report for
     * @param artwork The artwork associated with the raffle
     * @param currentUser The current user viewing the raffle
     * @return The path to the generated PDF file (temporary file)
     * @throws Exception If there's an error generating the PDF
     */
    public static String generateRaffleReport(Raffle raffle, Artwork artwork, User currentUser) throws Exception {
        Document document = new Document();
        
        // Create unique filename in temporary directory
        String filename = System.getProperty("java.io.tmpdir") + File.separator + 
                         "raffle_" + raffle.getTitle().replace(" ", "_") + "_" + 
                         System.currentTimeMillis() + ".pdf";
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();
            
            // Add title
            Paragraph title = new Paragraph("NFT Raffle Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Add generation info
            Paragraph generationInfo = new Paragraph("Generated on: " + DATE_FORMAT.format(new Date()) + 
                                                   " by: " + currentUser.getName(), SMALL_FONT);
            generationInfo.setAlignment(Element.ALIGN_RIGHT);
            generationInfo.setSpacingAfter(20);
            document.add(generationInfo);
            
            // Add raffle details
            document.add(new Paragraph("Raffle Details", SUBTITLE_FONT));
            
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            // Add raffle information
            addTableRow(table, "Title:", raffle.getTitle());
            addTableRow(table, "Description:", raffle.getRaffleDescription());
            addTableRow(table, "Status:", raffle.getStatus());
            
            // Creator
            if (raffle.getCreator() != null) {
                addTableRow(table, "Creator:", raffle.getCreator().getName());
            }
            
            // Dates with null checks
            Date startDate = raffle.getStartTime();
            if (startDate != null) {
                addTableRow(table, "Start Date:", DATE_FORMAT.format(startDate));
            }
            
            Date endDate = raffle.getEndTime();
            if (endDate != null) {
                addTableRow(table, "End Date:", DATE_FORMAT.format(endDate));
            }
            
            // Winner if applicable
            if (raffle.getStatus().equals("ended") && raffle.getWinnerId() != null) {
                try {
                    User winner = new org.esprit.services.UserService().getOne(raffle.getWinnerId());
                    if (winner != null) {
                        addTableRow(table, "Winner:", winner.getName());
                    }
                } catch (Exception e) {
                    addTableRow(table, "Winner:", "Selected participant");
                }
            }
            
            document.add(table);
            
            // Artwork details if available
            if (artwork != null) {
                document.add(new Paragraph("Artwork Details", SUBTITLE_FONT));
                
                PdfPTable artworkTable = new PdfPTable(2);
                artworkTable.setWidthPercentage(100);
                artworkTable.setSpacingBefore(10f);
                artworkTable.setSpacingAfter(10f);
                
                addTableRow(artworkTable, "Title:", artwork.getTitle());
                addTableRow(artworkTable, "Description:", artwork.getDescription());
                
                document.add(artworkTable);
                
                // Try to add artwork image if available
                try {
                    if (artwork.getImageName() != null) {
                        File imageFile = new File("src/main/resources/uploads/" + artwork.getImageName());
                        if (imageFile.exists()) {
                            Image image = Image.getInstance(imageFile.getAbsolutePath());
                            
                            // Scale image to fit page width
                            float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                                    - document.rightMargin()) / image.getWidth()) * 100;
                            image.scalePercent(scaler);
                            
                            document.add(image);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error adding image to PDF: " + e.getMessage());
                }
            }
            
            // Participants section
            document.add(new Paragraph("Participants", SUBTITLE_FONT));
            
            if (raffle.getParticipants() != null && !raffle.getParticipants().isEmpty()) {
                PdfPTable participantsTable = new PdfPTable(2);
                participantsTable.setWidthPercentage(100);
                participantsTable.setSpacingBefore(10f);
                
                // Table headers
                PdfPCell headerCell1 = new PdfPCell(new Phrase("Name", SUBTITLE_FONT));
                headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell1.setPadding(5);
                participantsTable.addCell(headerCell1);
                
                PdfPCell headerCell2 = new PdfPCell(new Phrase("Email", SUBTITLE_FONT));
                headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell2.setPadding(5);
                participantsTable.addCell(headerCell2);
                
                // Add participants
                for (User participant : raffle.getParticipants()) {
                    PdfPCell cell1 = new PdfPCell(new Phrase(participant.getName(), NORMAL_FONT));
                    cell1.setPadding(5);
                    participantsTable.addCell(cell1);
                    
                    PdfPCell cell2 = new PdfPCell(new Phrase(participant.getEmail(), NORMAL_FONT));
                    cell2.setPadding(5);
                    participantsTable.addCell(cell2);
                }
                
                document.add(participantsTable);
            } else {
                document.add(new Paragraph("No participants have joined this raffle yet.", NORMAL_FONT));
            }
            
            // Add footer
            Paragraph footer = new Paragraph("NFT Marketplace - Generated Report", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);
            
        } catch (DocumentException e) {
            throw new Exception("Error creating PDF: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        
        return filename;
    }
    
    /**
     * Helper method to add rows to a PDF table.
     */
    private static void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, SUBTITLE_FONT));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
} 