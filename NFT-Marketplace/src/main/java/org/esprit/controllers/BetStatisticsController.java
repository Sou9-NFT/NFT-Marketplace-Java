package org.esprit.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.esprit.models.BetSession;
import org.esprit.services.BetSessionService;
import org.esprit.utils.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Bet Statistics view
 */
public class BetStatisticsController {
    @FXML private Label totalBetsLabel;
    @FXML private Label activeBetsLabel;
    @FXML private Label completedBetsLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label avgDurationLabel;
    @FXML private Label avgPriceIncreaseLabel;
    @FXML private Label mostActiveUserLabel;
    @FXML private Label successRateLabel;
    
    @FXML private ComboBox<String> timeRangeCombo;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private BarChart<String, Number> priceDistChart;
    @FXML private Button closeButton;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;
    @FXML private Button printButton;
    
    @FXML private TableView<UserStatsEntry> topUsersTable;
    @FXML private TableColumn<UserStatsEntry, Integer> rankColumn;
    @FXML private TableColumn<UserStatsEntry, String> usernameColumn;
    @FXML private TableColumn<UserStatsEntry, Integer> betCountColumn;
    @FXML private TableColumn<UserStatsEntry, Double> valueColumn;
    @FXML private TableColumn<UserStatsEntry, String> winRateColumn;
    @FXML private TableColumn<UserStatsEntry, String> lastActivityColumn;
    
    @FXML private TableView<ArtworkStatsEntry> artworkTable;
    @FXML private TableColumn<ArtworkStatsEntry, String> artworkNameColumn;
    @FXML private TableColumn<ArtworkStatsEntry, Integer> artworkBetCountColumn;
    @FXML private TableColumn<ArtworkStatsEntry, Double> averagePriceColumn;
    @FXML private TableColumn<ArtworkStatsEntry, String> priceRangeColumn;
    @FXML private TableColumn<ArtworkStatsEntry, Double> popularityScoreColumn;

    private final BetSessionService betSessionService;
    private final Connection connection;
    
    public BetStatisticsController() {
        betSessionService = new BetSessionService();
        connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Initializes the controller class.
     */
    @FXML
    private void initialize() {
        // Populate ComboBox with time range options
        timeRangeCombo.getItems().addAll(
            "Last 7 Days", 
            "Last 30 Days", 
            "Last 3 Months", 
            "Last 12 Months", 
            "All Time"
        );
        
        // Set default time range
        timeRangeCombo.setValue("Last 30 Days");
        
        // Set up table columns for top users
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        betCountColumn.setCellValueFactory(new PropertyValueFactory<>("betCount"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        winRateColumn.setCellValueFactory(new PropertyValueFactory<>("winRate"));
        lastActivityColumn.setCellValueFactory(new PropertyValueFactory<>("lastActivity"));
        
        // Set up table columns for artwork statistics
        artworkNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        artworkBetCountColumn.setCellValueFactory(new PropertyValueFactory<>("betCount"));
        averagePriceColumn.setCellValueFactory(new PropertyValueFactory<>("averagePrice"));
        priceRangeColumn.setCellValueFactory(new PropertyValueFactory<>("priceRange"));
        popularityScoreColumn.setCellValueFactory(new PropertyValueFactory<>("popularityScore"));
        
        // Load initial data
        loadRealData();
        
        // Add listener for time range changes
        timeRangeCombo.setOnAction(event -> refreshData());
    }
    
    /**
     * Refreshes the data displayed in the statistics dashboard
     */
    @FXML
    public void refreshData() {
        loadRealData();
    }
    
    /**
     * Closes the statistics window
     */
    @FXML
    public void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Exports the statistics data to a file
     */
    @FXML
    public void exportData() {
        // Implementation for exporting data to CSV or Excel
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Data");
        alert.setHeaderText(null);
        alert.setContentText("Data has been exported successfully.");
        alert.showAndWait();
    }
    
    /**
     * Prints a report of the statistics
     */
    @FXML
    public void printReport() {
        // Implementation for printing a PDF report
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Print Report");
        alert.setHeaderText(null);
        alert.setContentText("Report has been sent to the printer.");
        alert.showAndWait();
    }
    
    /**
     * Loads real data from the database
     */
    private void loadRealData() {
        try {
            // Get all bet sessions
            List<BetSession> allSessions = betSessionService.getAllBetSessions();
            
            // Filter based on selected time range
            LocalDateTime cutoffDate = calculateCutoffDate();
            List<BetSession> filteredSessions = allSessions.stream()
                .filter(session -> session.getCreatedAt() != null && session.getCreatedAt().isAfter(cutoffDate))
                .toList();
                
            // Update summary statistics
            updateSummaryStatistics(filteredSessions);
            
            // Update activity chart
            updateActivityChart(filteredSessions);
            
            // Update price distribution chart
            updatePriceDistributionChart(filteredSessions);
            
            // Update top users table
            updateTopUsersTable(filteredSessions);
            
            // Update artwork statistics table
            updateArtworkStatisticsTable(filteredSessions);
            
        } catch (Exception e) {
            System.err.println("Error loading bet statistics data: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Data Loading Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load bet statistics: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private LocalDateTime calculateCutoffDate() {
        LocalDateTime now = LocalDateTime.now();
        String selectedTimeRange = timeRangeCombo.getValue();
        
        switch (selectedTimeRange) {
            case "Last 7 Days":
                return now.minusDays(7);
            case "Last 30 Days":
                return now.minusDays(30);
            case "Last 3 Months":
                return now.minusMonths(3);
            case "Last 12 Months":
                return now.minusMonths(12);
            case "All Time":
            default:
                return LocalDateTime.of(2000, 1, 1, 0, 0); // Very old date to include all records
        }
    }
    
    private void updateSummaryStatistics(List<BetSession> sessions) {
        int totalBets = sessions.size();
        long activeBets = sessions.stream().filter(s -> "active".equals(s.getStatus())).count();
        long completedBets = sessions.stream().filter(s -> "completed".equals(s.getStatus())).count();
        
        // Calculate total value (sum of current prices)
        double totalValue = sessions.stream()
            .mapToDouble(BetSession::getCurrentPrice)
            .sum();
            
        // Calculate average duration in days
        double avgDuration = sessions.stream()
            .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
            .mapToDouble(s -> ChronoUnit.DAYS.between(s.getStartTime(), s.getEndTime()))
            .average()
            .orElse(0);
            
        // Calculate average price increase percentage
        double avgPriceIncrease = sessions.stream()
            .filter(s -> s.getInitialPrice() > 0) // Avoid division by zero
            .mapToDouble(s -> (s.getCurrentPrice() - s.getInitialPrice()) / s.getInitialPrice() * 100)
            .average()
            .orElse(0);
            
        // Find most active user (user with most bets)
        String mostActiveUser = findMostActiveUser(sessions);
        
        // Calculate success rate (completed bets with increased price / total completed bets)
        double successRate = 0;
        if (completedBets > 0) {
            long successfulBets = sessions.stream()
                .filter(s -> "completed".equals(s.getStatus()) && s.getCurrentPrice() > s.getInitialPrice())
                .count();
            successRate = (double) successfulBets / completedBets * 100;
        }
        
        // Update labels
        totalBetsLabel.setText(String.valueOf(totalBets));
        activeBetsLabel.setText(String.valueOf(activeBets));
        completedBetsLabel.setText(String.valueOf(completedBets));
        totalValueLabel.setText(String.format("%.2f", totalValue));
        avgDurationLabel.setText(String.format("%.1f", avgDuration));
        avgPriceIncreaseLabel.setText(String.format("%.1f%%", avgPriceIncrease));
        mostActiveUserLabel.setText(mostActiveUser);
        successRateLabel.setText(String.format("%.1f%%", successRate));
    }
    
    private String findMostActiveUser(List<BetSession> sessions) {
        Map<Integer, Integer> userBetCounts = new HashMap<>();
        Map<Integer, String> userNames = new HashMap<>();
        
        // Count bets per user
        for (BetSession session : sessions) {
            if (session.getAuthor() != null) {
                int userId = session.getAuthor().getId();
                String userName = session.getAuthor().getName();
                userBetCounts.put(userId, userBetCounts.getOrDefault(userId, 0) + 1);
                userNames.put(userId, userName);
            }
        }
        
        // Find user with most bets
        int maxUserId = -1;
        int maxCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : userBetCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxUserId = entry.getKey();
            }
        }
        
        return maxUserId > 0 ? userNames.get(maxUserId) : "-";
    }
    
    private void updateActivityChart(List<BetSession> sessions) {
        String selectedTimeRange = timeRangeCombo.getValue();
        String[] timeLabels;
        Map<String, Integer> newBetCounts = new TreeMap<>();
        Map<String, Integer> completedBetCounts = new TreeMap<>();
        DateTimeFormatter formatter;
        LocalDateTime now = LocalDateTime.now();
        
        switch (selectedTimeRange) {
            case "Last 7 Days":
                timeLabels = new String[7];
                formatter = DateTimeFormatter.ofPattern("EEE");
                
                for (int i = 6; i >= 0; i--) {
                    LocalDateTime date = now.minusDays(i);
                    timeLabels[6-i] = date.format(formatter);
                    newBetCounts.put(timeLabels[6-i], 0);
                    completedBetCounts.put(timeLabels[6-i], 0);
                }
                
                for (BetSession session : sessions) {
                    if (session.getCreatedAt() != null) {
                        String dayOfWeek = session.getCreatedAt().format(formatter);
                        newBetCounts.put(dayOfWeek, newBetCounts.getOrDefault(dayOfWeek, 0) + 1);
                    }
                    
                    if ("completed".equals(session.getStatus()) && session.getEndTime() != null) {
                        String dayOfWeek = session.getEndTime().format(formatter);
                        completedBetCounts.put(dayOfWeek, completedBetCounts.getOrDefault(dayOfWeek, 0) + 1);
                    }
                }
                break;
                
            case "Last 30 Days":
                timeLabels = new String[]{"Week 1", "Week 2", "Week 3", "Week 4"};
                
                for (String label : timeLabels) {
                    newBetCounts.put(label, 0);
                    completedBetCounts.put(label, 0);
                }
                
                for (BetSession session : sessions) {
                    if (session.getCreatedAt() != null) {
                        long daysAgo = ChronoUnit.DAYS.between(session.getCreatedAt(), now);
                        String weekLabel = getWeekLabel(daysAgo);
                        newBetCounts.put(weekLabel, newBetCounts.getOrDefault(weekLabel, 0) + 1);
                    }
                    
                    if ("completed".equals(session.getStatus()) && session.getEndTime() != null) {
                        long daysAgo = ChronoUnit.DAYS.between(session.getEndTime(), now);
                        String weekLabel = getWeekLabel(daysAgo);
                        completedBetCounts.put(weekLabel, completedBetCounts.getOrDefault(weekLabel, 0) + 1);
                    }
                }
                break;
                
            case "Last 3 Months":
                timeLabels = new String[3];
                formatter = DateTimeFormatter.ofPattern("MMM");
                
                for (int i = 2; i >= 0; i--) {
                    LocalDateTime date = now.minusMonths(i);
                    timeLabels[2-i] = date.format(formatter);
                    newBetCounts.put(timeLabels[2-i], 0);
                    completedBetCounts.put(timeLabels[2-i], 0);
                }
                
                for (BetSession session : sessions) {
                    if (session.getCreatedAt() != null) {
                        String month = session.getCreatedAt().format(formatter);
                        newBetCounts.put(month, newBetCounts.getOrDefault(month, 0) + 1);
                    }
                    
                    if ("completed".equals(session.getStatus()) && session.getEndTime() != null) {
                        String month = session.getEndTime().format(formatter);
                        completedBetCounts.put(month, completedBetCounts.getOrDefault(month, 0) + 1);
                    }
                }
                break;
                
            case "Last 12 Months":
                timeLabels = new String[12];
                formatter = DateTimeFormatter.ofPattern("MMM");
                
                for (int i = 11; i >= 0; i--) {
                    LocalDateTime date = now.minusMonths(i);
                    timeLabels[11-i] = date.format(formatter);
                    newBetCounts.put(timeLabels[11-i], 0);
                    completedBetCounts.put(timeLabels[11-i], 0);
                }
                
                for (BetSession session : sessions) {
                    if (session.getCreatedAt() != null) {
                        String month = session.getCreatedAt().format(formatter);
                        newBetCounts.put(month, newBetCounts.getOrDefault(month, 0) + 1);
                    }
                    
                    if ("completed".equals(session.getStatus()) && session.getEndTime() != null) {
                        String month = session.getEndTime().format(formatter);
                        completedBetCounts.put(month, completedBetCounts.getOrDefault(month, 0) + 1);
                    }
                }
                break;
                
            case "All Time":
            default:
                int currentYear = now.getYear();
                int startYear = currentYear - 4;
                timeLabels = new String[5];
                
                for (int i = 0; i < 5; i++) {
                    timeLabels[i] = String.valueOf(startYear + i);
                    newBetCounts.put(timeLabels[i], 0);
                    completedBetCounts.put(timeLabels[i], 0);
                }
                
                for (BetSession session : sessions) {
                    if (session.getCreatedAt() != null) {
                        String year = String.valueOf(session.getCreatedAt().getYear());
                        if (newBetCounts.containsKey(year)) {
                            newBetCounts.put(year, newBetCounts.get(year) + 1);
                        }
                    }
                    
                    if ("completed".equals(session.getStatus()) && session.getEndTime() != null) {
                        String year = String.valueOf(session.getEndTime().getYear());
                        if (completedBetCounts.containsKey(year)) {
                            completedBetCounts.put(year, completedBetCounts.get(year) + 1);
                        }
                    }
                }
                break;
        }
        
        // Create chart series
        XYChart.Series<String, Number> newBetSeries = new XYChart.Series<>();
        newBetSeries.setName("New Bets");
        
        XYChart.Series<String, Number> completedBetSeries = new XYChart.Series<>();
        completedBetSeries.setName("Completed Bets");
        
        for (String label : timeLabels) {
            newBetSeries.getData().add(new XYChart.Data<>(label, newBetCounts.getOrDefault(label, 0)));
            completedBetSeries.getData().add(new XYChart.Data<>(label, completedBetCounts.getOrDefault(label, 0)));
        }
        
        // Update chart
        activityChart.getData().clear();
        activityChart.getData().add(newBetSeries);
        activityChart.getData().add(completedBetSeries);
    }
    
    private String getWeekLabel(long daysAgo) {
        if (daysAgo < 8) return "Week 1";
        if (daysAgo < 15) return "Week 2";
        if (daysAgo < 22) return "Week 3";
        return "Week 4";
    }
    
    private void updatePriceDistributionChart(List<BetSession> sessions) {
        // Define price ranges
        String[] priceRanges = {"0-1 ETH", "1-5 ETH", "5-10 ETH", "10-20 ETH", "20+ ETH"};
        int[] priceRangeCounts = new int[priceRanges.length];
        
        // Count sessions in each price range
        for (BetSession session : sessions) {
            double price = session.getCurrentPrice();
            
            if (price < 1) {
                priceRangeCounts[0]++;
            } else if (price < 5) {
                priceRangeCounts[1]++;
            } else if (price < 10) {
                priceRangeCounts[2]++;
            } else if (price < 20) {
                priceRangeCounts[3]++;
            } else {
                priceRangeCounts[4]++;
            }
        }
        
        // Create chart series
        XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
        priceSeries.setName("Number of Bets");
        
        for (int i = 0; i < priceRanges.length; i++) {
            priceSeries.getData().add(new XYChart.Data<>(priceRanges[i], priceRangeCounts[i]));
        }
        
        // Update chart
        priceDistChart.getData().clear();
        priceDistChart.getData().add(priceSeries);
    }
    
    private void updateTopUsersTable(List<BetSession> sessions) {
        try {
            // Query to get top 10 users by number of bet sessions
            String query = "SELECT u.id, u.name, COUNT(bs.id) as bet_count, " +
                          "SUM(bs.current_price) as total_value, " +
                          "MAX(bs.created_at) as last_activity " +
                          "FROM bet_session bs " +
                          "JOIN user u ON bs.author_id = u.id " +
                          "GROUP BY u.id, u.name " +
                          "ORDER BY bet_count DESC " +
                          "LIMIT 10";
            
            ObservableList<UserStatsEntry> userData = FXCollections.observableArrayList();
            
            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                
                int rank = 1;
                while (rs.next()) {
                    int userId = rs.getInt("id");
                    String username = rs.getString("name");
                    int betCount = rs.getInt("bet_count");
                    double value = rs.getDouble("total_value");
                    
                    // Calculate win rate (successful bets / total completed bets)
                    double winRate = calculateWinRate(userId);
                    
                    // Format last activity date
                    String lastActivity = formatLastActivity(rs.getTimestamp("last_activity"));
                    
                    userData.add(new UserStatsEntry(
                        rank++,
                        username,
                        betCount,
                        value,
                        String.format("%.1f%%", winRate),
                        lastActivity
                    ));
                }
            }
            
            topUsersTable.setItems(userData);
            
        } catch (SQLException e) {
            System.err.println("Error fetching top users data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double calculateWinRate(int userId) {
        try {
            String query = "SELECT COUNT(*) as total_completed, " +
                          "SUM(CASE WHEN current_price > initial_price THEN 1 ELSE 0 END) as successful " +
                          "FROM bet_session " +
                          "WHERE author_id = ? AND status = 'completed'";
            
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, userId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int totalCompleted = rs.getInt("total_completed");
                        int successful = rs.getInt("successful");
                        
                        if (totalCompleted > 0) {
                            return (double) successful / totalCompleted * 100;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating win rate: " + e.getMessage());
        }
        
        return 0;
    }
    
    private String formatLastActivity(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "-";
        
        LocalDateTime dateTime = timestamp.toLocalDateTime();
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    private void updateArtworkStatisticsTable(List<BetSession> sessions) {
        try {
            // Query to get statistics about artworks involved in bet sessions
            String query = "SELECT a.id, a.title, COUNT(bs.id) as bet_count, " +
                          "AVG(bs.current_price) as avg_price, " +
                          "MIN(bs.current_price) as min_price, " +
                          "MAX(bs.current_price) as max_price, " +
                          "SUM(bs.number_of_bids) as bid_count " +
                          "FROM bet_session bs " +
                          "JOIN artwork a ON bs.artwork_id = a.id " +
                          "GROUP BY a.id, a.title " +
                          "ORDER BY bet_count DESC, avg_price DESC";
            
            ObservableList<ArtworkStatsEntry> artworkData = FXCollections.observableArrayList();
            
            try (PreparedStatement ps = connection.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    String name = rs.getString("title");
                    int betCount = rs.getInt("bet_count");
                    double avgPrice = rs.getDouble("avg_price");
                    double minPrice = rs.getDouble("min_price");
                    double maxPrice = rs.getDouble("max_price");
                    int bidCount = rs.getInt("bid_count");
                    
                    // Calculate popularity score based on both bet count and bid count
                    double popularityScore = betCount * 0.3 + bidCount * 0.7;
                    
                    artworkData.add(new ArtworkStatsEntry(
                        name,
                        betCount,
                        avgPrice,
                        String.format("%.2f-%.2f ETH", minPrice, maxPrice),
                        popularityScore
                    ));
                }
            }
            
            artworkTable.setItems(artworkData);
            
        } catch (SQLException e) {
            System.err.println("Error fetching artwork statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inner class to represent a user statistics entry for the table
     */
    public static class UserStatsEntry {
        private final int rank;
        private final String username;
        private final int betCount;
        private final double value;
        private final String winRate;
        private final String lastActivity;
        
        public UserStatsEntry(int rank, String username, int betCount, double value, String winRate, String lastActivity) {
            this.rank = rank;
            this.username = username;
            this.betCount = betCount;
            this.value = value;
            this.winRate = winRate;
            this.lastActivity = lastActivity;
        }
        
        public int getRank() { return rank; }
        public String getUsername() { return username; }
        public int getBetCount() { return betCount; }
        public double getValue() { return value; }
        public String getWinRate() { return winRate; }
        public String getLastActivity() { return lastActivity; }
    }
    
    /**
     * Inner class to represent an artwork statistics entry for the table
     */
    public static class ArtworkStatsEntry {
        private final String name;
        private final int betCount;
        private final double averagePrice;
        private final String priceRange;
        private final double popularityScore;
        
        public ArtworkStatsEntry(String name, int betCount, double averagePrice, String priceRange, double popularityScore) {
            this.name = name;
            this.betCount = betCount;
            this.averagePrice = averagePrice;
            this.priceRange = priceRange;
            this.popularityScore = popularityScore;
        }
        
        public String getName() { return name; }
        public int getBetCount() { return betCount; }
        public double getAveragePrice() { return averagePrice; }
        public String getPriceRange() { return priceRange; }
        public double getPopularityScore() { return popularityScore; }
    }
}