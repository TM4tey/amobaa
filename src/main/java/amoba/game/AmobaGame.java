package amoba.game;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

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

@SuppressWarnings({"PMD.TooManyMethods"})
public class AmobaGame {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmobaGame.class);

    private static final int DEFAULT_HIGHSCORE_LIMIT = 10;
    private static final int MIN_LEP_PARTS = 2;
    private static final String AI_NAME = "Gép";

    private final ConsoleUI ui = new ConsoleUI();
    private final RandomAI ai = new RandomAI();
    private final ScoreService scoreService = new ScoreService();

    private Board board;
    private String humanName = "Játékos";

    private enum Command {
        LEP, SAVE, LOAD, SAVEXML, LOADXML, HIGHSCORE, QUIT, POSITION
    }

    private Map<Command, Function<String[], Boolean>> commandHandlers;

    public void start() {
        initCommands();
        ui.println("Amőba (NxM)");
        ui.println("Szabály: csak már lerakott jelekhez szomszédosan (átlós is) lehet rakni. 5 egymás után = győzelem.");
        ui.println("");

        String name = ui.ask("Add meg a neved (Enter = Játékos): ").trim();
        if (!name.isEmpty()) {
            humanName = name;
        }

        String load = ui.ask("Betöltés TXT-ből? (y/n) ").trim().toLowerCase(Locale.ROOT);
        if (load.startsWith("y")) {
            loadBoardInteractive();
        } else {
            board = createBoardInteractive();
        }

        if (board.legalPositionsByAdjacency().isEmpty()) {
            Position center = board.center();
            board.place(Cell.X, center);
            ui.println("Automatikus kezdő lépés X középen: " + formatPos(center));
        }

        runLoop();
        ui.println("Játék vége. Köszönöm a játékot!");
    }

    private void initCommands() {
        commandHandlers = new EnumMap<>(Command.class);
        commandHandlers.put(Command.LEP, this::handleLep);
        commandHandlers.put(Command.SAVE, this::handleSave);
        commandHandlers.put(Command.LOAD, this::handleLoad);
        commandHandlers.put(Command.SAVEXML, this::handleSaveXml);
        commandHandlers.put(Command.LOADXML, this::handleLoadXml);
        commandHandlers.put(Command.HIGHSCORE, this::handleHighscore);
        commandHandlers.put(Command.QUIT, this::handleQuit);
        // POSITION: külön kezelem a fallback-ben
    }

    private void runLoop() {
        Cell current = Cell.O;
        boolean running = true;
        while (running) {
            ui.println("");
            ui.println(board.render());
            ui.println("Parancsok: lep <b3> | save <f.txt> | load <f.txt> | savexml <f.xml> |");
            ui.println("           loadxml <f.xml> | highscore | quit | <pozíció pl. b3>");

            running = (current == Cell.X) ? humanTurn() : aiTurn();
            current = (current == Cell.X) ? Cell.O : Cell.X;
        }
    }

    private void loadBoardInteractive() {
        String file = ui.ask("Fájlnév (pl. input.txt): ").trim();
        try {
            board = FileIO.loadFromTxt(Path.of(file));
            ui.println("Pálya betöltve.");
        } catch (Exception e) {
            ui.println("Betöltési hiba: " + e.getMessage());
            LOGGER.warn("Betöltési hiba", e);
            board = createBoardInteractive();
        }
    }

    private Board createBoardInteractive() {
        int n = parseInt(ui.ask("N (4 <= M <= N <= 25): "));
        int m = parseInt(ui.ask("M (4 <= M <= N <= 25): "));
        try {
            return new Board(n, m);
        } catch (IllegalArgumentException e) {
            ui.println("Hibás méretek: " + e.getMessage());
            return createBoardInteractive();
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            ui.println("Adj meg egy számot!");
            return parseInt(ui.ask("> "));
        }
    }

    private boolean humanTurn() {
        while (true) {
            String cmdLine = ui.ask(humanName + " (X) lépése: ").trim();
            if (cmdLine.isEmpty()) {
                continue;
            }
            String[] parts = cmdLine.split("\\s+");
            Command command = parseCommand(parts[0]);
            if (command == Command.POSITION) {
                if (handlePosition(parts[0])) {
                    return true;
                }
                continue;
            }
            Function<String[], Boolean> handler = commandHandlers.get(command);
            try {
                boolean continueGame = handler.apply(parts);
                return continueGame;
            } catch (Exception e) {
                ui.println("Hiba: " + e.getMessage());
            }
        }
    }

    private Command parseCommand(String token) {
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

    private Boolean handleLep(String[] parts) {
        if (parts.length < MIN_LEP_PARTS) {
            ui.println("Használat: lep b3");
            return true;
        }
        Position p = ui.parsePosition(parts[1], board.rows(), board.cols());
        if (!board.legalPositionsByAdjacency().contains(p)) {
            ui.println("Nem szomszédos mező.");
            return true;
        }
        board.place(Cell.X, p);
        return !checkWin(Cell.X, p, humanName);
    }

    private Boolean handleSave(String[] parts) {
        if (parts.length < 2) {
            ui.println("Használat: save állapot.txt");
            return true;
        }
        try {
            FileIO.saveToTxt(board, Path.of(parts[1]));
            ui.println("Mentve TXT-be.");
        } catch (Exception e) {
            ui.println("I/O hiba: " + e.getMessage());
        }
        return true;
    }

    private Boolean handleLoad(String[] parts) {
        if (parts.length < 2) {
            ui.println("Használat: load állapot.txt");
            return true;
        }
        try {
            board = FileIO.loadFromTxt(Path.of(parts[1]));
            ui.println("Betöltve TXT-ből.");
        } catch (Exception e) {
            ui.println("Betöltési hiba: " + e.getMessage());
        }
        return true;
    }

    private Boolean handleSaveXml(String[] parts) {
        if (parts.length < 2) {
            ui.println("Használat: savexml állapot.xml");
            return true;
        }
        try {
            XmlPersistence.saveToXml(board, Path.of(parts[1]));
            ui.println("Mentve XML-be.");
        } catch (Exception e) {
            ui.println("I/O hiba: " + e.getMessage());
        }
        return true;
    }

    private Boolean handleLoadXml(String[] parts) {
        if (parts.length < 2) {
            ui.println("Használat: loadxml állapot.xml");
            return true;
        }
        try {
            board = XmlPersistence.loadFromXml(Path.of(parts[1]));
            ui.println("Betöltve XML-ből.");
        } catch (Exception e) {
            ui.println("Betöltési hiba: " + e.getMessage());
        }
        return true;
    }

    private Boolean handleHighscore(String[] parts) {
        Map<String, Integer> scores = scoreService.highScoresTop(DEFAULT_HIGHSCORE_LIMIT);
        if (scores.isEmpty()) {
            ui.println("Nincs high score adat.");
            return true;
        }
        ui.println("High score:");
        int rank = 1;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            ui.println(rank + ". " + e.getKey() + " - " + e.getValue());
            rank++;
        }
        return true;
    }

    private Boolean handleQuit(String[] parts) {
        ui.println("Kilépés...");
        return false;
    }

    private boolean handlePosition(String token) {
        try {
            Position p = ui.parsePosition(token, board.rows(), board.cols());
            if (!board.legalPositionsByAdjacency().contains(p)) {
                ui.println("Nem szomszédos mező.");
                return true;
            }
            board.place(Cell.X, p);
            return !checkWin(Cell.X, p, humanName);
        } catch (IllegalArgumentException e) {
            ui.println("Ismeretlen parancs vagy pozíció.");
            return true;
        }
    }

    private boolean aiTurn() {
        Position aiMove = ai.chooseMove(board);
        if (aiMove == null) {
            ui.println("A gép nem tud lépni. Döntetlen.");
            return false;
        }
        board.place(Cell.O, aiMove);
        ui.println(AI_NAME + " (O) lépése: " + formatPos(aiMove));
        return !checkWin(Cell.O, aiMove, AI_NAME);
    }

    private boolean checkWin(Cell cell, Position p, String playerName) {
        if (board.hasFiveInARow(cell, p)) {
            ui.println(board.render());
            ui.println(playerName + " nyert!");
            scoreService.recordWin(playerName);
            return true;
        }
        return false;
    }

    private String formatPos(Position p) {
        char col = (char) ('a' + p.col());
        int row = p.row() + 1;
        return String.valueOf(col) + row;
    }
}