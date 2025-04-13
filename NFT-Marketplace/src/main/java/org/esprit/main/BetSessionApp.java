package org.esprit.main;

import org.esprit.views.BetSessionView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class BetSessionApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        BetSessionView betSessionView = new BetSessionView();
        root.setCenter(betSessionView.getView());

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Bet Session Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
