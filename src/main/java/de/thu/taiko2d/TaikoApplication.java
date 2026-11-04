package de.thu.taiko2d;

import de.thu.taiko2d.audio.SongPreview;
import de.thu.taiko2d.game.InputState;
import de.thu.taiko2d.game.Judgement;
import de.thu.taiko2d.game.JudgementEngine;
import de.thu.taiko2d.model.NoteType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import de.thu.taiko2d.render.GameCanvas;
import de.thu.taiko2d.io.BeatmapJson;
import de.thu.taiko2d.model.MapManifest;
import de.thu.taiko2d.model.Note;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/** Composition root. AI-assisted bootstrap, 2026-10-13; see docs/AI_USAGE.md. */
public final class TaikoApplication extends Application {

    private BorderPane root = new BorderPane();
    private final SongPreview preview = new SongPreview();
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
        // osu!lazer-styled carousel layout.
        ObservableList<Song> all = FXCollections.observableArrayList();
        String loadError = "";
        Path mapsDir = Path.of("examples");
        try (Stream<Path> folders = Files.list(mapsDir)) {
            for (Path folder : folders.filter(f -> Files.exists(f.resolve("manifest.json"))).toList()) {
                all.add(new Song(folder, BeatmapJson.loadManifest(folder)));
            }
        } catch (IOException e) {
            loadError = "Could not read maps: " + e.getMessage();
        }
        final String errorText = loadError;
        FilteredList<Song> visible = new FilteredList<>(all);

        // Top bar: search and map count.
        TextField search = new TextField();
        search.setPromptText("Type to search...");
        search.getStyleClass().add("search-box");
        HBox.setHgrow(search, Priority.ALWAYS);
        Label count = new Label();
        count.getStyleClass().add("map-count");
        HBox topBar = new HBox(16, search, count);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        // Left: details of the selected map.
        Label title = new Label("No map selected");
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);
        Label artist = new Label();
        artist.getStyleClass().add("detail-artist");
        artist.setWrapText(true);
        Label mapper = new Label();
        mapper.getStyleClass().add("detail-mapper");
        mapper.setWrapText(true);
        FlowPane chips = new FlowPane(8, 8);
        Label status = new Label(errorText);
        status.getStyleClass().add("detail-status");
        status.setWrapText(true);
        VBox details = new VBox(10, title, artist, mapper, chips, status);
        details.getStyleClass().add("detail-card");
        details.setPrefWidth(340);
        details.setMinWidth(260);
        details.setMaxHeight(Region.USE_PREF_SIZE);

        // Right: carousel of map cards.
        ListView<Song> songs = new ListView<>(visible);
        songs.getStyleClass().add("song-list");
        songs.setFixedCellSize(88);
        songs.setFocusTraversable(false);
        Button start = new Button("Play");
        start.getStyleClass().add("play-button");
        start.disableProperty().bind(songs.getSelectionModel().selectedItemProperty().isNull());
        songs.setCellFactory(list -> new SongCard());
        songs.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) start.fire();
        });

        songs.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            chips.getChildren().clear();
            if (selected == null) {
                title.setText("No map selected");
                artist.setText("");
                mapper.setText("");
                status.setText(errorText);
                preview.stop();
                return;
            }
            MapManifest m = selected.manifest();
            title.setText(m.title());
            artist.setText(m.artist());
            mapper.setText("mapped by " + m.mapper());
            for (MapManifest.DifficultyRef difficulty : m.difficulties()) {
                Label chip = new Label(difficulty.name() + "  ★ " + difficulty.level());
                chip.getStyleClass().add("diff-chip");
                chips.getChildren().add(chip);
            }
            status.setText("♪ Preview from " + m.previewStartMs() / 1000.0 + " s");
            preview.play(selected.folder().resolve(m.audio()), m.previewStartMs());
        });

        Runnable refreshCount = () -> count.setText(visible.size() + (visible.size() == 1 ? " map" : " maps"));
        search.textProperty().addListener((observable, oldText, text) -> {
            String query = text.trim().toLowerCase();
            visible.setPredicate(song -> query.isEmpty()
                    || song.manifest().title().toLowerCase().contains(query)
                    || song.manifest().artist().toLowerCase().contains(query)
                    || song.manifest().mapper().toLowerCase().contains(query));
            refreshCount.run();
            songs.getSelectionModel().selectFirst();
        });
        refreshCount.run();

        start.setOnAction(event -> {
            Song selected = songs.getSelectionModel().getSelectedItem();
            if (selected != null) showGameplaySelection(selected);
        });
        Button back = new Button("Back");
        back.getStyleClass().add("back-button");
        back.setOnAction(event -> showMainMenu());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label help = new Label("Wheel / ↑ ↓ browse · Enter or double-click to play · Esc back");
        help.getStyleClass().add("help-text");
        HBox bottomBar = new HBox(16, back, help, spacer, start);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getStyleClass().add("bottom-bar");

        HBox centre = new HBox(32, details, songs);
        centre.setAlignment(Pos.CENTER_LEFT);
        centre.getStyleClass().add("select-centre");
        HBox.setHgrow(songs, Priority.ALWAYS);
        BorderPane screen = new BorderPane();
        screen.getStyleClass().add("select-screen");
        screen.setTop(topBar);
        screen.setCenter(centre);
        screen.setBottom(bottomBar);

        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                moveSongSelection(songs, event.getCode() == KeyCode.DOWN ? 1 : -1);
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                start.fire();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                showMainMenu();
                event.consume();
            }
        });
        songs.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                moveSongSelection(songs, event.getDeltaY() < 0 ? 1 : -1);
                event.consume();
            }
        });

        root.setPadding(Insets.EMPTY);
        showScreen(screen);
        songs.getSelectionModel().selectFirst();
        Platform.runLater(search::requestFocus);
    }

    /** One map card in the carousel; the selected card slides out and glows. */
    private static final class SongCard extends ListCell<Song> {
        private final Label cardTitle = new Label();
        private final Label cardArtist = new Label();
        private final Label cardMeta = new Label();
        private final VBox card = new VBox(2, cardTitle, cardArtist, cardMeta);
        private final TranslateTransition slide = new TranslateTransition(Duration.millis(140), card);

        SongCard() {
            card.getStyleClass().add("song-card");
            cardTitle.getStyleClass().add("card-title");
            cardArtist.getStyleClass().add("card-artist");
            cardMeta.getStyleClass().add("card-meta");
            selectedProperty().addListener((observable, was, selected) -> {
                slide.stop();
                slide.setToX(selected ? -36 : 0);
                slide.play();
            });
        }

        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            if (empty || song == null) {
                setGraphic(null);
                return;
            }
            int n = song.manifest().difficulties().size();
            cardTitle.setText(song.manifest().title());
            cardArtist.setText(song.manifest().artist());
            cardMeta.setText(song.manifest().mapper() + " · " + n + (n == 1 ? " difficulty" : " difficulties"));
            card.setTranslateX(isSelected() ? -36 : 0);
            setGraphic(card);
        }
    }

    private void moveSongSelection(ListView<Song> songs, int direction) {
        int count = songs.getItems().size();
        if (count == 0) return;
        int next = Math.floorMod(songs.getSelectionModel().getSelectedIndex() + direction, count);
        songs.getSelectionModel().select(next);
        songs.scrollTo(Math.max(0, next - 2));
    }
    public void showGameplaySelection(Song  selected) {
        List<Note> notes;
        JudgementEngine engine;
        try {
            String chart = selected.manifest().difficulties().get(0).chart();
            notes = BeatmapJson.loadNotes(selected.folder().resolve(chart));
            engine = new JudgementEngine(notes);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        GameCanvas game =  new GameCanvas();
        StackPane root = new StackPane(game);
        game.widthProperty().bind(root.widthProperty());
        game.heightProperty().bind(root.heightProperty());

        InputState<KeyCode> input = new InputState<>();

        Media media = new Media(selected.folder().resolve(selected.manifest().audio()).toUri().toString());
        MediaPlayer player = new MediaPlayer(media);

        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
        if (event.getCode() == KeyCode.ESCAPE) {
            game.stop();
            player.dispose();
            showSongSelection();
            event.consume();
            return;
        }
        NoteType type = noteTypeFor(event.getCode());
        if (type != null && input.press(event.getCode())) {
            double timeMs = player.getCurrentTime().toMillis();
            Judgement result = engine.hit(type, timeMs);
            if (result != null) {
                game.showJudgement(result, timeMs);
                game.noteHit(engine.lastHitNote(), timeMs);
            }
        }
            event.consume();
        });
        root.addEventFilter(KeyEvent.KEY_RELEASED, event -> input.release(event.getCode()));
        game.focusedProperty().addListener((observable, was, focused) -> {
            if (!focused) input.clear();      // spec: clear held keys on focus loss
        });
        showScreen(root);
        player.setOnError(() -> System.err.println("Audio error: " + player.getError()));
        player.setOnReady(() -> {
            game.start(notes, () -> player.getCurrentTime().toMillis(), ms -> {
                if (engine.update(ms) > 0) game.showJudgement(Judgement.MISS, ms);
                game.setStats(engine.score(), engine.combo());
            });
            player.play();
        });
        game.setFocusTraversable(true);
        Platform.runLater(game::requestFocus);
        player.setOnEndOfMedia(() -> {
            game.stop();
            player.dispose();
            showResults(selected, engine);
        });
    }

    public void showResults(Song selected, JudgementEngine engine) {
        Label heading = new Label("Results");
        heading.getStyleClass().add("title");
        Label song = new Label(selected.toString());
        song.getStyleClass().add("subtitle");
        Label score = new Label("Score " + engine.score());
        Label accuracy = new Label(String.format("Accuracy %.1f%%", engine.accuracy()));
        Label combo = new Label("Max combo " + engine.maxCombo());
        Label counts = new Label("Perfect " + engine.perfect() + " · Good " + engine.good()
                + " · Miss " + engine.miss());
        VBox stats = new VBox(8, score, accuracy, combo, counts);
        stats.setAlignment(Pos.CENTER);

        Button retry = new Button("Retry");
        retry.setOnAction(event -> showGameplaySelection(selected));
        Button back = new Button("Back");
        back.getStyleClass().add("secondary");
        back.setOnAction(event -> showSongSelection());
        HBox actions = new HBox(12, retry, back);
        actions.setAlignment(Pos.CENTER);

        VBox screen = new VBox(20, heading, song, stats, actions);
        screen.setAlignment(Pos.CENTER);
        showScreen(screen);
    }
    public void showEditorSelection(){
    }
    private void showScreen(Node screen) {
        preview.stop();   // every screen change ends the song-select preview
        root.setTop(null);
        root.setBottom(null);
        root.setLeft(null);
        root.setRight(null);
        root.setCenter(screen);
    }
        private record Song(Path folder, MapManifest manifest) {
        @Override
        public String toString() {
            return manifest.title() + " — " + manifest.artist();
        }
    }

    private static NoteType noteTypeFor(KeyCode code) {
    return switch (code) {
        case F, J -> NoteType.DON;
        case D, K -> NoteType.KA;
        default -> null;
    };
}

}
