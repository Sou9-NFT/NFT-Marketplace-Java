package org.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import org.esprit.services.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminDashboardController implements Initializable {

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
    
    // New UI components for sidebar layout
    @FXML
    private VBox welcomeScreen;
    
    @FXML
    private ScrollPane userManagementSection;
    
    @FXML
    private ScrollPane blogManagementSection;
    
    @FXML
    private ScrollPane contentArea;

    // Blog Management
    @FXML private ListView<Blog> blogListView;
    @FXML private TextField blogTitleField;
    @FXML private TextArea blogContentArea;
    @FXML private ImageView blogImageView;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button saveBlogButton;
    @FXML private Button deleteBlogButton;
    @FXML private Button translateButton;
    
    private UserService userService;
    private BlogService blogService;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUserList;
    private User currentAdminUser;
    private Blog currentBlog;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    private final ObservableList<String> languages = FXCollections.observableArrayList(
        "French", "Spanish", "German", "Italian", "Arabic"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize user management
        userService = new UserService();
        setupTableColumns();
        loadAllUsers();
        
        // Setup real-time search filtering
        setupSearchFilter();

        // Initialize blog management only if UI components are available
        blogService = new BlogService();
        
        // Check if blog UI components exist before using them
        if (languageComboBox != null) {
            languageComboBox.setItems(languages);
            
            // Initialize blog list view if it exists
            if (blogListView != null) {
                refreshBlogList();
                
                // Add selection listener to the blog list view
                blogListView.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            loadBlogDetails(newSelection);
                        }
                    }
                );
            }
        }
        
        // Initially show the welcome screen
        showWelcomeScreen();
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
        if (adminNameLabel != null) {
            adminNameLabel.setText("Welcome, " + user.getName());
        }
    }
    
    /**
     * Helper method to hide all content sections
     */
    private void hideAllSections() {
        if (welcomeScreen != null) welcomeScreen.setVisible(false);
        if (welcomeScreen != null) welcomeScreen.setManaged(false);
        if (userManagementSection != null) userManagementSection.setVisible(false);
        if (userManagementSection != null) userManagementSection.setManaged(false);
        if (blogManagementSection != null) blogManagementSection.setVisible(false);
        if (blogManagementSection != null) blogManagementSection.setManaged(false);
        if (contentArea != null) contentArea.setVisible(false);
        if (contentArea != null) contentArea.setManaged(false);
    }
    
    /**
     * Show the welcome screen
     */
    private void showWelcomeScreen() {
        hideAllSections();
        if (welcomeScreen != null) {
            welcomeScreen.setVisible(true);
            welcomeScreen.setManaged(true);
        }
        
        // Update window title - safely check if the scene and window exist
        if (adminNameLabel != null && adminNameLabel.getScene() != null && adminNameLabel.getScene().getWindow() != null) {
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.setTitle("NFT Marketplace - Admin Dashboard");
        }
    }
    
    /**
     * Show the user management section
     */
    private void showUserManagement() {
        hideAllSections();
        if (userManagementSection != null) {
            userManagementSection.setVisible(true);
            userManagementSection.setManaged(true);
            loadAllUsers();  // Refresh user list
            
            // Update window title - safely check if the scene and window exist
            if (adminNameLabel != null && adminNameLabel.getScene() != null && adminNameLabel.getScene().getWindow() != null) {
                Stage stage = (Stage) adminNameLabel.getScene().getWindow();
                stage.setTitle("NFT Marketplace - User Management");
            }
        }
    }
    
    /**
     * Show the blog management section
     */
    private void showBlogManagement() {
        hideAllSections();
        if (blogManagementSection != null) {
            blogManagementSection.setVisible(true);
            blogManagementSection.setManaged(true);
            refreshBlogList();  // Refresh blog list
            
            // Update window title - safely check if the scene and window exist
            if (adminNameLabel != null && adminNameLabel.getScene() != null && adminNameLabel.getScene().getWindow() != null) {
                Stage stage = (Stage) adminNameLabel.getScene().getWindow();
                stage.setTitle("NFT Marketplace - Blog Management");
            }
        }
    }
    
    /**
     * Load content into the dynamic content area
     */
    private void loadContentInPlace(Parent content, String title) {
        hideAllSections();
        if (contentArea != null) {
            // Clear existing content and set new content
            VBox container = (VBox) contentArea.getContent();
            if (container != null) {
                container.getChildren().clear();
                container.getChildren().add(content);
            }
            
            contentArea.setVisible(true);
            contentArea.setManaged(true);
            
            // Update window title - safely check if the scene and window exist
            if (adminNameLabel != null && adminNameLabel.getScene() != null && adminNameLabel.getScene().getWindow() != null) {
                Stage stage = (Stage) adminNameLabel.getScene().getWindow();
                stage.setTitle("NFT Marketplace - " + title);
            }
        }
    }
    
    @FXML
    private void handleManageUsers(ActionEvent event) {
        showUserManagement();
    }
    
    @FXML
    private void handleManageCategories(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryManagement.fxml"));
            Parent categoriesView = loader.load();
            
            CategoryManagementController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            loadContentInPlace(categoriesView, "Category Management");
        } catch (IOException e) {
            showAlert("Error", "Could not load category management: " + e.getMessage());
            System.err.println("Error in handleManageCategories: " + e.getMessage());
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
                    // Set the current user and indicate that we're coming from admin dashboard
                    controller.setCurrentUser(currentAdminUser, true);
                }
                
                loadContentInPlace(artworkView, "Artwork Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load artwork management: " + e.getMessage());
                System.err.println("Error in handleManageArtworks: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Artwork Management");
        }
    }
    
    @FXML
    private void handleManageBlog(ActionEvent event) {
        // If we have our own blog management UI integrated in the dashboard
        if (blogManagementSection != null) {
            showBlogManagement();
        } else {
            // Otherwise load the standalone blog management
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BlogManagement.fxml"));
                Parent blogView = loader.load();
                
                BlogController controller = loader.getController();
                controller.setAdminMode(true); // Enable admin-specific features
                controller.setCurrentUser(currentAdminUser);
                
                loadContentInPlace(blogView, "Blog Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load blog management: " + e.getMessage());
                System.err.println("Error in handleManageBlog: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleManageRaffles(ActionEvent event) {
        try {
            URL resourceUrl = getClass().getResource("/fxml/RaffleManagement.fxml");
            
            if (resourceUrl == null) {
                showComingSoonInPlace("Raffle Management");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent raffleView = loader.load();
            
            RaffleManagementController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            loadContentInPlace(raffleView, "Raffle Management");
        } catch (IOException e) {
            System.err.println("ERROR in handleManageRaffles: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load raffle management: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR in handleManageRaffles: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unexpected error loading raffle management: " + e.getMessage());
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
                
                loadContentInPlace(transactionView, "Transaction Management");
            } catch (IOException e) {
                showAlert("Error", "Could not load transaction management: " + e.getMessage());
                System.err.println("Error in handleManageTransactions: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Transaction Management");
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
                
                loadContentInPlace(analyticsView, "Analytics");
            } catch (IOException e) {
                showAlert("Error", "Could not load analytics: " + e.getMessage());
                System.err.println("Error in handleAnalytics: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Analytics");
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
                
                loadContentInPlace(settingsView, "Settings");
            } catch (IOException e) {
                showAlert("Error", "Could not load settings: " + e.getMessage());
                System.err.println("Error in handleSettings: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Settings");
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
                
                loadContentInPlace(reportsView, "Reports");
            } catch (IOException e) {
                showAlert("Error", "Could not load reports: " + e.getMessage());
                System.err.println("Error in handleReports: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("Reports");
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
                
                loadContentInPlace(logsView, "System Logs");
            } catch (IOException e) {
                showAlert("Error", "Could not load system logs: " + e.getMessage());
                System.err.println("Error in handleSystemLogs: " + e.getMessage());
            }
        } else {
            showComingSoonInPlace("System Logs");
        }
    }
    
    @FXML
    private void handleBetSessions(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BetSession.fxml"));
            Parent betSessionView = loader.load();
            
            // If there's a controller with setCurrentUser method, we can set the admin as a user
            BetSessionController controller = loader.getController();
            if (controller != null && currentAdminUser != null) {
                controller.setCurrentUser(currentAdminUser);
            }
            
            loadContentInPlace(betSessionView, "Bet Sessions Management");
        } catch (IOException e) {
            System.err.println("Error in handleBetSessions: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Bet Sessions interface: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleTradeDisputes(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminTradeDisputes.fxml"));
            Parent tradeDisputesView = loader.load();
            
            AdminTradeDisputesController controller = loader.getController();
            controller.setUser(currentAdminUser);
            
            loadContentInPlace(tradeDisputesView, "Trade Disputes");
        } catch (IOException e) {
            showAlert("Error", "Could not load trade disputes: " + e.getMessage());
            System.err.println("Error in handleTradeDisputes: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleUserStatistics(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserStatistics.fxml"));
            Parent userStatsView = loader.load();
            
            UserStatisticsController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            loadContentInPlace(userStatsView, "User Statistics");
        } catch (IOException e) {
            showAlert("Error", "Could not load user statistics: " + e.getMessage());
            System.err.println("Error in handleUserStatistics: " + e.getMessage());
        }
    }
    
    /**
     * Show a "Coming Soon" message in the content area
     */
    private void showComingSoonInPlace(String featureName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ComingSoon.fxml"));
            
            if (loader.getLocation() == null) {
                showAlert("Coming Soon", featureName + " feature is coming soon!");
                return;
            }
            
            Parent comingSoonView = loader.load();
            
            if (loader.getController() != null) {
                try {
                    Object controller = loader.getController();
                    controller.getClass().getMethod("setFeatureName", String.class)
                        .invoke(controller, featureName);
                    
                    // If there's a setUser method, set the current user
                    try {
                        controller.getClass().getMethod("setUser", User.class)
                            .invoke(controller, currentAdminUser);
                    } catch (Exception e) {
                        // Ignore if method doesn't exist
                    }
                    
                    // If there's a setAdmin method, indicate this is the admin
                    try {
                        controller.getClass().getMethod("setAdmin", boolean.class)
                            .invoke(controller, true);
                    } catch (Exception e) {
                        // Ignore if method doesn't exist
                    }
                } catch (Exception e) {
                    // Ignore if method not available
                }
            }
            
            loadContentInPlace(comingSoonView, "Coming Soon: " + featureName);
        } catch (IOException e) {
            showAlert("Coming Soon", featureName + " feature is coming soon!");
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
        actionsColumn.setCellFactory(param -> new TableCell<>() {
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
        });
    }

    private void loadAllUsers() {
        try {
            List<User> users = userService.getAll();
            userList.clear();
            userList.addAll(users);
            filteredUserList = new FilteredList<>(userList, p -> true);
            userTable.setItems(filteredUserList);
        } catch (Exception e) {
            showStatus("Error loading users: " + e.getMessage(), true);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load users: " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUserList.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return user.getName().toLowerCase().contains(lowerCaseFilter) ||
                       user.getEmail().toLowerCase().contains(lowerCaseFilter);
            });
        });
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
            System.err.println("Error in handleSearch: " + e.getMessage());
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
            System.err.println("Error opening add user form: " + e.getMessage());
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
            System.err.println("Error opening edit user form: " + e.getMessage());
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
            
            loadContentInPlace(profileView, "Profile");
        } catch (IOException e) {
            showStatus("Error loading profile page: " + e.getMessage(), true);
            System.err.println("Error loading profile page: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent loginView = loader.load();
            
            // This is one of the few cases where we actually navigate to a new scene
            // Most operations now use loadContentInPlace instead
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showStatus("Error logging out: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void navigateToView(ActionEvent event, Parent view, String title) {
        // This is only used for rare cases where we need to navigate away
        // Most operations now use loadContentInPlace instead
        Scene currentScene = ((Button) event.getSource()).getScene();
        Stage stage = (Stage) currentScene.getWindow();
        
        stage.setScene(new Scene(view));
        stage.setTitle(title);
        stage.show();
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

    @FXML
    private void handleCreateBlog() {
        clearBlogFields();
        currentBlog = new Blog();
        enableBlogFields(true);
    }    
    
    @FXML
    private void handleSaveBlog() {
        if (currentAdminUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Admin user information is required to create or edit a blog.");
            return;
        }

        if (currentBlog == null) {
            currentBlog = new Blog();
        }

        currentBlog.setTitle(blogTitleField.getText());
        currentBlog.setContent(blogContentArea.getText());
        currentBlog.setUser(currentAdminUser);
        
        Blog.ValidationResult validationResult = currentBlog.validate();
        if (!validationResult.isValid()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                String.join("\n", validationResult.getErrors().values()));
            return;
        }

        try {
            if (currentBlog.getId() == null) {
                blogService.add(currentBlog);
            } else {
                blogService.update(currentBlog);
            }
            refreshBlogList();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Blog saved successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save blog: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteBlog() {
        if (currentBlog != null && currentBlog.getId() != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this blog?");
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        blogService.delete(currentBlog);
                        refreshBlogList();
                        clearBlogFields();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Blog deleted successfully!");
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Error", 
                            "Failed to delete blog: " + e.getMessage());
                    }
                }
            });
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                String fileName = "blog_" + System.currentTimeMillis() + 
                    selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                Path destination = Paths.get(UPLOAD_DIR + fileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                
                currentBlog.setImageFilename(fileName);
                blogImageView.setImage(new Image(destination.toUri().toString()));
            } catch (IOException | RuntimeException e) {
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to upload image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleTranslate() {
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a language first!");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Information", 
            "Translation feature will be implemented soon!");
    }

    private void refreshBlogList() {
        try {
            blogListView.setItems(FXCollections.observableArrayList(blogService.readAll()));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Failed to load blogs: " + e.getMessage());
        }
    }

    private void loadBlogDetails(Blog blog) {
        currentBlog = blog;
        blogTitleField.setText(blog.getTitle());
        blogContentArea.setText(blog.getContent());
        
        if (blog.getImageFilename() != null) {
            try {
                Path imagePath = Paths.get(UPLOAD_DIR + blog.getImageFilename());
                blogImageView.setImage(new Image(imagePath.toUri().toString()));
            } catch (Exception e) {
                blogImageView.setImage(null);
            }
        } else {
            blogImageView.setImage(null);
        }
        
        languageComboBox.setValue(blog.getTranslationLanguage());
        enableBlogFields(true);
    }

    private void clearBlogFields() {
        blogTitleField.clear();
        blogContentArea.clear();
        blogImageView.setImage(null);
        languageComboBox.setValue(null);
        currentBlog = null;
    }

    private void enableBlogFields(boolean enable) {
        blogTitleField.setDisable(!enable);
        blogContentArea.setDisable(!enable);
        saveBlogButton.setDisable(!enable);
        deleteBlogButton.setDisable(!enable);
        translateButton.setDisable(!enable);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}