package pt.goncalo3.batalhanaval;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipPlacementController implements Game.GameStateListener {
    @FXML private GridPane placementGrid;
    @FXML private ToggleButton orientationToggle;
    @FXML private Label currentShipLabel;
    @FXML private Button startGameButton;
    @FXML private Button resetButton;
    @FXML private Label opponentDisconnectedLabel; // Added this line
    @FXML private Button backToHomeButton; // Added this line
    @FXML private HBox gameControlsContainer; // Added this line
    
    // Ship selection boxes
    @FXML private VBox carrierBox;
    @FXML private VBox battleshipBox;
    @FXML private VBox cruiserBox;
    @FXML private VBox submarineBox;
    @FXML private VBox destroyerBox;
    
    // Ship definitions
    private final Map<String, Integer> shipTypes = new HashMap<>();
    private final Map<String, VBox> shipBoxes = new HashMap<>();
    private final Map<String, Boolean> placedShips = new HashMap<>();
    
    // Current selection state
    private String currentShipType = "carrier";
    private boolean isHorizontal = true;
    private boolean allShipsPlaced = false;
    private boolean waitingForOpponent = false;
    
    // Grid cells for placement
    private Rectangle[][] gridCells = new Rectangle[10][10];
    
    // List to track placed ships
    private List<Ship> ships = new ArrayList<>();
    private int nextShipId = 1;
    
    @FXML
    public void initialize() {
        // Initialize ship types and their lengths
        shipTypes.put("carrier", 5);
        shipTypes.put("battleship", 4);
        shipTypes.put("cruiser", 3);
        shipTypes.put("submarine", 3);
        shipTypes.put("destroyer", 2);
        
        // Initialize ship boxes mapping
        shipBoxes.put("carrier", carrierBox);
        shipBoxes.put("battleship", battleshipBox);
        shipBoxes.put("cruiser", cruiserBox);
        shipBoxes.put("submarine", submarineBox);
        shipBoxes.put("destroyer", destroyerBox);
        
        // Initialize all ships as not placed
        for (String shipType : shipTypes.keySet()) {
            placedShips.put(shipType, false);
        }
        
        buildPlacementGrid();
        updateCurrentShipLabel();
        updateUIState();
        
        // Set up game state listener
        Game.getInstance().setGameStateListener(this);

        // Ensure the new UI elements are initially hidden
        opponentDisconnectedLabel.setVisible(false);
        opponentDisconnectedLabel.setManaged(false);
        backToHomeButton.setVisible(false);
        backToHomeButton.setManaged(false);
    }
    
    /**
     * Build the grid for ship placement
     */
    private void buildPlacementGrid() {
        placementGrid.getChildren().clear();
        
        // Create 10x10 grid of rectangles
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Rectangle cell = new Rectangle(30, 30);
                cell.setFill(Color.LIGHTBLUE);
                cell.setStroke(Color.DARKBLUE);
                cell.setStrokeWidth(1);
                
                final int cellRow = row;
                final int cellCol = col;
                
                // Add event handlers for ship placement
                cell.setOnMouseEntered(e -> {
                    if (!allShipsPlaced && !placedShips.get(currentShipType)) {
                        showPlacementPreview(cellRow, cellCol);
                    }
                });
                
                cell.setOnMouseExited(_ -> clearPlacementPreview());
                
                cell.setOnMouseClicked(_ -> {
                    if (!allShipsPlaced && !placedShips.get(currentShipType)) {
                        placeShip(cellRow, cellCol);
                    }
                });
                
                gridCells[row][col] = cell;
                placementGrid.add(cell, col, row);
            }
        }
    }
    
    /**
     * Toggle ship orientation between horizontal and vertical
     */
    @FXML
    public void toggleOrientation() {
        if (!allShipsPlaced) {
            isHorizontal = !isHorizontal;
            orientationToggle.setText(isHorizontal ? "Horizontal" : "Vertical");
        }
    }
    
    /**
     * Select a ship type to place
     */
    @FXML
    public void selectShip(MouseEvent event) {
        if (allShipsPlaced) return;
        
        VBox clickedBox = (VBox) event.getSource();
        
        // Find which ship was clicked
        for (Map.Entry<String, VBox> entry : shipBoxes.entrySet()) {
            if (entry.getValue() == clickedBox && !placedShips.get(entry.getKey())) {
                currentShipType = entry.getKey();
                updateCurrentShipLabel();
                updateShipSelection();
                break;
            }
        }
    }
    
    /**
     * Update the label showing the current ship being placed
     */
    private void updateCurrentShipLabel() {
        if (allShipsPlaced) {
            currentShipLabel.setText("All ships placed!");
        } else if (placedShips.get(currentShipType)) {
            // Find next unplaced ship
            for (String shipType : shipTypes.keySet()) {
                if (!placedShips.get(shipType)) {
                    currentShipType = shipType;
                    break;
                }
            }
            currentShipLabel.setText("Placing: " + currentShipType.toUpperCase() + 
                                   " (Length: " + shipTypes.get(currentShipType) + ")");
        } else {
            currentShipLabel.setText("Placing: " + currentShipType.toUpperCase() + 
                                   " (Length: " + shipTypes.get(currentShipType) + ")");
        }
    }
    
    /**
     * Update ship selection visual indicators
     */
    private void updateShipSelection() {
        for (Map.Entry<String, VBox> entry : shipBoxes.entrySet()) {
            VBox box = entry.getValue();
            String shipType = entry.getKey();
            
            // Remove all style classes first
            box.getStyleClass().removeAll("ship-selected", "ship-placed", "ship-available");
            
            if (placedShips.get(shipType)) {
                box.getStyleClass().add("ship-placed");
            } else if (shipType.equals(currentShipType)) {
                box.getStyleClass().add("ship-selected");
            } else {
                box.getStyleClass().add("ship-available");
            }
        }
    }
    
    /**
     * Show a preview of where the ship will be placed
     */
    private void showPlacementPreview(int row, int col) {
        clearPlacementPreview();
        
        int shipLength = shipTypes.get(currentShipType);
        
        // Check if ship fits and mark preview cells
        if (canPlaceShip(row, col, shipLength, isHorizontal)) {
            for (int i = 0; i < shipLength; i++) {
                int previewRow = isHorizontal ? row : row + i;
                int previewCol = isHorizontal ? col + i : col;
                
                gridCells[previewRow][previewCol].setFill(Color.YELLOW);
            }
        } else {
            // Show invalid placement in red
            for (int i = 0; i < shipLength; i++) {
                int previewRow = isHorizontal ? row : row + i;
                int previewCol = isHorizontal ? col + i : col;
                
                if (previewRow >= 0 && previewRow < 10 && previewCol >= 0 && previewCol < 10) {
                    gridCells[previewRow][previewCol].setFill(Color.LIGHTCORAL);
                }
            }
        }
    }
    
    /**
     * Clear the placement preview
     */
    private void clearPlacementPreview() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Rectangle cell = gridCells[row][col];
                if (cell.getFill() == Color.YELLOW || cell.getFill() == Color.LIGHTCORAL) {
                    if (isShipAt(row, col)) {
                        cell.setFill(Color.DARKGREEN);
                    } else {
                        cell.setFill(Color.LIGHTBLUE);
                    }
                }
            }
        }
    }
    
    /**
     * Check if a ship can be placed at the given position
     */
    private boolean canPlaceShip(int row, int col, int length, boolean horizontal) {
        // Check bounds
        if (horizontal) {
            if (col + length > 10) return false;
        } else {
            if (row + length > 10) return false;
        }
        
        // Check for overlaps with existing ships
        for (int i = 0; i < length; i++) {
            int checkRow = horizontal ? row : row + i;
            int checkCol = horizontal ? col + i : col;
            
            if (isShipAt(checkRow, checkCol)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if there's a ship at the given coordinates
     */
    private boolean isShipAt(int row, int col) {
        for (Ship ship : ships) {
            if (ship.isAt(col, row)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Place a ship at the given position
     */
    private void placeShip(int row, int col) {
        int shipLength = shipTypes.get(currentShipType);
        
        if (!canPlaceShip(row, col, shipLength, isHorizontal)) {
            return;
        }
        
        // Create and add the ship
        Ship newShip = new Ship(nextShipId++, col, row, shipLength, isHorizontal);
        ships.add(newShip);
        
        // Mark this ship type as placed
        placedShips.put(currentShipType, true);
        
        // Update grid visual
        for (int i = 0; i < shipLength; i++) {
            int shipRow = isHorizontal ? row : row + i;
            int shipCol = isHorizontal ? col + i : col;
            gridCells[shipRow][shipCol].setFill(Color.DARKGREEN);
        }
        
        // Check if all ships are placed
        checkAllShipsPlaced();
        
        // Update UI
        updateCurrentShipLabel();
        updateShipSelection();
        updateUIState();
    }
    
    /**
     * Check if all ships have been placed
     */
    private void checkAllShipsPlaced() {
        allShipsPlaced = true;
        for (boolean placed : placedShips.values()) {
            if (!placed) {
                allShipsPlaced = false;
                break;
            }
        }
    }
    
    /**
     * Update UI state based on current game state
     */
    private void updateUIState() {
        startGameButton.setDisable(!allShipsPlaced || waitingForOpponent);
        orientationToggle.setDisable(allShipsPlaced || waitingForOpponent);
        resetButton.setDisable(waitingForOpponent);
        
        // Update ship boxes to show they can't be selected if all ships are placed or waiting
        for (VBox box : shipBoxes.values()) {
            box.setDisable(allShipsPlaced || waitingForOpponent);
        }
    }
    
    /**
     * Reset all ships and start over (FXML event handler)
     */
    @FXML
    public void onResetClick() {
        resetShips();
    }
    
    /**
     * Start the game with placed ships (FXML event handler)
     */
    @FXML
    public void onStartGameClick() {
        startGame();
    }

    /**
     * Reset all ships and start over
     */
    @FXML
    public void resetShips() {
        // Don't allow reset when waiting for opponent
        if (waitingForOpponent) {
            return;
        }
        
        // Clear all ships
        ships.clear();
        nextShipId = 1;
        
        // Reset placement status
        for (String shipType : shipTypes.keySet()) {
            placedShips.put(shipType, false);
        }
        
        // Reset state
        allShipsPlaced = false;
        currentShipType = "carrier";
        isHorizontal = true;
        
        // Reset grid visual
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                gridCells[row][col].setFill(Color.LIGHTBLUE);
            }
        }
        
        // Update UI
        orientationToggle.setText("Horizontal");
        updateCurrentShipLabel();
        updateShipSelection();
        updateUIState();
    }
    
    /**
     * Start the game with placed ships
     */
    @FXML
    public void startGame() {
        if (!allShipsPlaced) {
            return;
        }
        
        // Send ships data to server
        Game.getInstance().sendShipsData(ships);
        
        
        // Set waiting state
        waitingForOpponent = true;
        startGameButton.setText("Waiting for opponent...");
        updateUIState();
    }

    // Game state listener methods
    @Override
    public void onConnected() {
        // Connection established
    }

    @Override
    public void onShipsAccepted() {
        startGameButton.setText("Waiting...");
    }

    @Override
    public void onYourTurn() {
        // Transition to game view
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/playing.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpponentTurn() {
       // Transition to game view
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/playing.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGameEnded(boolean won) {
        // Handle game end
    }

    @Override
    public void onError(String error) {
        System.err.println("Game error: " + error);
        waitingForOpponent = false;
        startGameButton.setText("Start Game");
        updateUIState();
    }

    @Override
    public void onDisconnected() {
        // Show error message to user
        opponentDisconnectedLabel.setText("Opponent has disconnected. You can return to the home screen.");
        opponentDisconnectedLabel.setVisible(true);
        opponentDisconnectedLabel.setManaged(true);

        // Hide game controls
        gameControlsContainer.setVisible(false);
        gameControlsContainer.setManaged(false);

        // Show back to home button
        backToHomeButton.setVisible(true);
        backToHomeButton.setManaged(true);

        // Disable other UI elements if necessary
        orientationToggle.setDisable(true);
    }

    @FXML
    private void goBackToHome() {
        try {
            // Assuming "home-view.fxml" is your homepage FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/home-view.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) startGameButton.getScene().getWindow(); // Or any other node in the current scene
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onQueueUpdate(int playersInQueue) {
        throw new UnsupportedOperationException("Unimplemented method 'onQueueUpdate'");
    }

    @Override
    public void onGameStarted() {
        throw new UnsupportedOperationException("Unimplemented method 'onGameStarted'");
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