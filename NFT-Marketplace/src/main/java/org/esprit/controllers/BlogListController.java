package org.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
                    container.getChildren().addAll(authorInfo, contentBox);                    // Create actions box for buttons
                    HBox actions = new HBox(10);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    
                    // Only show delete button if user is authorized
                    if (currentUser != null && (currentUser.getId() == blog.getUser().getId() || 
                            currentUser.getRoles().contains("ROLE_ADMIN"))) {
                        Button deleteButton = new Button("Delete");
                        deleteButton.getStyleClass().add("button-danger");
                        deleteButton.setOnAction(e -> {
                            handleDeleteBlog(blog);
                            e.consume(); // Prevent event bubbling
                        });
                        actions.getChildren().add(deleteButton);
                    }
                    
                    // Add actions to container
                    container.getChildren().add(actions);
                    
                    // Add hover effect
                    container.setOnMouseEntered(e -> container.setStyle("-fx-background-color: #f0f0f0;"));
                    container.setOnMouseExited(e -> container.setStyle(""));
                    
                    // Add click handler for the container (excluding the delete button)
                    container.setOnMouseClicked(e -> {
                        System.out.println("Blog clicked: " + blog.getTitle());
                        showBlogDetails(blog);
                        e.consume(); // Prevent event bubbling
                    });
                    
                    setGraphic(container);
                }
            }
        });
        
        // Add click handler for the ListView itself as a backup
        blogListView.setOnMouseClicked(event -> {
            Blog selectedBlog = blogListView.getSelectionModel().getSelectedItem();
            if (selectedBlog != null && event.getClickCount() == 1) {
                System.out.println("ListView clicked, selected blog: " + selectedBlog.getTitle());
                showBlogDetails(selectedBlog);
            }
        });
    }

    private void showBlogDetails(Blog blog) {
        try {
            if (blog == null) {
                System.err.println("Error: Attempted to open details for null blog");
                showAlert(Alert.AlertType.ERROR, "Error", "Cannot open blog details: Blog data is missing");
                return;
            }

            System.out.println("Opening blog details for blog ID: " + blog.getId());
            
            // Create new stage for blog details
            Stage detailStage = new Stage();            // Load the FXML
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            
            // Try multiple ways to load the FXML
            URL fxmlUrl = getClass().getResource("/fxml/BlogDetail.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getClassLoader().getResource("fxml/BlogDetail.fxml");
            }
            if (fxmlUrl == null) {
                // Try absolute path as last resort
                String absolutePath = System.getProperty("user.dir") + "/src/main/resources/fxml/BlogDetail.fxml";
                System.err.println("Trying absolute path: " + absolutePath);
                File fxmlFile = new File(absolutePath);
                if (fxmlFile.exists()) {
                    fxmlUrl = fxmlFile.toURI().toURL();
                }
            }
            
            if (fxmlUrl == null) {
                System.err.println("Could not find BlogDetail.fxml resource");
                System.err.println("Class loader paths:");
                ClassLoader cl = getClass().getClassLoader();
                while (cl != null) {
                    System.err.println("ClassLoader: " + cl);
                    if (cl instanceof java.net.URLClassLoader) {
                        java.net.URL[] urls = ((java.net.URLClassLoader) cl).getURLs();
                        for (java.net.URL url : urls) {
                            System.err.println("  " + url);
                        }
                    }
                    cl = cl.getParent();
                }
                showAlert(Alert.AlertType.ERROR, "Error", "Could not find blog details view file. Check console for details.");
                return;
            }
            
            System.out.println("Loading FXML from: " + fxmlUrl);
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent detailView;
            try {
                detailView = loader.load();
            } catch (IOException e) {
                System.err.println("Failed to load BlogDetail.fxml: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Could not load blog details view: " + e.getMessage());
                return;
            }
            
            // Get and initialize the controller
            BlogDetailController controller = loader.getController();
            if (controller == null) {
                System.err.println("Failed to get BlogDetailController");
                showAlert(Alert.AlertType.ERROR, "Error", "Could not initialize blog details controller");
                return;
            }
            
            // Set up the controller with required data
            System.out.println("Setting up BlogDetailController...");
            controller.setCurrentUser(currentUser);
            controller.setBlog(blog);
            
            // Create and configure the scene
            Scene scene = new Scene(detailView, 900, 700);
              // Configure the stage
            detailStage.setScene(scene);
            detailStage.setTitle(blog.getTitle());
            detailStage.initOwner(blogListView.getScene().getWindow());
            detailStage.setResizable(true);
            
            // Center the window on screen
            detailStage.centerOnScreen();
            
            // Set the stage to fullscreen
            detailStage.setMaximized(true);
            
            System.out.println("Showing blog details window");
            detailStage.show();
            
        } catch (Exception e) {
            System.err.println("Unexpected error showing blog details: " + e.getMessage());
            e.printStackTrace();
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
            
            // Set the stage to fullscreen before showing it
            stage.setMaximized(true);
            stage.show();
            
            // Call the controller's method to ensure fullscreen is set
            controller.setStageFullScreen();
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
            createStage.setResizable(true);
            
            // Set the stage to fullscreen
            createStage.setMaximized(true);
            
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
    }    private void handleDeleteBlog(Blog blog) {
        if (blog == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No blog selected.");
            return;
        }

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must be logged in to delete a blog.");
            return;
        }

        // Check if the current user is either the blog creator or an admin
        boolean isAdmin = currentUser.getRoles().contains("ROLE_ADMIN");
        boolean isCreator = currentUser.getId() == blog.getUser().getId();
        
        if (!isAdmin && !isCreator) {
            showAlert(Alert.AlertType.ERROR, "Error", "You can only delete your own blogs.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Blog");
        confirmation.setHeaderText("Delete Blog");
        confirmation.setContentText("Are you sure you want to delete this blog?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    blogService.delete(blog);
                    refreshBlogList();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Blog deleted successfully!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete blog: " + e.getMessage());
                }
            }
        });
    }
}
