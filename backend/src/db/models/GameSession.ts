// src/db/models/GameSession.ts
import { executeQuery } from '../connection';

export interface GameSession {
  id: number;
  player1_id: number;
  player2_id: number;
  winner_id: number | null;
  loser_id: number | null;
  game_duration_seconds: number | null;
  started_at: Date;
  ended_at: Date | null;
  game_status: 'active' | 'completed' | 'abandoned';
}

export interface CreateGameSessionData {
  player1_id: number;
  player2_id: number;
}

export interface GameResult {
  winner_id: number;
  loser_id: number;
  game_duration_seconds: number;
}

export class GameSessionModel {
  /**
   * Create a new game session
   */
  static async create(gameData: CreateGameSessionData): Promise<GameSession> {
    const query = `
      INSERT INTO game_sessions (player1_id, player2_id, game_status)
      VALUES (?, ?, 'active')
    `;

    const result = await executeQuery<any>(query, [
      gameData.player1_id,
      gameData.player2_id,
    ]);

    const session = await this.findById(result.insertId);
    if (!session) {
      throw new Error('Failed to create game session');
    }
    return session;
  }

  /**
   * Find game session by ID
   */
  static async findById(id: number): Promise<GameSession | null> {
    const query = 'SELECT * FROM game_sessions WHERE id = ?';
    const sessions = await executeQuery<GameSession[]>(query, [id]);
    return sessions[0] || null;
  }

  /**
   * Find active game for a player
   */
  static async findActiveGameForPlayer(playerId: number): Promise<GameSession | null> {
    const query = `
      SELECT * FROM game_sessions 
      WHERE (player1_id = ? OR player2_id = ?) 
      AND game_status = 'active'
      ORDER BY started_at DESC
      LIMIT 1
    `;
    const sessions = await executeQuery<GameSession[]>(query, [playerId, playerId]);
    return sessions[0] || null;
  }

  /**
   * Complete a game session with results
   */
  static async completeGame(
    sessionId: number,
    result: GameResult
  ): Promise<GameSession | null> {
    const query = `
      UPDATE game_sessions 
      SET winner_id = ?, loser_id = ?, game_duration_seconds = ?, 
          ended_at = NOW(), game_status = 'completed'
      WHERE id = ?
    `;

    await executeQuery(query, [
      result.winner_id,
      result.loser_id,
      result.game_duration_seconds,
      sessionId,
    ]);

    return this.findById(sessionId);
  }

  /**
   * Abandon a game session (when player disconnects)
   */
  static async abandonGame(sessionId: number): Promise<GameSession | null> {
    const query = `
      UPDATE game_sessions 
      SET ended_at = NOW(), game_status = 'abandoned'
      WHERE id = ?
    `;

    await executeQuery(query, [sessionId]);
    return this.findById(sessionId);
  }


  /**
   * Get game statistics
   */
  static async getGameStats() {
    const queries = {
      total_games: 'SELECT COUNT(*) as count FROM game_sessions',
      active_games: "SELECT COUNT(*) as count FROM game_sessions WHERE game_status = 'active'",
      completed_games: "SELECT COUNT(*) as count FROM game_sessions WHERE game_status = 'completed'",
      avg_game_duration: `
        SELECT AVG(game_duration_seconds) as avg_duration 
        FROM game_sessions 
        WHERE game_status = 'completed' AND game_duration_seconds IS NOT NULL
      `,
    };

    const results = await Promise.all([
      executeQuery<any[]>(queries.total_games),
      executeQuery<any[]>(queries.active_games),
      executeQuery<any[]>(queries.completed_games),
      executeQuery<any[]>(queries.avg_game_duration),
    ]);

    return {
      total_games: results[0][0].count,
      active_games: results[1][0].count,
      completed_games: results[2][0].count,
      avg_game_duration: results[3][0].avg_duration || 0,
    };
  }
}
