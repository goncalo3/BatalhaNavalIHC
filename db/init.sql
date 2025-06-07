-- Database initialization script for Battleship game
-- Run this script to create the database schema

CREATE DATABASE IF NOT EXISTS battleship_game;
USE battleship_game;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Game sessions table
CREATE TABLE IF NOT EXISTS game_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    winner_id INT NULL,
    loser_id INT NULL,
    game_duration_seconds INT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    game_status ENUM('active', 'completed', 'abandoned') DEFAULT 'active',
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (loser_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_player1 (player1_id),
    INDEX idx_player2 (player2_id),
    INDEX idx_winner (winner_id),
    INDEX idx_status (game_status),
    INDEX idx_started_at (started_at)
);

-- User statistics view for easy querying
CREATE OR REPLACE VIEW user_stats AS
SELECT 
    u.id,
    u.username,
    COUNT(CASE WHEN gs.winner_id = u.id THEN 1 END) as wins,
    COUNT(CASE WHEN gs.loser_id = u.id THEN 1 END) as losses,
    COUNT(CASE WHEN (gs.player1_id = u.id OR gs.player2_id = u.id) AND gs.game_status = 'completed' THEN 1 END) as total_games,
    ROUND(
        CASE 
            WHEN COUNT(CASE WHEN (gs.player1_id = u.id OR gs.player2_id = u.id) AND gs.game_status = 'completed' THEN 1 END) = 0 THEN 0
            ELSE (COUNT(CASE WHEN gs.winner_id = u.id THEN 1 END) * 100.0) / 
                 COUNT(CASE WHEN (gs.player1_id = u.id OR gs.player2_id = u.id) AND gs.game_status = 'completed' THEN 1 END)
        END, 2
    ) as win_percentage,
    AVG(CASE WHEN gs.winner_id = u.id THEN gs.game_duration_seconds END) as avg_win_duration
FROM users u
LEFT JOIN game_sessions gs ON (gs.player1_id = u.id OR gs.player2_id = u.id)
GROUP BY u.id, u.username;

-- Insert some sample users for testing (passwords are 'password123')
INSERT IGNORE INTO users (username, email, password_hash) VALUES 
('testuser1', 'test1@example.com', '$2a$10$9RgLn3iZJhqk5E6oHKfXaOgBrWu5k6fJGtFb5hCtTmIWcYGpUtHqu'),
('testuser2', 'test2@example.com', '$2a$10$9RgLn3iZJhqk5E6oHKfXaOgBrWu5k6fJGtFb5hCtTmIWcYGpUtHqu'),
('player1', 'player1@example.com', '$2a$10$9RgLn3iZJhqk5E6oHKfXaOgBrWu5k6fJGtFb5hCtTmIWcYGpUtHqu');
