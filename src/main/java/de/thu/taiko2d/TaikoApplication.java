package de.thu.taiko2d;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import de.thu.taiko2d.render.GameCanvas;

/** Composition root. AI-assisted bootstrap, 2026-10-13; see docs/AI_USAGE.md. */
public final class TaikoApplication extends Application {

    private BorderPane root = new BorderPane();
    @Override
    public void start(Stage stage) {
        showMainMenu();
        Scene scene = new Scene(root, 960, 560);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("TaikoFX — Starter");
        stage.setMinWidth(640);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public void showMainMenu() {
        Label title = new Label("TaikoFX");
        title.getStyleClass().add("title");
        Label subtitle = new Label("A rhythm game, ready for your first beat.");
        subtitle.getStyleClass().add("subtitle");
        VBox header = new VBox(8, title, subtitle);
        header.setAlignment(Pos.CENTER);



        Label status = new Label("Starter project · Gameplay and editor are next.");
        status.setWrapText(true);
        status.getStyleClass().add("status");
        Button play = new Button("Play");
        play.setOnAction(event -> {status.setText("Play placeholder · Next: audio transport and song timing.");showSongSelection();});
        Button editor = new Button("Editor");
        editor.setOnAction(event -> status.setText("Editor placeholder · Add chart editing after normal-note gameplay."));
        Button exit = new Button("Exit");
        exit.getStyleClass().add("secondary");
        exit.setOnAction(event -> Platform.exit());
        VBox buttons = new VBox(18, play, editor, exit);
        buttons.setPadding(new Insets(32));

        buttons.setAlignment(Pos.CENTER);
        Label controls = new Label("Planned controls: F / J centre · D / K rim");
        controls.getStyleClass().add("subtitle");
        VBox footer = new VBox(16, buttons, controls, status);
        footer.setAlignment(Pos.CENTER);

        root.setPadding(new Insets(32));
        VBox menu = new VBox(24, header, footer);
        menu.setAlignment(Pos.CENTER);

        showScreen(menu);
    }
    public void showSongSelection(){
        // AI-assisted mock list and navigation, 2026-10-20.
        Label heading = new Label("Select a song");
        heading.getStyleClass().add("title");
        Label selectedSong = new Label();
        selectedSong.setWrapText(true);
        Label status = new Label("Mock songs · Audio is not connected yet.");
        status.getStyleClass().add("status");
        status.setWrapText(true);
        ListView<Song> songs = new ListView<>();
        songs.getStyleClass().add("song-list");
        songs.setFixedCellSize(64);

        songs.getItems().addAll(
            new Song("Neon Steps", "Demo Artist", 120),
            new Song("Evening Circuit", "Demo Artist", 140),
            new Song("Paper Drums", "Demo Artist", 160)
        );
        for (int i = 4; i <= 40; i++) {
            songs.getItems().add(new Song("Practice Track " + i, "Mock Artist", 100 + (i % 9) * 10));
        }

        songs.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            selectedSong.setText(selected == null ? "Select a song" : "Selected: " + selected);
            status.setText("Mock songs · Audio is not connected yet.");
        });

        Button start = new Button("Start");
        start.disableProperty().bind(songs.getSelectionModel().selectedItemProperty().isNull());
        start.setOnAction(event -> {
            Song selected = songs.getSelectionModel().getSelectedItem();
            if (selected != null) {
                status.setText("Ready: " + selected.title() + " · Connect audio playback next.");
                showGameplaySelection(selected);
            }
        });
        Button back = new Button("Back");
        back.getStyleClass().add("secondary");
        back.setOnAction(event -> showMainMenu());
        HBox actions = new HBox(12, start, back);
        actions.setAlignment(Pos.CENTER);

        songs.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                moveSongSelection(songs, event.getCode() == KeyCode.DOWN ? 1 : -1);
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                start.fire();
                event.consume();
            }
        });
        songs.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                moveSongSelection(songs, event.getDeltaY() < 0 ? 1 : -1);
                event.consume();
            }
        });

        Label help = new Label("Click a song · Wheel / ↑ ↓ to browse · Enter to select · Esc to go back");
        help.getStyleClass().add("subtitle");
        help.setWrapText(true);
        VBox screen = new VBox(12, heading, selectedSong, songs, actions, status, help);
        VBox.setVgrow(songs, Priority.ALWAYS);
        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                showMainMenu();
                event.consume();
            }
        });

        showScreen(screen);
        songs.getSelectionModel().selectFirst();
        Platform.runLater(songs::requestFocus);
    }

    private void moveSongSelection(ListView<Song> songs, int direction) {
        int count = songs.getItems().size();
        if (count == 0) return;
        int next = Math.floorMod(songs.getSelectionModel().getSelectedIndex() + direction, count);
        songs.getSelectionModel().select(next);
        songs.getFocusModel().focus(next);
        songs.scrollTo(next);
    }
    public void showGameplaySelection(Song  selected) {
        GameCanvas game =  new GameCanvas();
    }
    public void showEditorSelection(){
    }
    private void showScreen(Node screen) {
        root.setTop(null);
        root.setBottom(null);
        root.setLeft(null);
        root.setRight(null);
        root.setCenter(screen);
    }
    private record Song(String title, String artist, int bpm) {
        @Override
        public String toString() {
            return title + " — " + artist + " | " + bpm + " BPM";
        }
    }

}
