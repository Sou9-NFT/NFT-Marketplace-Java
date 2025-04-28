package org.esprit.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.esprit.models.User;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserManagementController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> userTable;

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
    
    private UserService userService;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredUserList;
    private User currentAdminUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userService = new UserService();
        setupTableColumns();
        loadAllUsers();
        
        // Setup real-time search filtering
        setupSearchFilter();
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
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
    
    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
            Parent dashboardView = loader.load();
            
            AdminDashboardController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(dashboardView));
            stage.setTitle("NFT Marketplace - Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", 
                "Could not navigate back to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        // Set up the table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        rolesColumn.setCellValueFactory(cellData -> {
            List<String> roles = cellData.getValue().getRoles();
            String rolesString = roles.stream().collect(Collectors.joining(", "));
            return new SimpleStringProperty(rolesString);
        });
        
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                String formattedDate = cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return new SimpleStringProperty(formattedDate);
            }
            return new SimpleStringProperty("N/A");
        });

        setupActionsColumn();
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
            
            // Create a filtered list wrapping the observable list
            filteredUserList = new FilteredList<>(userList, p -> true);
            
            // Set the filtered list as the table items
            userTable.setItems(filteredUserList);
            
            showStatus("", false);
        } catch (Exception e) {
            showStatus("Error loading users: " + e.getMessage(), true);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load users: " + e.getMessage());
        }
    }
    
    private void setupSearchFilter() {
        // Add a listener to the search field to filter the table in real-time
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUserList.setPredicate(user -> {
                // If search text is empty, show all users
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                // Convert search string to lowercase for case-insensitive comparison
                String lowerCaseFilter = newValue.toLowerCase();
                
                // Match against name or email (partial match)
                if (user.getName() != null && user.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches name
                } else if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches email
                }
                return false; // No match
            });
            
            // Update status based on search results
            if (filteredUserList.isEmpty()) {
                showStatus("No users found matching: " + newValue, true);
            } else {
                showStatus("", false);
            }
        });
    }

    private void openAddUserForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserForm.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setMode(UserFormController.FormMode.ADD);
            controller.setParentController(this); // Ensure the UserFormController can work with this controller
            
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
            controller.setParentController(this); // Connect to this controller instead of AdminDashboard
            
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
    
    // This method will be called by UserFormController after add/edit operations
    public void refreshUserList() {
        loadAllUsers();
        showStatus("User list refreshed.", false);
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}