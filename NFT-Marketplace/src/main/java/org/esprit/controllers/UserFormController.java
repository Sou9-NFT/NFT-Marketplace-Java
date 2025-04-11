package org.esprit.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.esprit.models.User;
import org.esprit.services.UserService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class UserFormController {

    public enum FormMode {
        ADD,
        EDIT
    }

    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField balanceField;

    @FXML
    private CheckBox roleUserCheckbox;

    @FXML
    private CheckBox roleAdminCheckbox;

    @FXML
    private TextField walletField;

    @FXML
    private TextField githubField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label errorLabel;

    private UserService userService;
    private FormMode mode = FormMode.ADD;
    private User userToEdit;
    private AdminDashboardController parentController;

    public void initialize() {
        userService = new UserService();
        balanceField.setText("0.00");
    }

    public void setMode(FormMode mode) {
        this.mode = mode;
        updateUI();
    }

    public void setUser(User user) {
        this.userToEdit = user;
        populateFields();
    }

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }

    private void updateUI() {
        if (mode == FormMode.ADD) {
            formTitleLabel.setText("Add New User");
            saveButton.setText("Create User");
            passwordField.setPromptText("Enter password");
            passwordField.setDisable(false);
        } else {
            formTitleLabel.setText("Edit User");
            saveButton.setText("Save Changes");
            passwordField.setPromptText("Enter new password (leave blank to keep current)");
        }
    }

    private void populateFields() {
        if (userToEdit != null && mode == FormMode.EDIT) {
            nameField.setText(userToEdit.getName());
            emailField.setText(userToEdit.getEmail());
            
            // Leave password field empty when editing
            
            if (userToEdit.getBalance() != null) {
                balanceField.setText(userToEdit.getBalance().toString());
            }
            
            // Set role checkboxes
            List<String> roles = userToEdit.getRoles();
            roleUserCheckbox.setSelected(roles.contains("ROLE_USER"));
            roleAdminCheckbox.setSelected(roles.contains("ROLE_ADMIN"));
            
            if (userToEdit.getWalletAddress() != null) {
                walletField.setText(userToEdit.getWalletAddress());
            }
            
            if (userToEdit.getGithubUsername() != null) {
                githubField.setText(userToEdit.getGithubUsername());
            }
        }
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            try {
                if (mode == FormMode.ADD) {
                    createNewUser();
                } else {
                    updateExistingUser();
                }
                
                // Close the form and refresh the parent list
                if (parentController != null) {
                    parentController.refreshUserList();
                }
                closeForm();
            } catch (Exception e) {
                showError("Error saving user: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String balanceText = balanceField.getText().trim();
        
        if (name.isEmpty()) {
            showError("Name is required.");
            return false;
        }
        
        if (email.isEmpty()) {
            showError("Email is required.");
            return false;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address.");
            return false;
        }
        
        if (mode == FormMode.ADD && password.isEmpty()) {
            showError("Password is required for new users.");
            return false;
        }
        
        if (mode == FormMode.EDIT && !password.isEmpty() && password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return false;
        }
        
        if (!balanceText.isEmpty()) {
            try {
                new BigDecimal(balanceText);
            } catch (NumberFormatException e) {
                showError("Balance must be a valid number.");
                return false;
            }
        }
        
        if (!roleUserCheckbox.isSelected() && !roleAdminCheckbox.isSelected()) {
            showError("At least one role must be selected.");
            return false;
        }
        
        return true;
    }

    private void createNewUser() throws Exception {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Check if user with this email already exists
        User existingUser = userService.getByEmail(email);
        if (existingUser != null) {
            showError("A user with this email already exists.");
            return;
        }
        
        // Create new user
        User newUser = new User(email, password, name);
        
        // Set roles
        List<String> roles = new ArrayList<>();
        if (roleUserCheckbox.isSelected()) roles.add("ROLE_USER");
        if (roleAdminCheckbox.isSelected()) roles.add("ROLE_ADMIN");
        newUser.setRoles(roles);
        
        // Set balance
        String balanceText = balanceField.getText().trim();
        if (!balanceText.isEmpty()) {
            newUser.setBalance(new BigDecimal(balanceText));
        }
        
        // Set optional fields
        String walletAddress = walletField.getText().trim();
        if (!walletAddress.isEmpty()) {
            newUser.setWalletAddress(walletAddress);
        }
        
        String githubUsername = githubField.getText().trim();
        if (!githubUsername.isEmpty()) {
            newUser.setGithubUsername(githubUsername);
        }
        
        // Save to database
        userService.add(newUser);
    }

    private void updateExistingUser() throws Exception {
        if (userToEdit == null) return;
        
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Check if email is being changed and if it's already in use
        if (!email.equals(userToEdit.getEmail())) {
            User existingUser = userService.getByEmail(email);
            if (existingUser != null && existingUser.getId() != userToEdit.getId()) {
                showError("This email is already in use by another account.");
                return;
            }
        }
        
        // Update user fields
        userToEdit.setName(name);
        userToEdit.setEmail(email);
        
        // Update password if provided
        if (!password.isEmpty()) {
            userToEdit.setPassword(password);
        }
        
        // Update roles
        List<String> roles = new ArrayList<>();
        if (roleUserCheckbox.isSelected()) roles.add("ROLE_USER");
        if (roleAdminCheckbox.isSelected()) roles.add("ROLE_ADMIN");
        userToEdit.setRoles(roles);
        
        // Update balance
        String balanceText = balanceField.getText().trim();
        if (!balanceText.isEmpty()) {
            userToEdit.setBalance(new BigDecimal(balanceText));
        }
        
        // Update optional fields
        String walletAddress = walletField.getText().trim();
        userToEdit.setWalletAddress(walletAddress.isEmpty() ? null : walletAddress);
        
        String githubUsername = githubField.getText().trim();
        userToEdit.setGithubUsername(githubUsername.isEmpty() ? null : githubUsername);
        
        // Save to database
        userService.update(userToEdit);
    }

    @FXML
    private void handleCancel() {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}