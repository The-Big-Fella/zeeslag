package nl.picobello.basecamp.battleship;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import nl.picobello.basecamp.shared.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BattleShipController {
    private final Server server = Server.getInstance();

    @FXML
    public Text state;
    @FXML
    private Label dataLabel;
    @FXML
    private Text userLabel;
    @FXML
    private GridPane grid;
    @FXML
    private Text log;
    @FXML
    private GridPane board;

    @FXML
    private TextField challengeNameField;
    @FXML
    private TextField debugCommand;
    private GameState currentState = GameState.WAITING_FOR_OPPONENT;

    private final BattleshipBoard gameBoard = new BattleshipBoard(server, Session.getInstance().getUsername());

    List<String[]> gameData = new ArrayList<String[]>();
    private int movesCount = 0;
    long startTime = 0;
    long duration = 0;
    private int hitCount = 0;

    public BattleShipController() {
        startDataFetchingTask();
        ChallengePopups.startListening();
    }

    private void updateStateHeader() {
        switch (currentState) {
            case WAITING_FOR_OPPONENT:
                state.setText("Op tegenstander wachten...");
                break;
            case YOUR_TURN:
                state.setText("Het is jouw beurt!");
                break;
            case OPPONENTS_TURN:
                state.setText("Tegenstander is aan de beurt...");
                break;
            case YOU_WON:
                state.setText("Je hebt gewonnen!");
                break;
            case YOU_LOST:
                state.setText("Je hebt verloren :(");
                break;
            case DRAW:
                state.setText("Gelijkspel!");
                break;
        }
    }

    public void initialize() {
        userLabel.setText(Session.getInstance().getUsername());
        Platform.runLater(() -> {
            updateStateHeader();
            fillGridPaneWithSymbols();
            fillBoardWithSymbols();
        });
        //System.out.println(gameBoard.getPlayerName()); //debug
    }

    private void startDataFetchingTask() {
        server.addEventListener(ServerEvents.PLAYER_LIST, event -> {
            HashMap<String, String> data = event.getData();
            // TODO: update the player list
            Platform.runLater(() -> dataLabel.setText(Arrays.toString(data.get("LIST").split(" "))));
        });
        server.addEventListener(ServerEvents.LOSE, event -> {
            currentState = GameState.YOU_LOST;
            //duration = System.currentTimeMillis() - startTime;
            //writeData();
            Platform.runLater(this::updateStateHeader);
        });
        server.addEventListener(ServerEvents.WIN, event -> {
            currentState = GameState.YOU_WON;
            //duration = System.currentTimeMillis() - startTime;
            //writeData();
            Platform.runLater(this::updateStateHeader);
        });
        server.addEventListener(ServerEvents.MOVE, event -> {
            HashMap<String, String> data = event.getData();
            //this.log.setText(log.getText() + String.format("\n%s: Fired at %s - %s", data.get("PLAYER"), data.get("MOVE"), data.get("RESULT")));
            //ALLEMAAL VOOR AI
            int move = Integer.parseInt(data.get("MOVE"));
            int length = 10;
            if (data.get("LENGTH") != null) {
                length = Integer.parseInt(data.get("LENGTH"));
            }
            if (!data.get("PLAYER").equals(Session.getInstance().getUsername())) {
                if (data.get("RESULT").equals("BOEM") || data.get("RESULT").equals("GEZONKEN")) {
                    editCell(grid, move, "💥");
                } else if (data.get("RESULT").equals("PLONS")) {
                    editCell(grid, move, "💦");
                }
            } else if(data.get("PLAYER").equals(Session.getInstance().getUsername())) {
                if(data.get("RESULT").equals("BOEM") || data.get("RESULT").equals("GEZONKEN")) {
                    editCell(board, move, "💥");
                    //hitCount++;
                } else if (data.get("RESULT").equals("PLONS")) {
                    editCell(board, move, "💦");
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            gameBoard.updateBoards(move, data.get("PLAYER"), data.get("RESULT"), length);
            //System.out.println("Board: \n" + gameBoard);
            //System.out.println("Opponent Board: \n" + gameBoard.oppToString());
            currentState = Objects.equals(Session.getInstance().getUsername(), data.get("PLAYER")) ? GameState.YOUR_TURN : GameState.OPPONENTS_TURN;
            Platform.runLater(this::updateStateHeader);
        });
        server.addEventListener(ServerEvents.YOUR_TURN, event -> {
            currentState = GameState.YOUR_TURN;
            if (!gameBoard.shipsPlaced()) { //Voor nu alleen AI
                gameBoard.aiPlaceShips();
                updateBoard(grid);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //System.out.println(gameBoard);
            } else {
                int move = gameBoard.aiMoveAlternate();
                server.sendCommand("move " + move);
                //movesCount++;
            }
            Platform.runLater(this::updateStateHeader);
        });
        server.addEventListener(ServerEvents.DRAW, event -> {
            currentState = GameState.DRAW;
            Platform.runLater(this::updateStateHeader);
        });
        server.addEventListener(ServerEvents.NEW_MATCH, event -> {
            //startTime = System.currentTimeMillis();
            HashMap<String, String> data = event.getData();
            currentState =
                    data.get("PLAYERTOMOVE").equals(Session.getInstance().getUsername())
                            ? GameState.YOUR_TURN
                            : GameState.OPPONENTS_TURN;
            Platform.runLater(this::updateStateHeader);
            gameBoard.resetBoard();//bord resetten
            resetGameBoard();
        });

        Thread playerTask = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        playerTask.setDaemon(true);
        playerTask.start();
    }

    private void fillBoardWithSymbols() {
        int numRows = 8; // Number of rows in the GridPane
        int numCols = 8; // Number of columns in the GridPane

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Label pane = createSymbol(); // Create a new label
                board.add(pane, col, row); // Add the label to the GridPane at the specified row and column
            }
        }
    }

    private void fillGridPaneWithSymbols() {
        int numRows = 8; // Number of rows in the GridPane
        int numCols = 8; // Number of columns in the GridPane

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Label pane = createSymbol(); // Create a new label
                grid.add(pane, col, row); // Add the label to the GridPane at the specified row and column
            }
        }
    }

    private void resetGameBoard() {
        for (int i = 0; i < 63; i++) {
            editCell(grid, i, "🌊");
        }
        for (int i = 0; i < 63; i++) {
            editCell(board, i, "🌊");
        }
    }

    private Label createSymbol() {
        Label symbol = new Label("🌊");
        // Customize the style or properties of the Pane as needed
        symbol.setPrefSize(200, 200);
        // symbol.setStyle("-fx-background-color: #0E65A3; -fx-border-color: #2C81BD");
        symbol.setTextFill(Color.CYAN);
        symbol.setFont(new Font(30));
        symbol.setAlignment(Pos.CENTER);
        // Add any other customization logic here
        return symbol;
    }

    public void challengePlayer(ActionEvent e) {
        String challengeName = challengeNameField.getText().toLowerCase();
        String spelType = "Battleship";
        String challengeRequest = "challenge " + challengeName + " " + spelType;
        server.sendCommand(challengeRequest);
    }

    public void debugSendCommand(ActionEvent e) {
        String command = debugCommand.getText();
        System.out.println(command);
        server.sendCommand(command);
    }

    public void logout(ActionEvent e) throws IOException {
        Platform.exit();
    }

    //Iterate through game board and update gui, only for own board visual
    private void updateBoard(GridPane grid) {
        for (int i = 0; i < 64; i++) {
            if(gameBoard.getSymbol(i) == 'S') {
                editCell(grid, i, "⛵");
            } else {
                //char symbol = gameBoard.getSymbol(i);
                editCell(grid, i, "🌊");
            }    
        }
    }

    //Edit specified cell
    private void editCell(GridPane grid, int index, String symbol) {
        Label label = getNodeFromGridPane(grid, index);
        if(symbol.equals("⛵")) {
            Platform.runLater(() -> {
                label.setTextFill(Color.BLACK);
                label.setText(symbol);
            });
        } else if(symbol.equals("🌊")) {
            Platform.runLater(() -> {
                label.setTextFill(Color.CYAN);
                label.setText(symbol);
            });
        } else if(symbol.equals("💥")) {
            Platform.runLater(() -> {
                label.setTextFill(Color.RED);
                label.setText(symbol);
            });
        } else if(symbol.equals("💦")) {
            Platform.runLater(() -> {
                label.setTextFill(Color.WHITE);
                label.setText(symbol);
            });
        }
    }

    //Debug
    public void debugPane(ActionEvent e) {
        gameBoard.aiPlaceShips();
        updateBoard(grid);
    }

    //Convert 1D index to 2D index
    private int[] convertIndex(int index) {
        int x = index % 8;
        int y = index / 8;
        return new int[]{x, y};

    }

    //Retrieve specific node within grid pane
    private Label getNodeFromGridPane(GridPane gridPane, int index) {
        int[] convertedIndex = convertIndex(index);
        int row = convertedIndex[1];
        int col = convertedIndex[0];

        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);
            if (rowIndex != null && colIndex != null && rowIndex == row && colIndex == col) {
                //System.out.println("Found node at row: " + rowIndex + ", column: " + colIndex);
                if (node instanceof Label) {
                    return (Label) node;
                } else {
                    System.out.println("Unexpected node type: " + node.getClass().getName());
                }
            }
        }
        System.out.println("Node not found at row: " + row + ", column: " + col);
        return null;
    }

    public void switchGame(ActionEvent e) throws IOException {
        JFXUtils.Navigate(this.getClass().getResource("/nl/picobello/basecamp/gamechoice.fxml"), (Stage) ((Node) e.getSource()).getScene().getWindow());
    }

    private List<String[]> readData(String filePath) {
        List<String[]> content = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // Each 'nextLine' is an array representing a row in the CSV
                content.add(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public void writeData() {

        String directoryPath = "./";
        String fileName = String.format("gameData.%s.csv", Session.getInstance().getUsername());
        String pathAndFileName = directoryPath + fileName;

        Path filePath = Paths.get(directoryPath, fileName);

        File file = new File(pathAndFileName);

        FileWriter outputfile;
        CSVWriter writer;

        try {
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
//                System.out.println("CSV file exists in the directory."); //debug
                outputfile = new FileWriter(file, true);
                writer = new CSVWriter(outputfile);

            } else {
//                System.out.println("CSV file does not exist in the directory."); //debug
                outputfile = new FileWriter(file);
                writer = new CSVWriter(outputfile);

                // adding header to csv
                String[] header = { "Resultaat(win/lose)", "Aantal zetten", "Tijdsduur(ms)", "Winrate(%)", "Hitrate(%)"};
                writer.writeNext(header);
            }

            List<String[]> games = readData(pathAndFileName);
            int gamesCount = games.size() - 1;
            int wins = 0;
            double winrate = 0.0;
            double hitRate = (double) hitCount / (double)movesCount * 100;

            for (String[] row : games) {
                for (String cell : row) {
                    if (cell.equals("WON")) {
                        wins++;
                    }
                }
            }

            String result = String.valueOf(currentState);
            result = result.substring(result.indexOf("_") + 1);

            System.out.println(winrate);
            System.out.println(result);
            if (result.equals("WON")) {
                wins++;
                System.out.println(wins);
                if (gamesCount < 0) {
                    gamesCount++;
                }
                System.out.println(gamesCount);
                gamesCount++;
                System.out.println(gamesCount);
                winrate = (double) wins / gamesCount * 100;
                System.out.println(winrate);
            } else if (wins != 0) {
                System.out.println(result);
                System.out.println(wins);
                gamesCount++;
                System.out.println(gamesCount);
                winrate = (double) wins / gamesCount * 100;
                System.out.println(winrate);
            }

            // add data to csv
            gameData.add(new String[] { result, String.valueOf(movesCount),
                    String.valueOf(duration), String.format("%.0f", winrate), String.format("%.0f", hitRate)});
            writer.writeAll(gameData);
            writer.close();
            System.out.println("moves " + movesCount);
            System.out.println("hits " + hitCount);
            movesCount = 0;
            hitCount = 0;
            gameData.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}