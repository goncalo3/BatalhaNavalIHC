// src/controllers/gameController.ts
import type { Request, Response } from 'express';
import { UserModel } from '../db/models/User';
import { GameSessionModel } from '../db/models/GameSession';
import { DEBUG } from '../config/env';
import * as gameService from '../services/gameService';

/**
 * Get leaderboard
 */
export const getLeaderboard = async (req: Request, res: Response): Promise<void> => {
  try {
    const limit = parseInt(req.query.limit as string) || 10;
    const leaderboard = await UserModel.getLeaderboard(limit);
    
    res.json({ leaderboard });
  } catch (error) {
    if (DEBUG) {
      console.error('Get leaderboard error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Get overall game statistics
 */
export const getGameStats = async (req: Request, res: Response): Promise<void> => {
  try {
    const [dbStats, liveStats] = await Promise.all([
      GameSessionModel.getGameStats(),
      Promise.resolve(gameService.getGameStats()),
    ]);

    res.json({
      database: dbStats,
      live: liveStats,
    });
  } catch (error) {
    if (DEBUG) {
      console.error('Get game stats error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};
