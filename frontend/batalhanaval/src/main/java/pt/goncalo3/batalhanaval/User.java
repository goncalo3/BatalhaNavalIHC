package pt.goncalo3.batalhanaval;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.prefs.Preferences;
import org.json.JSONObject;

/**
 * User class to handle authentication and user-related operations
 */
public class User {
    private static final String AUTH_ENDPOINT = ServerConfig.API_BASE_URL + "/auth"; // Use ServerConfig and ensure /api/auth path
    private static final String PREFS_NODE = "pt.goncalo3.batalhanaval";
    private static final String TOKEN_KEY = "jwt_token";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";

    private String username;
    private String email;
    private String token;
    private boolean isAuthenticated;
    
    // Store the last error message from server responses
    private String lastErrorMessage = null;

    private static User instance;
    private final HttpClient httpClient;
    private final Preferences prefs;

    // Private constructor for singleton pattern
    private User() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        prefs = Preferences.userRoot().node(PREFS_NODE);
        loadUserFromPrefs();
    }

    // Get singleton instance
    public static User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    // Load user data from preferences (if saved from previous session)
    private void loadUserFromPrefs() {
        token = prefs.get(TOKEN_KEY, null);
        username = prefs.get(USERNAME_KEY, null);
        email = prefs.get(EMAIL_KEY, null);
        isAuthenticated = token != null;
    }

    // Save user data to preferences
    private void saveUserToPrefs() {
        if (token != null) {
            prefs.put(TOKEN_KEY, token);
        }
        if (username != null) {
            prefs.put(USERNAME_KEY, username);
        }
        if (email != null) {
            prefs.put(EMAIL_KEY, email);
        }
    }

    // Clear user data from preferences
    private void clearUserPrefs() {
        prefs.remove(TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        prefs.remove(EMAIL_KEY);
    }

    /**
     * Register a new user
     * @param username The desired username
     * @param email The user's email
     * @param password The user's password
     * @return True if registration was successful
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public boolean register(String username, String email, String password)
            throws IOException, InterruptedException {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("email", email);
            requestBody.put("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_ENDPOINT + "/register"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "BattleshipGame/1.0")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            System.out.println("Making registration request to: " + AUTH_ENDPOINT + "/register");
            System.out.println("Request body: " + requestBody.toString());

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response headers: " + response.headers().map());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 201) {
                // Registration successful - backend returns 201 for created
                JSONObject responseBody = new JSONObject(response.body());
                this.token = responseBody.getString("token");
                JSONObject user = responseBody.getJSONObject("user");
                this.username = user.getString("username");
                this.email = user.getString("email");
                this.isAuthenticated = true;
                saveUserToPrefs();
                this.lastErrorMessage = null; // Clear any previous error
                System.out.println("Registration successful! User: " + this.username + ", Email: " + this.email);
                return true;
            } else {
                // Registration failed - extract error message from response
                try {
                    JSONObject errorResponse = new JSONObject(response.body());
                    this.lastErrorMessage = errorResponse.optString("error", "Registration failed");
                } catch (Exception e) {
                    this.lastErrorMessage = "Registration failed with status: " + response.statusCode();
                }
                System.err.println("Registration failed with status: " + response.statusCode());
                System.err.println("Response body: " + response.body());
                return false;
            }
        } catch (Exception e) {
            this.lastErrorMessage = "Connection error: " + e.getMessage();
            System.err.println("Exception during registration: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Log in user with username and password
     * @param username The username
     * @param password The password
     * @return True if login was successful
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public boolean login(String username, String password)
            throws IOException, InterruptedException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", username);
        requestBody.put("password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_ENDPOINT + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Login successful
            JSONObject responseBody = new JSONObject(response.body());
            this.token = responseBody.getString("token");
            
            // Extract user data from login response if available
            if (responseBody.has("user")) {
                JSONObject user = responseBody.getJSONObject("user");
                this.username = user.getString("username");
                this.email = user.getString("email");
            } else {
                // Fallback to the provided username if user object not in response
                this.username = username;
            }
            
            this.isAuthenticated = true;
            saveUserToPrefs();
            this.lastErrorMessage = null; // Clear any previous error
            return true;
        } else {
            // Login failed - extract error message from response
            try {
                JSONObject errorResponse = new JSONObject(response.body());
                this.lastErrorMessage = errorResponse.optString("error", "Login failed");
            } catch (Exception e) {
                this.lastErrorMessage = "Login failed with status: " + response.statusCode();
            }
            return false;
        }
    }

    /**
     * Fetch user profile information
     * @return True if profile was fetched successfully
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public boolean fetchProfile() throws IOException, InterruptedException {
        if (token == null) {
            return false;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_ENDPOINT + "/profile"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Profile fetch successful
            JSONObject responseBody = new JSONObject(response.body());
            JSONObject user = responseBody.getJSONObject("user");
            this.username = user.getString("username");
            this.email = user.getString("email");
            saveUserToPrefs();
            return true;
        } else {
            // Profile fetch failed
            return false;
        }
    }

    /**
     * Update user profile information
     * @param newUsername New username (or null to keep current)
     * @param newEmail New email (or null to keep current)
     * @return True if update was successful
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public boolean updateProfile(String newUsername, String newEmail)
            throws IOException, InterruptedException {
        if (token == null) {
            return false;
        }

        JSONObject requestBody = new JSONObject();
        if (newUsername != null) {
            requestBody.put("username", newUsername);
        }
        if (newEmail != null) {
            requestBody.put("email", newEmail);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_ENDPOINT + "/profile"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Update successful
            JSONObject responseBody = new JSONObject(response.body());
            JSONObject user = responseBody.getJSONObject("user");
            this.username = user.getString("username");
            this.email = user.getString("email");
            saveUserToPrefs();
            return true;
        } else {
            // Update failed
            return false;
        }
    }

    /**
     * Log out the current user
     */
    public void logout() {
        this.token = null;
        this.username = null;
        this.email = null;
        this.isAuthenticated = false;
        clearUserPrefs();
    }

    /**
     * Get the JWT token for authenticated requests
     * @return The JWT token or null if not authenticated
     */
    public String getToken() {
        return token;
    }

    /**
     * Get the current username
     * @return The username or null if not authenticated
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the current user email
     * @return The email or null if not authenticated
     */
    public String getEmail() {
        return email;
    }

    /**
     * Check if user is authenticated
     * @return True if user is authenticated
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    /**
     * Get the last error message from the server
     * @return The last error message or null if no error
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}
