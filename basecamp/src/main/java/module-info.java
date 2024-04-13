module nl.picobello.basecamp {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencsv;


    opens nl.picobello.basecamp to javafx.fxml;
    exports nl.picobello.basecamp;
    exports nl.picobello.basecamp.shared;
    opens nl.picobello.basecamp.shared to javafx.fxml;
    exports nl.picobello.basecamp.tictactoe;
    opens nl.picobello.basecamp.tictactoe to javafx.fxml;
    exports nl.picobello.basecamp.battleship;
    opens nl.picobello.basecamp.battleship to javafx.fxml;
}