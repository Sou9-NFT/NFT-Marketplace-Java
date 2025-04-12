package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BlogController implements Initializable {
    @FXML private ListView<Blog> blogListView;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private ImageView blogImageView;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button translateButton;

    private BlogService blogService;
    private Blog currentBlog;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    private final ObservableList<String> languages = FXCollections.observableArrayList(
        "French", "Spanish", "German", "Italian", "Arabic"
    );    @Override
    public void initialize(URL url, ResourceBundle rb) {
        blogService = new BlogService();
        languageComboBox.setItems(languages);
        
        // Set up the ListView cell factory for custom display
        blogListView.setCellFactory(lv -> new ListCell<Blog>() {
            @Override
            protected void updateItem(Blog blog, boolean empty) {
                super.updateItem(blog, empty);
                if (empty || blog == null) {
                    setText(null);
                } else {
                    setText(blog.getTitle() + " (" + blog.getDate() + ")");
                }
            }
        });
        
        // Initialize the list view
        refreshBlogList();
        
        // Add selection listener to the list view
        blogListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadBlogDetails(newSelection);
                }
            }
        );
    }

    @FXML
    private void handleCreateBlog() {
        clearFields();
        currentBlog = new Blog();
        enableFields(true);
    }

    @FXML
    private void handleSave() {
        if (currentBlog == null) {
            currentBlog = new Blog();
        }

        currentBlog.setTitle(titleField.getText());
        currentBlog.setContent(contentArea.getText());
        
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
    private void handleDelete() {
        if (currentBlog != null && currentBlog.getId() != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this blog?");
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {                    try {
                        blogService.delete(currentBlog);
                        refreshBlogList();
                        clearFields();
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
            } catch (Exception e) {
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

        // TODO: Implement translation logic here
        // This would typically involve calling a translation service
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
        titleField.setText(blog.getTitle());
        contentArea.setText(blog.getContent());
        
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
        enableFields(true);
    }

    private void clearFields() {
        titleField.clear();
        contentArea.clear();
        blogImageView.setImage(null);
        languageComboBox.setValue(null);
        currentBlog = null;
    }

    private void enableFields(boolean enable) {
        titleField.setDisable(!enable);
        contentArea.setDisable(!enable);
        saveButton.setDisable(!enable);
        deleteButton.setDisable(!enable);
        translateButton.setDisable(!enable);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
