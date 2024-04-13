package nl.picobello.basecamp.shared;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class Server {
    private static final Server instance = new Server(Constants.SERVER_IP, Constants.SERVER_PORT, Constants.TIMEOUT_MILLIS);
    // private final List<DataEventListener> listeners = new ArrayList<>();
    private final Map<ServerEvents, List<DataEventListener>> listeners = new HashMap<>();
    protected BufferedReader in;
    protected PrintWriter out;
    private Socket socket;

    private Server(String serverAddress, int serverPort, int timeoutMillis) {
        System.out.printf("Connecting to server %s:%d%n", serverAddress, serverPort);
        try {
            socket = new Socket(serverAddress, serverPort);
            socket.setSoTimeout(timeoutMillis);

            System.out.println("Connected to server");

            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

            // listen for server messages
            // the server can return data in the form of "SVR <data>"
            // "SVR" messages can be of the following types:
            //   - "SVR GAMELIST [ "<gametype>" ]*"
            //   - "SVR PLAYERLIST [ "<username>" ]*"
            //   - "SVR GAME MATCH { PLAYERTOMOVE: <username>, OPPONENT: <username>, GAMETYPE: <gametype> }
            //   - "SVR GAME YOURTURN { TURNMESSAGE: <turnmessage> }"
            //   - "SVR GAME MOVE { PLAYER: <username>, MOVE: <move>, RESULT: <result> }"
            //   - "SVR GAME LOSS|WIN|DRAW { PLAYERONESCORE: "<score player 1>", PLAYERTWOSCORE: "<score player 2>", COMMENT: "<result comment>" }
            //   - "SVR GAME CHALLENGE { CHALLENGER: <username>, GAMETYPE: <gametype>, CHALLENGENUMBER: <id> }"
            //   - "SVR GAME CHALLENGE CANCELLED { CHALLENGENUMBER: <id> }"

            Thread serverMessages = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("Received response: " + response);
                        if (response.startsWith("SVR")) {
                            ServerEvents type = getEventType(response);
                            DataEvent event = switch (type) {
                                case GAME_LIST, PLAYER_LIST -> {
                                    // the data is a list of strings, so we can just split it by spaces
                                    // DataEvent still wants a HashMap, so we'll just put the data in a HashMap
                                    HashMap<String, String> data = new HashMap<>();
                                    data.put("LIST", String.join(" ", response.substring(type
                                                            == ServerEvents.GAME_LIST ? 14 : 16)
                                                    .split(" "))
                                            .replaceAll("[\\[\\]\",]", ""));
                                    yield new DataEvent(this, data, type);
                                }
                                default -> {
                                    Pattern pattern = Pattern.compile("([A-Za-z]+): \"([^\"]*)\"");
                                    HashMap<String, String> data = new HashMap<>();
                                    pattern.matcher(response.substring(4)).results().forEach(matchResult -> data.put(matchResult.group(1), matchResult.group(2)));
                                    yield new DataEvent(this, data, type);
                                }
                            };
                            // notify all the listeners that a new event has occurred
                            if (listeners.containsKey(type)) {
                                listeners.get(type).forEach(listener -> {
                                    listener.data(event);
                                });
                            }
                        } else if (response.startsWith("ERR")) {
                            // the server has returned an error
                            // the error message is in the form of "ERR <errormessage>"
                            // we'll just put the error message in a HashMap and notify the listeners
                            HashMap<String, String> data = new HashMap<>();
                            data.put("ERROR", response.substring(4));
                            DataEvent event = new DataEvent(this, data, ServerEvents.ERROR);
                            listeners.get(ServerEvents.ERROR).forEach(listener -> {
                                listener.data(event);
                            });
                        } else if (response.startsWith("OK")) {
                            // the server has returned an OK message
                            // the message is in the form of "OK <okmessage>"
                            // we'll just put the ok message in a HashMap and notify the listeners
                            HashMap<String, String> data = new HashMap<>();
                            DataEvent event = new DataEvent(this, data, ServerEvents.OK);
                            listeners.get(ServerEvents.OK).forEach(listener -> {
                                listener.data(event);
                            });
                        } else {
                            // we dont know what to do with this message
                            // just ignore it
                            System.out.println("Unknown message type: " + response);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    closeConnection();
                    Thread.currentThread().interrupt();
                }
            });
            serverMessages.setDaemon(true);
            serverMessages.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ServerEvents getEventType(String response) {
        // we need to go from "SVR <eventtype> <data>" to <eventtype>
        // its easiest to parse if we just substring till we either hit a "{" or a "["
        String messageType = response.substring(4).split("[\\[{]")[0].trim();
        ServerEvents eventType = switch (messageType) {
            case "GAMELIST" -> ServerEvents.GAME_LIST;
            case "PLAYERLIST" -> ServerEvents.PLAYER_LIST;
            case "GAME MATCH" -> ServerEvents.NEW_MATCH;
            case "GAME YOURTURN" -> ServerEvents.YOUR_TURN;
            case "GAME MOVE" -> ServerEvents.MOVE;
            case "GAME LOSS" -> ServerEvents.LOSE;
            case "GAME WIN" -> ServerEvents.WIN;
            case "GAME DRAW" -> ServerEvents.DRAW;
            case "GAME CHALLENGE" -> ServerEvents.CHALLENGE;
            case "GAME CHALLENGE CANCELLED" -> ServerEvents.CHALLENGE_CANCEL;
            default ->
                    throw new RuntimeException("Unknown SVR packet type - Inferred type:" + messageType + " - Full packet: " + response);
        };
        System.out.println("Inferred event type: " + eventType);
        return eventType;
    }

    public static Server getInstance() {
        return instance;
    }

    public void sendCommand(String command) {
        System.out.println("Sending command: " + command);
        out.println(command);
    }

    // event handlers
    public void addEventListener(ServerEvents type, DataEventListener listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public void removeEventListener(ServerEvents type, DataEventListener listener) {
        listeners.get(type).remove(listener);
    }

    public void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
