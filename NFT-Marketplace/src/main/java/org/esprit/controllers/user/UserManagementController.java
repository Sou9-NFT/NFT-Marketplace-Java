package org.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> usersTableView;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, BigDecimal> balanceColumn;

    @FXML
    private TableColumn<User, List<String>> roleColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @FXML
    private Label totalUsersLabel;

    @FXML
    private VBox userDetailPane;

    @FXML
    private TextField idField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField balanceField;

    @FXML
    private TextField walletAddressField;

    @FXML
    private TextField githubUsernameField;

    @FXML
    private TextField createdAtField;

    @FXML
    private CheckBox roleUserCheckbox;

    @FXML
    private CheckBox roleAdminCheckbox;

    @FXML
    private Label errorLabel;

    private UserService userService;
    private User selectedUser;
    private boolean isNewUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();

        // Set up table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        
        // Custom cell factory for roles
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("roles"));
        roleColumn.setCellFactory(column -> {
            return new TableCell<User, List<String>>() {
                @Override
                protected void updateItem(List<String> roles, boolean empty) {
                    super.updateItem(roles, empty);
                    if (empty || roles == null) {
                        setText(null);
                    } else {
                        setText(String.join(", ", roles));
                    }
                }
            };
        });

        // Add action buttons
        setupActionsColumn();

        // Load users
        loadUsers();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                viewBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    showUserDetails(user);
                });

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(Pos.CENTER);
                    buttons.getChildren().addAll(viewBtn, editBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAll();
            usersTableView.setItems(FXCollections.observableArrayList(users));
            totalUsersLabel.setText("Total Users: " + users.size());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        try {
            if (searchTerm.isEmpty()) {
                loadUsers();
                return;
            }
            
            List<User> allUsers = userService.getAll();
            List<User> filteredUsers = new ArrayList<>();
            
            for (User user : allUsers) {
                if ((user.getName() != null && user.getName().toLowerCase().contains(searchTerm)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTerm))) {
                    filteredUsers.add(user);
                }
            }
            
            usersTableView.setItems(FXCollections.observableArrayList(filteredUsers));
            totalUsersLabel.setText("Found Users: " + filteredUsers.size());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Search Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddUser() {
        clearUserForm();
        selectedUser = new User();
        isNewUser = true;
        userDetailPane.setVisible(true);
    }

    private void showUserDetails(User user) {
        selectedUser = user;
        isNewUser = false;
        populateUserForm(user);
        userDetailPane.setVisible(true);
    }

    private void editUser(User user) {
        selectedUser = user;
        isNewUser = false;
        populateUserForm(user);
        userDetailPane.setVisible(true);
    }

    private void deleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete User");
        confirmation.setHeaderText("Delete User: " + user.getName());
        confirmation.setContentText("Are you sure you want to delete this user? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user);
                loadUsers();
                userDetailPane.setVisible(false);
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "User Deleted", 
                         "User has been successfully deleted.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSaveUser() {
        try {
            // Get form data
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String balanceText = balanceField.getText().trim();
            String walletAddress = walletAddressField.getText().trim();
            String githubUsername = githubUsernameField.getText().trim();
            
            // Validate input
            if (name.isEmpty() || email.isEmpty()) {
                errorLabel.setText("Name and email are required");
                return;
            }
            
            BigDecimal balance;
            try {
                balance = new BigDecimal(balanceText);
                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    errorLabel.setText("Balance cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Invalid balance format");
                return;
            }
            
            // Update user data
            selectedUser.setName(name);
            selectedUser.setEmail(email);
            selectedUser.setBalance(balance);
            selectedUser.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
            selectedUser.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
            
            // Update roles
            List<String> roles = new ArrayList<>();
            if (roleUserCheckbox.isSelected()) {
                roles.add("ROLE_USER");
            }
            if (roleAdminCheckbox.isSelected()) {
                roles.add("ROLE_ADMIN");
            }
            selectedUser.setRoles(roles);
            
            // Save user
            if (isNewUser) {
                userService.add(selectedUser);
            } else {
                userService.update(selectedUser);
            }
            
            // Refresh table
            loadUsers();
            userDetailPane.setVisible(false);
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "User Saved", 
                     "User has been successfully saved.");
            
        } catch (Exception e) {
            errorLabel.setText("Error saving user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelEdit() {
        userDetailPane.setVisible(false);
        clearUserForm();
    }

    private void populateUserForm(User user) {
        idField.setText(String.valueOf(user.getId()));
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        balanceField.setText(user.getBalance().toString());
        walletAddressField.setText(user.getWalletAddress() != null ? user.getWalletAddress() : "");
        githubUsernameField.setText(user.getGithubUsername() != null ? user.getGithubUsername() : "");
        createdAtField.setText(user.getCreatedAt().toString());
        
        roleUserCheckbox.setSelected(user.getRoles().contains("ROLE_USER"));
        roleAdminCheckbox.setSelected(user.getRoles().contains("ROLE_ADMIN"));
        
        errorLabel.setText("");
    }

    private void clearUserForm() {
        idField.clear();
        nameField.clear();
        emailField.clear();
        balanceField.clear();
        walletAddressField.clear();
        githubUsernameField.clear();
        createdAtField.clear();
        
        roleUserCheckbox.setSelected(true);
        roleAdminCheckbox.setSelected(false);
        
        errorLabel.setText("");
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}