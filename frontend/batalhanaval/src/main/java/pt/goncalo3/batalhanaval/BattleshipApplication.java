package pt.goncalo3.batalhanaval;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BattleshipApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1) Locate the FXML on the classpath (with a leading “/”):
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find home-view.fxml at '/pt/goncalo3/batalhanaval/fxml/home-view.fxml' on the classpath."
            );
        }

        // 2) Load that FXML hierarchy:
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // 3) Create a Scene (size 400×700) and attach the CSS stylesheet:
        Scene scene = new Scene(root, 400, 700);

        // 4) Show the Stage
        stage.setTitle("Battleship Game");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
