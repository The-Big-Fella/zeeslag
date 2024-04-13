package nl.picobello.basecamp.shared;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.IOException;

public class GameChoiceController {

    public void ticTacToe(ActionEvent e) throws IOException {
        JFXUtils.Navigate(this.getClass().getResource("/nl/picobello/basecamp/tictactoe.fxml"), (Stage) ((Node) e.getSource()).getScene().getWindow());
    }

    public void battleShip(ActionEvent e) throws IOException {
        //Doorstruren naar tweede scherm
        JFXUtils.Navigate(this.getClass().getResource("/nl/picobello/basecamp/battleShip.fxml"), (Stage) ((Node) e.getSource()).getScene().getWindow());
    }
}
