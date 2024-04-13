package nl.picobello.basecamp;

import nl.picobello.basecamp.battleship.BattleshipBoard;
import nl.picobello.basecamp.shared.Server;

import java.util.Map;
import java.util.Scanner;

public class AITest {
    public static void main(String[] args) {
        boolean PLAYER_TURN;
        boolean AI_TURN;
        boolean noShips;

        boolean inGame = true;
        Server server = Server.getInstance();
        BattleshipBoard playerBoard = new BattleshipBoard(server, "player");
        BattleshipBoard aiBoard = new BattleshipBoard(server, "AI");

        PLAYER_TURN = true;
        AI_TURN = false;
        noShips = true;

        Scanner scanner = new Scanner(System.in);

        while (inGame) {
            if (noShips) {
                playerBoard.placeShips();
                aiBoard.aiPlaceShips();
                System.out.print(aiBoard);
                noShips = false;
            }

            while (PLAYER_TURN) {
                System.out.println("Player turn.");

                System.out.println("Please enter the index of your move: ");
                int move = scanner.nextInt();

                while (move < 0 || move > 63) {
                    System.out.println("Invalid index. Please enter the index of your move: ");
                    move = scanner.nextInt();
                }

                String result = "";
                int sunkShipSize = 0;

                if (aiBoard.getSymbol(move) == 'S') {
                    result = "PLONS";
                    aiBoard.editCell(move, 'X');
                    int shipSunk = isShipSunk(aiBoard, move);

                    if (shipSunk != 0) {
                        result = "GEZONKEN";
                        System.out.println("Ship of size " + shipSunk + " is sunk!");
                        sunkShipSize = shipSunk;
                    }
                } else if (aiBoard.getSymbol(move) == '-' || aiBoard.getSymbol(move) == 'O') {
                    result = "BOEM";
                } else {
                    System.out.println("Index already hit.");
                }

                aiBoard.updateBoards(move, "player", result, sunkShipSize);
                playerBoard.updateBoards(move, "player", result, sunkShipSize);
                //System.out.print(playerBoard.getOppShips());
                System.out.println(playerBoard.oppToString());

                PLAYER_TURN = false;
                AI_TURN = true;
            }

            while (AI_TURN) {
                System.out.println("AI turn");

                int move = aiBoard.aiMove();

                String result = "";
                int sunkShipSize = 0;

                if (playerBoard.getSymbol(move) == 'S') {
                    result = "PLONS";
                    playerBoard.editCell(move, 'X');
                    int shipSunk = isShipSunk(playerBoard, move);

                    if (shipSunk != 0) {
                        result = "GEZONKEN";
                        System.out.println("Ship of size " + shipSunk + " is sunk!");
                        sunkShipSize = shipSunk;
                    }
                } else if (playerBoard.getSymbol(move) == '-' || playerBoard.getSymbol(move) == 'O') {
                    result = "BOEM";
                } else {
                    System.out.println("Index already hit.");
                }

                aiBoard.updateBoards(move, "AI", result, sunkShipSize);
                playerBoard.updateBoards(move, "AI", result, sunkShipSize);
                System.out.println(playerBoard);

                PLAYER_TURN = true;
                AI_TURN = false;
            }
            if (playerBoard.gameOver() || aiBoard.gameOver()) {
                System.out.println("Game Over");
                inGame = false;
            }
        }
        scanner.close();
    }

    private static int isShipSunk(BattleshipBoard board, int move) {
        Map<Integer, int[]> shipPositions = board.getShipPositions();
        char symbol = board.getSymbol(move);

        if (symbol == 'S' || symbol == 'X') {
            for (Map.Entry<Integer, int[]> entry : shipPositions.entrySet()) {
                int size = entry.getKey();
                int[] indices = entry.getValue();
                int start = indices[0];
                int end = indices[1];
                boolean isVertical = false;
                boolean isHorizontal = false;

                if ((end - start) % 8 == 0) {
                    isVertical = true;
                } else if (end - start < 9) {
                    isHorizontal = true;
                }

                if (isVertical && (move >= start && move <= end) && (move % 8 - start % 8 == 0)) {
                    //System.out.println(size + " vertical");
                    // Check if the ship is sunk
                    boolean shipSunk = true;
                    for (int i = start; i <= end; i += 8) {
                        if (board.getSymbol(i) == 'S') {
                            shipSunk = false; // The ship is not completely sunk
                            break;
                        }
                    }

                    if (shipSunk) {
                        System.out.println("Sunk");
                        return size; // The size of the ship that is completely sunk
                    }
                } else if (isHorizontal && (move >= start && move <= end)) {
                    //System.out.println(size + "horizontal");
                    // Check if the ship is sunk
                    boolean shipSunk = true;
                    for (int i = start; i <= end; i++) {
                        if (board.getSymbol(i) == 'S') {
                            shipSunk = false; // The ship is not completely sunk
                            break;
                        }
                    }

                    if (shipSunk) {
                        return size; // The size of the ship that is completely sunk
                    }
                }

            }
        }
        return 0; // No ship is sunk
    }
}