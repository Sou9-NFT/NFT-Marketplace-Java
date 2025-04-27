package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import java.time.format.DateTimeFormatter;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.models.Comment;
import org.esprit.services.BlogService;
import org.esprit.services.CommentService;
import org.esprit.utils.TranslationService;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;


public class BlogDetailController implements Initializable {
    @FXML private ImageView authorProfilePicture;
    @FXML private Label authorNameLabel;
    @FXML private Label postDateLabel;
    @FXML private Text blogTitleText;
    @FXML private ImageView blogImage;
    @FXML private Text blogContentText;    @FXML private ImageView currentUserProfilePicture;
    @FXML private TextArea commentTextArea;
    @FXML private ListView<Comment> commentsListView;
    @FXML private ComboBox<String> languageComboBox;
    
    private BlogService blogService;
    private CommentService commentService;
    private TranslationService translationService;
    private Blog currentBlog;
    private User currentUser;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
      @Override
    public void initialize(URL url, ResourceBundle rb) {        blogService = new BlogService();
        commentService = new CommentService();
        translationService = TranslationService.getInstance();
        setupCommentsList();
        setupLanguageComboBox();
    }
    
    private void setupLanguageComboBox() {
        languageComboBox.setItems(FXCollections.observableArrayList(
            "French", "Spanish", "German", "Italian", "Arabic"
        ));
    }
    
    @FXML
    private void handleTranslate() {
        if (currentBlog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No blog post selected");
            return;
        }

        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a language first!");
            return;
        }

        try {
            String targetLangCode = translationService.getLanguageCode(selectedLanguage);
            
            // Translate title
            String translatedTitle = translationService.translate(
                currentBlog.getTitle(), targetLangCode, "en"
            );
            if (translatedTitle != null) {
                blogTitleText.setText(translatedTitle);
            }
            
            // Translate content
            String translatedContent = translationService.translate(
                currentBlog.getContent(), targetLangCode, "en"
            );
            if (translatedContent != null) {
                blogContentText.setText(translatedContent);
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                "Blog has been translated to " + selectedLanguage);
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Translation Error", 
                "Failed to translate the blog: " + e.getMessage());
        }
    }
    
    public void setBlog(Blog blog) {
        System.out.println("Setting blog: " + (blog != null ? "Blog ID: " + blog.getId() : "null"));
        this.currentBlog = blog;
        if (blog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Blog data is null");
            return;
        }
        loadBlogDetails();
        refreshComments();
    }
    
    private void loadBlogDetails() {
        if (currentBlog != null) {
            try {
                System.out.println("Loading blog details for blog ID: " + currentBlog.getId());
                System.out.println("Author: " + currentBlog.getUser().getName());
                System.out.println("Title: " + currentBlog.getTitle());

                // Load author profile picture
                String profilePicPath = UPLOAD_DIR + "user_" + currentBlog.getUser().getId() + "_icon.png";
                System.out.println("Looking for profile picture at: " + profilePicPath);
                Image profileImage = new Image(new File(profilePicPath).toURI().toString());
                authorProfilePicture.setImage(profileImage);
            } catch (Exception e) {
                authorProfilePicture.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
            }
            
            // Set author info
            authorNameLabel.setText(currentBlog.getUser().getName());
            postDateLabel.setText(currentBlog.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            
            // Set blog content
            blogTitleText.setText(currentBlog.getTitle());
            blogContentText.setText(currentBlog.getContent());
            
            // Load blog image if exists
            if (currentBlog.getImageFilename() != null) {
                try {
                    String imagePath = UPLOAD_DIR + currentBlog.getImageFilename();
                    blogImage.setImage(new Image(new File(imagePath).toURI().toString()));
                } catch (Exception e) {
                    blogImage.setImage(null);
                }
            }
            
            // Load current user profile picture
            updateCurrentUserProfilePicture();
        }
    }    private void setupCommentsList() {
        commentsListView.setCellFactory(lv -> new ListCell<Comment>() {            private VBox container;
            private Label nameLabel;
            private Label dateLabel;
            private Text contentText;
            private ImageView profilePic;
            private Button deleteButton;

            {
                container = new VBox(5);
                container.getStyleClass().add("comment-container");
                
                HBox header = new HBox(10);                header.getStyleClass().add("comment-header");
                header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                profilePic = new ImageView();
                profilePic.setFitHeight(32);
                profilePic.setFitWidth(32);
                profilePic.getStyleClass().add("comment-profile-pic");
                
                VBox userInfo = new VBox(2);
                nameLabel = new Label();
                nameLabel.getStyleClass().add("comment-author");
                  dateLabel = new Label();
                dateLabel.getStyleClass().add("comment-date");
                
                deleteButton = new Button("×");
                deleteButton.getStyleClass().addAll("delete-button", "comment-delete-button");
                deleteButton.setVisible(false); // Initially hidden
                
                userInfo.getChildren().addAll(nameLabel, dateLabel);
                header.getChildren().addAll(profilePic, userInfo);
                
                // Add delete button with spring to push it to the right
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                header.getChildren().addAll(spacer, deleteButton);
                
                contentText = new Text();
                contentText.getStyleClass().add("comment-content");
                contentText.wrappingWidthProperty().bind(widthProperty().subtract(50));
                
                container.getChildren().addAll(header, contentText);
                container.setPadding(new Insets(10));
            }

            @Override
            protected void updateItem(Comment comment, boolean empty) {
                super.updateItem(comment, empty);
                if (empty || comment == null) {
                    setText(null);
                    setGraphic(null);                } else {
                    nameLabel.setText(comment.getUser().getName());
                    dateLabel.setText(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));                    contentText.setText(comment.getContent());
                    
                    // Load user profile picture
                    String profilePicPath = UPLOAD_DIR + "user_" + comment.getUser().getId() + "_icon.png";
                    try {
                        Image image = new Image(new File(profilePicPath).toURI().toString());
                        profilePic.setImage(image);
                    } catch (Exception e) {
                        profilePic.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
                    }
                      // Show delete button only for comment author
                    deleteButton.setVisible(currentUser != null && currentUser.getId() == comment.getUser().getId());
                    
                    // Configure delete button action
                    deleteButton.setOnAction(e -> {
                        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmDialog.setTitle("Delete Comment");
                        confirmDialog.setHeaderText(null);
                        confirmDialog.setContentText("Are you sure you want to delete this comment?");
                        
                        confirmDialog.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {                                try {
                                    commentService.delete(comment);
                                    refreshComments();
                                } catch (Exception ex) {
                                    showAlert(Alert.AlertType.ERROR, "Error", 
                                        "Failed to delete comment: " + ex.getMessage());
                                }
                            }
                        });
                    });
                    
                    setGraphic(container);
                }
            }
        });
    }
    
    private void refreshComments() {
        if (currentBlog != null) {
            try {
                var comments = commentService.getByBlog(currentBlog);
                commentsListView.setItems(FXCollections.observableArrayList(comments));
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to load comments: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleAddComment() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to comment.");
            return;
        }

        String content = commentTextArea.getText().trim();
        if (content.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Comment cannot be empty.");
            return;
        }

        try {
            Comment comment = new Comment(content, currentUser, currentBlog);
            commentService.add(comment);
            commentTextArea.clear();
            refreshComments();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add comment: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BlogList.fxml"));
            Parent root = loader.load();
            
            BlogListController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Stage stage = (Stage) blogTitleText.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not return to blog list: " + e.getMessage());
        }
    }
    
    private void updateCurrentUserProfilePicture() {
        if (currentUserProfilePicture != null && currentUser != null) {
            String profilePicPath = UPLOAD_DIR + "user_" + currentUser.getId() + "_icon.png";
            try {
                Image image = new Image(new File(profilePicPath).toURI().toString());
                currentUserProfilePicture.setImage(image);
            } catch (Exception e) {
                currentUserProfilePicture.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
            }
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateCurrentUserProfilePicture();
    }
      private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text for cleaner look
        alert.setContentText(content);
        alert.showAndWait();
    }
}

