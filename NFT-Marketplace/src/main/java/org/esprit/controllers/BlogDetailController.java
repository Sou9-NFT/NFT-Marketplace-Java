package org.esprit.controllers;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.esprit.models.Blog;
import org.esprit.models.Comment;
import org.esprit.models.User;
import org.esprit.services.BlogService;
import org.esprit.services.CommentService;
import org.esprit.utils.GiphyService;
import org.esprit.utils.TranslationService;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class BlogDetailController implements Initializable {
    @FXML private ImageView authorProfilePicture;
    @FXML private Label authorNameLabel;
    @FXML private Label postDateLabel;
    @FXML private Text blogTitleText;
    @FXML private ImageView blogImage;
    @FXML private Text blogContentText;    
    @FXML private ImageView currentUserProfilePicture;
    @FXML private TextArea commentTextArea;
    @FXML private ListView<Comment> commentsListView;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private TextField gifSearchField;
    @FXML private FlowPane gifPreviewPane;
    @FXML private ImageView selectedGifPreview;
    @FXML private Button clearGifButton;
    
    private BlogService blogService;
    private CommentService commentService;
    private TranslationService translationService;
    private GiphyService giphyService;
    private Blog currentBlog;
    private User currentUser;
    private String selectedGifUrl;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        blogService = new BlogService();
        commentService = new CommentService();
        translationService = TranslationService.getInstance();
        giphyService = GiphyService.getInstance();
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
    }    
    
    private void setupCommentsList() {
        commentsListView.setCellFactory(lv -> new ListCell<Comment>() {            
            private VBox container;
            private Label nameLabel;
            private Label dateLabel;
            private Text contentText;
            private ImageView profilePic;
            private Button deleteButton;

            {
                container = new VBox(5);
                container.getStyleClass().add("comment-container");
                
                HBox header = new HBox(10);                
                header.getStyleClass().add("comment-header");
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
                super.updateItem(comment, empty);                if (empty || comment == null) {
                    setText(null);
                    setGraphic(null);                
                } else {
                    // Reset the container's children to just the header
                    container.getChildren().clear();
                    
                    // Recreate and set up the header
                    HBox header = new HBox(10);
                    header.getStyleClass().add("comment-header");
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    // Set up user info
                    nameLabel.setText(comment.getUser().getName());
                    dateLabel.setText(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
                    
                    VBox userInfo = new VBox(2);
                    userInfo.getChildren().addAll(nameLabel, dateLabel);
                    
                    // Set up delete button and spacer
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                    
                    header.getChildren().addAll(profilePic, userInfo, spacer, deleteButton);
                    container.getChildren().add(header);
                    
                    // Add content if present
                    if (comment.getContent() != null && !comment.getContent().isEmpty()) {
                        contentText.setText(comment.getContent());
                        container.getChildren().add(contentText);
                    }
                    
                    // Add GIF if present
                    if (comment.getGifUrl() != null && !comment.getGifUrl().isEmpty()) {
                        ImageView gifView = new ImageView(new Image(comment.getGifUrl()));
                        gifView.setFitWidth(200);
                        gifView.setPreserveRatio(true);
                        container.getChildren().add(gifView);
                    }
                    
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
                            if (response == ButtonType.OK) {                                
                                try {
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
        if (content.isEmpty() && selectedGifUrl == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Comment must have text or a GIF.");
            return;
        }

        try {
            Comment comment = new Comment(content, currentUser, currentBlog);
            comment.setGifUrl(selectedGifUrl);
            commentService.add(comment);
            
            // Clear inputs
            commentTextArea.clear();
            handleClearGif();
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
    
    @FXML
    private void handleGifSearch() {
        try {
            gifPreviewPane.getChildren().clear();
            String query = gifSearchField.getText().trim();
            if (query.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Empty Search", "Please enter a search term for GIFs");
                return;
            }

            System.out.println("Searching for GIFs with query: " + query);
            JSONArray gifs = giphyService.searchGifs(query);
            System.out.println("Found " + gifs.length() + " GIFs");
            
            if (gifs.length() == 0) {
                Label noResultsLabel = new Label("No GIFs found matching your search");
                noResultsLabel.setStyle("-fx-text-fill: #757575;");
                gifPreviewPane.getChildren().add(noResultsLabel);
                return;
            }

            for (int i = 0; i < gifs.length(); i++) {
                JSONObject gif = gifs.getJSONObject(i);
                String gifUrl = gif.getJSONObject("images")
                    .getJSONObject("fixed_height_small")
                    .getString("url");
                
                ImageView gifPreview = new ImageView(new Image(gifUrl, true));
                gifPreview.setFitHeight(100);
                gifPreview.setPreserveRatio(true);
                gifPreview.setOnMouseClicked(e -> selectGif(gifUrl));
                
                // Add loading indicator
                ProgressIndicator loadingIndicator = new ProgressIndicator();
                loadingIndicator.setPrefSize(30, 30);
                
                // Replace loading indicator with gif when loaded
                gifPreview.imageProperty().addListener((obs, oldImg, newImg) -> {
                    if (newImg != null && !newImg.isError()) {
                        System.out.println("GIF loaded successfully: " + gifUrl);
                    } else if (newImg != null && newImg.isError()) {
                        System.out.println("Failed to load GIF: " + gifUrl);
                    }
                });
                
                gifPreviewPane.getChildren().add(gifPreview);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "GIF Search Failed", "Failed to search GIFs: " + e.getMessage());
        }
    }

    private void selectGif(String gifUrl) {
        selectedGifUrl = gifUrl;
        selectedGifPreview.setImage(new Image(gifUrl));
        selectedGifPreview.setVisible(true);
        clearGifButton.setVisible(true);
        gifPreviewPane.getChildren().clear();
    }

    @FXML
    private void handleClearGif() {
        selectedGifUrl = null;
        selectedGifPreview.setImage(null);
        selectedGifPreview.setVisible(false);
        clearGifButton.setVisible(false);
    }
}

