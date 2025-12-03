package amoba.game;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import amoba.ai.RandomAI;
import amoba.board.Board;
import amoba.db.ScoreService;
import amoba.io.ConsoleUI;
import amoba.io.FileIO;
import amoba.io.XmlPersistence;
import amoba.model.Cell;
import amoba.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Amőba játék fő vezérlő osztály.
 */
public class AmobaGame {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmobaGame.class);

    private static final int DEFAULT_HIGHSCORE_LIMIT = 10;
    private static final int MIN_LEP_PARTS = 2;
    private static final int ARG_MIN = 2;
    private static final String AI_NAME = "Gép";

    private final ConsoleUI ui = new ConsoleUI();
    private final RandomAI ai = new RandomAI();
    private final ScoreService scoreService = new ScoreService(); // TXT alapú score tárolás
    private Map<Command, CommandHandler> handlers;

    private Board board;
    private String humanName = "Játékos";

    private enum Command {
        LEP, SAVE, LOAD, SAVEXML, LOADXML, HIGHSCORE, QUIT, POSITION
    }

    private enum TurnResult {
        KEEP_TURN,
        MOVE_DONE_CONTINUE,
        MOVE_DONE_STOP
    }

    @FunctionalInterface
    private interface CommandHandler {
        TurnResult handle(String... parts) throws IOException;
    }

    public void start() {
        initHandlers();
        setupBoard();
        gameLoop();
    }

    private void setupBoard() {
        ui.println("Amőba (NxM)");
        ui.println("Szabály: csak már lerakott jelekhez szomszédosan (átlós is) lehet rakni." +
                " 5 egymás után = győzelem.\n");

        String name = ui.ask("Add meg a neved (Enter = Játékos): ").trim();
        if (!name.isEmpty()) {
            humanName = name;
        }

        if (ui.ask("Betöltés TXT-ből? (y/n) ").trim().toLowerCase(Locale.ROOT).startsWith("y")) {
            loadBoardFromTxtInteractive();
        } else {
            board = createBoardInteractive();
        }

        if (board.legalPositionsByAdjacency().isEmpty()) {
            Position center = board.center();
            board.place(Cell.X, center);
            ui.println("Automatikus kezdő lépés X középen: " + formatPos(center));
        }
    }

    private void initHandlers() {
        handlers = new EnumMap<>(Command.class);
        handlers.put(Command.LEP, this::handleLep);
        handlers.put(Command.SAVE, this::handleSave);
        handlers.put(Command.LOAD, this::handleLoad);
        handlers.put(Command.SAVEXML, this::handleSaveXml);
        handlers.put(Command.LOADXML, this::handleLoadXml);
        handlers.put(Command.HIGHSCORE, p -> handleHighscore());
        handlers.put(Command.QUIT, p -> handleQuit());
        // POSITION külön kezelve
    }

    private void loadBoardFromTxtInteractive() {
        String file = ui.ask("Fájlnév (pl. input.txt): ").trim();
        try {
            board = FileIO.loadFromTxt(Path.of(file));
            ui.println("Pálya betöltve.");
        } catch (IllegalArgumentException | IOException e) {
            ui.println("Betöltési hiba: " + e.getMessage());
            LOGGER.warn("Betöltési hiba", e);
            board = createBoardInteractive();
        }
    }

    private Board createBoardInteractive() {
        while (true) {
            int n = parsePositiveInt(ui.ask("N (4 <= M <= N <= 25): "));
            int m = parsePositiveInt(ui.ask("M (4 <= M <= N <= 25): "));
            try {
                return new Board(n, m);
            } catch (IllegalArgumentException e) {
                ui.println("Hibás méretek: " + e.getMessage());
            }
        }
    }

    private int parsePositiveInt(String input) {
        String current = input;
        while (true) {
            try {
                return Integer.parseInt(current.trim());
            } catch (NumberFormatException e) {
                ui.println("Adj meg egy számot!");
                current = ui.ask("> ");
            }
        }
    }

    private void gameLoop() {
        Cell turn = Cell.X;
        boolean running = true;
        while (running) {
            ui.println("");
            ui.println(board.render());
            ui.println("Parancsok: lep <b3> | save <f.txt> | load <f.txt> | savexml <f.xml> |" +
                    " loadxml <f.xml> | highscore | quit | <pozíció pl. b3>");

            if (turn == Cell.X) {
                TurnResult r = humanTurn();
                if (r == TurnResult.MOVE_DONE_STOP) {
                    running = false;
                } else if (r == TurnResult.MOVE_DONE_CONTINUE) {
                    turn = Cell.O;
                }
            } else {
                running = aiTurn();
                if (running) {
                    turn = Cell.X;
                }
            }
        }
    }

    private TurnResult humanTurn() {
        TurnResult result = TurnResult.KEEP_TURN;
        boolean waiting = true;
        while (waiting) {
            String line = ui.ask(humanName + " (X) lépése: ").trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            Command cmd = resolveCommand(parts[0]);

            try {
                result = executeCommand(cmd, parts);
            } catch (IllegalArgumentException e) {
                ui.println("Hiba: " + e.getMessage());
                continue;
            } catch (IOException e) {
                ui.println("I/O hiba: " + e.getMessage());
                LOGGER.warn("I/O hiba", e);
                continue;
            }

            if (result != TurnResult.KEEP_TURN) {
                waiting = false;
            }
        }
        return result;
    }

    private TurnResult executeCommand(Command cmd, String... parts) throws IOException {
        if (cmd == Command.POSITION) {
            return handlePosition(parts[0]);
        }
        CommandHandler handler = handlers.get(cmd);
        if (handler == null) {
            throw new IllegalArgumentException("Ismeretlen parancs");
        }
        return handler.handle(parts);
    }

    private Command resolveCommand(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "lep" -> Command.LEP;
            case "save" -> Command.SAVE;
            case "load" -> Command.LOAD;
            case "savexml" -> Command.SAVEXML;
            case "loadxml" -> Command.LOADXML;
            case "highscore" -> Command.HIGHSCORE;
            case "quit" -> Command.QUIT;
            default -> Command.POSITION;
        };
    }

    /* ===== Handler metódusok ===== */

    private TurnResult handleLep(String... parts) {
        if (parts.length < MIN_LEP_PARTS) {
            ui.println("Használat: lep b3");
            return TurnResult.KEEP_TURN;
        }
        Position p = ui.parsePosition(parts[1], board.rows(), board.cols());
        if (!board.legalPositionsByAdjacency().contains(p)) {
            ui.println("Nem szomszédos mező.");
            return TurnResult.KEEP_TURN;
        }
        board.place(Cell.X, p);
        return afterMove(Cell.X, p, humanName);
    }

    private TurnResult handleSave(String... parts) throws IOException {
        if (parts.length < ARG_MIN) {
            ui.println("Használat: save állapot.txt");
            return TurnResult.KEEP_TURN;
        }
        FileIO.saveToTxt(board, Path.of(parts[1]));
        ui.println("Mentve TXT-be.");
        return TurnResult.KEEP_TURN;
    }

    private TurnResult handleLoad(String... parts) throws IOException {
        if (parts.length < ARG_MIN) {
            ui.println("Használat: load állapot.txt");
            return TurnResult.KEEP_TURN;
        }
        try {
            board = FileIO.loadFromTxt(Path.of(parts[1]));
            ui.println("Betöltve TXT-ből.");
        } catch (IllegalArgumentException e) {
            ui.println("Betöltési hiba: " + e.getMessage());
        }
        return TurnResult.KEEP_TURN;
    }

    private TurnResult handleSaveXml(String... parts) throws IOException {
        if (parts.length < ARG_MIN) {
            ui.println("Használat: savexml állapot.xml");
            return TurnResult.KEEP_TURN;
        }
        try {
            XmlPersistence.saveToXml(board, Path.of(parts[1]));
            ui.println("Mentve XML-be.");
        } catch (IOException e) {
            ui.println("I/O hiba: " + e.getMessage());
            LOGGER.warn("XML mentési hiba", e);
        }
        return TurnResult.KEEP_TURN;
    }

    private TurnResult handleLoadXml(String... parts) throws IOException {
        if (parts.length < ARG_MIN) {
            ui.println("Használat: loadxml állapot.xml");
            return TurnResult.KEEP_TURN;
        }
        try {
            board = XmlPersistence.loadFromXml(Path.of(parts[1]));
            ui.println("Betöltve XML-ből.");
        } catch (IllegalArgumentException | IOException e) {
            ui.println("Betöltési hiba: " + e.getMessage());
            LOGGER.warn("XML betöltési hiba", e);
        }
        return TurnResult.KEEP_TURN;
    }

    private TurnResult handleHighscore() {
        Map<String, Integer> scores = scoreService.highScoresTop(DEFAULT_HIGHSCORE_LIMIT);
        if (scores.isEmpty()) {
            ui.println("Nincs high score adat.");
            return TurnResult.KEEP_TURN;
        }
        ui.println("High score:");
        int rank = 1;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            ui.println(rank + ". " + e.getKey() + " - " + e.getValue());
            rank++;
        }
        return TurnResult.KEEP_TURN;
    }

    private TurnResult handleQuit() {
        ui.println("Kilépés...");
        return TurnResult.MOVE_DONE_STOP;
    }

    private TurnResult handlePosition(String token) {
        Position p = ui.parsePosition(token, board.rows(), board.cols());
        if (!board.legalPositionsByAdjacency().contains(p)) {
            ui.println("Nem szomszédos mező.");
            return TurnResult.KEEP_TURN;
        }
        board.place(Cell.X, p);
        return afterMove(Cell.X, p, humanName);
    }

    /* ===== AI és közös segéd ===== */

    private boolean aiTurn() {
        Position aiMove = ai.chooseMove(board);
        if (aiMove == null) {
            ui.println("A gép nem tud lépni. Döntetlen.");
            return false;
        }
        board.place(Cell.O, aiMove);
        ui.println(AI_NAME + " (O) lépése: " + formatPos(aiMove));
        if (board.hasFiveInARow(Cell.O, aiMove)) {
            ui.println(board.render());
            ui.println(AI_NAME + " nyert!");
            scoreService.recordWin(AI_NAME);
            return false;
        }
        return true;
    }

    private TurnResult afterMove(Cell cell, Position p, String playerName) {
        if (board.hasFiveInARow(cell, p)) {
            ui.println(board.render());
            ui.println(playerName + " nyert!");
            scoreService.recordWin(playerName);
            return TurnResult.MOVE_DONE_STOP;
        }
        return TurnResult.MOVE_DONE_CONTINUE;
    }

    private String formatPos(Position p) {
        char col = (char) ('a' + p.col());
        return col + Integer.toString(p.row() + 1);
    }
}