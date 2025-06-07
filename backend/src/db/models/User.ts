// src/db/models/User.ts
import { executeQuery } from '../connection';
import bcrypt from 'bcryptjs';

export interface User {
  id: number;
  username: string;
  email: string;
  password_hash: string;
  created_at: Date;
  updated_at: Date;
}

export interface UserStats {
  id: number;
  username: string;
  wins: number;
  losses: number;
  total_games: number;
  win_percentage: number;
  avg_win_duration: number | null;
}

export interface CreateUserData {
  username: string;
  email: string;
  password: string;
}

export class UserModel {
  /**
   * Create a new user
   */
  static async create(userData: CreateUserData): Promise<User> {
    const saltRounds = 10;
    const password_hash = await bcrypt.hash(userData.password, saltRounds);

    const query = `
      INSERT INTO users (username, email, password_hash)
      VALUES (?, ?, ?)
    `;

    const result = await executeQuery<any>(query, [
      userData.username,
      userData.email,
      password_hash,
    ]);

    const user = await this.findById(result.insertId);
    if (!user) {
      throw new Error('Failed to create user');
    }
    return user;
  }

  /**
   * Find user by ID
   */
  static async findById(id: number): Promise<User | null> {
    const query = 'SELECT * FROM users WHERE id = ?';
    const users = await executeQuery<User[]>(query, [id]);
    return users[0] || null;
  }

  /**
   * Find user by username
   */
  static async findByUsername(username: string): Promise<User | null> {
    const query = 'SELECT * FROM users WHERE username = ?';
    const users = await executeQuery<User[]>(query, [username]);
    return users[0] || null;
  }

  /**
   * Find user by email
   */
  static async findByEmail(email: string): Promise<User | null> {
    const query = 'SELECT * FROM users WHERE email = ?';
    const users = await executeQuery<User[]>(query, [email]);
    return users[0] || null;
  }

  /**
   * Verify user password
   */
  static async verifyPassword(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  /**
   * Get user statistics including wins/losses
   */
  static async getUserStats(userId: number): Promise<UserStats | null> {
    const query = 'SELECT * FROM user_stats WHERE id = ?';
    const stats = await executeQuery<UserStats[]>(query, [userId]);
    return stats[0] || null;
  }

  /**
   * Get leaderboard (top players by wins)
   */
  static async getLeaderboard(limit: number = 10): Promise<UserStats[]> {
    // Ensure limit is a positive integer and convert to string for MySQL compatibility
    const safeLimit = Math.max(1, Math.floor(limit));
    const query = `
      SELECT id, username, wins, losses, total_games, win_percentage, avg_win_duration FROM user_stats 
      WHERE total_games > 0 
      ORDER BY wins DESC, win_percentage DESC 
      LIMIT ${safeLimit}
    `;
    return executeQuery<UserStats[]>(query, []);
  }

  /**
   * Update user profile
   */
  static async updateProfile(
    userId: number,
    updates: Partial<Pick<User, 'username' | 'email'>>
  ): Promise<User | null> {
    const fields = Object.keys(updates);
    const values = Object.values(updates);
    
    if (fields.length === 0) {
      return this.findById(userId);
    }

    const setClause = fields.map(field => `${field} = ?`).join(', ');
    const query = `UPDATE users SET ${setClause} WHERE id = ?`;
    
    await executeQuery(query, [...values, userId]);
    return this.findById(userId);
  }
}
