package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import org.esprit.models.Blog;
import org.esprit.models.User;
import org.esprit.services.BlogService;

public class BlogListController implements Initializable {

    @FXML private ListView<Blog> blogListView;
    @FXML private TextField searchField;
    
    private BlogService blogService;
    private User currentUser;
    private final String UPLOAD_DIR = "src/main/resources/uploads/";
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        blogService = new BlogService();
        
        // Setup blog list view with custom cell factory
        setupBlogListView();
        
        // Add search functionality
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterBlogs(newValue);
            });
        }
        
        // Load initial blogs
        refreshBlogList();
    }
    
    private void setupBlogListView() {
        blogListView.setCellFactory(lv -> new ListCell<Blog>() {
            @Override
            protected void updateItem(Blog blog, boolean empty) {
                super.updateItem(blog, empty);
                if (empty || blog == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(10);
                    container.setPadding(new Insets(15));
                    container.getStyleClass().add("blog-card");
                    
                    // Author info section
                    HBox authorInfo = new HBox(10);
                    authorInfo.setAlignment(Pos.CENTER_LEFT);
                    
                    // Profile picture
                    ImageView profilePic = new ImageView();
                    profilePic.setFitHeight(50);
                    profilePic.setFitWidth(50);
                    profilePic.setPreserveRatio(true);
                    
                    // Load author's profile picture
                    String profilePicPath = UPLOAD_DIR + "user_" + blog.getUser().getId() + "_icon.png";
                    try {
                        Image image = new Image(new File(profilePicPath).toURI().toString());
                        profilePic.setImage(image);
                    } catch (Exception e) {
                        profilePic.setImage(new Image(getClass().getResourceAsStream("/assets/default/profile.png")));
                    }
                    
                    // Make profile picture circular
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
        
        // Add click handler for blog items
        blogListView.setOnMouseClicked(event -> {
            Blog selectedBlog = blogListView.getSelectionModel().getSelectedItem();
            if (selectedBlog != null && event.getClickCount() == 2) {
                showBlogDetails(selectedBlog);
            }
        });
    }
    
    private void showBlogDetails(Blog blog) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Blog.fxml"));
            Parent detailView = loader.load();
            
            BlogDetailController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setBlog(blog);
            
            Scene currentScene = blogListView.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(detailView));
            stage.setTitle("NFT Marketplace - " + blog.getTitle());
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open blog details: " + e.getMessage());
        }
    }
    
    private void filterBlogs(String searchText) {
        try {
            List<Blog> allBlogs = blogService.readAll();
            if (searchText == null || searchText.isEmpty()) {
                blogListView.setItems(FXCollections.observableArrayList(allBlogs));
            } else {
                String lowerCaseFilter = searchText.toLowerCase();
                List<Blog> filteredList = allBlogs.stream()
                    .filter(blog -> 
                        blog.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                        blog.getContent().toLowerCase().contains(lowerCaseFilter) ||
                        blog.getUser().getName().toLowerCase().contains(lowerCaseFilter))
                    .toList();
                blogListView.setItems(FXCollections.observableArrayList(filteredList));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to filter blogs: " + e.getMessage());
        }
    }
    
    private void refreshBlogList() {
        try {
            List<Blog> blogs = blogService.readAll();
            blogListView.setItems(FXCollections.observableArrayList(blogs));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load blogs: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBackToHome(ActionEvent event) {
        try {
            String fxmlPath = "/fxml/UserDashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();
            
            UserDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Scene currentScene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            stage.setScene(new Scene(dashboardView));
            stage.setTitle("NFT Marketplace - User Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not return to dashboard: " + e.getMessage());
        }
    }    @FXML
    private void handleCreateBlog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateBlog.fxml"));
            Parent createView = loader.load();
            
            CreateBlogController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // Create new stage for blog creation
            Stage createStage = new Stage();
            createStage.setScene(new Scene(createView));
            createStage.setTitle("Create New Blog");
            createStage.initOwner(blogListView.getScene().getWindow());
            createStage.setResizable(false);
            
            // Show the dialog and wait for it to close
            createStage.showAndWait();
            
            // Refresh the blog list after dialog closes
            refreshBlogList();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open create blog form: " + e.getMessage());
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshBlogList(); // Refresh list after setting user
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
