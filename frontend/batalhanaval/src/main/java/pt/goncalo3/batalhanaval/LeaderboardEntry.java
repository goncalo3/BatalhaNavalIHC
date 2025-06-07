package pt.goncalo3.batalhanaval;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a leaderboard entry with player statistics
 */
public class LeaderboardEntry {
    private final IntegerProperty rank = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final IntegerProperty wins = new SimpleIntegerProperty();
    private final IntegerProperty losses = new SimpleIntegerProperty();
    private final IntegerProperty totalGames = new SimpleIntegerProperty();
    private final StringProperty winPercentage = new SimpleStringProperty();

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(String username, int wins, int losses, int totalGames, double winPercentage) {
        setUsername(username);
        setWins(wins);
        setLosses(losses);
        setTotalGames(totalGames);
        setWinPercentage(String.format("%.1f%%", winPercentage));
    }

    // Rank property
    public IntegerProperty rankProperty() {
        return rank;
    }

    public int getRank() {
        return rank.get();
    }

    public void setRank(int rank) {
        this.rank.set(rank);
    }

    // Username property
    public StringProperty usernameProperty() {
        return username;
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    // Wins property
    public IntegerProperty winsProperty() {
        return wins;
    }

    public int getWins() {
        return wins.get();
    }

    public void setWins(int wins) {
        this.wins.set(wins);
    }

    // Losses property
    public IntegerProperty lossesProperty() {
        return losses;
    }

    public int getLosses() {
        return losses.get();
    }

    public void setLosses(int losses) {
        this.losses.set(losses);
    }

    // Total games property
    public IntegerProperty totalGamesProperty() {
        return totalGames;
    }

    public int getTotalGames() {
        return totalGames.get();
    }

    public void setTotalGames(int totalGames) {
        this.totalGames.set(totalGames);
    }

    // Win percentage property
    public StringProperty winPercentageProperty() {
        return winPercentage;
    }

    public String getWinPercentage() {
        return winPercentage.get();
    }

    public void setWinPercentage(String winPercentage) {
        this.winPercentage.set(winPercentage);
    }
}
