package pt.goncalo3.batalhanaval;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the leaderboard page
 */
public class LeaderboardController implements Initializable {

    @FXML private TableView<LeaderboardEntry> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntry, Integer> rankColumn;
    @FXML private TableColumn<LeaderboardEntry, String> usernameColumn;
    @FXML private TableColumn<LeaderboardEntry, Integer> winsColumn;
    @FXML private TableColumn<LeaderboardEntry, Integer> lossesColumn;
    @FXML private TableColumn<LeaderboardEntry, Integer> totalGamesColumn;
    @FXML private TableColumn<LeaderboardEntry, String> winPercentageColumn;
    @FXML private Button backButton;
    @FXML private Label loadingLabel;
    @FXML private Label errorLabel;

    private final Leaderboard leaderboard = new Leaderboard();
    private final ObservableList<LeaderboardEntry> leaderboardData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        loadLeaderboard();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
        lossesColumn.setCellValueFactory(new PropertyValueFactory<>("losses"));
        totalGamesColumn.setCellValueFactory(new PropertyValueFactory<>("totalGames"));
        winPercentageColumn.setCellValueFactory(new PropertyValueFactory<>("winPercentage"));

        leaderboardTable.setItems(leaderboardData);
    }

    /**
     * Load leaderboard data from the server
     */
    private void loadLeaderboard() {
        loadingLabel.setVisible(true);
        errorLabel.setVisible(false);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return leaderboard.getLeaderboard(20); // Get top 20 players
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(leaderboardData -> {
            Platform.runLater(() -> {
                loadingLabel.setVisible(false);
                if (leaderboardData != null && !leaderboardData.isEmpty()) {
                    this.leaderboardData.clear();
                    for (int i = 0; i < leaderboardData.size(); i++) {
                        LeaderboardEntry entry = leaderboardData.get(i);
                        entry.setRank(i + 1);
                        this.leaderboardData.add(entry);
                    }
                } else {
                    errorLabel.setText("No leaderboard data available");
                    errorLabel.setVisible(true);
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                loadingLabel.setVisible(false);
                errorLabel.setText("Failed to load leaderboard: " + throwable.getMessage());
                errorLabel.setVisible(true);
            });
            return null;
        });
    }

    /**
     * Go back to home page
     */
    @FXML
    public void onBackButtonClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/home-view.fxml"));
        Parent homeView = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(homeView);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Refresh leaderboard data
     */
    @FXML
    public void onRefreshButtonClick(ActionEvent event) {
        loadLeaderboard();
    }
}
