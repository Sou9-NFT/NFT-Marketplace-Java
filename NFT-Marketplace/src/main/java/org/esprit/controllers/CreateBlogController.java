package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class CreateBlogController implements Initializable {
    
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private ImageView imagePreview;
    @FXML private ComboBox<String> languageComboBox;
    
    private BlogService blogService;
    private User currentUser;
    private String selectedImagePath;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        blogService = new BlogService();
        
        // Initialize language options
        languageComboBox.setItems(FXCollections.observableArrayList(
            "French", "Spanish", "German", "Italian", "Arabic"
        ));
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
                // Create upload directory if it doesn't exist
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                // Generate unique filename
                String originalFileName = selectedFile.getName();
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String fileName = "blog_" + System.currentTimeMillis() + extension;
                
                // Create full destination path
                Path destination = uploadPath.resolve(fileName);
                
                // Copy file with overwrite
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                
                // Update image preview
                Image image = new Image(destination.toUri().toString());
                imagePreview.setImage(image);
                selectedImagePath = fileName;
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Image uploaded successfully!");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload image: " + e.getMessage());
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
        
        // TODO: Implement translation logic
        showAlert(Alert.AlertType.INFORMATION, "Information", 
            "Translation feature will be implemented soon!");
    }
    
    @FXML
    private void handleSave() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to create a blog.");
            return;
        }
        
        // Create new blog
        Blog newBlog = new Blog();
        newBlog.setTitle(titleField.getText());
        newBlog.setContent(contentArea.getText());
        newBlog.setUser(currentUser);
        if (selectedImagePath != null) {
            newBlog.setImageFilename(selectedImagePath);
        }
        
        // Validate
        Blog.ValidationResult validationResult = newBlog.validate();
        if (!validationResult.isValid()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                String.join("\n", validationResult.getErrors().values()));
            return;
        }
        
        try {
            // Save blog
            blogService.add(newBlog);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Blog created successfully!");
            closeWindow();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save blog: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
