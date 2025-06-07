package pt.goncalo3.batalhanaval;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Leaderboard class to handle leaderboard-related operations
 */
public class Leaderboard {
    private static final String LEADERBOARD_ENDPOINT = ServerConfig.API_BASE_URL + "/leaderboard"; // Use ServerConfig and ensure /api path if needed

    private final HttpClient httpClient;

    public Leaderboard() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Get leaderboard data from the server
     * @param limit Maximum number of entries to retrieve (default 10)
     * @return List of leaderboard entries
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) 
            throws IOException, InterruptedException {
        String url = LEADERBOARD_ENDPOINT + "?limit=" + limit;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .GET()
                .build();

        System.out.println("Making leaderboard request to: " + url);

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Leaderboard response status: " + response.statusCode());
        System.out.println("Leaderboard response body: " + response.body());

        if (response.statusCode() == 200) {
            JSONObject responseBody = new JSONObject(response.body());
            JSONArray leaderboardArray = responseBody.getJSONArray("leaderboard");
            
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            
            for (int i = 0; i < leaderboardArray.length(); i++) {
                JSONObject entry = leaderboardArray.getJSONObject(i);
                
                String username = entry.getString("username");
                int wins = entry.optInt("wins", 0);
                int losses = entry.optInt("losses", 0);
                int totalGames = entry.optInt("total_games", 0);
                double winPercentage = entry.optDouble("win_percentage", 0.0);
                
                LeaderboardEntry leaderboardEntry = new LeaderboardEntry(
                    username, wins, losses, totalGames, winPercentage
                );
                leaderboard.add(leaderboardEntry);
            }
            
            return leaderboard;
        } else {
            throw new IOException("Failed to fetch leaderboard: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Get leaderboard data with default limit of 10
     * @return List of leaderboard entries
     * @throws IOException If network issues occur
     * @throws InterruptedException If request is interrupted
     */
    public List<LeaderboardEntry> getLeaderboard() throws IOException, InterruptedException {
        return getLeaderboard(10);
    }
}
