package org.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.esprit.models.Blog;
import org.esprit.models.Comment;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import org.esprit.services.CommentService;
import org.esprit.utils.TranslationService;
import org.json.JSONObject;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
    @FXML private TextArea commentTextArea;
    @FXML private Button addCommentButton;
    @FXML private ListView<Comment> commentsListView;
    @FXML private ImageView currentUserProfilePicture;    private BlogService blogService;
    private CommentService commentService;
    private TranslationService translationService;
    private Blog currentBlog;
    private User currentUser;
    private boolean isAdminMode = false;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    private final ObservableList<String> languages = FXCollections.observableArrayList(
            "French", "Spanish", "German", "Italian", "Arabic"
    );
    private static final Logger LOGGER = Logger.getLogger(BlogController.class.getName());

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
    }    @Override    public void initialize(URL url, ResourceBundle rb) {        blogService = new BlogService();
        commentService = new CommentService();
        translationService = TranslationService.getInstance();
        languageComboBox.setItems(languages);

        // Setup blog list view with profile pictures
        if (blogListView != null) {
            blogListView.setCellFactory(lv -> new ListCell<Blog>() {
                @Override
                protected void updateItem(Blog blog, boolean empty) {
                    super.updateItem(blog, empty);
                    if (empty || blog == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Main container
                        VBox container = new VBox(10);
                        container.setPadding(new Insets(15));
                        container.getStyleClass().add("blog-card");

                        // Author info section
                        HBox authorInfo = new HBox(10);
                        authorInfo.setAlignment(Pos.CENTER_LEFT);

                        // Profile picture
                        ImageView profilePic = new ImageView();
                        profilePic.setFitHeight(50);
                        profilePic.setFitWidth(50);                        profilePic.setPreserveRatio(true);
                        profilePic.getStyleClass().add("profile-picture");

                        // Load profile picture
                        String profilePicPath = UPLOAD_DIR + "user_" + blog.getUser().getId() + "_icon.png";
                        try {
                            // For now, load from local path - in future this could check for URL-based profile pictures
                            Image image = new Image(new File(profilePicPath).toURI().toString());
                            profilePic.setImage(image);
                        } catch (Exception e) {
                            profilePic.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
                        }

                        // Circular clip for profile picture
                        Circle clip = new Circle(25, 25, 25);
                        profilePic.setClip(clip);

                        // Author details
                        VBox authorDetails = new VBox(2);
                        Label authorName = new Label(blog.getUser().getName());
                        authorName.getStyleClass().add("author-name");

                        Label postDate = new Label(blog.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                        postDate.getStyleClass().add("post-date");

                        authorDetails.getChildren().addAll(authorName, postDate);
                        authorInfo.getChildren().addAll(profilePic, authorDetails);

                        // Blog content
                        VBox contentBox = new VBox(5);
                        Label titleLabel = new Label(blog.getTitle());
                        titleLabel.getStyleClass().add("blog-title");

                        Label previewLabel = new Label(blog.getContent().length() > 100
                                ? blog.getContent().substring(0, 100) + "..."
                                : blog.getContent());
                        previewLabel.getStyleClass().add("blog-preview");
                        previewLabel.setWrapText(true);

                        contentBox.getChildren().addAll(titleLabel, previewLabel);

                        // Add all sections to main container
                        container.getChildren().addAll(authorInfo, contentBox);
                        setGraphic(container);
                    }
                }
            });
        }

        // Setup comments list view
        if (commentsListView != null) {
            commentsListView.setCellFactory(lv -> new ListCell<Comment>() {
                @Override
                protected void updateItem(Comment comment, boolean empty) {
                    super.updateItem(comment, empty);
                    if (empty || comment == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        VBox container = new VBox(5);
                        container.setPadding(new Insets(10));

                        HBox header = new HBox(10);
                        header.setAlignment(Pos.CENTER_LEFT);

                        Label userLabel = new Label(comment.getUser().getName());
                        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                        Button deleteButton = new Button("Delete");
                        deleteButton.getStyleClass().add("button-danger");

                        // Only show delete button if the user is the comment author or an admin
                        if (currentUser != null && (currentUser.getId() == comment.getUser().getId() ||
                                currentUser.getRoles().contains("ROLE_ADMIN"))) {
                            deleteButton.setVisible(true);
                        } else {
                            deleteButton.setVisible(false);
                        }

                        deleteButton.setOnAction(e -> handleDeleteComment(comment));

                        header.getChildren().addAll(userLabel, deleteButton);

                        Label contentLabel = new Label(comment.getContent());
                        contentLabel.setWrapText(true);
                        contentLabel.setStyle("-fx-font-size: 12px;");

                        Label dateLabel = new Label(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        dateLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");

                        container.getChildren().addAll(header, contentLabel, dateLabel);
                        setGraphic(container);
                    }
                }
            });
        }

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
                private final Button commentsBtn = new Button("Comments");
                private final HBox buttons = new HBox(5, editBtn, deleteBtn, commentsBtn);

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

                    commentsBtn.setOnAction(e -> {
                        Blog blog = getTableRow().getItem();
                        if (blog != null) {
                            handleShowComments(blog);
                        }
                    });

                    buttons.setAlignment(Pos.CENTER);
                    editBtn.getStyleClass().add("button-primary");
                    deleteBtn.getStyleClass().add("button-danger");
                    commentsBtn.getStyleClass().add("button-info"); // Add CSS class for styling
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
    }    private String uploadImageToImgur(String imagePath) {
        String clientId = "117e88e67ef5f48"; // Imgur client ID from config.properties
        Path path = Paths.get(imagePath);
        try {
            byte[] imageBytes = Files.readAllBytes(path);
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadRequest = new HttpPost("https://api.imgur.com/3/image");
            uploadRequest.setHeader("Authorization", "Client-ID " + clientId);

            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("image", encodedImage);
            String json = new JSONObject(jsonMap).toString();

            uploadRequest.setEntity(new StringEntity(json));
            uploadRequest.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
                String responseString = EntityUtils.toString(response.getEntity());
                JSONObject jsonResponse = new JSONObject(responseString);
                if (jsonResponse.getBoolean("success")) {
                    return jsonResponse.getJSONObject("data").getString("link");
                } else {
                    System.err.println("Imgur upload failed: " + jsonResponse.toString());
                }
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Invalid argument", ex);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error", ex);
        }
        return null;
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
                // Upload image to Imgur
                String imgurUrl = uploadImageToImgur(selectedFile.getAbsolutePath());

                if (imgurUrl != null) {
                    // Update blog and image view
                    if (currentBlog == null) {
                        currentBlog = new Blog();
                    }
                    currentBlog.setImageFilename(imgurUrl);

                    // Load and display the image
                    Image image = new Image(imgurUrl);
                    blogImageView.setImage(image);

                    showAlert(Alert.AlertType.INFORMATION, "Success", "Image uploaded successfully to Imgur!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload image to Imgur.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Unexpected error while uploading image: " + e.getMessage());
            }
        }
    }    @FXML
    private void handleTranslate() {
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a language first!");
            return;
        }

        if (currentBlog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No blog selected for translation!");
            return;
        }

        try {
            String targetLangCode = translationService.getLanguageCode(selectedLanguage);
            
            // Translate title
            String translatedTitle = translationService.translate(currentBlog.getTitle(), targetLangCode, "en");
            if (translatedTitle != null) {
                titleField.setText(translatedTitle);
            }
            
            // Translate content
            String translatedContent = translationService.translate(currentBlog.getContent(), targetLangCode, "en");
            if (translatedContent != null) {
                contentArea.setText(translatedContent);
            }
            
            // Store the translation language in the blog
            currentBlog.setTranslationLanguage(selectedLanguage);
            
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Blog has been translated to " + selectedLanguage + " successfully!");
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Translation Error", 
                    "Failed to translate the blog: " + e.getMessage());
        }
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

    @FXML
    private void handleAddComment() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to add a comment.");
            return;
        }

        if (currentBlog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No blog selected.");
            return;
        }

        String content = commentTextArea.getText().trim();
        if (content.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Comment cannot be empty.");
            return;
        }

        Comment newComment = new Comment(content, currentUser, currentBlog);

        Comment.ValidationResult validationResult = newComment.validate();
        if (!validationResult.isValid()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                    String.join("\n", validationResult.getErrors().values()));
            return;
        }

        try {
            commentService.add(newComment);
            commentTextArea.clear();
            // Immediately refresh comments to show the new comment
            refreshComments();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add comment: " + e.getMessage());
        }
    }

    private void refreshComments() {
        if (currentBlog != null && commentsListView != null) {
            try {
                List<Comment> comments = commentService.getByBlog(currentBlog);
                commentsListView.setItems(FXCollections.observableArrayList(comments));

                // Set up the cell factory for comments with profile pictures
                commentsListView.setCellFactory(lv -> new ListCell<Comment>() {
                    @Override
                    protected void updateItem(Comment comment, boolean empty) {
                        super.updateItem(comment, empty);
                        if (empty || comment == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox container = new VBox(8);
                            container.setPadding(new Insets(10));
                            container.getStyleClass().add("comment-container");

                            // Header with profile picture and user info
                            HBox header = new HBox(10);
                            header.setAlignment(Pos.CENTER_LEFT);

                            // Profile picture
                            ImageView profilePic = new ImageView();
                            profilePic.setFitHeight(32);
                            profilePic.setFitWidth(32);
                            profilePic.setPreserveRatio(true);                            // Load commenter's profile picture
                            String profilePicPath = UPLOAD_DIR + "user_" + comment.getUser().getId() + "_icon.png";
                            try {
                                // For now, load from local path - in future this could check for URL-based profile pictures
                                Image image = new Image(new File(profilePicPath).toURI().toString());
                                profilePic.setImage(image);
                            } catch (Exception e) {
                                // Load default profile picture if user's picture is not found
                                profilePic.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
                            }

                            // Add circular clip to profile picture
                            Circle clip = new Circle(16, 16, 16);
                            profilePic.setClip(clip);

                            // User info (name and date)
                            VBox userInfo = new VBox(2);
                            Label nameLabel = new Label(comment.getUser().getName());
                            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                            Label dateLabel = new Label(comment.getCreatedAt().format(
                                    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
                            dateLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

                            userInfo.getChildren().addAll(nameLabel, dateLabel);

                            // Delete button (only for comment author or admin)
                            Button deleteButton = new Button("Delete");
                            deleteButton.getStyleClass().add("delete-button");

                            if (currentUser != null &&
                                    (currentUser.getId() == comment.getUser().getId() ||
                                            currentUser.getRoles().contains("ROLE_ADMIN"))) {
                                deleteButton.setVisible(true);
                                deleteButton.setOnAction(e -> handleDeleteComment(comment));
                            } else {
                                deleteButton.setVisible(false);
                            }

                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

                            header.getChildren().addAll(profilePic, userInfo, spacer, deleteButton);

                            // Comment content
                            Label contentLabel = new Label(comment.getContent());
                            contentLabel.setWrapText(true);
                            contentLabel.setStyle("-fx-font-size: 13px;");

                            container.getChildren().addAll(header, contentLabel);
                            setGraphic(container);
                        }
                    }
                });

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load comments: " + e.getMessage());
            }
        }
    }

    private void loadBlogDetails(Blog blog) {
        currentBlog = blog;
        titleField.setText(blog.getTitle());
        contentArea.setText(blog.getContent());

        if (blog.getImageFilename() != null) {
            try {
                // Check if the imageFilename is an Imgur URL
                if (blog.getImageFilename().startsWith("http")) {
                    // Directly load from URL
                    blogImageView.setImage(new Image(blog.getImageFilename()));
                } else {
                    // Legacy approach for local files
                    Path imagePath = Paths.get(UPLOAD_DIR + blog.getImageFilename());
                    blogImageView.setImage(new Image(imagePath.toUri().toString()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load blog image", e);
                blogImageView.setImage(null);
            }
        } else {
            blogImageView.setImage(null);
        }

        languageComboBox.setValue(blog.getTranslationLanguage());
        enableFields(true);

        // Clear and load comments for the selected blog
        if (commentTextArea != null) {
            commentTextArea.clear();
        }
        refreshComments();
    }
    public void clearFields() {
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
            updateCurrentUserProfilePicture();
        }
        refreshBlogList();
    }

    @FXML
    private void handleShowComments(Blog blog) {
        if (blog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No blog selected.");
            return;
        }

        try {
            // Create a dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Blog Comments");
            dialog.setHeaderText("Comments for: " + blog.getTitle());

            // Set the button types
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));

            // Create ListView for comments
            ListView<Comment> commentsList = new ListView<>();
            commentsList.setPrefHeight(300);
            commentsList.setPrefWidth(400);

            // Add create comment section for admins
            if (currentUser != null && currentUser.getRoles().contains("ROLE_ADMIN")) {
                HBox createCommentBox = new HBox(10);
                createCommentBox.setAlignment(Pos.CENTER_LEFT);

                TextArea newCommentArea = new TextArea();
                newCommentArea.setPromptText("Write a comment...");
                newCommentArea.setPrefRowCount(2);
                newCommentArea.setWrapText(true);
                HBox.setHgrow(newCommentArea, Priority.ALWAYS);

                Button createCommentBtn = new Button("Create Comment");
                createCommentBtn.getStyleClass().add("button-primary");
                createCommentBtn.setOnAction(e -> {
                    String commentText = newCommentArea.getText().trim();
                    if (!commentText.isEmpty()) {
                        Comment newComment = new Comment(commentText, currentUser, blog);
                        try {
                            commentService.add(newComment);
                            newCommentArea.clear();
                            List<Comment> updatedComments = commentService.getByBlog(blog);
                            commentsList.setItems(FXCollections.observableArrayList(updatedComments));
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to create comment: " + ex.getMessage());
                        }
                    }
                });

                createCommentBox.getChildren().addAll(newCommentArea, createCommentBtn);
                content.getChildren().add(createCommentBox);
            }

            // Get comments for the blog
            List<Comment> comments = commentService.getByBlog(blog);

            if (comments.isEmpty()) {
                Label noCommentsLabel = new Label("No comments yet.");
                noCommentsLabel.setStyle("-fx-font-style: italic;");
                content.getChildren().add(noCommentsLabel);
            } else {
                // Set up cell factory for comments
                commentsList.setCellFactory(lv -> new ListCell<Comment>() {
                    @Override
                    protected void updateItem(Comment comment, boolean empty) {
                        super.updateItem(comment, empty);
                        if (empty || comment == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox container = new VBox(5);
                            container.setPadding(new Insets(10));

                            HBox header = new HBox(10);
                            header.setAlignment(Pos.CENTER_LEFT);

                            Label userLabel = new Label(comment.getUser().getName());
                            userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                            Button deleteButton = new Button("Delete");
                            deleteButton.getStyleClass().add("button-danger");

                            // Only show delete button if the user is the comment author or an admin
                            if (currentUser != null && (currentUser.getId() == comment.getUser().getId() ||
                                    currentUser.getRoles().contains("ROLE_ADMIN"))) {
                                deleteButton.setVisible(true);
                            } else {
                                deleteButton.setVisible(false);
                            }

                            deleteButton.setOnAction(e -> handleDeleteComment(comment));

                            header.getChildren().addAll(userLabel, deleteButton);

                            Label contentLabel = new Label(comment.getContent());
                            contentLabel.setWrapText(true);
                            contentLabel.setStyle("-fx-font-size: 12px;");

                            Label dateLabel = new Label(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                            dateLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");

                            container.getChildren().addAll(header, contentLabel, dateLabel);
                            setGraphic(container);
                        }
                    }
                });

                commentsList.setItems(FXCollections.observableArrayList(comments));
                content.getChildren().add(commentsList);
            }

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load comments: " + e.getMessage());
        }
    }

    private void handleDeleteComment(Comment comment) {
        if (currentUser == null || (currentUser.getId() != comment.getUser().getId() &&
                !currentUser.getRoles().contains("ROLE_ADMIN"))) {
            showAlert(Alert.AlertType.ERROR, "Error", "You don't have permission to delete this comment.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Comment");
        confirmation.setHeaderText("Delete Comment");
        confirmation.setContentText("Are you sure you want to delete this comment?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentService.delete(comment);
                    refreshComments();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Comment deleted successfully!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete comment: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void handleBackToHome(ActionEvent event) {
        try {
            // Determine which dashboard to return to based on user role
            String fxmlPath = isAdminMode ? "/fxml/AdminDashboard.fxml" : "/fxml/UserDashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();

            // Set the current user in the appropriate controller
            if (isAdminMode) {
                AdminDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else {
                UserDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }

            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();

            stage.setScene(new Scene(dashboardView));
            stage.setTitle("NFT Marketplace - " + (isAdminMode ? "Admin" : "User") + " Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not navigate back to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }    private void updateCurrentUserProfilePicture() {
        if (currentUserProfilePicture != null && currentUser != null) {
            // Load current user's profile picture
            String profilePicPath = UPLOAD_DIR + "user_" + currentUser.getId() + "_icon.png";
            try {
                // For now, load from local path - in future this could check for URL-based profile pictures
                Image image = new Image(new File(profilePicPath).toURI().toString());
                currentUserProfilePicture.setImage(image);
            } catch (Exception e) {
                // Load default profile picture if user's picture is not found
                currentUserProfilePicture.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
            }

            // Add circular clip to profile picture
            Circle clip = new Circle(20, 20, 20);
            currentUserProfilePicture.setClip(clip);
        }
    }
}
