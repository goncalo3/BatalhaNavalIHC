package pt.goncalo3.batalhanaval;

public class ServerConfig {
    // WebSocket URL
    public static final String WEBSOCKET_URL = "wss://battleships.goncalo3.pt";

    // Base URL for HTTP API calls (e.g., for login, leaderboard)
    public static final String API_BASE_URL = "https://battleships.goncalo3.pt"; 

    // Private constructor to prevent instantiation
    private ServerConfig() {
    }
}
