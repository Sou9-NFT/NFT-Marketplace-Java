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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.esprit.models.Category;
import org.esprit.models.User;
import org.esprit.services.CategoryService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoryManagementController implements Initializable {

    // Form fields
    @FXML
    private TextField nameField;
    
    @FXML
    private ComboBox<String> typeComboBox;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private ListView<String> mimeTypesListView;
    
    // Table view
    @FXML
    private TableView<Category> categoryTable;
    
    @FXML
    private TableColumn<Category, Integer> idColumn;
    
    @FXML
    private TableColumn<Category, String> nameColumn;
    
    @FXML
    private TableColumn<Category, String> typeColumn;
    
    @FXML
    private TableColumn<Category, String> descriptionColumn;
    
    @FXML
    private TableColumn<Category, List<String>> mimeTypesColumn;
    
    @FXML
    private TableColumn<Category, Void> actionsColumn;
    
    // Search and filter controls
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> filterTypeComboBox;
    
    @FXML
    private Label statusLabel;
    
    // Service and data
    private CategoryService categoryService;
    private ObservableList<Category> categoriesList;
    private ObservableList<String> mimeTypesList;
    private Category currentCategory;
    private User currentUser;
    private static final List<String> PREDEFINED_TYPES = Arrays.asList("Image", "Video", "Audio", "3D Model", "Document", "Other");
    
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
        
        // Set up type combo boxes
        typeComboBox.setItems(FXCollections.observableArrayList(PREDEFINED_TYPES));
        filterTypeComboBox.setItems(FXCollections.observableArrayList(PREDEFINED_TYPES));
        filterTypeComboBox.getItems().add(0, "All Types");
        filterTypeComboBox.getSelectionModel().selectFirst();
        
        // Set up mime types list
        mimeTypesListView.setItems(mimeTypesList);
        
        // Add listener for type selection to automatically populate MIME types
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleTypeChange(newValue);
            }
        });
        
        // Set up table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Custom cell factory for MIME types list column
        mimeTypesColumn.setCellValueFactory(new PropertyValueFactory<>("allowedMimeTypes"));
        mimeTypesColumn.setCellFactory(column -> {
            return new TableCell<Category, List<String>>() {
                @Override
                protected void updateItem(List<String> mimeTypes, boolean empty) {
                    super.updateItem(mimeTypes, empty);
                    
                    if (empty || mimeTypes == null) {
                        setText(null);
                    } else {
                        setText(mimeTypes.stream().collect(Collectors.joining(", ")));
                    }
                }
            };
        });
        
        // Setup actions column with edit and delete buttons
        setupActionsColumn();
        
        // Load all categories
        loadCategories();
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
            categoryTable.setItems(categoriesList);
            updateStatusLabel(categories.size() + " categories loaded");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    /**
     * Set up actions column with edit and delete buttons
     */
    private void setupActionsColumn() {
        Callback<TableColumn<Category, Void>, TableCell<Category, Void>> cellFactory = new Callback<TableColumn<Category, Void>, TableCell<Category, Void>>() {
            @Override
            public TableCell<Category, Void> call(final TableColumn<Category, Void> param) {
                return new TableCell<Category, Void>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox buttonsBox = new HBox(5, editButton, deleteButton);

                    {
                        // Edit button action
                        editButton.setOnAction((ActionEvent event) -> {
                            Category category = getTableView().getItems().get(getIndex());
                            populateForm(category);
                        });
                        
                        // Delete button action
                        deleteButton.setOnAction((ActionEvent event) -> {
                            Category category = getTableView().getItems().get(getIndex());
                            deleteCategory(category);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonsBox);
                        }
                    }
                };
            }
        };
        
        actionsColumn.setCellFactory(cellFactory);
    }
    
    /**
     * Populate the form with category data for editing
     * @param category The category to edit
     */
    private void populateForm(Category category) {
        currentCategory = category;
        nameField.setText(category.getName());
        typeComboBox.setValue(category.getType());
        descriptionArea.setText(category.getDescription());
        
        // When editing an existing category, we'll show its current MIME types
        // but they will be overwritten if the user changes the category type
        mimeTypesList.clear();
        if (category.getAllowedMimeTypes() != null) {
            mimeTypesList.addAll(category.getAllowedMimeTypes());
        }
        
        updateStatusLabel("Editing category: " + category.getName());
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
            
            // Refresh the table
            loadCategories();
            clearForm();
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
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        String selectedType = filterTypeComboBox.getValue();
        
        try {
            List<Category> filteredCategories;
            
            if (!searchTerm.isEmpty()) {
                filteredCategories = categoryService.searchByName(searchTerm);
            } else {
                filteredCategories = categoryService.getAll();
            }
            
            // Apply type filter if not "All Types"
            if (!"All Types".equals(selectedType)) {
                filteredCategories = filteredCategories.stream()
                    .filter(c -> selectedType.equals(c.getType()))
                    .collect(Collectors.toList());
            }
            
            categoriesList.clear();
            categoriesList.addAll(filteredCategories);
            updateStatusLabel(filteredCategories.size() + " categories found");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Search Error", "Failed to search categories: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        filterTypeComboBox.getSelectionModel().select(0); // "All Types"
        loadCategories();
    }
    
    @FXML
    private void handleBackToDashboard() {
        try {
            loadScreen("AdminDashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Navigation Error", "Could not navigate back to dashboard: " + e.getMessage());
        }
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
    }
    
    /**
     * Update the status label text
     * @param message The message to display
     */
    private void updateStatusLabel(String message) {
        statusLabel.setText(message);
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
     * Load a screen by FXML file name
     * @param fxml The FXML file name
     */
    private void loadScreen(String fxml) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            // Get the current stage - more safely
            Stage stage = null;
            if (nameField != null && nameField.getScene() != null && nameField.getScene().getWindow() != null) {
                stage = (Stage) nameField.getScene().getWindow();
            } else if (currentUser != null) {
                // We don't have a reference to the stage yet, so create a new one
                // This happens when we're checking roles before the UI is fully loaded
                stage = new Stage();
            }
            
            if (stage != null) {
                stage.setScene(scene);
                stage.show();
            } else {
                throw new IOException("Could not get or create a stage for navigation");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading screen: " + fxml + " - " + e.getMessage());
        }
    }
}