package com.dlsc.opencv;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

public class ChallengeApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        OpenCV.loadLocally();

        Image soccerOriginal = new Image(ChallengeApp.class.getResourceAsStream("soccer-original.png"));
        Image soccerTarget = new Image(ChallengeApp.class.getResourceAsStream("soccer-target.png"));
        Image soccerConverted = OpenCVUtil.process(soccerOriginal);

        Image basketballOriginal = new Image(ChallengeApp.class.getResourceAsStream("basketball-original.png"));
        Image basketballTarget = new Image(ChallengeApp.class.getResourceAsStream("basketball-target.png"));
        Image basketballConverted = OpenCVUtil.process(basketballOriginal);

        ImageView soccerOriginalImageView = new ImageView(soccerOriginal);
        ImageView soccerTargetImageView = new ImageView(soccerTarget);
        ImageView soccerConvertedImageView = new ImageView(soccerConverted);

        ImageView basketballOriginalImageView = new ImageView(basketballOriginal);
        ImageView basketballTargetImageView = new ImageView(basketballTarget);
        ImageView basketballConvertedImageView = new ImageView(basketballConverted);

        soccerTargetImageView.setFitWidth(soccerOriginal.getWidth());
        soccerTargetImageView.setPreserveRatio(true);

        // all images same width
        basketballOriginalImageView.setFitWidth(soccerOriginal.getWidth());
        basketballOriginalImageView.setPreserveRatio(true);

        basketballTargetImageView.setFitWidth(soccerOriginal.getWidth());
        basketballTargetImageView.setPreserveRatio(true);

        basketballConvertedImageView.setFitWidth(soccerOriginal.getWidth());
        basketballConvertedImageView.setPreserveRatio(true);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(20);
        gridPane.setVgap(20);

        gridPane.add(createLabel("Original"), 0, 0);
        gridPane.add(createLabel("Target"), 1, 0);
        gridPane.add(createLabel("OpenCV"), 2, 0);

        gridPane.add(wrap(soccerOriginalImageView), 0, 1);
        gridPane.add(wrap(soccerTargetImageView), 1, 1);
        gridPane.add(wrap(soccerConvertedImageView), 2, 1);

        gridPane.add(wrap(basketballOriginalImageView), 0, 2);
        gridPane.add(wrap(basketballTargetImageView), 1, 2);
        gridPane.add(wrap(basketballConvertedImageView), 2, 2);

        Scene scene = new Scene(gridPane);

        primaryStage.setTitle("OpenCV Challenge");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private Label createLabel(String original) {
        Label label = new Label(original);
        label.setStyle("-fx-font-size: 16px;");
        return label;
    }

    private Node wrap(ImageView view) {
        StackPane stackPane = new StackPane(view);
        stackPane.setStyle("-fx-border-color: black;");
        return stackPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
