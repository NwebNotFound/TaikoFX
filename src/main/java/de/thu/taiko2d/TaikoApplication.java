package de.thu.taiko2d;

import de.thu.taiko2d.render.GameCanvas;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Composition root. AI-assisted bootstrap, 2026-10-13; see docs/AI_USAGE.md. */
public final class TaikoApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("TaikoFX");
        title.getStyleClass().add("title");
        Label subtitle = new Label("A rhythm game, ready for your first beat.");
        subtitle.getStyleClass().add("subtitle");
        VBox header = new VBox(8, title, subtitle);

        GameCanvas canvas = new GameCanvas();
        StackPane lane = new StackPane(canvas);
        lane.setMinHeight(220);
        canvas.widthProperty().bind(lane.widthProperty());
        canvas.heightProperty().bind(lane.heightProperty());

        Label status = new Label("Starter project · Gameplay and editor are next.");
        status.setWrapText(true);
        status.getStyleClass().add("status");
        Button play = new Button("Play");
        play.setOnAction(event -> status.setText("Play placeholder · Next: audio transport and song timing."));
        Button editor = new Button("Editor");
        editor.setOnAction(event -> status.setText("Editor placeholder · Add chart editing after normal-note gameplay."));
        Button exit = new Button("Exit");
        exit.getStyleClass().add("secondary");
        exit.setOnAction(event -> Platform.exit());
        HBox buttons = new HBox(12, play, editor, exit);
        buttons.setAlignment(Pos.CENTER);
        Label controls = new Label("Planned controls: F / J centre · D / K rim");
        controls.getStyleClass().add("subtitle");
        VBox footer = new VBox(16, buttons, controls, status);
        footer.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(32));
        root.setTop(header);
        root.setCenter(lane);
        root.setBottom(footer);
        BorderPane.setMargin(lane, new Insets(24, 0, 24, 0));
        Scene scene = new Scene(root, 960, 560);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("TaikoFX — Starter");
        stage.setMinWidth(640);
        stage.setMinHeight(440);
        stage.setScene(scene);
        stage.show();
    }
}
