package org.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.esprit.models.Artwork;
import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.CategoryService;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArtworkManagementController {
    @FXML private TextField titleField;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Button uploadButton;
    @FXML private Label fileNameLabel;
    @FXML private Label allowedTypesLabel;
    @FXML private Label errorMessageLabel;
    @FXML private ImageView previewImageView;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private Button refreshButton;
    
    @FXML private TableView<Artwork> artworksTableView;
    @FXML private TableColumn<Artwork, Integer> idColumn;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> categoryColumn;
    @FXML private TableColumn<Artwork, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<Artwork, Artwork> actionsColumn;
    @FXML private TableColumn<Artwork, String> imageColumn;
    
    private User currentUser;
    private File selectedFile;
    private ArtworkService artworkService;
    private CategoryService categoryService;
    private ObservableList<Artwork> userArtworks;
    private final String UPLOAD_DIRECTORY = "src/main/resources/uploads/";
    private boolean isFromAdminDashboard = false; // Track if accessed from admin dashboard
    
    public void initialize() {
        artworkService = new ArtworkService();
        categoryService = new CategoryService();
        userArtworks = FXCollections.observableArrayList();
        
        try {
            // Load categories
            loadCategories();
            
            // Setup category change listener to show allowed file types
            categoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    List<String> allowedTypes = newVal.getAllowedMimeTypes();
                    allowedTypesLabel.setText("Allowed types: " + String.join(", ", allowedTypes));
                } else {
                    allowedTypesLabel.setText("");
                }
            });
            
            // Clear error message
            errorMessageLabel.setText("");
            
            // Setup table columns
            setupTableColumns();
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Initialization Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        priceColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrice()));
        
        // For category name, we need to fetch the category
        categoryColumn.setCellValueFactory(cellData -> {
            try {
                Category category = categoryService.getOne(cellData.getValue().getCategoryId());
                return new SimpleStringProperty(category != null ? category.getName() : "Unknown");
            } catch (Exception e) {
                return new SimpleStringProperty("Error");
            }
        });
        
        // Format date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        createdAtColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getCreatedAt()));
        createdAtColumn.setCellFactory(column -> new TableCell<Artwork, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        // Image preview column
        imageColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getImageName()));
        imageColumn.setCellFactory(column -> new TableCell<Artwork, String>() {
            private final ImageView imageView = new ImageView();
            
            {
                imageView.setFitHeight(80);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);
            }
            
            @Override
            protected void updateItem(String imageName, boolean empty) {
                super.updateItem(imageName, empty);
                
                if (imageName == null || empty) {
                    setGraphic(null);
                } else {
                    try {
                        File imageFile = new File(UPLOAD_DIRECTORY + imageName);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            imageView.setImage(image);
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                            setText("Image not found");
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                        setText("Error loading");
                    }
                }
            }
        });
        
        // Actions column with buttons
        actionsColumn.setCellFactory(column -> new TableCell<Artwork, Artwork>() {
            private final Button viewBtn = new Button("View");
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            
            {
                // Configure action buttons with more compact style to fit in column
                String baseButtonStyle = "-fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8 3 8;";
                
                viewBtn.setStyle("-fx-background-color: #4CAF50; " + baseButtonStyle);
                viewBtn.setOnAction(event -> {
                    Artwork artwork = getTableView().getItems().get(getIndex());
                    viewArtwork(artwork);
                });
                
                updateBtn.setStyle("-fx-background-color: #2196F3; " + baseButtonStyle);
                updateBtn.setOnAction(event -> {
                    Artwork artwork = getTableView().getItems().get(getIndex());
                    updateArtwork(artwork);
                });
                
                deleteBtn.setStyle("-fx-background-color: #f44336; " + baseButtonStyle);
                deleteBtn.setOnAction(event -> {
                    Artwork artwork = getTableView().getItems().get(getIndex());
                    deleteArtwork(artwork);
                });
            }
            
            @Override
            protected void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(3, viewBtn, updateBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void loadCategories() throws Exception {
        List<Category> categories = categoryService.getAll();
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        // Load user's artworks when the user is set
        if (user != null) {
            loadUserArtworks();
        }
    }
    
    /**
     * Sets the current user and marks if coming from admin dashboard
     * @param user The current user
     * @param isAdmin Whether the user is coming from the admin dashboard
     */
    public void setCurrentUser(User user, boolean isAdmin) {
        this.currentUser = user;
        this.isFromAdminDashboard = isAdmin;
        
        // Load user's artworks when the user is set
        if (user != null) {
            loadUserArtworks();
        }
    }
    
    private void loadUserArtworks() {
        try {
            if (currentUser != null) {
                List<Artwork> artworks = artworkService.getByCreator(currentUser.getId());
                userArtworks.setAll(artworks);
                artworksTableView.setItems(userArtworks);
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Load Error", "Failed to load artworks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRefreshArtworks() {
        loadUserArtworks();
    }
    
    @FXML
    private void handleFileUpload() {
        // Get selected category to check allowed MIME types
        Category selectedCategory = categoryComboBox.getValue();
        if (selectedCategory == null) {
            showAlert(AlertType.WARNING, "Category Required", "Please select a category before uploading a file.");
            return;
        }
        
        List<String> allowedExtensions = selectedCategory.getAllowedMimeTypes();
        
        // Create file chooser with filters based on allowed types
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Artwork File");
        
        // Add filters based on allowed MIME types
        if (allowedExtensions.contains("image/jpeg") || allowedExtensions.contains("image/jpg")) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg")
            );
        }
        if (allowedExtensions.contains("image/png")) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Images", "*.png")
            );
        }
        if (allowedExtensions.contains("image/gif")) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("GIF Images", "*.gif")
            );
        }
        
        // Show open file dialog
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            // Update the file name label
            fileNameLabel.setText(selectedFile.getName());
            
            // Validate file type
            String fileExtension = getExtension(selectedFile.getName()).toLowerCase();
            boolean isValid = false;
            
            for (String mimeType : allowedExtensions) {
                if ((mimeType.equals("image/jpeg") || mimeType.equals("image/jpg")) && 
                    (fileExtension.equals("jpg") || fileExtension.equals("jpeg"))) {
                    isValid = true;
                    break;
                }
                if (mimeType.equals("image/png") && fileExtension.equals("png")) {
                    isValid = true;
                    break;
                }
                if (mimeType.equals("image/gif") && fileExtension.equals("gif")) {
                    isValid = true;
                    break;
                }
            }
            
            if (!isValid) {
                showAlert(AlertType.ERROR, "Invalid File", 
                          "The selected file type is not allowed for this category. Allowed types: " 
                          + String.join(", ", allowedExtensions));
                selectedFile = null;
                fileNameLabel.setText("No file selected");
                return;
            }
            
            // Show preview if it's an image
            try {
                if (fileExtension.matches("jpg|jpeg|png|gif")) {
                    Image image = new Image(selectedFile.toURI().toString());
                    previewImageView.setImage(image);
                }
            } catch (Exception e) {
                showAlert(AlertType.WARNING, "Preview Error", 
                          "Could not preview the image: " + e.getMessage());
            }
        }
    }
    
    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    @FXML
    private void handleSubmitArtwork() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        
        try {
            // Create upload directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Generate a unique file name
            String uniqueFileName = generateUniqueFileName(selectedFile.getName());
            Path destination = Paths.get(UPLOAD_DIRECTORY + uniqueFileName);
            
            // Copy the file to the uploads directory
            Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            
            // Create the artwork object
            Artwork artwork = new Artwork();
            artwork.setTitle(titleField.getText().trim());
            artwork.setDescription(descriptionArea.getText().trim());
            artwork.setPrice(Double.parseDouble(priceField.getText().trim()));
            artwork.setCategoryId(categoryComboBox.getValue().getId());
            artwork.setCreatorId(currentUser.getId());
            artwork.setOwnerId(currentUser.getId()); // Initially creator is also the owner
            artwork.setImageName(uniqueFileName);
            artwork.setCreatedAt(LocalDateTime.now());
            artwork.setUpdatedAt(LocalDateTime.now());
            
            // Save to the database
            artworkService.add(artwork);
            
            // Show success message
            showAlert(AlertType.INFORMATION, "Success", 
                     "Artwork '" + artwork.getTitle() + "' created successfully.");
            
            // Clear form
            clearForm();
            
            // Refresh the table
            loadUserArtworks();
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to create artwork: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        
        // Check title
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errorMessage.append("Title is required.\n");
        }
        
        // Check price
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                errorMessage.append("Price must be greater than 0.\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Price must be a valid number.\n");
        }
        
        // Check description
        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            errorMessage.append("Description is required.\n");
        }
        
        // Check category
        if (categoryComboBox.getValue() == null) {
            errorMessage.append("Category must be selected.\n");
        }
        
        // Check file
        if (selectedFile == null) {
            errorMessage.append("You must upload a file.\n");
        }
        
        // Display error message if any
        if (errorMessage.length() > 0) {
            errorMessageLabel.setText(errorMessage.toString());
            return false;
        } else {
            errorMessageLabel.setText("");
            return true;
        }
    }
    
    private void clearForm() {
        titleField.clear();
        priceField.clear();
        descriptionArea.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        selectedFile = null;
        fileNameLabel.setText("No file selected");
        previewImageView.setImage(null);
        errorMessageLabel.setText("");
    }
    
    private String generateUniqueFileName(String originalName) {
        String extension = getExtension(originalName);
        return "user_" + currentUser.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + 
               (extension.isEmpty() ? "" : "." + extension);
    }
    
    private void viewArtwork(Artwork artwork) {
        try {
            // Create dialog to show artwork details with larger preview
            Alert dialog = new Alert(AlertType.INFORMATION);
            dialog.setTitle("Artwork Details");
            dialog.setHeaderText(artwork.getTitle());
            
            // Create content for the dialog
            VBox content = new VBox(10);
            content.setPadding(new javafx.geometry.Insets(20));
            
            // Create image view with larger image
            ImageView detailsImageView = new ImageView();
            detailsImageView.setFitHeight(300);
            detailsImageView.setPreserveRatio(true);
            
            File imageFile = new File(UPLOAD_DIRECTORY + artwork.getImageName());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                detailsImageView.setImage(image);
            }
            
            // Add image to content
            content.getChildren().add(detailsImageView);
            
            // Get category name
            String categoryName = "Unknown";
            try {
                Category category = categoryService.getOne(artwork.getCategoryId());
                if (category != null) {
                    categoryName = category.getName();
                }
            } catch (Exception e) {
                // Use default "Unknown" if category can't be found
            }
            
            // Add artwork details
            content.getChildren().addAll(
                new Label("Title: " + artwork.getTitle()),
                new Label("Price: $" + artwork.getPrice()),
                new Label("Category: " + categoryName),
                new Label("Created: " + 
                    (artwork.getCreatedAt() != null ? 
                    artwork.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown")),
                new Label("Description:"),
                new TextArea(artwork.getDescription())
            );
            
            // Make description text area read-only and auto-resize
            TextArea descArea = (TextArea) content.getChildren().get(5);
            descArea.setEditable(false);
            descArea.setWrapText(true);
            descArea.setPrefRowCount(4);
            
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Could not display artwork details: " + e.getMessage());
        }
    }
    
    private void updateArtwork(Artwork artwork) {
        try {
            // Create dialog to update artwork details
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Update Artwork");
            dialog.setHeaderText("Update details for '" + artwork.getTitle() + "'");
            
            // Create content for the dialog
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            // Create fields and populate with current artwork details
            TextField titleField = new TextField(artwork.getTitle());
            TextField priceField = new TextField(String.valueOf(artwork.getPrice()));
            TextArea descriptionArea = new TextArea(artwork.getDescription());
            descriptionArea.setWrapText(true);
            descriptionArea.setPrefRowCount(4);
            
            // Get current category and load all categories for ComboBox
            ComboBox<Category> categoryComboBox = new ComboBox<>();
            try {
                List<Category> categories = categoryService.getAll();
                categoryComboBox.setItems(FXCollections.observableArrayList(categories));
                
                // Set current category as selected
                Category currentCategory = categoryService.getOne(artwork.getCategoryId());
                if (currentCategory != null) {
                    categoryComboBox.setValue(currentCategory);
                }
            } catch (Exception e) {
                System.err.println("Error loading categories: " + e.getMessage());
            }
            
            // Preview current image
            ImageView imagePreview = new ImageView();
            imagePreview.setFitHeight(150);
            imagePreview.setPreserveRatio(true);
            
            File imageFile = new File(UPLOAD_DIRECTORY + artwork.getImageName());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                imagePreview.setImage(image);
            }
            
            Label currentImageLabel = new Label("Current Image:");
            HBox imageBox = new HBox(10, currentImageLabel, imagePreview);
            imageBox.setAlignment(Pos.CENTER_LEFT);
            
            // Add all fields to the content
            content.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Price:"), priceField,
                new Label("Category:"), categoryComboBox,
                new Label("Description:"), descriptionArea,
                imageBox
            );
            
            // Set dialog content and buttons
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Handle OK button action
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Validate the input
                if (titleField.getText().trim().isEmpty()) {
                    showAlert(AlertType.ERROR, "Validation Error", "Title cannot be empty.");
                    return;
                }
                
                double price;
                try {
                    price = Double.parseDouble(priceField.getText().trim());
                    if (price <= 0) {
                        showAlert(AlertType.ERROR, "Validation Error", "Price must be greater than 0.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert(AlertType.ERROR, "Validation Error", "Price must be a valid number.");
                    return;
                }
                
                if (descriptionArea.getText().trim().isEmpty()) {
                    showAlert(AlertType.ERROR, "Validation Error", "Description cannot be empty.");
                    return;
                }
                
                if (categoryComboBox.getValue() == null) {
                    showAlert(AlertType.ERROR, "Validation Error", "Category must be selected.");
                    return;
                }
                
                // Update artwork details
                artwork.setTitle(titleField.getText().trim());
                artwork.setPrice(price);
                artwork.setDescription(descriptionArea.getText().trim());
                artwork.setCategoryId(categoryComboBox.getValue().getId());
                artwork.setUpdatedAt(LocalDateTime.now());
                
                // Save changes to the database
                artworkService.update(artwork);
                
                // Refresh the table
                loadUserArtworks();
                
                showAlert(AlertType.INFORMATION, "Success", 
                         "Artwork '" + artwork.getTitle() + "' updated successfully.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to update artwork: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void deleteArtwork(Artwork artwork) {
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Artwork");
        confirmAlert.setContentText("Are you sure you want to delete '" + artwork.getTitle() + "'?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete artwork from the database
                artworkService.delete(artwork);
                
                // Try to delete the file
                try {
                    Files.deleteIfExists(Paths.get(UPLOAD_DIRECTORY + artwork.getImageName()));
                } catch (IOException e) {
                    System.err.println("Warning: Could not delete file: " + e.getMessage());
                }
                
                // Refresh the list
                loadUserArtworks();
                
                showAlert(AlertType.INFORMATION, "Success", 
                         "Artwork '" + artwork.getTitle() + "' deleted successfully.");
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Error", 
                         "Failed to delete artwork: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleBackButton(ActionEvent event) {
        try {
            String fxmlPath;
            String title;
            
            // Determine which dashboard to return to
            if (isFromAdminDashboard) {
                fxmlPath = "/fxml/AdminDashboard.fxml";
                title = "NFT Marketplace - Admin Dashboard";
            } else {
                fxmlPath = "/fxml/UserDashboard.fxml";
                title = "NFT Marketplace - User Dashboard";
            }
            
            // Load the appropriate dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashboardView = loader.load();
            
            // Set the current user on the controller
            if (isFromAdminDashboard) {
                AdminDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else {
                UserDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }
            
            Scene scene = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) scene.getWindow();
            
            scene.setRoot(dashboardView);
            stage.setTitle(title);
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error", 
                     "Failed to return to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}