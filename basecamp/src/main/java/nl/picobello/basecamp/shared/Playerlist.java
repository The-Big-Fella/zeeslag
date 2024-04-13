package nl.picobello.basecamp.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Playerlist {

    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 7789;
    private final int timeoutMillis = 600000;

    public String fetchList() {

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            socket.setSoTimeout(timeoutMillis);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("get playerlist");

                StringBuilder responseBuilder = new StringBuilder();
                String response;
                while ((response = in.readLine()) != null) {
                    if (response.charAt(0) == 'S') {
                        responseBuilder.append(response).append('\n');
                        break;
                    }
                }
                int startIndex = responseBuilder.toString().indexOf("[");
                String list = responseBuilder.substring(startIndex);
                String content = list.substring(1, list.length() - 1).trim();
                String element = content.replaceAll("\"", "");
                element = element.replaceAll("]", "");
                element = element.replaceAll(", ", "\n");

                String[] words = element.split("\n");
                StringBuilder capitalized = new StringBuilder();

                for (String word : words) {
                    if (!word.isEmpty()) {
                        String firstLetter = word.substring(0, 1).toUpperCase();
                        String restOfWord = word.substring(1).toLowerCase();
                        capitalized.append(firstLetter).append(restOfWord).append(" ");
                    }
                }

                element = capitalized.toString().trim();
                element = element.replaceAll(" ", "\n");
                return element;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

