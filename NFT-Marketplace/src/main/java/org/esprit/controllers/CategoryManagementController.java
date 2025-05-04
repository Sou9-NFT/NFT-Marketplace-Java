package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.CategoryService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CategoryManagementController implements Initializable {

    // Form fields for Create Category tab
    @FXML
    private TextField nameField;
    
    @FXML
    private ComboBox<String> typeComboBox;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private ListView<String> mimeTypesListView;
    
    @FXML
    private Label createTabStatusLabel;
    
    @FXML
    private Label errorMessageLabel;

    // Search and filter controls for List Categories tab
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> filterTypeComboBox;
    
    @FXML
    private Label listTabStatusLabel;

    // Flow pane for card view in List Categories tab
    @FXML
    private FlowPane categoriesFlowPane;
    
    // Tab pane
    @FXML
    private TabPane categoryTabPane;
    
    // Service and data
    private CategoryService categoryService;
    private ObservableList<Category> categoriesList;
    private ObservableList<String> mimeTypesList;
    private Category currentCategory;
    private User currentUser;
    private static final List<String> PREDEFINED_TYPES = Arrays.asList("Image", "Video", "Audio", "3D Model", "Document", "Other");
    private boolean isFromAdminDashboard = true;
    
    // Predefined MIME types map based on category type
    private static final Map<String, List<String>> PREDEFINED_MIME_TYPES = new HashMap<>();
    
    static {
        // Initialize MIME types for different categories
        PREDEFINED_MIME_TYPES.put("Image", Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/webp", "image/tiff", "image/bmp"
        ));
        
        PREDEFINED_MIME_TYPES.put("Video", Arrays.asList(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/x-flv", "video/webm", "video/ogg"
        ));
        
        PREDEFINED_MIME_TYPES.put("Audio", Arrays.asList(
            "audio/mpeg", "audio/mp4", "audio/ogg", "audio/wav", "audio/webm", "audio/aac", "audio/flac"
        ));
        
        PREDEFINED_MIME_TYPES.put("3D Model", Arrays.asList(
            "model/gltf-binary", "model/gltf+json", "model/obj", "model/fbx", "application/octet-stream"
        ));
        
        PREDEFINED_MIME_TYPES.put("Document", Arrays.asList(
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        ));
        
        // For "Other" category, we'll provide some basic common MIME types as examples
        PREDEFINED_MIME_TYPES.put("Other", Arrays.asList(
            "application/json", "application/xml", "application/zip", "application/x-tar"
        ));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        categoryService = new CategoryService();
        
        // Initialize lists
        categoriesList = FXCollections.observableArrayList();
        mimeTypesList = FXCollections.observableArrayList();
        
        // Set up type combo boxes for category creation
        typeComboBox.setItems(FXCollections.observableArrayList(PREDEFINED_TYPES));
        
        // Set up mime types list
        mimeTypesListView.setItems(mimeTypesList);
        
        // Add listener for type selection to automatically populate MIME types
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleTypeChange(newValue);
            }
        });
        
        // Setup dynamic filtering for search
        setupDynamicSearch();
        
        // Clear error messages
        if (errorMessageLabel != null) {
            errorMessageLabel.setText("");
        }
        
        // Load all categories and update the filter combobox
        loadCategories();
    }
    
    /**
     * Setup dynamic search functionality that filters as the user types
     */
    private void setupDynamicSearch() {
        // Add listener to the search field for real-time filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategories(newValue, filterTypeComboBox.getValue());
        });
        
        // Add listener to the filter type combobox for real-time filtering
        filterTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                filterCategories(searchField.getText(), newValue);
            }
        });
    }
    
    /**
     * Setup the filter type ComboBox with dynamically generated types from existing categories
     */
    private void setupFilterComboBox() {
        try {
            Set<String> categoryTypes = new HashSet<>();
            
            // Get all unique category types from the database
            for (Category category : categoriesList) {
                if (category.getType() != null && !category.getType().isEmpty()) {
                    categoryTypes.add(category.getType());
                }
            }
            
            // Create observable list for the ComboBox
            ObservableList<String> filterTypes = FXCollections.observableArrayList();
            
            // Add "All Types" as first option
            filterTypes.add("All Types");
            
            // Add all unique category types
            filterTypes.addAll(categoryTypes);
            
            // Set the items and select "All Types" by default
            filterTypeComboBox.setItems(filterTypes);
            filterTypeComboBox.getSelectionModel().selectFirst();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Filter Setup Error", "Failed to setup category type filter: " + e.getMessage());
        }
    }

    /**
     * Filter categories based on search term and selected type
     */
    private void filterCategories(String searchTerm, String selectedType) {
        try {
            List<Category> filteredCategories = new ArrayList<>(categoriesList);
            
            // Filter by search term if provided
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String term = searchTerm.trim().toLowerCase();
                filteredCategories = filteredCategories.stream()
                    .filter(category -> 
                        category.getName().toLowerCase().contains(term) ||
                        category.getDescription().toLowerCase().contains(term))
                    .collect(Collectors.toList());
            }
            
            // Filter by type if not "All Types"
            if (selectedType != null && !selectedType.equals("All Types")) {
                filteredCategories = filteredCategories.stream()
                    .filter(category -> selectedType.equals(category.getType()))
                    .collect(Collectors.toList());
            }
            
            // Display filtered categories
            displayCategoriesAsCards(filteredCategories);
            
            // Update status label
            updateStatusLabel(filteredCategories.size() + " categories found");
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Filter Error", "Failed to filter categories: " + e.getMessage());
        }
    }

    /**
     * Handle change in the category type to automatically populate MIME types
     * @param categoryType The selected category type
     */
    private void handleTypeChange(String categoryType) {
        List<String> defaultMimeTypes = PREDEFINED_MIME_TYPES.getOrDefault(categoryType, new ArrayList<>());
        
        // Update the MIME types list with predefined values for this category type
        mimeTypesList.clear();
        mimeTypesList.addAll(defaultMimeTypes);
        
        updateStatusLabel("MIME types automatically set for " + categoryType + " category.");
    }
    
    /**
     * Set the current user - to be called when loading this view
     * @param user The current user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        // Debug: Print the user's roles to help diagnose the issue
        System.out.println("User: " + (user != null ? user.getName() : "null") + 
                          ", Roles: " + (user != null ? user.getRoles() : "null"));
        
        // Check if user is admin using case-insensitive check for both "ADMIN" and "ROLE_ADMIN"
        boolean isAdmin = false;
        if (user != null && user.getRoles() != null) {
            for (String role : user.getRoles()) {
                if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
        }
        
        // Only show the unauthorized message if we actually determined they're not an admin
        if (user != null && !isAdmin) {
            showUnauthorizedAccessMessage();
        }
    }
    
    /**
     * Sets the current user and marks if coming from admin dashboard
     * @param user The current user
     * @param isAdmin Whether access comes from admin dashboard
     */
    public void setCurrentUser(User user, boolean isAdmin) {
        this.currentUser = user;
        this.isFromAdminDashboard = isAdmin;
        
        // Check if user is admin using case-insensitive check for both "ADMIN" and "ROLE_ADMIN"
        boolean hasAdminRole = false;
        if (user != null && user.getRoles() != null) {
            for (String role : user.getRoles()) {
                if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN")) {
                    hasAdminRole = true;
                    break;
                }
            }
        }
        
        // Only show the unauthorized message if we actually determined they're not an admin
        if (user != null && !hasAdminRole) {
            showUnauthorizedAccessMessage();
        }
    }
    
    /**
     * Show unauthorized access message and redirect
     */
    private void showUnauthorizedAccessMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Unauthorized Access");
        alert.setHeaderText("Admin Access Required");
        alert.setContentText("You must be an administrator to access the Category Management.");
        alert.showAndWait();
    }
    
    /**
     * Load all categories from the database
     */
    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAll();
            categoriesList.clear();
            categoriesList.addAll(categories);
            
            // Display categories as cards
            if (categoriesFlowPane != null) {
                displayCategoriesAsCards(categories);
            }
            
            // Setup the filter ComboBox with types from actual categories
            setupFilterComboBox();
            
            updateStatusLabel(categories.size() + " categories loaded");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    /**
     * Display categories as cards in the flow pane
     */
    private void displayCategoriesAsCards(List<Category> categories) {
        // Clear existing cards
        categoriesFlowPane.getChildren().clear();
        
        if (categories.isEmpty()) {
            Label noDataLabel = new Label("No categories found");
            noDataLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #757575;");
            categoriesFlowPane.getChildren().add(noDataLabel);
            return;
        }
        
        // Create a card for each category
        for (Category category : categories) {
            VBox card = createCategoryCard(category);
            categoriesFlowPane.getChildren().add(card);
        }
    }
    
    /**
     * Create a card view for a single category
     */
    private VBox createCategoryCard(Category category) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 5; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER);
        
        // Category type as header
        Label typeLabel = new Label(category.getType());
        typeLabel.setStyle("-fx-font-size: 14px; -fx-background-color: #f0f0f0; -fx-padding: 5px 10px; " +
                          "-fx-background-radius: 5px; -fx-text-fill: #616161;");
        
        // Category name
        Label nameLabel = new Label(category.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        
        // Description
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5px 0 0 0;");
        
        TextArea descArea = new TextArea(category.getDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(3);
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-control-inner-background: #f9f9f9; -fx-background-radius: 5px;");
        
        // MIME Types
        Label mimeTypesLabel = new Label("Allowed Types:");
        mimeTypesLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5px 0 0 0;");
        
        TextArea mimeTypesArea = new TextArea();
        if (category.getAllowedMimeTypes() != null && !category.getAllowedMimeTypes().isEmpty()) {
            mimeTypesArea.setText(String.join("\n", category.getAllowedMimeTypes()));
        } else {
            mimeTypesArea.setText("No types specified");
        }
        mimeTypesArea.setWrapText(true);
        mimeTypesArea.setEditable(false);
        mimeTypesArea.setPrefRowCount(2);
        mimeTypesArea.setPrefHeight(60);
        mimeTypesArea.setStyle("-fx-control-inner-background: #f9f9f9; -fx-background-radius: 5px;");
        
        // Buttons container
        HBox buttonsBox = new HBox(5);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: #FFA000; -fx-text-fill: white;");
        editButton.setOnAction(e -> updateCategory(category));
        
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteCategory(category));
        
        buttonsBox.getChildren().addAll(editButton, deleteButton);
        
        // Add all components to card
        card.getChildren().addAll(
            typeLabel,
            nameLabel,
            descLabel,
            descArea,
            mimeTypesLabel,
            mimeTypesArea,
            buttonsBox
        );
        
        return card;
    }
    
    /**
     * Update a category using a dialog (in-place editing)
     * Similar to how ArtworkManagementController handles it
     * @param category The category to update
     */
    private void updateCategory(Category category) {
        try {
            // Create a dialog for updating the category
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Update Category");
            dialog.setHeaderText("Update details for '" + category.getName() + "'");
            
            // Create content for the dialog
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            // Create fields and populate with current category details
            TextField nameField = new TextField(category.getName());
            ComboBox<String> typeComboBox = new ComboBox<>(FXCollections.observableArrayList(PREDEFINED_TYPES));
            typeComboBox.setValue(category.getType());
            TextArea descriptionArea = new TextArea(category.getDescription());
            descriptionArea.setWrapText(true);
            descriptionArea.setPrefRowCount(4);
            
            // MIME Types list
            Label mimeTypesLabel = new Label("Allowed MIME Types:");
            ListView<String> mimeTypesListView = new ListView<>();
            ObservableList<String> mimeTypesList = FXCollections.observableArrayList(category.getAllowedMimeTypes());
            mimeTypesListView.setItems(mimeTypesList);
            mimeTypesListView.setPrefHeight(120);
            
            // Setup type change listener to update MIME types
            typeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldType, newType) -> {
                if (newType != null && !newType.equals(oldType)) {
                    // Ask for confirmation if changing type 
                    Alert confirmTypeChange = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmTypeChange.setTitle("Change Type Confirmation");
                    confirmTypeChange.setHeaderText("Change Category Type");
                    confirmTypeChange.setContentText(
                            "Changing the category type will update the allowed MIME types. Continue?");
                    
                    Optional<ButtonType> result = confirmTypeChange.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // Update MIME types list with default types for the new category type
                        List<String> defaultMimeTypes = PREDEFINED_MIME_TYPES.getOrDefault(newType, new ArrayList<>());
                        mimeTypesList.clear();
                        mimeTypesList.addAll(defaultMimeTypes);
                    } else {
                        // Reset to previous type
                        typeComboBox.setValue(oldType);
                    }
                }
            });
            
            // Add all fields to the content
            content.getChildren().addAll(
                new Label("Name:"), nameField,
                new Label("Type:"), typeComboBox,
                new Label("Description:"), descriptionArea,
                mimeTypesLabel, mimeTypesListView
            );
            
            // Set dialog content and buttons
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(450);
            
            // Handle OK button action
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    // Create updated category object
                    Category updatedCategory = new Category();
                    updatedCategory.setId(category.getId());
                    updatedCategory.setManagerId(category.getManagerId());
                    updatedCategory.setName(nameField.getText().trim());
                    updatedCategory.setType(typeComboBox.getValue());
                    updatedCategory.setDescription(descriptionArea.getText().trim());
                    
                    // Set allowed MIME types
                    List<String> updatedMimeTypes = new ArrayList<>(mimeTypesList);
                    updatedCategory.setAllowedMimeTypes(updatedMimeTypes);
                    
                    // Perform validation
                    updatedCategory.validate();
                    
                    // Update in database
                    categoryService.update(updatedCategory);
                    
                    // Show success message and refresh display
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                             "Category '" + updatedCategory.getName() + "' updated successfully.");
                    loadCategories();
                    
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", e.getMessage());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Update Error", 
                             "Failed to update category: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Failed to open update dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Delete a category from the database
     * @param category The category to delete
     */
    private void deleteCategory(Category category) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Confirmation");
        confirmation.setHeaderText("Delete Category");
        confirmation.setContentText("Are you sure you want to delete the category: " + category.getName() + "?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    categoryService.delete(category);
                    loadCategories();
                    updateStatusLabel("Category deleted: " + category.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorMessage("Delete Error", "Failed to delete category: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Validate the form inputs
     * @return true if valid, false otherwise
     */
    private boolean validateForm() {
        try {
            // Create a temporary category object for validation
            Category category = new Category();
            
            // Validate name
            if (nameField.getText() != null && !nameField.getText().trim().isEmpty()) {
                category.setName(nameField.getText().trim());
            } else {
                showErrorMessage("Validation Error", "Category name is required.");
                return false;
            }
            
            // Validate type
            String type = typeComboBox.getValue();
            if (type != null && !type.trim().isEmpty()) {
                category.setType(type);
            } else {
                showErrorMessage("Validation Error", "Category type is required.");
                return false;
            }
            
            // Validate description
            if (descriptionArea.getText() != null && !descriptionArea.getText().trim().isEmpty()) {
                try {
                    category.setDescription(descriptionArea.getText().trim());
                } catch (IllegalArgumentException e) {
                    showErrorMessage("Validation Error", e.getMessage());
                    return false;
                }
            } else {
                showErrorMessage("Validation Error", "Description is required.");
                return false;
            }
            
            // Validate MIME types
            if (mimeTypesList.isEmpty()) {
                showErrorMessage("Validation Error", "At least one MIME type must be specified.");
                return false;
            }
            
            // All validations passed
            return true;
            
        } catch (IllegalArgumentException e) {
            // Catch any validation errors from the Category entity
            showErrorMessage("Validation Error", e.getMessage());
            return false;
        }
    }
    
    @FXML
    private void handleSaveCategory() {
        if (!validateForm()) {
            return;
        }
        
        try {
            // Create or update category
            Category category = (currentCategory != null) ? currentCategory : new Category();
            
            // Use the setters for validation
            category.setName(nameField.getText().trim());
            category.setType(typeComboBox.getValue());
            category.setDescription(descriptionArea.getText().trim());
            
            // Set allowed MIME types
            List<String> mimeTypes = new ArrayList<>(mimeTypesList);
            category.setAllowedMimeTypes(mimeTypes);
            
            // Set manager ID (current admin user)
            category.setManagerId(currentUser.getId());
            
            // Perform final validation
            category.validate();
            
            if (currentCategory == null || category.getId() == 0) {
                // New category
                categoryService.add(category);
                updateStatusLabel("New category added: " + category.getName());
            } else {
                // Update existing category
                categoryService.update(category);
                updateStatusLabel("Category updated: " + category.getName());
            }
            
            // Refresh the card view
            loadCategories();
            
            // Clear form
            clearForm();
            
            // Switch to the list tab to show the updated list
            if (categoryTabPane != null) {
                categoryTabPane.getSelectionModel().select(1);
            }
        } catch (IllegalArgumentException e) {
            showErrorMessage("Validation Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Save Error", "Failed to save category: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClear() {
        clearForm();
    }
    
    @FXML
    private void handleNewCategory() {
        clearForm();
        updateStatusLabel("Creating a new category");
        
        // Switch to the create tab
        if (categoryTabPane != null) {
            categoryTabPane.getSelectionModel().select(0);
        }
    }
    
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        filterTypeComboBox.getSelectionModel().selectFirst(); // "All Types"
        loadCategories();
    }
    
    /**
     * Clear the form fields
     */
    private void clearForm() {
        currentCategory = null;
        nameField.clear();
        typeComboBox.getSelectionModel().clearSelection();
        descriptionArea.clear();
        mimeTypesList.clear();
        if (errorMessageLabel != null) {
            errorMessageLabel.setText("");
        }
    }
    
    /**
     * Update the status label text
     * @param message The message to display
     */
    private void updateStatusLabel(String message) {
        // Update both status labels if they exist
        if (createTabStatusLabel != null) {
            createTabStatusLabel.setText(message);
        }
        if (listTabStatusLabel != null) {
            listTabStatusLabel.setText(message);
        }
    }
    
    /**
     * Show error message dialog
     * @param title Dialog title
     * @param message Error message
     */
    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show a general alert message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Switch to the create tab
     */
    @FXML
    private void switchToCreateTab() {
        if (categoryTabPane != null) {
            categoryTabPane.getSelectionModel().select(0);
        }
    }
    
    /**
     * Switch to the list tab
     */
    @FXML
    private void switchToListTab() {
        if (categoryTabPane != null) {
            categoryTabPane.getSelectionModel().select(1);
        }
    }
}