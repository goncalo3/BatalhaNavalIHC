package pt.goncalo3.batalhanaval;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class BattleshipController implements Game.GameStateListener {
    private static final double CIRCLE_RADIUS = 12.0;
    private static final double CELL_SIZE = CIRCLE_RADIUS * 2.0;
    private static final int GRID_SIZE = 10;

    @FXML private GridPane playerGrid;
    @FXML private GridPane enemyGrid;
    @FXML private Label turnIndicator;
    @FXML private Label enemyTurnIndicator;
    @FXML private Label statusMessage;
    @FXML private VBox playerBoardSection;
    @FXML private VBox enemyBoardSection;

    private Rectangle[][] playerSquares = new Rectangle[GRID_SIZE][GRID_SIZE];
    private Rectangle[][] enemySquares  = new Rectangle[GRID_SIZE][GRID_SIZE];
    private List<Ship> playerShips;
    private Game gameInstance = Game.getInstance();
    private String lastGameEventSummary = "";

    @FXML
    public void initialize() {
        buildGameGrids();
        initializeUIState();
        gameInstance.setGameStateListener(this);
        playerShips = gameInstance.getShips();
        displayPlayerShips();

        if (gameInstance.isYourTurn()) {
            onYourTurn();
        } else {
            onOpponentTurn();
        }
    }

    private void initializeUIState() {
        if (statusMessage != null) {
            statusMessage.setText("Connecting to game...");
        }
    }

    @FXML
    private void switchToOpponentView() {
        System.out.println("Switching to opponent view");
        if (playerBoardSection != null) {
            playerBoardSection.setVisible(false);
            playerBoardSection.setManaged(false);
        }
        if (enemyBoardSection != null) {
            enemyBoardSection.setVisible(true);
            enemyBoardSection.setManaged(true);
        }
        if (statusMessage != null) {
            statusMessage.setText("Attack the enemy! Left click to hit, right click to mark as miss.");
        }
    }

    @FXML
    private void switchToYourView() {
        System.out.println("Switching to your view");
        if (playerBoardSection != null) {
            playerBoardSection.setVisible(true);
            playerBoardSection.setManaged(true);
        }
        if (enemyBoardSection != null) {
            enemyBoardSection.setVisible(false);
            enemyBoardSection.setManaged(false);
        }
        if (statusMessage != null) {
            statusMessage.setText("Your board - Ships are shown in yellow.");
        }
    }

    private void buildGameGrids() {
        buildPlayerGrid();
        buildEnemyGrid();
    }

    private void buildPlayerGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.getStyleClass().addAll("dot", "player");
                playerGrid.add(rect, col, row);
                playerSquares[row][col] = rect;
            }
        }
        playerGrid.setDisable(true);
    }

    private void buildEnemyGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.getStyleClass().add("dot");
                rect.setOnMouseClicked(event -> handleEnemySquareClick(rect, event.getButton()));
                enemyGrid.add(rect, col, row);
                enemySquares[row][col] = rect;
            }
        }
    }

    private void handleEnemySquareClick(Rectangle square, MouseButton button) {
        if (!gameInstance.isYourTurn()) {
            updateStatusMessage("It\'s not your turn!");
            return;
        }

        if (square.getStyleClass().contains("hit") ||
                square.getStyleClass().contains("miss")) {
            return;
        }

        if (button == MouseButton.PRIMARY) {
            int col = GridPane.getColumnIndex(square);
            int row = GridPane.getRowIndex(square);
            Game.getInstance().attack(col, row);

            if (enemyGrid != null) enemyGrid.setDisable(true);
            updateStatusMessage("Attack sent to (" + (char)('A' + row) + "," + (col + 1) + "). Waiting for result...");
        }
        else if (button == MouseButton.SECONDARY) {
            if (!square.getStyleClass().contains("hit")) {
                square.getStyleClass().removeAll("miss");
                square.getStyleClass().add("miss");
                updateStatusMessage("Marked as potential miss.");
            }
        }
    }

    private void displayPlayerShips() {
        if (playerGrid == null || playerSquares == null) {
            System.err.println("Player grid not initialized before displaying ships.");
            return;
        }

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (playerSquares[r][c] != null) {
                    playerSquares[r][c].getStyleClass().remove("ship");
                }
            }
        }

        for (Ship ship : this.playerShips) {
            int len = ship.getLength();
            int r = ship.getPosY();
            int c = ship.getPosX();
            boolean horizontal = ship.isHorizontal();

            for (int i = 0; i < len; i++) {
                int currentR = r;
                int currentC = c;
                if (horizontal) {
                    currentC = c + i;
                } else {
                    currentR = r + i;
                }

                if (currentR >= 0 && currentR < GRID_SIZE && currentC >= 0 && currentC < GRID_SIZE) {
                    if (playerSquares[currentR][currentC] != null) {
                        playerSquares[currentR][currentC].getStyleClass().add("ship");
                    } else {
                        System.err.println("Error: playerSquares cell is null at row " + currentR + ", col " + currentC);
                    }
                } else {
                    System.err.println("Error: Ship part out of bounds at row " + currentR + ", col " + currentC);
                }
            }
        }
    }

    private void updateStatusMessage(String message) {
        if (statusMessage != null) {
            statusMessage.setText(message);
        }
    }

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            if (statusMessage != null) {
                statusMessage.setText("Connected. Waiting for game to start...");
            }
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            if (enemyGrid != null) enemyGrid.setDisable(true);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Disconnected");
            alert.setHeaderText("Disconnected from server");
            alert.setContentText("You have been disconnected from the server. Game over.");

            ButtonType homeButtonType = new ButtonType("Go to Home Page");
            alert.getButtonTypes().setAll(homeButtonType);

            if (statusMessage != null && statusMessage.getScene() != null && statusMessage.getScene().getWindow() != null) {
                alert.initOwner(statusMessage.getScene().getWindow());
            } else if (playerGrid != null && playerGrid.getScene() != null && playerGrid.getScene().getWindow() != null) {
                alert.initOwner(playerGrid.getScene().getWindow());
            } else if (enemyGrid != null && enemyGrid.getScene() != null && enemyGrid.getScene().getWindow() != null) {
                alert.initOwner(enemyGrid.getScene().getWindow());
            }


            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == homeButtonType) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml"));
                    Parent homeRoot = loader.load();

                    Scene currentScene = null;
                    if (statusMessage != null && statusMessage.getScene() != null) {
                        currentScene = statusMessage.getScene();
                    } else if (playerGrid != null && playerGrid.getScene() != null) {
                        currentScene = playerGrid.getScene();
                    } else if (enemyGrid != null && enemyGrid.getScene() != null) {
                        currentScene = enemyGrid.getScene();
                    }


                    if (currentScene != null) {
                        Stage primaryStage = (Stage) currentScene.getWindow();
                        primaryStage.setScene(new Scene(homeRoot));
                        primaryStage.setTitle("Battleship - Home");
                        primaryStage.show();
                    } else {
                        System.err.println("Error: Could not retrieve current scene to navigate home after disconnection.");
                        if (statusMessage != null) {
                            statusMessage.setText("Disconnected. Error navigating to home (scene not found).");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (statusMessage != null) {
                        statusMessage.setText("Disconnected. Error loading home screen.");
                    }
                }
            } else {
                // Fallback message if the alert is closed without pressing the button
                if (statusMessage != null) {
                    statusMessage.setText("Disconnected. Game over.");
                }
            }
        });
    }

    @Override
    public void onShipsAccepted() {
        System.out.println("Ships accepted (received in BattleshipController)");
    }


    @Override
    public void onGameStarted() {
        Platform.runLater(() -> {
            if (statusMessage != null) {
                statusMessage.setText("Game started! Waiting for first turn...");
                lastGameEventSummary = "";
            }
            if (enemyGrid != null) enemyGrid.setDisable(true);
        });
    }

    @Override
    public void onYourTurn() {
        Platform.runLater(() -> {
            String baseMessage = "Your turn! Click on the enemy grid to attack.";
            String finalMessage = baseMessage;
            if (statusMessage != null) {
                if (!lastGameEventSummary.isEmpty()) {
                    finalMessage = lastGameEventSummary + "! " + baseMessage;
                }
                statusMessage.setText(finalMessage);
                lastGameEventSummary = "";
            }

            if (turnIndicator != null) {
                turnIndicator.getStyleClass().remove("enemy-turn");
                turnIndicator.getStyleClass().add("your-turn");
                turnIndicator.setText("");
            }
            if (enemyTurnIndicator != null) {
                enemyTurnIndicator.getStyleClass().remove("your-turn");
                enemyTurnIndicator.getStyleClass().add("enemy-turn");
                enemyTurnIndicator.setText("");
            }
            if (enemyGrid != null) enemyGrid.setDisable(false);
        });
    }

    @Override
    public void onOpponentTurn() {
        Platform.runLater(() -> {
            String baseMessage = "Opponent's turn. Please wait...";
            String finalMessage = baseMessage;
            if (statusMessage != null) {
                if (!lastGameEventSummary.isEmpty()) {
                    finalMessage = lastGameEventSummary + "! " + baseMessage;
                }
                statusMessage.setText(finalMessage);
                lastGameEventSummary = "";
            }

            if (turnIndicator != null) {
                turnIndicator.getStyleClass().remove("your-turn");
                turnIndicator.getStyleClass().add("enemy-turn");
                turnIndicator.setText(" ");
            }
            if (enemyTurnIndicator != null) {
                enemyTurnIndicator.getStyleClass().remove("enemy-turn");
                enemyTurnIndicator.getStyleClass().add("your-turn");
                enemyTurnIndicator.setText(" ");
            }
            if (enemyGrid != null) enemyGrid.setDisable(true);
        });
    }

    @Override
    public void onGameEnded(boolean won) {
        Platform.runLater(() -> {
            if (enemyGrid != null) enemyGrid.setDisable(true);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(won ? "You Won!" : "You Lost!");
            alert.setContentText(won ? "Congratulations! Well played." : "Better luck next time!");

            ButtonType homeButtonType = new ButtonType("Go to Home Page");
            alert.getButtonTypes().setAll(homeButtonType);

            if (statusMessage != null && statusMessage.getScene() != null && statusMessage.getScene().getWindow() != null) {
                alert.initOwner(statusMessage.getScene().getWindow());
            }

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == homeButtonType) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml"));
                    Parent homeRoot = loader.load();

                    Scene currentScene = statusMessage.getScene();
                    if (currentScene == null) {
                        if (playerGrid != null && playerGrid.getScene() != null) {
                            currentScene = playerGrid.getScene();
                        } else if (enemyGrid != null && enemyGrid.getScene() != null) {
                            currentScene = enemyGrid.getScene();
                        }
                    }

                    if (currentScene != null) {
                        Stage primaryStage = (Stage) currentScene.getWindow();
                        primaryStage.setScene(new Scene(homeRoot));
                        primaryStage.setTitle("Battleship - Home");
                        primaryStage.show();
                    } else {
                        System.err.println("Error: Could not retrieve current scene to navigate home.");
                        if (statusMessage != null) {
                            statusMessage.setText("Game Over. Error navigating to home (scene not found).");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (statusMessage != null) {
                        statusMessage.setText("Game Over. Error loading home screen.");
                    }
                }
            } else {
                if (statusMessage != null) {
                    statusMessage.setText(won ? "You won! Congratulations!" : "You lost. Better luck next time!");
                }
            }
        });
    }

    @Override
    public void onQueueUpdate(int playersInQueue) {
        // Not used in this controller
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            if (statusMessage != null) {
                statusMessage.setText("Error: " + error);
            }
            if (enemyGrid != null) enemyGrid.setDisable(true);
        });
    }



    @Override
    public void onPlayerAttackResult(int x, int y, String result) {
        Platform.runLater(() -> {
            if (enemySquares[y][x] != null) {
                enemySquares[y][x].getStyleClass().remove("miss");
                enemySquares[y][x].getStyleClass().remove("hit");
                enemySquares[y][x].getStyleClass().add(result);
                lastGameEventSummary = result.toUpperCase();
            }
        });
    }

    @Override
    public void onOpponentAttackResult(int x, int y, String result) {
        Platform.runLater(() -> {
            if (playerSquares[y][x] != null) {
                playerSquares[y][x].getStyleClass().remove("miss");
                playerSquares[y][x].getStyleClass().remove("hit");

                if ("hit".equals(result)) {
                    playerSquares[y][x].getStyleClass().remove("ship");
                }

                playerSquares[y][x].getStyleClass().add(result);
                lastGameEventSummary = "Opponent's " + result.toUpperCase();
            }
        });
    }

    @Override
    public void onShipDestroyed(Ship ship, boolean onPlayerGrid) {
        Platform.runLater(() -> {
            lastGameEventSummary = "Ship Destroyed";
            Rectangle[][] targetGridSquares = onPlayerGrid ? playerSquares : enemySquares;

            int r = ship.getPosY();
            int c = ship.getPosX();
            int len = ship.getLength();
            boolean horizontal = ship.isHorizontal();

            for (int i = 0; i < len; i++) {
                int currentR = r;
                int currentC = c;
                if (horizontal) {
                    currentC = c + i;
                } else {
                    currentR = r + i;
                }

                if (currentR >= 0 && currentR < GRID_SIZE && currentC >= 0 && currentC < GRID_SIZE) {
                    if (targetGridSquares[currentR][currentC] != null) {
                        targetGridSquares[currentR][currentC].getStyleClass().remove("hit");
                        if (onPlayerGrid) {
                            targetGridSquares[currentR][currentC].getStyleClass().remove("ship");
                        }
                        targetGridSquares[currentR][currentC].getStyleClass().add("destroyed");
                    }
                }
            }
        });
    }
}