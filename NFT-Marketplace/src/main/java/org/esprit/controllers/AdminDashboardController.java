package org.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> rolesColumn;

    @FXML
    private TableColumn<User, BigDecimal> balanceColumn;

    @FXML
    private TableColumn<User, String> createdAtColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @FXML
    private Label statusLabel;
    
    @FXML
    private Label adminNameLabel;
    
    @FXML
    private VBox userManagementSection;

    private UserService userService;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User currentAdminUser;

    public void initialize() {
        userService = new UserService();
        setupTableColumns();
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
        if (adminNameLabel != null) {
            adminNameLabel.setText("Welcome, " + user.getName());
        }
    }
    
    @FXML
    private void handleManageUsers(ActionEvent event) {
        // Show the user management section instead of reloading the whole view
        if (userManagementSection != null) {
            userManagementSection.setVisible(true);
            userManagementSection.setManaged(true);
            loadAllUsers();
        } else {
            showAlert("Error", "User management section not found in the interface");
        }
    }
    
    @FXML
    private void handleManageCategories(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryManagement.fxml"));
            Parent categoriesView = loader.load();
            
            CategoryManagementController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            navigateToView(event, categoriesView, "NFT Marketplace - Category Management");
        } catch (IOException e) {
            showAlert("Error", "Could not load category management: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleManageArtworks(ActionEvent event) {
        if (getClass().getResource("/fxml/ArtworkManagement.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArtworkManagement.fxml"));
                Parent artworkView = loader.load();
                
                if (loader.getController() instanceof ArtworkManagementController) {
                    ArtworkManagementController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, artworkView, "NFT Marketplace - Artwork Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load artwork management: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Artwork Management");
        }
    }
    
    @FXML
    private void handleManageRaffles(ActionEvent event) {
        if (getClass().getResource("/fxml/RaffleManagement.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RaffleManagement.fxml"));
                Parent raffleView = loader.load();
                
                if (loader.getController() instanceof RaffleManagementController) {
                    RaffleManagementController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, raffleView, "NFT Marketplace - Raffle Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load raffle management: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Raffle Management");
        }
    }
    
    @FXML
    private void handleManageTransactions(ActionEvent event) {
        if (getClass().getResource("/fxml/TransactionManagement.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TransactionManagement.fxml"));
                Parent transactionView = loader.load();
                
                if (loader.getController() instanceof TransactionManagementController) {
                    TransactionManagementController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, transactionView, "NFT Marketplace - Transaction Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load transaction management: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Transaction Management");
        }
    }
    
    @FXML
    private void handleAnalytics(ActionEvent event) {
        if (getClass().getResource("/fxml/Analytics.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Analytics.fxml"));
                Parent analyticsView = loader.load();
                
                if (loader.getController() instanceof AnalyticsController) {
                    AnalyticsController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, analyticsView, "NFT Marketplace - Analytics");
            } catch (IOException e) {
                showAlert("Error", "Could not load analytics: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Analytics");
        }
    }
    
    @FXML
    private void handleSettings(ActionEvent event) {
        if (getClass().getResource("/fxml/Settings.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Settings.fxml"));
                Parent settingsView = loader.load();
                
                if (loader.getController() instanceof SettingsController) {
                    SettingsController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, settingsView, "NFT Marketplace - Settings");
            } catch (IOException e) {
                showAlert("Error", "Could not load settings: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Settings");
        }
    }
    
    @FXML
    private void handleReports(ActionEvent event) {
        if (getClass().getResource("/fxml/Reports.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reports.fxml"));
                Parent reportsView = loader.load();
                
                if (loader.getController() instanceof ReportsController) {
                    ReportsController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, reportsView, "NFT Marketplace - Reports");
            } catch (IOException e) {
                showAlert("Error", "Could not load reports: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "Reports");
        }
    }
    
    @FXML
    private void handleSystemLogs(ActionEvent event) {
        if (getClass().getResource("/fxml/SystemLogs.fxml") != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SystemLogs.fxml"));
                Parent logsView = loader.load();
                
                if (loader.getController() instanceof SystemLogsController) {
                    SystemLogsController controller = loader.getController();
                    controller.setCurrentUser(currentAdminUser);
                }
                
                navigateToView(event, logsView, "NFT Marketplace - System Logs");
            } catch (IOException e) {
                showAlert("Error", "Could not load system logs: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showComingSoonView(event, "System Logs");
        }
    }
    
    private void setupTableColumns() {
        // Only set up if columns are properly injected
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        
        if (nameColumn != null) {
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        }
        
        if (emailColumn != null) {
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        }
        
        if (rolesColumn != null) {
            rolesColumn.setCellValueFactory(cellData -> {
                List<String> roles = cellData.getValue().getRoles();
                String rolesString = roles.stream().collect(Collectors.joining(", "));
                return new SimpleStringProperty(rolesString);
            });
        }
        
        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        }
        
        if (createdAtColumn != null) {
            createdAtColumn.setCellValueFactory(cellData -> {
                if (cellData.getValue().getCreatedAt() != null) {
                    String formattedDate = cellData.getValue().getCreatedAt()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    return new SimpleStringProperty(formattedDate);
                }
                return new SimpleStringProperty("N/A");
            });
        }

        if (actionsColumn != null) {
            setupActionsColumn();
        }
    }

    private void setupActionsColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox hbox = new HBox(5, editButton, deleteButton);

                    {
                        editButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                        deleteButton.setStyle("-fx-font-size: 10px; -fx-padding: 2px 5px;");
                        
                        editButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            openEditUserForm(user);
                        });
                        
                        deleteButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            confirmAndDeleteUser(user);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hbox);
                        }
                    }
                };
            }
        };
        
        actionsColumn.setCellFactory(cellFactory);
    }

    private void loadAllUsers() {
        try {
            List<User> users = userService.getAll();
            userList.clear();
            userList.addAll(users);
            userTable.setItems(userList);
        } catch (Exception e) {
            showStatus("Error loading users: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            loadAllUsers();
            return;
        }
        
        try {
            User user = userService.getByEmail(searchText);
            userList.clear();
            
            if (user != null) {
                userList.add(user);
                showStatus("User found.", false);
            } else {
                showStatus("No user found with this email.", true);
            }
            
            userTable.setItems(userList);
        } catch (Exception e) {
            showStatus("Error searching for user: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        loadAllUsers();
        showStatus("", false);
    }

    @FXML
    private void handleAddUser() {
        openAddUserForm();
    }

    private void openAddUserForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserForm.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setMode(UserFormController.FormMode.ADD);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showStatus("Error opening add user form: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void openEditUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserForm.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setMode(UserFormController.FormMode.EDIT);
            controller.setUser(user);
            controller.setParentController(this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showStatus("Error opening edit user form: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void confirmAndDeleteUser(User user) {
        if (currentAdminUser != null && user.getId() == currentAdminUser.getId()) {
            showStatus("You cannot delete your own account.", true);
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete user: " + user.getName() + " (" + user.getEmail() + ")?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user);
                loadAllUsers();
                showStatus("User deleted successfully.", false);
            } catch (Exception e) {
                showStatus("Error deleting user: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent profileView = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setUser(currentAdminUser);
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(profileView, 800, 600));
            stage.setTitle("NFT Marketplace - Profile");
            stage.show();
        } catch (IOException e) {
            showStatus("Error loading profile page: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            navigateToView(event, loginView, "NFT Marketplace - Login");
        } catch (IOException e) {
            showStatus("Error logging out: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void navigateToView(ActionEvent event, Parent view, String title) {
        Scene currentScene = ((Button) event.getSource()).getScene();
        Stage stage = (Stage) currentScene.getWindow();
        
        stage.setScene(new Scene(view));
        stage.setTitle(title);
        stage.show();
    }
    
    private void showComingSoonView(ActionEvent event, String featureName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ComingSoon.fxml"));
            
            Parent comingSoonView;
            if (loader.getLocation() == null) {
                showAlert("Coming Soon", featureName + " feature is coming soon!");
                return;
            } else {
                comingSoonView = loader.load();
                
                if (loader.getController() != null) {
                    try {
                        loader.getController().getClass().getMethod("setFeatureName", String.class)
                            .invoke(loader.getController(), featureName);
                    } catch (Exception e) {
                    }
                }
                
                navigateToView(event, comingSoonView, "NFT Marketplace - Coming Soon");
            }
        } catch (IOException e) {
            showAlert("Coming Soon", featureName + " feature is coming soon!");
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void refreshUserList() {
        loadAllUsers();
        showStatus("User list refreshed.", false);
    }

    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setVisible(!message.isEmpty());
            
            if (isError) {
                statusLabel.getStyleClass().removeAll("status-success");
                statusLabel.getStyleClass().add("status-error");
            } else {
                statusLabel.getStyleClass().removeAll("status-error");
                statusLabel.getStyleClass().add("status-success");
            }
        }
    }
}