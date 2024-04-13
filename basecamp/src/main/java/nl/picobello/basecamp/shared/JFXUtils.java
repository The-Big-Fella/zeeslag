package nl.picobello.basecamp.shared;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public interface JFXUtils {
    static void ShowChallengeNotification() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(JFXUtils.class.getResource("/nl/picobello/basecamp/popup.fxml"));
        Stage stage = new Stage();
        Scene scene = new Scene(fxmlLoader.load());

        // paint the background #0e65a3
        scene.setFill(javafx.scene.paint.Color.valueOf("#0e65a3"));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("A new foe has appeared!");
        stage.setScene(scene);
        TranslateTransition shakeTransition = new TranslateTransition(Duration.millis(50), stage.getScene().getRoot());
        shakeTransition.setByX(10);
        shakeTransition.setCycleCount(5);
        shakeTransition.setAutoReverse(true);
        stage.show();
        shakeTransition.play();
    }

    static void Navigate(URL fxml, Stage current) throws IOException {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                fxmlLoader.setLocation(fxml);
                Parent root = fxmlLoader.load();
                Scene scene = new Scene(root);
                current.setScene(scene);
                current.setMaximized(true); // set window to fullscreen
                current.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
