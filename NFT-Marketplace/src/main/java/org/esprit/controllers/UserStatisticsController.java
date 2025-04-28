package org.esprit.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.esprit.models.User;
import org.esprit.services.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

public class UserStatisticsController implements Initializable {

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label githubUsersLabel;

    @FXML
    private Label activeUsersLabel;

    @FXML
    private Label newUsersLabel;

    @FXML
    private PieChart userRolesChart;

    @FXML
    private BarChart<String, Number> userRegistrationChart;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private TextField searchTextField;

    @FXML
    private TableView<User> userStatsTable;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> rolesColumn;

    @FXML
    private TableColumn<User, String> githubConnectedColumn;

    @FXML
    private TableColumn<User, String> createdAtColumn;

    @FXML
    private TableColumn<User, String> lastLoginColumn;

    private UserService userService;
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUserList;
    // currentAdminUser is kept for future implementation
    private User currentAdminUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userService = new UserService();
        setupTableColumns();
        setupFilterComboBox();
        loadUserData();
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        rolesColumn.setCellValueFactory(cellData -> {
            List<String> roles = cellData.getValue().getRoles();
            String rolesString = roles.stream().collect(Collectors.joining(", "));
            return new SimpleStringProperty(rolesString);
        });
        
        githubConnectedColumn.setCellValueFactory(cellData -> {
            String githubUsername = cellData.getValue().getGithubUsername();
            return new SimpleStringProperty(githubUsername != null && !githubUsername.isEmpty() ? "✓" : "");
        });
        
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                String formattedDate = cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return new SimpleStringProperty(formattedDate);
            }
            return new SimpleStringProperty("N/A");
        });
        
        lastLoginColumn.setCellValueFactory(cellData -> {
            // Since the User class doesn't have getLastLogin(), we'll use createdAt as a placeholder
            // This can be updated when a lastLogin field is added to the User class
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            if (createdAt != null) {
                String formattedDate = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                return new SimpleStringProperty(formattedDate);
            }
            return new SimpleStringProperty("N/A");
        });
    }

    private void setupFilterComboBox() {
        ObservableList<String> filterOptions = FXCollections.observableArrayList(
                "All Users", 
                "GitHub Connected", 
                "Admin Users", 
                "Regular Users",
                "New Users (This Month)"
        );
        filterComboBox.setItems(filterOptions);
        filterComboBox.setValue("All Users");
        
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters(newValue, searchTextField.getText());
        });
    }

    private void applyFilters(String filterOption, String searchText) {
        filteredUserList.setPredicate(user -> {
            boolean matchesSearch = true;
            boolean matchesFilter = true;
            
            // Apply search filter
            if (searchText != null && !searchText.isEmpty()) {
                matchesSearch = user.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(searchText.toLowerCase());
            }
            
            // Apply category filter
            if (filterOption != null) {
                switch (filterOption) {
                    case "GitHub Connected":
                        matchesFilter = user.getGithubUsername() != null && !user.getGithubUsername().isEmpty();
                        break;
                    case "Admin Users":
                        matchesFilter = user.getRoles().contains("ADMIN");
                        break;
                    case "Regular Users":
                        matchesFilter = !user.getRoles().contains("ADMIN");
                        break;
                    case "New Users (This Month)":
                        matchesFilter = user.getCreatedAt() != null && 
                                user.getCreatedAt().getMonth() == LocalDate.now().getMonth() &&
                                user.getCreatedAt().getYear() == LocalDate.now().getYear();
                        break;
                    case "All Users":
                    default:
                        matchesFilter = true;
                        break;
                }
            }
            
            return matchesSearch && matchesFilter;
        });
    }

    @FXML
    private void handleRefreshData() {
        loadUserData();
        showAlert(Alert.AlertType.INFORMATION, "Refresh Complete", "User statistics have been refreshed.");
    }

    @FXML
    private void handleSearch() {
        String searchText = searchTextField.getText();
        String filterOption = filterComboBox.getValue();
        applyFilters(filterOption, searchText);
    }

    @FXML
    private void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save User Statistics");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.append("Name,Email,Roles,GitHub Connected,Joined Date\n");
                
                // Write data for filtered users
                for (User user : filteredUserList) {
                    writer.append(user.getName()).append(",");
                    writer.append(user.getEmail()).append(",");
                    writer.append(user.getRoles().stream().collect(Collectors.joining("; "))).append(",");
                    writer.append(user.getGithubUsername() != null && !user.getGithubUsername().isEmpty() ? "Yes" : "No").append(",");
                    writer.append(user.getCreatedAt() != null ? 
                            user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A").append("\n");
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                        "User statistics have been exported to " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Error", 
                        "Failed to export user statistics: " + e.getMessage());
            }
        }
    }

    private void loadUserData() {
        try {
            List<User> users = userService.getAll();
            userList.clear();
            userList.addAll(users);
            
            filteredUserList = new FilteredList<>(userList, p -> true);
            userStatsTable.setItems(filteredUserList);
            
            // Update key metrics
            updateKeyMetrics(users);
            
            // Update charts
            updateRolesDistributionChart(users);
            updateRegistrationTrendChart(users);
            
            // Apply any existing filters
            applyFilters(filterComboBox.getValue(), searchTextField.getText());
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to load user data: " + e.getMessage());
        }
    }

    private void updateKeyMetrics(List<User> users) {
        // Total users count
        int totalUsers = users.size();
        totalUsersLabel.setText(String.valueOf(totalUsers));
        
        // GitHub connected users - users that have a GitHub username
        long githubUsers = users.stream()
                .filter(user -> user.getGithubUsername() != null && !user.getGithubUsername().isEmpty())
                .count();
        githubUsersLabel.setText(String.valueOf(githubUsers));
        
        // Active users - since we don't have last login, we'll count users created in the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();
        activeUsersLabel.setText(String.valueOf(activeUsers));
        
        // New users this month
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        long newUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null && 
                        user.getCreatedAt().isAfter(firstDayOfMonth.atStartOfDay()))
                .count();
        newUsersLabel.setText(String.valueOf(newUsers));
    }

    private void updateRolesDistributionChart(List<User> users) {
        Map<String, Integer> roleDistribution = new HashMap<>();
        
        // Count users by role
        for (User user : users) {
            for (String role : user.getRoles()) {
                roleDistribution.put(role, roleDistribution.getOrDefault(role, 0) + 1);
            }
        }
        
        // Create chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : roleDistribution.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        
        userRolesChart.setData(pieChartData);
        userRolesChart.setTitle("User Roles Distribution");
    }

    private void updateRegistrationTrendChart(List<User> users) {
        // Group users by month
        Map<Month, Integer> usersByMonth = new HashMap<>();
        
        // Initialize all months
        for (Month month : Month.values()) {
            usersByMonth.put(month, 0);
        }
        
        // Count users by registration month for the current year
        int currentYear = LocalDate.now().getYear();
        for (User user : users) {
            if (user.getCreatedAt() != null && user.getCreatedAt().getYear() == currentYear) {
                Month month = user.getCreatedAt().getMonth();
                usersByMonth.put(month, usersByMonth.getOrDefault(month, 0) + 1);
            }
        }
        
        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("User Registrations in " + currentYear);
        
        // Add data points for each month
        for (Month month : Month.values()) {
            series.getData().add(new XYChart.Data<>(month.toString(), usersByMonth.get(month)));
        }
        
        userRegistrationChart.getData().clear();
        userRegistrationChart.getData().add(series);
        userRegistrationChart.setTitle("User Registration Trend (" + currentYear + ")");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}