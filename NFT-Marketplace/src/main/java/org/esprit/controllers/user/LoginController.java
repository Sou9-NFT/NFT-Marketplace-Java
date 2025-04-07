package org.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.esprit.models.User;
import org.esprit.services.UserService;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    private UserService userService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        clearFields();
    }
    
    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }
    
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        
        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password are required");
            return;
        }
        
        try {
            // Attempt to find user by email
            User user = userService.getByEmail(email);
            
            if (user != null && user.getPassword().equals(password)) {
                // Successful login
                MainViewController.setCurrentUser(user);
                
                // Get the main controller and update UI
                MainViewController mainController = (MainViewController) 
                    emailField.getScene().getWindow().getUserData();
                if (mainController != null) {
                    mainController.updateUIForUser();
                }
                
                // Clear the form and go back to main view
                clearFields();
                handleCancel();
            } else {
                errorLabel.setText("Invalid email or password");
            }
        } catch (Exception e) {
            errorLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCancel() {
        // Clear the form fields and return to main view
        clearFields();
        
        // Get the main controller to clear the content area
        MainViewController mainController = (MainViewController) 
            emailField.getScene().getWindow().getUserData();
        if (mainController != null) {
            try {
                mainController.updateUIForUser();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleRegisterLink() {
        // Navigate to registration form
        MainViewController mainController = (MainViewController) 
            emailField.getScene().getWindow().getUserData();
        if (mainController != null) {
            try {
                // Use reflection to call the handleRegister method
                java.lang.reflect.Method method = MainViewController.class.getDeclaredMethod("handleRegister");
                method.setAccessible(true);
                method.invoke(mainController);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}