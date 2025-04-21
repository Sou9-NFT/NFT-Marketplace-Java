package org.esprit.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.esprit.models.User;
import org.esprit.services.UserService;
import org.esprit.utils.PasswordHasher;

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
    private Label nameErrorLabel;

    @FXML
    private TextField emailField;
    
    @FXML
    private Label emailErrorLabel;

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label passwordErrorLabel;

    @FXML
    private TextField balanceField;
    
    @FXML
    private Label balanceErrorLabel;

    @FXML
    private CheckBox roleUserCheckbox;

    @FXML
    private CheckBox roleAdminCheckbox;
    
    @FXML
    private Label rolesErrorLabel;

    @FXML
    private TextField walletField;
    
    @FXML
    private Label walletErrorLabel;

    @FXML
    private TextField githubField;
    
    @FXML
    private Label githubErrorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label errorLabel;

    private UserService userService;
    private FormMode mode = FormMode.ADD;
    private User userToEdit;
    private Object parentController; // Changed to Object to support multiple controller types

    public void initialize() {
        userService = new UserService();
        balanceField.setText("0.00");
        // Hide all error labels initially
        hideAllErrorLabels();
    }
    
    private void hideAllErrorLabels() {
        nameErrorLabel.setVisible(false);
        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);
        balanceErrorLabel.setVisible(false);
        rolesErrorLabel.setVisible(false);
        walletErrorLabel.setVisible(false);
        githubErrorLabel.setVisible(false);
        errorLabel.setVisible(false);
    }

    public void setMode(FormMode mode) {
        this.mode = mode;
        updateUI();
    }

    public void setUser(User user) {
        this.userToEdit = user;
        populateFields();
    }

    public void setParentController(Object controller) {
        this.parentController = controller;
    }

    public void setParentController(AdminDashboardController controller) {
        this.parentController = controller;
    }
    
    public void setParentController(UserManagementController controller) {
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
        // Reset error labels before validation
        hideAllErrorLabels();
        
        if (validateInput()) {
            try {
                if (mode == FormMode.ADD) {
                    createNewUser();
                } else {
                    updateExistingUser();
                }

                // Close the form and refresh the parent list
                if (parentController != null) {
                    if (parentController instanceof AdminDashboardController) {
                        ((AdminDashboardController) parentController).refreshUserList();
                    } else if (parentController instanceof UserManagementController) {
                        ((UserManagementController) parentController).refreshUserList();
                    }
                }
                closeForm();
            } catch (Exception e) {
                showError("Error saving user: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInput() {
        // Create a new User with the form data to use entity validation
        User userToValidate;
        
        if (mode == FormMode.ADD) {
            userToValidate = new User(
                emailField.getText().trim(),
                passwordField.getText(),
                nameField.getText().trim()
            );
        } else {
            userToValidate = new User();
            userToValidate.setId(userToEdit != null ? userToEdit.getId() : 0);
            userToValidate.setEmail(emailField.getText().trim());
            userToValidate.setName(nameField.getText().trim());
            
            // Only set password if it's not empty (for edit mode)
            String password = passwordField.getText();
            if (!password.isEmpty()) {
                userToValidate.setPassword(password);
            }
        }
        
        // Set other fields
        String balanceText = balanceField.getText().trim();
        if (!balanceText.isEmpty()) {
            try {
                userToValidate.setBalance(new BigDecimal(balanceText));
            } catch (NumberFormatException e) {
                // Will be caught by validation
            }
        }
        
        String walletAddress = walletField.getText().trim();
        if (!walletAddress.isEmpty()) {
            userToValidate.setWalletAddress(walletAddress);
        }
        
        String githubUsername = githubField.getText().trim();
        if (!githubUsername.isEmpty()) {
            userToValidate.setGithubUsername(githubUsername);
        }
        
        // Set roles
        List<String> roles = new ArrayList<>();
        if (roleUserCheckbox.isSelected()) {
            roles.add("ROLE_USER");
        }
        if (roleAdminCheckbox.isSelected()) {
            roles.add("ROLE_ADMIN");
        }
        userToValidate.setRoles(roles);
        
        // Use the User entity validation
        User.ValidationResult result = userToValidate.validate();
        
        // Reset all field styling
        nameField.getStyleClass().removeAll("error");
        emailField.getStyleClass().removeAll("error");
        passwordField.getStyleClass().removeAll("error");
        balanceField.getStyleClass().removeAll("error");
        walletField.getStyleClass().removeAll("error");
        githubField.getStyleClass().removeAll("error");
        
        // Hide all error labels first
        hideAllErrorLabels();
        
        // If valid, return true immediately
        if (result.isValid()) {
            return true;
        }
        
        // Handle validation errors
        Map<String, String> errors = result.getErrors();
        
        // Show field-specific errors
        if (errors.containsKey("name")) {
            showFieldError(nameErrorLabel, errors.get("name"));
            nameField.getStyleClass().add("error");
        }
        
        if (errors.containsKey("email")) {
            showFieldError(emailErrorLabel, errors.get("email"));
            emailField.getStyleClass().add("error");
        }
        
        if (errors.containsKey("password")) {
            showFieldError(passwordErrorLabel, errors.get("password"));
            passwordField.getStyleClass().add("error");
        }
        
        if (errors.containsKey("balance")) {
            showFieldError(balanceErrorLabel, errors.get("balance"));
            balanceField.getStyleClass().add("error");
        }
        
        if (errors.containsKey("walletAddress")) {
            showFieldError(walletErrorLabel, errors.get("walletAddress"));
            walletField.getStyleClass().add("error");
        }
        
        if (errors.containsKey("githubUsername")) {
            showFieldError(githubErrorLabel, errors.get("githubUsername"));
            githubField.getStyleClass().add("error");
        }
        
        // Check if roles are selected (this is not in User entity validation)
        if (roles.isEmpty()) {
            showFieldError(rolesErrorLabel, "At least one role must be selected");
        }
        
        return false;
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void createNewUser() throws Exception {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Check if user with this email already exists
        User existingUser = userService.getByEmail(email);
        if (existingUser != null) {
            showFieldError(emailErrorLabel, "A user with this email already exists");
            emailField.getStyleClass().add("error");
            return;
        }

        // Create new user
        User newUser = new User(email, PasswordHasher.hashPassword(password), name);

        // Set default profile picture
        newUser.setProfilePicture("/assets/default/default_profile.jpg");

        // Set roles
        List<String> roles = new ArrayList<>();
        if (roleUserCheckbox.isSelected())
            roles.add("ROLE_USER");
        if (roleAdminCheckbox.isSelected())
            roles.add("ROLE_ADMIN");
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
        if (userToEdit == null)
            return;

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Check if email is being changed and if it's already in use
        if (!email.equals(userToEdit.getEmail())) {
            User existingUser = userService.getByEmail(email);
            if (existingUser != null && existingUser.getId() != userToEdit.getId()) {
                showFieldError(emailErrorLabel, "This email is already in use by another account");
                emailField.getStyleClass().add("error");
                return;
            }
        }

        // Update user fields
        userToEdit.setName(name);
        userToEdit.setEmail(email);

        // Update password if provided
        if (!password.isEmpty()) {
            userToEdit.setPassword(PasswordHasher.hashPassword(password));
        }

        // Update roles
        List<String> roles = new ArrayList<>();
        if (roleUserCheckbox.isSelected())
            roles.add("ROLE_USER");
        if (roleAdminCheckbox.isSelected())
            roles.add("ROLE_ADMIN");
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