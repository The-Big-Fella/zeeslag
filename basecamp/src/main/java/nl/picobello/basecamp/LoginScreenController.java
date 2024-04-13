package nl.picobello.basecamp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import nl.picobello.basecamp.shared.*;

import java.io.IOException;

public class LoginScreenController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private TextField textLogin;
    @FXML
    private Label errorMessageLabel;

    public void login(ActionEvent e) {
        String text = textLogin.getText();
        Server server = Server.getInstance();
        server.addEventListener(ServerEvents.ERROR, event -> {
            Platform.runLater(() -> errorMessageLabel.setText(event.getData().get("ERROR")));
        });
        server.addEventListener(ServerEvents.OK, new DataEventListener() {
            @Override
            public void data(DataEvent event) {
                Session session = Session.getInstance();
                session.setUsername(text.toLowerCase());

                try {
                    JFXUtils.Navigate(this.getClass().getResource("/nl/picobello/basecamp/gamechoice.fxml"), (Stage) ((Node) e.getSource()).getScene().getWindow());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                Platform.runLater(() -> server.removeEventListener(ServerEvents.OK, this));
            }
        });

        if (text.isEmpty()) {
            errorMessageLabel.setText("Voer een naam in.");
        } else {
            server.sendCommand("login " + '"' + text.toLowerCase() + '"');
        }
    }
}
