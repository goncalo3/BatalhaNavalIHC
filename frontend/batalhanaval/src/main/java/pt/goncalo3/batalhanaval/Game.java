package pt.goncalo3.batalhanaval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Game {
    private static Game instance;
    private WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final User user;

    // array of ships
    private List<Ship> ships;

    private boolean yourTurn;

    // Getter for yourTurn
    public boolean isYourTurn() {
        return yourTurn;
    }
    // Getter for ships
    public List<Ship> getShips() {
        return ships;
    }
    // Setter for ships
    public void setShips(List<Ship> ships) {
        this.ships = ships;
    }
    
    // Listeners for UI updates
    private GameStateListener gameStateListener;
    
    private Game() {
        if (instance != null) {
            throw new IllegalStateException("Game instance already exists!");
        }
        user = User.getInstance();
        if (user == null || !user.isAuthenticated()) {
            throw new IllegalStateException("User must be authenticated before creating Game instance!");
        }

        // When a new Game instance is created, initialize the WebSocket
        this.connect();

    }
    
    public static Game createInstance() {
        if (instance == null) {
            instance = new Game();
        } else {
            throw new IllegalStateException("Game instance already exists!");
        }
        return instance;
    }

    public static Game getInstance() {
        if (instance == null) {
            throw new IllegalStateException("No game instance available.");
        }
        return instance;
    }
    
    /**
     * Interface for listening to game state changes
     */
    public interface GameStateListener {
        void onConnected();
        void onDisconnected();
        void onShipsAccepted();
        void onGameStarted();
        void onYourTurn();
        void onOpponentTurn();
        void onGameEnded(boolean won);
        void onQueueUpdate(int playersInQueue);
        void onError(String error);
        void onPlayerAttackResult(int x, int y, String result);
        void onOpponentAttackResult(int x, int y, String result);
        void onShipDestroyed(Ship ship, boolean onPlayerGrid); // New
    }
    
    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }
    
    /**
     * Connect to the WebSocket server with authentication
     */
    public CompletableFuture<Void> connect() {

        String token = User.getInstance().getToken();
        
        String wsUrl = ServerConfig.WEBSOCKET_URL + "?token=" + token; // Use ServerConfig
        
        return HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    Platform.runLater(() -> {
                        if (gameStateListener != null) {
                            gameStateListener.onConnected();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        if (gameStateListener != null) {
                            gameStateListener.onError("Failed to connect: " + throwable.getMessage());
                        }
                    });
                    return null;
                });
    }
    

    /**
     * Disconnect from the WebSocket server
     */
    public void disconnect() {
        System.out.println("=== DISCONNECTING WEBSOCKET ===");
        
        if (webSocket != null) {
            System.out.println("Sending close message to server...");
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            webSocket = null;
        }
        
        instance = null; // Reset the instance
    
        System.out.println("WebSocket disconnected and state reset");
        System.out.println("===============================");
    }
    

    /**
     * Join the matchmaking queue
     */
    public void joinQueue() {
        System.out.println("=== JOINING MATCHMAKING QUEUE ===");
        try {
            var message = objectMapper.createObjectNode();
            message.put("type", "join_queue");
            String messageStr = objectMapper.writeValueAsString(message);
            sendMessage(messageStr);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to join queue: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Leave the matchmaking queue
     */
    public void leaveQueue() {
        try {
            var message = objectMapper.createObjectNode();
            message.put("type", "leave_queue");
            String messageStr = objectMapper.writeValueAsString(message);
            sendMessage(messageStr);
            
            // Disconnect WebSocket after leaving queue, the diconnect method will handle cleanup
            disconnect();
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to leave queue: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Send ship placement data to the server
     */
    public void sendShipsData(List<Ship> ships) {
        try {

            this.ships = ships;
            // Convert ships to JSON format expected by server
            var shipsData = objectMapper.createObjectNode();
            shipsData.put("type", "ships_data");
            
            var shipsArray = objectMapper.createArrayNode();
            for (Ship ship : ships) {
                var shipNode = objectMapper.createObjectNode();
                shipNode.put("posX", ship.getPosX());
                shipNode.put("posY", ship.getPosY());
                shipNode.put("length", ship.getLength());
                shipNode.put("isHorizontal", ship.isHorizontal());
                shipsArray.add(shipNode);
            }
            shipsData.set("ships", shipsArray);
            
            String message = objectMapper.writeValueAsString(shipsData);
            sendMessage(message);
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to send ships data: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Send an attack to the specified coordinates
     */
    public void attack(int x, int y) {
        if (!yourTurn) {
            System.out.println("=== ATTACK FAILED ===");
            System.out.println("Not your turn to attack.");
            System.out.println("======================");
            return; // Not your turn, do not send the attack
        }
        try {
            var message = objectMapper.createObjectNode();
            message.put("type", "attack");
            message.put("x", x);
            message.put("y", y);
            String messageStr = objectMapper.writeValueAsString(message);
            sendMessage(messageStr);
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to send attack: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Check if the WebSocket connection is still valid
     */
    public boolean isConnectionValid() {
        return webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }
    
    
    /**
     * Helper method to send WebSocket messages with logging and connection validation
     */
    private void sendMessage(String message) {
        if (!isConnectionValid()) {
            System.out.println("=== WEBSOCKET SEND FAILED ===");
            
            // Trigger disconnection event
            Platform.runLater(() -> {
                if (gameStateListener != null) {
                    gameStateListener.onDisconnected();
                }
            });
            
            // null game instance
            instance = null;
            return;
        }
        
        System.out.println("=== WEBSOCKET MESSAGE SENDING ===");
        System.out.println("Outgoing message: " + message);
        System.out.println("=================================");
        webSocket.sendText(message, true);
    }
    
    /**
     * WebSocket listener implementation
     */
    private class WebSocketListener implements WebSocket.Listener {
        
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("=== WEBSOCKET CONNECTED ===");
            System.out.println("Connection opened to: " + webSocket.getSubprotocol());
            System.out.println("============================");
            
            // Request to start receiving messages
            webSocket.request(1);
        }
        
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            System.out.println("=== WEBSOCKET MESSAGE RECEIVED ===");
            System.out.println("Raw message: " + data.toString());
            System.out.println("Message is last fragment: " + last);
            System.out.println("================================");
            Platform.runLater(() -> handleMessage(data.toString()));
            
            // Request the next message - this is crucial!
            webSocket.request(1);
            return null;
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("=== WEBSOCKET DISCONNECTED ===");
            System.out.println("Status code: " + statusCode);
            System.out.println("Reason: " + reason);
            System.out.println("===============================");
            Platform.runLater(() -> {
            instance = null; // Reset the instance
            
            });
            return null;
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("=== WEBSOCKET ERROR ===");
            System.out.println("Error: " + error.getMessage());
            error.printStackTrace();
            System.out.println("========================");
            Platform.runLater(() -> {
                instance = null; // Reset the instance
                if (gameStateListener != null) {
                    gameStateListener.onError("WebSocket error: " + error.getMessage());
                }
            });
        }
    }
    
    /**
     * Handle incoming WebSocket messages
     */
    private void handleMessage(String message) {
        System.out.println("=== PROCESSING RECEIVED MESSAGE ===");
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String type = jsonNode.get("type").asText();
            System.out.println("Message type: " + type);
            System.out.println("Full message content: " + message);
            
            switch (type) {
                case "connection_success":
                    // Extract user info if available
                    if (jsonNode.has("username")) {
                        String username = jsonNode.get("username").asText();
                        if (user == null || !user.getUsername().equals(username)) {
                            // This should never happen, something went really wrong, throw an error
                            throw new IllegalStateException("User instance mismatch: expected " + user.getUsername() + ", got " + username);
                        }
                    }
                    break;
                    
                case "connection_error":
                    String error = jsonNode.get("error").asText();
                    if (gameStateListener != null) {
                        gameStateListener.onError("Connection error: " + error);
                    }
                    break;
                    
                case "ships_accepted":
                    if (gameStateListener != null) {
                        gameStateListener.onShipsAccepted();
                    }
                    break;
                    
                case "ships_validation_error":
                    String validationError = jsonNode.get("error").asText();
                    if (gameStateListener != null) {
                        gameStateListener.onError("Ships validation error: " + validationError);
                    }
                    break;
                    
                case "start_game":
                    if (gameStateListener != null) {
                        gameStateListener.onGameStarted();
                    }
                    break;
                    
                case "players_in_queue":
                    int queueCount = jsonNode.get("count").asInt();
                    if (gameStateListener != null) {
                        gameStateListener.onQueueUpdate(queueCount);
                    }
                    break;
                    
                case "your_turn":
                    yourTurn = true; // Update the turn state
                    if (gameStateListener != null) {
                        gameStateListener.onYourTurn();
                    }
                    break;
                case "opponent_turn":
                    yourTurn = false; // Update the turn state
                    if (gameStateListener != null) {
                        gameStateListener.onOpponentTurn();
                    }
                    break;
                    
                case "attack_result": // Player's attack outcome
                    int attackX = jsonNode.get("x").asInt();
                    int attackY = jsonNode.get("y").asInt();
                    String attackResult = jsonNode.get("result").asText();
                    if (gameStateListener != null) {
                        gameStateListener.onPlayerAttackResult(attackX, attackY, attackResult);
                    }
                    break;

                case "opponent_attack": // Opponent's attack outcome on player's grid
                    int opponentAttackX = jsonNode.get("x").asInt();
                    int opponentAttackY = jsonNode.get("y").asInt();
                    String opponentAttackResult = jsonNode.get("result").asText();
                    if (gameStateListener != null) {
                        gameStateListener.onOpponentAttackResult(opponentAttackX, opponentAttackY, opponentAttackResult);
                    }
                    break;
                
                
                case "ship_destroyed":
                    JsonNode shipNode = jsonNode.get("ship");
                    int id = shipNode.get("id").asInt();
                    int posX = shipNode.get("posX").asInt();
                    int posY = shipNode.get("posY").asInt();
                    int length = shipNode.get("length").asInt();
                    boolean isHorizontal = shipNode.get("isHorizontal").asBoolean();

    
                    Ship destroyedShip = new Ship(id, posX, posY, length, isHorizontal);

                    if (gameStateListener != null) {
                        gameStateListener.onShipDestroyed(destroyedShip, false);
                    }
                    break;
                    
                case "you_win":
                    if (gameStateListener != null) {
                        gameStateListener.onGameEnded(true);
                    }
                    disconnect();
                    break;
                    
                case "you_lose":
                    if (gameStateListener != null) {
                        gameStateListener.onGameEnded(false);
                    }
                    disconnect();
                    break;
                case "opponent_disconnected":
                    disconnect();
                    if (gameStateListener != null) {
                        gameStateListener.onDisconnected();

                    }
                    break;
                    
                default:
                    System.out.println("Unknown message type: " + type);
                    break;
            }
            System.out.println("=== MESSAGE PROCESSED SUCCESSFULLY ===");
            
        } catch (Exception e) {
            System.out.println("=== ERROR PROCESSING MESSAGE ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("===============================");
            if (gameStateListener != null) {
                gameStateListener.onError("Failed to parse message: " + e.getMessage());
            }
        }
    }
}