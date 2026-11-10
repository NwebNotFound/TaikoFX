package de.thu.taiko2d;

import de.thu.taiko2d.audio.SongPreview;
import de.thu.taiko2d.game.HealthGauge;
import de.thu.taiko2d.game.InputState;
import de.thu.taiko2d.game.Judgement;
import de.thu.taiko2d.game.JudgementEngine;
import de.thu.taiko2d.game.LeadInClock;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
import de.thu.taiko2d.model.ChartInfo;
import de.thu.taiko2d.model.MapManifest;
import de.thu.taiko2d.model.Note;
import de.thu.taiko2d.io.ScoreStore;
import de.thu.taiko2d.model.Leaderboard;
import de.thu.taiko2d.model.ScoreEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/** Composition root. AI-assisted bootstrap, 2026-10-13; see docs/AI_USAGE.md. */
public final class TaikoApplication extends Application {

    private BorderPane root = new BorderPane();
    private final SongPreview preview = new SongPreview();
    /** Scroll-speed setting from song select, multiplies each map's own speed. */
    private double playerSpeed = 1.0;
    /** Called when the window loses focus (gameplay sets it to pause). */
    private Runnable onWindowBlur;
    /** Local high scores, saved in ~/.taikofx/scores.json. */
    private final ScoreStore scoreStore = new ScoreStore(ScoreStore.defaultFile());
    private Leaderboard leaderboard = new Leaderboard();

    @Override
    public void start(Stage stage) {
        try {
            leaderboard = scoreStore.load();
        } catch (IOException e) {
            System.err.println("Could not load scores: " + e.getMessage());
        }
        showMainMenu();
        Scene scene = new Scene(root, 960, 560);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.focusedProperty().addListener((o, was, focused) -> {
            if (!focused && onWindowBlur != null) onWindowBlur.run();
        });
        stage.setTitle("TaikoFX");
        stage.setMinWidth(640);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public void showMainMenu() {
        Label title = new Label("TaikoFX");
        title.getStyleClass().add("title");
        Label subtitle = new Label("~ a night festival rhythm game ~");
        subtitle.getStyleClass().add("subtitle");
        VBox header = new VBox(8, title, subtitle);
        header.setAlignment(Pos.CENTER);

        Button play = new Button("Play");
        play.setOnAction(event -> showSongSelection());
        // The editor button is hidden until the editor exists (showEditorSelection is kept).
        Button exit = new Button("Exit");
        exit.getStyleClass().add("secondary");
        exit.setOnAction(event -> Platform.exit());
        VBox buttons = new VBox(18, play, exit);
        buttons.setPadding(new Insets(32));
        buttons.setAlignment(Pos.CENTER);
        Label controls = new Label("F / J  don (centre)   ·   D / K  ka (rim)   ·   Esc  pause");
        controls.getStyleClass().add("subtitle");
        VBox footer = new VBox(16, buttons, controls);
        footer.setAlignment(Pos.CENTER);

        VBox menu = new VBox(24, header, footer);
        menu.setPadding(new Insets(32));
        menu.setAlignment(Pos.CENTER);
        menu.getStyleClass().add("menu-screen");
        Platform.runLater(play::requestFocus);

        showScreen(new ScaledPane(menu));
    }
    public void showSongSelection(){
        // osu!-style carousel: the selected song opens and its difficulties appear underneath.
        // Grouping and local leaderboard AI-assisted 2026-11-09; see docs/AI_USAGE.md.
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

        // Left: details of the selected map and its local leaderboard.
        Label title = new Label("No map selected");
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);
        Label artist = new Label();
        artist.getStyleClass().add("detail-artist");
        artist.setWrapText(true);
        Label mapper = new Label();
        mapper.getStyleClass().add("detail-mapper");
        mapper.setWrapText(true);
        Label boardTitle = new Label();
        boardTitle.getStyleClass().add("board-title");
        VBox board = new VBox(2);
        ScrollPane boardScroll = new ScrollPane(board);
        boardScroll.setFitToWidth(true);
        boardScroll.setPrefHeight(170);
        boardScroll.setMaxHeight(170);
        boardScroll.getStyleClass().add("chip-scroll");
        Label status = new Label(errorText);
        status.getStyleClass().add("detail-status");
        status.setWrapText(true);
        // Player scroll-speed setting, applied on top of each map's own speed.
        Label speedLabel = new Label();
        speedLabel.getStyleClass().add("speed-label");
        Runnable showSpeed = () -> speedLabel.setText(String.format("Scroll speed %.1fx", playerSpeed));
        Button slower = new Button("-");
        Button faster = new Button("+");
        slower.getStyleClass().add("speed-button");
        faster.getStyleClass().add("speed-button");
        slower.setFocusTraversable(false);
        faster.setFocusTraversable(false);
        slower.setOnAction(e -> { playerSpeed = Math.max(0.5, playerSpeed - 0.1); showSpeed.run(); });
        faster.setOnAction(e -> { playerSpeed = Math.min(3.0, playerSpeed + 0.1); showSpeed.run(); });
        showSpeed.run();
        HBox speedRow = new HBox(8, slower, speedLabel, faster);
        speedRow.setAlignment(Pos.CENTER_LEFT);
        VBox details = new VBox(10, title, artist, mapper, boardTitle, boardScroll, speedRow, status);
        details.getStyleClass().add("detail-card");
        details.setPrefWidth(340);
        details.setMinWidth(260);
        details.setMaxHeight(Region.USE_PREF_SIZE);

        // Right: carousel rows. Every song is a row; the open song is followed by its difficulties.
        ObservableList<Object> rows = FXCollections.observableArrayList();
        ListView<Object> list = new ListView<>(rows);
        list.getStyleClass().add("song-list");
        list.setFocusTraversable(false);
        list.setCellFactory(l -> new CarouselCell());
        Song[] open = {null};
        Song[] previewing = {null};
        boolean[] rebuilding = {false};
        ObjectProperty<DiffRow> chosen = new SimpleObjectProperty<>();
        Button start = new Button("Play");
        start.getStyleClass().add("play-button");
        start.disableProperty().bind(chosen.isNull());

        Runnable rebuild = () -> {
            rebuilding[0] = true;
            List<Object> next = new ArrayList<>();
            for (Song song : visible) {
                next.add(new SongRow(song, song == open[0]));
                if (song == open[0]) {
                    for (MapManifest.DifficultyRef d : sortedDifficulties(song.manifest())) next.add(new DiffRow(song, d));
                }
            }
            rows.setAll(next);
            rebuilding[0] = false;
        };
        // Opens the song if needed and selects one of its difficulties.
        BiConsumer<Song, MapManifest.DifficultyRef> select = (song, difficulty) -> {
            if (open[0] != song) { open[0] = song; rebuild.run(); }
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i) instanceof DiffRow d && d.song() == song && d.difficulty() == difficulty) {
                    list.getSelectionModel().select(i);
                    list.scrollTo(Math.max(0, i - 3));
                    return;
                }
            }
        };
        Consumer<Song> openSong = song -> select.accept(song, sortedDifficulties(song.manifest()).getFirst());

        list.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (rebuilding[0]) return;
            if (selected instanceof SongRow s) {                 // clicked a song: open it
                Platform.runLater(() -> openSong.accept(s.song()));
                return;
            }
            if (!(selected instanceof DiffRow d)) return;
            chosen.set(d);
            MapManifest m = d.song().manifest();
            title.setText(m.title());
            artist.setText(m.artist());
            mapper.setText("mapped by " + m.mapper());
            showLeaderboard(boardTitle, board, d);
            if (previewing[0] != d.song()) {                     // only restart the music for a new song
                previewing[0] = d.song();
                status.setText("♪ Preview from " + m.previewStartMs() / 1000.0 + " s");
                preview.play(d.song().folder().resolve(m.audio()), m.previewStartMs());
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() instanceof DiffRow) start.fire();
        });

        Runnable showNothing = () -> {
            chosen.set(null);
            title.setText("No map selected");
            artist.setText("");
            mapper.setText("");
            boardTitle.setText("");
            board.getChildren().clear();
            status.setText(errorText);
            previewing[0] = null;
            preview.stop();
        };
        Runnable refreshCount = () -> count.setText(visible.size() + (visible.size() == 1 ? " map" : " maps"));
        search.textProperty().addListener((observable, oldText, text) -> {
            String query = text.trim().toLowerCase();
            visible.setPredicate(song -> query.isEmpty()
                    || song.manifest().title().toLowerCase().contains(query)
                    || song.manifest().artist().toLowerCase().contains(query)
                    || song.manifest().mapper().toLowerCase().contains(query));
            refreshCount.run();
            if (visible.isEmpty()) { open[0] = null; rebuild.run(); showNothing.run(); }
            else if (visible.contains(open[0])) rebuild.run();
            else { open[0] = null; openSong.accept(visible.getFirst()); }
            if (chosen.get() != null) select.accept(chosen.get().song(), chosen.get().difficulty());
        });
        refreshCount.run();

        start.setOnAction(event -> {
            DiffRow d = chosen.get();
            if (d != null) showGameplaySelection(d.song(), d.difficulty());
        });
        Button back = new Button("Back");
        back.getStyleClass().add("back-button");
        back.setOnAction(event -> showMainMenu());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label help = new Label("↑ ↓ difficulty · ← → song · F3 / F4 speed · Enter play · Esc back");
        help.getStyleClass().add("help-text");
        HBox bottomBar = new HBox(16, back, help, spacer, start);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getStyleClass().add("bottom-bar");

        HBox centre = new HBox(32, details, list);
        centre.setAlignment(Pos.CENTER_LEFT);
        centre.getStyleClass().add("select-centre");
        HBox.setHgrow(list, Priority.ALWAYS);
        BorderPane screen = new BorderPane();
        screen.getStyleClass().add("select-screen");
        screen.setTop(topBar);
        screen.setCenter(centre);
        screen.setBottom(bottomBar);

        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            DiffRow current = chosen.get();
            KeyCode code = event.getCode();
            if ((code == KeyCode.UP || code == KeyCode.DOWN) && current != null) {
                // Walk through every difficulty in order, moving on to the next song at the end.
                List<DiffRow> order = new ArrayList<>();
                for (Song song : visible) {
                    for (MapManifest.DifficultyRef d : sortedDifficulties(song.manifest())) order.add(new DiffRow(song, d));
                }
                int index = 0;
                for (int i = 0; i < order.size(); i++) {
                    if (order.get(i).song() == current.song() && order.get(i).difficulty() == current.difficulty()) index = i;
                }
                DiffRow next = order.get(Math.floorMod(index + (code == KeyCode.DOWN ? 1 : -1), order.size()));
                select.accept(next.song(), next.difficulty());
                event.consume();
            } else if ((code == KeyCode.LEFT || code == KeyCode.RIGHT) && current != null) {
                int index = Math.floorMod(visible.indexOf(current.song()) + (code == KeyCode.RIGHT ? 1 : -1), visible.size());
                openSong.accept(visible.get(index));
                event.consume();
            } else if (code == KeyCode.F3 || code == KeyCode.F4) {
                (code == KeyCode.F3 ? slower : faster).fire();
                event.consume();
            } else if (code == KeyCode.ENTER) {
                start.fire();
                event.consume();
            } else if (code == KeyCode.ESCAPE) {
                showMainMenu();
                event.consume();
            }
        });

        showScreen(new ScaledPane(screen));
        if (visible.isEmpty()) { rebuild.run(); showNothing.run(); } else openSong.accept(visible.getFirst());
        Platform.runLater(search::requestFocus);
    }

    /** Fills the detail card's leaderboard with the local top plays of one difficulty. */
    private void showLeaderboard(Label boardTitle, VBox board, DiffRow row) {
        boardTitle.setText("Local scores · " + row.difficulty().name());
        board.getChildren().clear();
        List<ScoreEntry> top = leaderboard.top(ScoreStore.key(row.song().folder(), row.difficulty().chart()), 10);
        if (top.isEmpty()) {
            Label none = new Label("No plays yet - be the first!");
            none.getStyleClass().add("board-empty");
            board.getChildren().add(none);
        }
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd.MM.yy").withZone(ZoneId.systemDefault());
        for (int i = 0; i < top.size(); i++) {
            ScoreEntry e = top.get(i);
            Label line = new Label(String.format("#%-2d %-2s %08d  %6.2f%%  %4dx%s  %s", i + 1, e.grade(), e.score(),
                    e.accuracy(), e.maxCombo(), e.fullCombo() ? " FC" : "   ", date.format(Instant.ofEpochMilli(e.playedAtMs()))));
            line.getStyleClass().add(i == 0 ? "board-best" : "board-row");
            board.getChildren().add(line);
        }
    }

    /** Carousel rows: a song (open or closed) and, under the open song, its difficulties. */
    private record SongRow(Song song, boolean open) {}
    private record DiffRow(Song song, MapManifest.DifficultyRef difficulty) {}

    /** Draws a song card or a smaller, indented difficulty card; the selected one slides out. */
    private static final class CarouselCell extends ListCell<Object> {
        private final Label cardTitle = new Label();
        private final Label cardArtist = new Label();
        private final Label cardMeta = new Label();
        private final VBox songCard = new VBox(2, cardTitle, cardArtist, cardMeta);
        private final Label diffName = new Label();
        private final Label diffStars = new Label();
        private final HBox diffCard = new HBox(12, diffName, diffStars);
        private final HBox diffIndent = new HBox(diffCard);   // difficulties sit indented under the song
        private final TranslateTransition slide = new TranslateTransition(Duration.millis(140));

        CarouselCell() {
            songCard.getStyleClass().add("song-card");
            cardTitle.getStyleClass().add("card-title");
            cardArtist.getStyleClass().add("card-artist");
            cardMeta.getStyleClass().add("card-meta");
            diffCard.getStyleClass().add("diff-card");
            diffCard.setAlignment(Pos.CENTER_LEFT);
            diffIndent.setPadding(new Insets(0, 0, 0, 40));
            HBox.setHgrow(diffCard, Priority.ALWAYS);
            diffName.getStyleClass().add("diff-name");
            diffStars.getStyleClass().add("diff-stars");
            selectedProperty().addListener((observable, was, selected) -> {
                if (!(getItem() instanceof DiffRow)) return;
                slide.stop();
                slide.setNode(diffCard);
                slide.setToX(selected ? -24 : 0);
                slide.play();
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            songCard.getStyleClass().remove("song-card-open");
            if (item instanceof SongRow row) {
                MapManifest m = row.song().manifest();
                int n = m.difficulties().size();
                cardTitle.setText(m.title());
                cardArtist.setText(m.artist());
                cardMeta.setText(m.mapper() + " · " + n + (n == 1 ? " difficulty" : " difficulties"));
                if (row.open()) songCard.getStyleClass().add("song-card-open");
                setGraphic(songCard);
            } else if (item instanceof DiffRow row) {
                diffName.setText(row.difficulty().name());
                diffStars.setText("★ " + row.difficulty().level());
                diffCard.setTranslateX(isSelected() ? -24 : 0);
                setGraphic(diffIndent);
            } else {
                setGraphic(null);
            }
        }
    }

    // Gameplay, pause menu and fail/clear flow. AI-assisted 2026-11-09; see docs/AI_USAGE.md.
    private static final double RESULTS_DELAY_MS = 2000;   // after the last note

    public void showGameplaySelection(Song selected, MapManifest.DifficultyRef difficulty) {
        List<Note> notes;
        JudgementEngine engine;
        ChartInfo chartInfo;
        try {
            Path chartFile = selected.folder().resolve(difficulty.chart());
            notes = BeatmapJson.loadNotes(chartFile);
            chartInfo = BeatmapJson.loadChartInfo(chartFile);
            engine = new JudgementEngine(notes);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return;
        }
        HealthGauge gauge = new HealthGauge(notes.size(),
                engine.lastNoteMs() - notes.stream().mapToLong(Note::timeMs).min().orElse(0));

        GameCanvas game = new GameCanvas();
        game.setScrollSpeed(chartInfo.pixelsPerMs(playerSpeed));
        game.setChartInfo(chartInfo);
        game.setGauge(gauge);
        StackPane screen = new StackPane(game);
        // Unmanaged: otherwise the canvas size becomes the pane's minimum size and the
        // window can never shrink again after being maximised / full screen.
        game.setManaged(false);
        game.widthProperty().bind(screen.widthProperty());
        game.heightProperty().bind(screen.heightProperty());

        InputState<KeyCode> input = new InputState<>();
        Media media = new Media(selected.folder().resolve(selected.manifest().audio()).toUri().toString());
        MediaPlayer player = new MediaPlayer(media);
        boolean[] paused = {false};
        boolean[] ended = {false};
        // Song clock with an artificial intro; created when the audio is ready (needs the lane width).
        LeadInClock[] clock = {null};
        double[] travel = {1};
        long[] noteTimes = notes.stream().mapToLong(Note::timeMs).sorted().toArray();
        long firstNoteMs = noteTimes[0];

        Runnable leave = () -> {
            ended[0] = true;
            game.stop();
            player.dispose();
        };
        Runnable finish = () -> {
            if (ended[0]) return;
            leave.run();
            showResults(selected, difficulty, engine, gauge);
        };

        // Pause overlay.
        Label pauseTitle = new Label("PAUSED");
        pauseTitle.getStyleClass().add("title");
        Button resume = new Button("Resume");
        Button retry = new Button("Retry");
        Button quit = new Button("Quit");
        quit.getStyleClass().add("secondary");
        VBox pauseMenu = new VBox(14, pauseTitle, resume, retry, quit);
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.getStyleClass().add("pause-overlay");
        pauseMenu.setVisible(false);
        screen.getChildren().add(new ScaledPane(pauseMenu));

        Runnable pause = () -> {
            if (paused[0] || ended[0]) return;
            paused[0] = true;
            if (clock[0] != null) clock[0].pause(); else player.pause();
            input.clear();
            pauseMenu.setVisible(true);
            resume.requestFocus();
        };
        Runnable unpause = () -> {
            if (!paused[0] || ended[0]) return;
            paused[0] = false;
            pauseMenu.setVisible(false);
            if (clock[0] != null) clock[0].resume();
            game.requestFocus();
        };
        resume.setOnAction(e -> unpause.run());
        retry.setOnAction(e -> { leave.run(); showGameplaySelection(selected, difficulty); });
        quit.setOnAction(e -> { leave.run(); showSongSelection(); });

        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (paused[0]) unpause.run(); else pause.run();
                event.consume();
                return;
            }
            if (paused[0] || ended[0]) return;   // let the pause buttons handle Enter/Space
            if (clock[0] == null) { event.consume(); return; }   // audio not ready yet
            if (event.getCode() == KeyCode.SPACE) {             // skip the intro or a long break
                double target = LeadInClock.skipTargetAt(noteTimes, clock[0].timeMs(), travel[0]);
                if (!Double.isNaN(target)) clock[0].skipTo(target);
                event.consume();
                return;
            }
            NoteType type = noteTypeFor(event.getCode());
            if (type != null && input.press(event.getCode())) {
                double timeMs = clock[0].timeMs();
                boolean leftHand = event.getCode() == KeyCode.D || event.getCode() == KeyCode.F;
                game.showKeyPress(type, leftHand, timeMs);
                Judgement result = engine.hit(type, timeMs);
                if (result != null) {
                    gauge.record(result, HealthGauge.basePoints(result), timeMs);
                    game.showJudgement(result, timeMs);
                    if (result != Judgement.MISS) game.noteHit(engine.lastHitNote(), timeMs);
                }
            }
            event.consume();
        });
        screen.addEventFilter(KeyEvent.KEY_RELEASED, event -> input.release(event.getCode()));
        game.focusedProperty().addListener((observable, was, focused) -> {
            if (!focused) input.clear();      // spec: clear held keys on focus loss
        });
        showScreen(screen);
        onWindowBlur = pause;                 // alt-tab pauses the song

        player.setOnError(() -> System.err.println("Audio error: " + player.getError()));
        player.setOnReady(() -> {
            // Time a note needs to scroll from the right edge to the drum (drum at 18 % width).
            double travelMs = Math.max(1, game.viewWidth()) * 0.82 / chartInfo.pixelsPerMs(playerSpeed);
            double leadIn = LeadInClock.leadInFor(firstNoteMs, travelMs);
            travel[0] = travelMs;
            clock[0] = new LeadInClock(new LeadInClock.Audio() {
                public double timeMs() { return player.getCurrentTime().toMillis(); }
                public void play() { player.play(); }
                public void pause() { player.pause(); }
                public void seek(double ms) { player.seek(Duration.millis(ms)); }
            }, System::nanoTime, leadIn);
            game.start(notes, clock[0]::timeMs, ms -> {
                int missed = engine.update(ms);
                for (int i = 0; i < missed; i++) gauge.record(Judgement.MISS, 0, ms);
                gauge.setKiai(chartInfo.isKiai(ms));
                gauge.update(ms);
                if (missed > 0) game.showJudgement(Judgement.MISS, ms);
                game.setStats(engine.score() + gauge.bonusScore(), engine.combo());
                boolean canSkip = !Double.isNaN(LeadInClock.skipTargetAt(noteTimes, ms, travel[0]));
                game.setSkipHint(canSkip ? (ms < firstNoteMs ? "SPACE  skip intro" : "SPACE  skip break") : null);
                if (gauge.failed() || (engine.finished() && ms > engine.lastNoteMs() + RESULTS_DELAY_MS)) {
                    Platform.runLater(finish);   // not inside the AnimationTimer callback
                }
            });
            if (paused[0]) clock[0].pause();   // paused before the audio was ready
        });
        player.setOnEndOfMedia(finish);
        game.setFocusTraversable(true);
        Platform.runLater(game::requestFocus);
    }

    public void showResults(Song selected, MapManifest.DifficultyRef difficulty, JudgementEngine engine,
                            HealthGauge gauge) {
        boolean cleared = !gauge.failed();
        double accuracy = engine.accuracy();
        Label grade = new Label(cleared ? grade(accuracy, engine.miss()) : "F");
        grade.getStyleClass().addAll("grade", cleared ? "grade-clear" : "grade-failed");
        Label verdict = new Label(cleared ? (engine.miss() == 0 ? "FULL COMBO!" : "CLEAR!") : "FAILED");
        verdict.getStyleClass().add("verdict");
        Label song = new Label(selected + "  [" + difficulty.name() + "]");
        song.getStyleClass().add("subtitle");
        song.setWrapText(true);
        // Cleared plays go on the local leaderboard (like osu!, failed plays are not kept).
        Label rank = new Label("Failed plays are not saved");
        rank.getStyleClass().add("subtitle");
        if (cleared) {
            String letter = grade(accuracy, engine.miss());
            int place = leaderboard.add(ScoreStore.key(selected.folder(), difficulty.chart()),
                    new ScoreEntry(engine.score() + gauge.bonusScore(), accuracy, engine.maxCombo(),
                            engine.perfect(), engine.good(), engine.miss(), letter, System.currentTimeMillis()));
            rank.setText(place == 1 ? "NEW LOCAL BEST!" : place > 0 ? "Local rank #" + place : "Not in the local top " + Leaderboard.KEEP);
            try {
                scoreStore.save(leaderboard);
            } catch (IOException e) {
                rank.setText(rank.getText() + "  (could not save: " + e.getMessage() + ")");
            }
        }

        GridPane stats = new GridPane();
        stats.getStyleClass().add("results-card");
        stats.setHgap(36);
        stats.setVgap(8);
        String[][] rows = {
            {"Score", String.format("%08d", engine.score() + gauge.bonusScore())},
            {"Fever bonus", "+" + gauge.bonusScore()},
            {"Accuracy", String.format("%.2f%%", accuracy)},
            {"Max combo", engine.maxCombo() + "x"},
            {"Perfect", String.valueOf(engine.perfect())},
            {"Good", String.valueOf(engine.good())},
            {"Miss", String.valueOf(engine.miss())},
        };
        for (int i = 0; i < rows.length; i++) {
            Label name = new Label(rows[i][0]);
            name.getStyleClass().add("stat-name");
            Label value = new Label(rows[i][1]);
            value.getStyleClass().add("stat-value");
            stats.addRow(i, name, value);
        }
        HBox summary = new HBox(40, grade, stats);
        summary.setAlignment(Pos.CENTER);

        Button retry = new Button("Retry");
        retry.setOnAction(event -> showGameplaySelection(selected, difficulty));
        Button back = new Button("Song select");
        back.getStyleClass().add("secondary");
        back.setOnAction(event -> showSongSelection());
        HBox actions = new HBox(12, retry, back);
        actions.setAlignment(Pos.CENTER);

        VBox screen = new VBox(18, verdict, song, rank, summary, actions);
        screen.setAlignment(Pos.CENTER);
        screen.getStyleClass().add("results-screen");
        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) { showSongSelection(); event.consume(); }
        });
        showScreen(new ScaledPane(screen));
        Platform.runLater(retry::requestFocus);
    }

    /** Letter grade from accuracy; any miss caps it at A. */
    static String grade(double accuracy, int misses) {
        if (accuracy >= 95 && misses == 0) return "S";
        if (accuracy >= 90) return "A";
        if (accuracy >= 80) return "B";
        if (accuracy >= 70) return "C";
        return "D";
    }

    private static List<MapManifest.DifficultyRef> sortedDifficulties(MapManifest manifest) {
        return manifest.difficulties().stream()
                .sorted(Comparator.comparingInt(MapManifest.DifficultyRef::level))
                .toList();
    }

    /** Hidden for now: no editor button on the main menu until the editor exists. */
    public void showEditorSelection(){
    }
    private void showScreen(Node screen) {
        preview.stop();   // every screen change ends the song-select preview
        onWindowBlur = null;
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
