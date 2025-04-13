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

    private UserService userService;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User currentAdminUser;

    public void initialize() {
        userService = new UserService();
        setupTableColumns();
        loadAllUsers();
    }

    public void setCurrentUser(User user) {
        this.currentAdminUser = user;
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
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

        // Setup actions column with edit and delete buttons
        setupActionsColumn();
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
            // Search by email
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
        // Don't allow admins to delete themselves
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
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(loginView, 600, 400));
            stage.setTitle("NFT Marketplace - Login");
            stage.show();
        } catch (IOException e) {
            showStatus("Error logging out: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleManageCategories(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryManagement.fxml"));
            Parent categoriesView = loader.load();
            
            CategoryManagementController controller = loader.getController();
            controller.setCurrentUser(currentAdminUser);
            
            Scene currentScene = ((Button) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(categoriesView));
            stage.setTitle("NFT Marketplace - Category Management");
            stage.show();
        } catch (IOException e) {
            showStatus("Error loading category management: " + e.getMessage(), true);
            e.printStackTrace();
        }
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
}