package org.esprit.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;

public class AlertUtils {
    
    public static void showInformation(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public static void showError(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public static void showWarning(String title, String content) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void setLabelError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.getStyleClass().removeAll("status-success");
            label.getStyleClass().add("status-error");
        }
    }
    
    public static void setLabelSuccess(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.getStyleClass().removeAll("status-error");
            label.getStyleClass().add("status-success");
        }
    }
}