package pt.goncalo3.batalhanaval;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the home page of the Battleship game.
 */
public class HomeController implements Initializable {

    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button playButton;
    @FXML private Button leaderboardButton;
    @FXML private Button logoutButton;
    @FXML private Label usernameLabel;

    private final User user = User.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateButtonVisibility();
    }

    /**
     * Show or hide buttons based on whether the user is authenticated.
     */
    private void updateButtonVisibility() {
        boolean isAuthenticated = user.isAuthenticated();

        // Show LOGIN & REGISTER only if not authenticated
        loginButton.setVisible(!isAuthenticated);
        loginButton.setManaged(!isAuthenticated);

        registerButton.setVisible(!isAuthenticated);
        registerButton.setManaged(!isAuthenticated);

        // Show PLAY & LOGOUT only if authenticated
        playButton.setVisible(isAuthenticated);
        playButton.setManaged(isAuthenticated);

        logoutButton.setVisible(isAuthenticated);
        logoutButton.setManaged(isAuthenticated);

        // LEADERBOARD is always visible
        leaderboardButton.setVisible(true);
        leaderboardButton.setManaged(true);

        // USERNAME LABEL only if authenticated
        usernameLabel.setVisible(isAuthenticated);
        usernameLabel.setManaged(isAuthenticated);
        if (isAuthenticated && user.getUsername() != null) {
            usernameLabel.setText("Welcome, " + user.getUsername() + "!");
        }
    }

    /**
     * Called when the user clicks “LOGIN” on the Home screen.
     * Loads login-view.fxml (login mode).
     */
    @FXML
    public void onLoginButtonClick(ActionEvent event) throws IOException {
        // 1) Locate login-view.fxml via its absolute classpath location
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/login-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find login-view.fxml at '/pt/goncalo3/batalhanaval/fxml/login-view.fxml'"
            );
        }

        // 2) Load it
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent loginRoot = loader.load();

        // 3) Make sure the controller is in login mode
        LoginController loginController = loader.getController();
        loginController.setRegistrationMode(false);

        // 4) Swap the scene on the same stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(loginRoot, 400, 600);


        stage.setTitle("Battleship – Login");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the user clicks “REGISTER” on the Home screen.
     * Loads login-view.fxml (registration mode).
     */
    @FXML
    public void onRegisterButtonClick(ActionEvent event) throws IOException {
        // 1) Locate login-view.fxml
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/login-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find login-view.fxml at '/pt/goncalo3/batalhanaval/fxml/login-view.fxml'"
            );
        }

        // 2) Load it
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent registerRoot = loader.load();

        // 3) Tell the controller to switch into registration mode
        LoginController loginController = loader.getController();
        loginController.setRegistrationMode(true);

        // 4) Swap the scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(registerRoot, 400, 600);

        stage.setTitle("Battleship – Register");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the user clicks “PLAY” (only visible if authenticated).
     * Loads ship-placement-view.fxml.
     */
    @FXML
    public void onPlayButtonClick(ActionEvent event) throws IOException {
        // 1) Locate waiting-view.fxml
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/waiting-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find waiting-view.fxml at '/pt/goncalo3/batalhanaval/fxml/waiting-view.fxml'"
            );
        }

        // 2) Load it
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent waitingRoot = loader.load();

        // 3) Swap the scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(waitingRoot, 400, 300);

        stage.setTitle("Battleship – Finding Match");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the user clicks “LEADERBOARD.” Always available.
     * Loads leaderboard-view.fxml.
     */
    @FXML
    public void onLeaderboardButtonClick(ActionEvent event) throws IOException {
        // 1) Locate leaderboard-view.fxml
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/leaderboard-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find leaderboard-view.fxml at '/pt/goncalo3/batalhanaval/fxml/leaderboard-view.fxml'"
            );
        }

        // 2) Load it
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent leaderboardRoot = loader.load();

        // 3) Swap the scene
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(leaderboardRoot, 900, 650);


        stage.setTitle("Battleship – Leaderboard");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the user clicks “LOGOUT.” Logs the user out and refreshes Home UI.
     */
    @FXML
    public void onLogoutButtonClick(ActionEvent event) {
        user.logout();
        updateButtonVisibility();
    }

    /**
     * Public method that other controllers (e.g. LoginController) can call
     * to refresh Home UI when the authentication state has changed.
     */
    public void refreshUI() {
        updateButtonVisibility();
    }
}
