package org.esprit.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class ProfanityFilter {
    private static final String API_BASE_URL = "https://www.purgomalum.com/service/xml";

    public static String filterText(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlString = API_BASE_URL + "?text=" + encodedText;
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }

            // Parse XML response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(response.toString())));

            // Get filtered text from response
            String filteredText = document.getElementsByTagName("result").item(0).getTextContent();
            return filteredText;
        } finally {
            connection.disconnect();
        }
    }

    public static boolean containsProfanity(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String filteredText = filterText(text);
        return !text.equals(filteredText);
    }
}
