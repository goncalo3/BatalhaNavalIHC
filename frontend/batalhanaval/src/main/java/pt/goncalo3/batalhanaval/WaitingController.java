package pt.goncalo3.batalhanaval;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the waiting/matchmaking view
 */
public class WaitingController implements Initializable, Game.GameStateListener {

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private Label queueInfoLabel;
    @FXML private Button cancelButton;

    private boolean gameStarted = false;
    private Game gameInstance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Initialize the game
        gameInstance = Game.createInstance();

        // Set up game state listener
        gameInstance.setGameStateListener(this);
    }
    
    /**
     * Cleanup method - called when the controller is no longer needed
     */
    public void cleanup() {
        gameInstance.leaveQueue();
        gameInstance.setGameStateListener(null); // Remove listener to avoid memory leaks
        gameInstance = null; // Clear the game instance
    }



    /**
     * Handle cancel button click
     */
    @FXML
    public void onCancelClick(ActionEvent event) {
  
            gameInstance.leaveQueue();
            gameInstance = null; // Clear the game instance

            
            // Return to home view
            try {
                URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml");
                if (fxmlUrl == null) {
                    throw new RuntimeException("Cannot find home-view.fxml");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent homeRoot = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(homeRoot, 400, 600);

                stage.setTitle("Battleship – Home");
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    

    // Game.GameStateListener implementation
    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            statusLabel.setText("Connected! Waiting for a match...");
        });
        // Join the queue once connected
        if (gameInstance != null && gameInstance.isConnectionValid()) {
            gameInstance.joinQueue();
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            if (!gameStarted) {
                statusLabel.setText("Connection lost. Click cancel to return home.");
                loadingIndicator.setVisible(false);
                queueInfoLabel.setVisible(false);
                cancelButton.setText("RETURN HOME");
            }
        });
    }

    @Override
    public void onGameStarted() {
        Platform.runLater(() -> {
            gameStarted = true;
            statusLabel.setText("Match found! Starting game...");
            loadingIndicator.setVisible(false);
            
            // Transition to ship placement view
            try {
                URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/ship-placement-view.fxml");
                if (fxmlUrl == null) {
                    throw new RuntimeException("Cannot find ship-placement-view.fxml");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent placementRoot = loader.load();

                Stage stage = (Stage) cancelButton.getScene().getWindow();
                Scene scene = new Scene(placementRoot, 800, 900);

                stage.setTitle("Battleship – Place Your Ships");
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            statusLabel.setText("Error: " + error);
            loadingIndicator.setVisible(false);
        });
    }

    /**
     * Handle queue updates from the server
     */
    public void onQueueUpdate(int playersInQueue) {
        Platform.runLater(() -> {
            if (playersInQueue <= 1) {
                queueInfoLabel.setText("You are first in queue");
            } else {
                queueInfoLabel.setText(playersInQueue + " players in queue");
            }
        });
    }

    // Game methods that are not used in this controller

    @Override
    public void onShipsAccepted() {
        throw new UnsupportedOperationException("Unimplemented method 'onShipsAccepted'");
    }
    
    @Override
    public void onYourTurn() {
        throw new UnsupportedOperationException("Unimplemented method 'onYourTurn'");
    }

    @Override
    public void onOpponentTurn() {
        throw new UnsupportedOperationException("Unimplemented method 'onOpponentTurn'");
    }

    @Override
    public void onGameEnded(boolean won) {
        throw new UnsupportedOperationException("Unimplemented method 'onGameEnded'");
    }

    @Override
    public void onPlayerAttackResult(int x, int y, String result) {
        throw new UnsupportedOperationException("Unimplemented method 'onPlayerAttackResult'");
    }

    @Override
    public void onOpponentAttackResult(int x, int y, String result) {
        throw new UnsupportedOperationException("Unimplemented method 'onOpponentAttackResult'");
    }

    @Override
    public void onShipDestroyed(Ship ship, boolean onPlayerGrid) {
        throw new UnsupportedOperationException("Unimplemented method 'onShipDestroyed'");
    }
}
