package org.esprit.controllers;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class BlogController implements Initializable {
    @FXML private ListView<Blog> blogListView;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private ImageView blogImageView;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button translateButton;
    @FXML private Button createBlogButton;
    @FXML private TextField searchField;
    @FXML private TableView<Blog> blogTableView;
    @FXML private TableColumn<Blog, String> titleColumn;
    @FXML private TableColumn<Blog, String> authorColumn;
    @FXML private TableColumn<Blog, LocalDate> dateColumn;
    @FXML private TableColumn<Blog, Void> actionsColumn;

    private BlogService blogService;
    private Blog currentBlog;
    private User currentUser;
    private boolean isAdminMode = false;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    private final ObservableList<String> languages = FXCollections.observableArrayList(
        "French", "Spanish", "German", "Italian", "Arabic"
    );

    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        if (createBlogButton != null) {
            createBlogButton.setVisible(isAdmin);
        }
        if (searchField != null) {
            searchField.setVisible(isAdmin);
        }
        if (blogTableView != null) {
            blogTableView.setVisible(isAdmin);
        }
        refreshBlogList();
    }    @Override
    public void initialize(URL url, ResourceBundle rb) {
        blogService = new BlogService();
        languageComboBox.setItems(languages);
        
        // Initialize table columns for admin mode
        if (titleColumn != null) {
            titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
            authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUser().getName()));
            dateColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));
            setupActionsColumn();
        }
        
        // Set up search functionality
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterBlogs(newValue);
            });
        }
        
        // Initialize list view or table view based on mode
        refreshBlogList();
        
        // Add selection listener
        if (blogTableView != null) {
            blogTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadBlogDetails(newSelection);
                    }
                }
            );
        } else if (blogListView != null) {
            blogListView.setCellFactory(lv -> new ListCell<Blog>() {
                @Override
                protected void updateItem(Blog blog, boolean empty) {
                    super.updateItem(blog, empty);
                    if (empty || blog == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        VBox container = new VBox(5);
                        container.setPadding(new Insets(10));
                        
                        Label titleLabel = new Label(blog.getTitle());
                        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                        
                        Label contentPreview = new Label(blog.getContent().length() > 100 
                            ? blog.getContent().substring(0, 100) + "..." 
                            : blog.getContent());
                        contentPreview.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
                        contentPreview.setWrapText(true);
                        
                        Label dateLabel = new Label(blog.getDate().toString());
                        dateLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
                        
                        container.getChildren().addAll(titleLabel, contentPreview, dateLabel);
                        setGraphic(container);
                    }
                }
            });
            
            blogListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadBlogDetails(newSelection);
                    }
                }
            );
        }
    }

    private void refreshBlogList() {
        try {
            List<Blog> blogs = blogService.readAll();
            if (isAdminMode && blogTableView != null) {
                blogTableView.setItems(FXCollections.observableArrayList(blogs));
            } else if (blogListView != null) {
                blogListView.setItems(FXCollections.observableArrayList(blogs));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Failed to load blogs: " + e.getMessage());
        }
    }

    private void setupActionsColumn() {
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<Blog, Void>() {
                private final Button editBtn = new Button("Edit");
                private final Button deleteBtn = new Button("Delete");
                private final HBox buttons = new HBox(5, editBtn, deleteBtn);
                
                {
                    editBtn.setOnAction(e -> {
                        Blog blog = getTableRow().getItem();
                        if (blog != null) {
                            loadBlogDetails(blog);
                        }
                    });
                    
                    deleteBtn.setOnAction(e -> {
                        Blog blog = getTableRow().getItem();
                        if (blog != null) {
                            handleDeleteBlog(blog);
                        }
                    });
                    
                    buttons.setAlignment(Pos.CENTER);
                    editBtn.getStyleClass().add("button-primary");
                    deleteBtn.getStyleClass().add("button-danger");
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : buttons);
                }
            });
        }
    }

    private void handleDeleteBlog(Blog blog) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete the blog '" + blog.getTitle() + "'?");
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText(null);
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    blogService.delete(blog);
                    refreshBlogList();
                    clearFields();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Blog deleted successfully!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete blog: " + e.getMessage());
                }
            }
        });
    }

    private void filterBlogs(String searchText) {
        try {
            List<Blog> allBlogs = blogService.readAll();
            if (searchText == null || searchText.isEmpty()) {
                updateBlogList(allBlogs);
            } else {
                String lowerCaseFilter = searchText.toLowerCase();
                List<Blog> filteredList = allBlogs.stream()
                    .filter(blog -> 
                        blog.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                        blog.getContent().toLowerCase().contains(lowerCaseFilter) ||
                        blog.getUser().getName().toLowerCase().contains(lowerCaseFilter))
                    .collect(Collectors.toList());
                updateBlogList(filteredList);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to filter blogs: " + e.getMessage());
        }
    }

    private void updateBlogList(List<Blog> blogs) {
        if (isAdminMode && blogTableView != null) {
            blogTableView.setItems(FXCollections.observableArrayList(blogs));
        } else if (blogListView != null) {
            blogListView.setItems(FXCollections.observableArrayList(blogs));
        }
    }

    @FXML
    private void handleCreateBlog() {
        clearFields();
        currentBlog = new Blog();
        enableFields(true);
    }    @FXML
    private void handleSave() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to create or edit a blog.");
            return;
        }

        if (currentBlog == null) {
            currentBlog = new Blog();
        }

        currentBlog.setTitle(titleField.getText());
        currentBlog.setContent(contentArea.getText());
        currentBlog.setUser(currentUser);
        
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

    @FXML
    private void handleAddBlog() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to create a blog.");
            return;
        }

        // Create a new blog object with current form data
        Blog newBlog = new Blog();
        newBlog.setTitle(titleField.getText());
        newBlog.setContent(contentArea.getText());
        newBlog.setUser(currentUser);
        
        // If an image has been selected, use the currentBlog's image filename
        if (currentBlog != null && currentBlog.getImageFilename() != null) {
            newBlog.setImageFilename(currentBlog.getImageFilename());
        }
        
        // Validate the blog
        Blog.ValidationResult validationResult = newBlog.validate();
        if (!validationResult.isValid()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                String.join("\n", validationResult.getErrors().values()));
            return;
        }

        try {
            // Add the new blog
            blogService.add(newBlog);
            refreshBlogList();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Blog added successfully!");
            
            // Clear the form for a new entry
            clearFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add blog: " + e.getMessage());
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
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Refresh UI components based on user role
        if (user != null) {
            boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
            saveButton.setVisible(isAdmin);
            deleteButton.setVisible(isAdmin);
        }
        refreshBlogList();
    }
}
