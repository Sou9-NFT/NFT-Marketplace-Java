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
import java.util.concurrent.CompletableFuture;

import org.esprit.models.Artwork;
import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.ArtworkService;
import org.esprit.services.CategoryService;
import org.esprit.services.StabilityAIService;

import javafx.application.Platform;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ArtworkManagementController {
    // Existing fields for upload tab
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
    
    // New fields for AI generation tab
    @FXML private TextField aiTitleField;
    @FXML private TextField aiPriceField;
    @FXML private TextArea aiDescriptionArea;
    @FXML private ComboBox<Category> aiCategoryComboBox;
    @FXML private TextArea aiPromptArea;
    @FXML private ComboBox<String> aiStyleComboBox;
    @FXML private ImageView aiPreviewImageView;
    @FXML private ProgressBar generationProgressBar;
    @FXML private Label generationStatusLabel;
    @FXML private Label aiErrorMessageLabel;
    @FXML private Button generateButton;
    @FXML private Button aiSubmitButton;
    
    // Table view components
    @FXML private TableView<Artwork> artworksTableView;
    @FXML private TableColumn<Artwork, Integer> idColumn;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> categoryColumn;
    @FXML private TableColumn<Artwork, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<Artwork, Artwork> actionsColumn;
    @FXML private TableColumn<Artwork, String> imageColumn;
    
    // New components for card view
    @FXML private FlowPane artworksFlowPane;
    @FXML private ComboBox<Category> filterCategoryComboBox;
    
    // Service fields
    private User currentUser;
    private File selectedFile;
    private ArtworkService artworkService;
    private CategoryService categoryService;
    private StabilityAIService stabilityAIService;
    private ObservableList<Artwork> userArtworks;
    private final String UPLOAD_DIRECTORY = "src/main/resources/uploads/";
    private boolean isFromAdminDashboard = false; // Track if accessed from admin dashboard
    
    // New field for AI-generated image
    private String generatedImageName;
    
    public void initialize() {
        artworkService = new ArtworkService();
        categoryService = new CategoryService();
        stabilityAIService = new StabilityAIService();
        userArtworks = FXCollections.observableArrayList();
        
        try {
            // Load categories for both tabs
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
            
            // Setup filter category ComboBox
            setupFilterComboBox();
            
            // Clear error messages
            errorMessageLabel.setText("");
            aiErrorMessageLabel.setText("");
            
            // Initialize AI style options
            aiStyleComboBox.setItems(FXCollections.observableArrayList(stabilityAIService.getArtStyles()));
            aiStyleComboBox.getSelectionModel().selectFirst(); // Select "Photorealistic" by default
            
            // Initially disable AI submit button until an image is generated
            aiSubmitButton.setDisable(true);
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Initialization Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Existing methods...
    
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
        
        // Actions column with buttons - EXACTLY matching CategoryManagementController implementation
        actionsColumn.setCellFactory(column -> new TableCell<Artwork, Artwork>() {
            private final Button viewBtn = new Button("View");
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttonsBox = new HBox(5, viewBtn, updateBtn, deleteBtn);

            {
                // View button action
                viewBtn.setOnAction((ActionEvent event) -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        Artwork artwork = getTableView().getItems().get(index);
                        viewArtwork(artwork);
                    }
                });
                
                // Update button action
                updateBtn.setOnAction((ActionEvent event) -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        Artwork artwork = getTableView().getItems().get(index);
                        updateArtwork(artwork);
                    }
                });
                
                // Delete button action
                deleteBtn.setOnAction((ActionEvent event) -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        Artwork artwork = getTableView().getItems().get(index);
                        deleteArtwork(artwork);
                    }
                });
            }

            @Override
            public void updateItem(Artwork item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsBox);
                }
            }
        });
    }
    
    private void loadCategories() throws Exception {
        List<Category> categories = categoryService.getAll();
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        aiCategoryComboBox.setItems(FXCollections.observableArrayList(categories));
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
                
                // Display artworks as cards
                displayArtworksAsCards(artworks);
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
    
    /**
     * Handle the generation of AI artwork using the Stability API
     */
    @FXML
    private void handleGenerateAIImage() {
        // Validate prompt and category
        String prompt = aiPromptArea.getText().trim();
        if (prompt.isEmpty()) {
            aiErrorMessageLabel.setText("Please enter a detailed prompt for the AI");
            return;
        }
        
        Category selectedCategory = aiCategoryComboBox.getValue();
        if (selectedCategory == null) {
            aiErrorMessageLabel.setText("Please select a category before generating an image");
            return;
        }
        
        String selectedStyle = aiStyleComboBox.getValue();
        
        // Show progress indicator
        generationProgressBar.setVisible(true);
        generationProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        generationStatusLabel.setText("Generating your artwork...");
        
        // Disable button during generation
        generateButton.setDisable(true);
        aiErrorMessageLabel.setText("");
        
        // Call the AI service to generate the image
        CompletableFuture<String> futureImage = stabilityAIService.generateImage(prompt, selectedStyle);
        
        futureImage.thenAccept(imageName -> {
            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                try {
                    // Update preview with the generated image
                    File imageFile = new File(UPLOAD_DIRECTORY + imageName);
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        aiPreviewImageView.setImage(image);
                        
                        // Store the generated image name
                        generatedImageName = imageName;
                        
                        // Show success status
                        generationStatusLabel.setText("Image generated successfully!");
                        
                        // Enable submit button
                        aiSubmitButton.setDisable(false);
                    } else {
                        generationStatusLabel.setText("Error: Generated image not found");
                    }
                } catch (Exception e) {
                    generationStatusLabel.setText("Error displaying generated image");
                    e.printStackTrace();
                } finally {
                    // Hide progress bar and re-enable generate button
                    generationProgressBar.setVisible(false);
                    generateButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            // Handle errors
            Platform.runLater(() -> {
                generationProgressBar.setVisible(false);
                generateButton.setDisable(false);
                generationStatusLabel.setText("");
                aiErrorMessageLabel.setText("Failed to generate image: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * Validate the AI artwork form before submission
     */
    private boolean validateAIForm() {
        try {
            // Create a temporary artwork object for validation
            Artwork artwork = new Artwork();
            
            // Validate title
            if (aiTitleField.getText() != null && !aiTitleField.getText().trim().isEmpty()) {
                artwork.setTitle(aiTitleField.getText().trim());
            } else {
                aiErrorMessageLabel.setText("Title is required.");
                return false;
            }
            
            // Validate price
            try {
                double price = Double.parseDouble(aiPriceField.getText().trim());
                artwork.setPrice(price);
            } catch (NumberFormatException e) {
                aiErrorMessageLabel.setText("Price must be a valid number.");
                return false;
            } catch (IllegalArgumentException e) {
                aiErrorMessageLabel.setText(e.getMessage());
                return false;
            }
            
            // Validate description
            if (aiDescriptionArea.getText() != null && !aiDescriptionArea.getText().trim().isEmpty()) {
                artwork.setDescription(aiDescriptionArea.getText().trim());
            } else {
                aiErrorMessageLabel.setText("Description is required.");
                return false;
            }
            
            // Check category
            if (aiCategoryComboBox.getValue() == null) {
                aiErrorMessageLabel.setText("Category must be selected.");
                return false;
            }
            
            // Check if image was generated
            if (generatedImageName == null || generatedImageName.isEmpty()) {
                aiErrorMessageLabel.setText("You must generate an image first.");
                return false;
            }
            
            // All validations passed
            aiErrorMessageLabel.setText("");
            return true;
            
        } catch (IllegalArgumentException e) {
            // Catch any validation errors from the Artwork entity
            aiErrorMessageLabel.setText(e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle the submission of the AI-generated artwork
     */
    @FXML
    private void handleSubmitAIArtwork() {
        // Validate form
        if (!validateAIForm()) {
            return;
        }
        
        try {
            // Create the artwork object
            Artwork artwork = new Artwork();
            artwork.setTitle(aiTitleField.getText().trim());
            artwork.setDescription(aiDescriptionArea.getText().trim());
            artwork.setPrice(Double.parseDouble(aiPriceField.getText().trim()));
            artwork.setCategoryId(aiCategoryComboBox.getValue().getId());
            artwork.setCreatorId(currentUser.getId());
            artwork.setOwnerId(currentUser.getId()); // Initially creator is also the owner
            artwork.setImageName(generatedImageName);
            artwork.setCreatedAt(LocalDateTime.now());
            artwork.setUpdatedAt(LocalDateTime.now());
            
            // Perform final validation
            artwork.validate();
            
            // Save to the database
            artworkService.add(artwork);
            
            // Show success message
            showAlert(AlertType.INFORMATION, "Success", 
                     "AI-generated artwork '" + artwork.getTitle() + "' created successfully.");
            
            // Clear form
            clearAIForm();
            
            // Refresh the table
            loadUserArtworks();
            
        } catch (IllegalArgumentException e) {
            showAlert(AlertType.ERROR, "Validation Error", e.getMessage());
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to create artwork: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear the AI artwork form fields
     */
    private void clearAIForm() {
        aiTitleField.clear();
        aiPriceField.clear();
        aiDescriptionArea.clear();
        aiPromptArea.clear();
        aiCategoryComboBox.getSelectionModel().clearSelection();
        aiStyleComboBox.getSelectionModel().selectFirst();
        aiPreviewImageView.setImage(null);
        generatedImageName = null;
        aiErrorMessageLabel.setText("");
        generationStatusLabel.setText("");
        aiSubmitButton.setDisable(true);
    }
    
    private boolean validateForm() {
        try {
            // Create a temporary artwork object for validation
            Artwork artwork = new Artwork();
            
            // Validate title
            if (titleField.getText() != null && !titleField.getText().trim().isEmpty()) {
                artwork.setTitle(titleField.getText().trim());
            } else {
                errorMessageLabel.setText("Title is required.");
                return false;
            }
            
            // Validate price
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                artwork.setPrice(price);
            } catch (NumberFormatException e) {
                errorMessageLabel.setText("Price must be a valid number.");
                return false;
            } catch (IllegalArgumentException e) {
                errorMessageLabel.setText(e.getMessage());
                return false;
            }
            
            // Validate description
            if (descriptionArea.getText() != null && !descriptionArea.getText().trim().isEmpty()) {
                artwork.setDescription(descriptionArea.getText().trim());
            } else {
                errorMessageLabel.setText("Description is required.");
                return false;
            }
            
            // Check category
            if (categoryComboBox.getValue() == null) {
                errorMessageLabel.setText("Category must be selected.");
                return false;
            }
            
            // Check file
            if (selectedFile == null) {
                errorMessageLabel.setText("You must upload a file.");
                return false;
            }
            
            // All validations passed
            errorMessageLabel.setText("");
            return true;
            
        } catch (IllegalArgumentException e) {
            // Catch any validation errors from the Artwork entity
            errorMessageLabel.setText(e.getMessage());
            return false;
        }
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
            
            // Perform final validation
            artwork.validate();
            
            // Save to the database
            artworkService.add(artwork);
            
            // Show success message
            showAlert(AlertType.INFORMATION, "Success", 
                     "Artwork '" + artwork.getTitle() + "' created successfully.");
            
            // Clear form
            clearForm();
            
            // Refresh the table
            loadUserArtworks();
            
        } catch (IllegalArgumentException e) {
            showAlert(AlertType.ERROR, "Validation Error", e.getMessage());
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to create artwork: " + e.getMessage());
            e.printStackTrace();
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
            detailsImageView.setFitWidth(450);
            detailsImageView.setPreserveRatio(true);
            
            boolean imageLoaded = false;
            
            // Try to load image from multiple possible locations
            try {
                // First try: Check upload directory
                File imageFile = new File(UPLOAD_DIRECTORY + artwork.getImageName());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    if (!image.isError()) {
                        detailsImageView.setImage(image);
                        imageLoaded = true;
                    }
                }
                
                // Second try: Check class resources
                if (!imageLoaded) {
                    ClassLoader classLoader = getClass().getClassLoader();
                    java.net.URL imageUrl = classLoader.getResource("uploads/" + artwork.getImageName());
                    if (imageUrl != null) {
                        Image image = new Image(imageUrl.toString());
                        if (!image.isError()) {
                            detailsImageView.setImage(image);
                            imageLoaded = true;
                        }
                    }
                }
                
                // Third try: Check target directory
                if (!imageLoaded) {
                    File targetFile = new File("target/classes/uploads/" + artwork.getImageName());
                    if (targetFile.exists()) {
                        Image image = new Image(targetFile.toURI().toString());
                        if (!image.isError()) {
                            detailsImageView.setImage(image);
                            imageLoaded = true;
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
            
            // Create a container for the image
            StackPane imageContainer = new StackPane();
            imageContainer.getChildren().add(detailsImageView);
            imageContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-radius: 3;");
            imageContainer.setMinHeight(300);
            
            // If image wasn't loaded, show placeholder
            if (!imageLoaded) {
                Label placeholderLabel = new Label("Image Not Available");
                placeholderLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #757575; -fx-font-weight: bold;");
                imageContainer.getChildren().add(placeholderLabel);
            }
            
            // Add image container to content
            content.getChildren().add(imageContainer);
            
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
                new Label("Description:")
            );
            
            // Add description text area
            TextArea descArea = new TextArea(artwork.getDescription());
            descArea.setEditable(false);
            descArea.setWrapText(true);
            descArea.setPrefRowCount(4);
            content.getChildren().add(descArea);
            
            // Resize dialog to fit content better
            dialog.getDialogPane().setPrefWidth(500);
            dialog.getDialogPane().setMinHeight(550);
            
            // Set content and show dialog
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
            
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Could not display artwork details: " + e.getMessage());
            e.printStackTrace();
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
                try {
                    // Update artwork details using the entity's validation
                    artwork.setTitle(titleField.getText().trim());
                    artwork.setPrice(Double.parseDouble(priceField.getText().trim()));
                    artwork.setDescription(descriptionArea.getText().trim());
                    artwork.setCategoryId(categoryComboBox.getValue().getId());
                    artwork.setUpdatedAt(LocalDateTime.now());
                    
                    // Final validation
                    artwork.validate();
                    
                    // Save changes to the database
                    artworkService.update(artwork);
                    
                    // Refresh the table
                    loadUserArtworks();
                    
                    showAlert(AlertType.INFORMATION, "Success", 
                             "Artwork '" + artwork.getTitle() + "' updated successfully.");
                } catch (NumberFormatException e) {
                    showAlert(AlertType.ERROR, "Validation Error", "Price must be a valid number.");
                } catch (IllegalArgumentException e) {
                    showAlert(AlertType.ERROR, "Validation Error", e.getMessage());
                }
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
            
            // Ensure the stage is fullscreen
            stage.setMaximized(true);
            
            // Call the appropriate controller method to set fullscreen
            if (isFromAdminDashboard) {
                AdminDashboardController controller = loader.getController();
                controller.setStageFullScreen();
            } else {
                UserDashboardController controller = loader.getController();
                controller.setStageFullScreen();
            }
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
    
    /**
     * Setup the category filter ComboBox
     */
    private void setupFilterComboBox() {
        try {
            // Create "All Categories" option
            Category allCategories = new Category();
            allCategories.setId(-1);
            allCategories.setName("All Categories");
            
            // Load categories
            List<Category> categories = categoryService.getAll();
            
            // Create observable list with "All Categories" at the top
            ObservableList<Category> filterCategories = FXCollections.observableArrayList();
            filterCategories.add(allCategories);
            filterCategories.addAll(categories);
            
            // Set items and default selection
            filterCategoryComboBox.setItems(filterCategories);
            filterCategoryComboBox.getSelectionModel().selectFirst();
            
            // Add change listener to filter artworks
            filterCategoryComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null) {
                    filterAndDisplayArtworks(newValue);
                }
            });
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Setup Error", "Failed to setup category filter: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Filter artworks by category and display them
     */
    private void filterAndDisplayArtworks(Category category) {
        try {
            if (currentUser != null) {
                List<Artwork> artworks;
                
                // Get all user artworks
                artworks = artworkService.getByCreator(currentUser.getId());
                
                // Filter by category if not "All Categories"
                if (category.getId() != -1) {
                    artworks = artworks.stream()
                        .filter(artwork -> artwork.getCategoryId() == category.getId())
                        .toList();
                }
                
                // Update the observable list (for TableView compatibility)
                userArtworks.setAll(artworks);
                
                // Display as cards
                displayArtworksAsCards(artworks);
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Filter Error", "Failed to filter artworks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Display artworks as cards in the FlowPane
     */
    private void displayArtworksAsCards(List<Artwork> artworks) {
        try {
            // Check if FlowPane exists
            if (artworksFlowPane == null) {
                System.err.println("Warning: artworksFlowPane is null. Skipping card display.");
                return;
            }
            
            // Clear existing cards
            artworksFlowPane.getChildren().clear();
            
            // Create cards for each artwork
            for (Artwork artwork : artworks) {
                VBox card = createArtworkCard(artwork);
                artworksFlowPane.getChildren().add(card);
            }
            
            // Show message if no artworks
            if (artworks.isEmpty()) {
                Label noArtworksLabel = new Label("No artworks found");
                noArtworksLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #757575;");
                artworksFlowPane.getChildren().add(noArtworksLabel);
            }
        } catch (Exception e) {
            System.err.println("Error displaying artwork cards: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a card view for an individual artwork
     */
    private VBox createArtworkCard(Artwork artwork) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER);
        
        // Image container
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-radius: 3;");
        imageContainer.setPrefHeight(180);
        imageContainer.setMinHeight(180);
        
        // Artwork image
        ImageView artworkImage = new ImageView();
        artworkImage.setFitWidth(240);
        artworkImage.setFitHeight(180);
        artworkImage.setPreserveRatio(true);
        
        try {
            File imageFile = new File(UPLOAD_DIRECTORY + artwork.getImageName());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                artworkImage.setImage(image);
            } else {
                // Try alternative paths
                ClassLoader classLoader = getClass().getClassLoader();
                java.net.URL imageUrl = classLoader.getResource("uploads/" + artwork.getImageName());
                
                if (imageUrl != null) {
                    Image image = new Image(imageUrl.toString());
                    artworkImage.setImage(image);
                } else {
                    // Create placeholder text if image not found
                    Label placeholderLabel = new Label("Image\nNot Found");
                    placeholderLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #757575; -fx-font-weight: bold; -fx-alignment: center;");
                    placeholderLabel.setWrapText(true);
                    placeholderLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    imageContainer.getChildren().add(placeholderLabel);
                }
            }
            
            imageContainer.getChildren().add(artworkImage);
        } catch (Exception e) {
            System.err.println("Error loading artwork image: " + e.getMessage());
        }
        
        // Artwork title
        Label titleLabel = new Label(artwork.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);
        
        // Price
        Label priceLabel = new Label(String.format("$%.2f", artwork.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #388E3C;");
        
        // Get category name
        String categoryName = "Unknown";
        try {
            Category category = categoryService.getOne(artwork.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        } catch (Exception e) {
            // Use default name if error occurs
        }
        Label categoryLabel = new Label("Category: " + categoryName);
        
        // Buttons container
        HBox buttonsBox = new HBox(5);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button viewButton = new Button("View");
        viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        viewButton.setOnAction(e -> viewArtwork(artwork));
        
        Button updateButton = new Button("Update");
        updateButton.setStyle("-fx-background-color: #FFA000; -fx-text-fill: white;");
        updateButton.setOnAction(e -> updateArtwork(artwork));
        
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteArtwork(artwork));
        
        buttonsBox.getChildren().addAll(viewButton, updateButton, deleteButton);
        
        // Add all components to card
        card.getChildren().addAll(
            imageContainer,
            titleLabel,
            priceLabel,
            categoryLabel,
            buttonsBox
        );
        
        return card;
    }
}